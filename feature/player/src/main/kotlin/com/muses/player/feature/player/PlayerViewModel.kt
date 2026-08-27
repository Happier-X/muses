package com.muses.player.feature.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.AmllMapper
import com.muses.player.feature.player.lyric.AmllPayload
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.player.lyric.coverUriToAppAssetsUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 播放页 ViewModel：包装 PlayerConnection 的 Flow 并提供位置轮询 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerConnection: PlayerConnection,
    private val songDao: SongDao,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val currentMediaItem = playerConnection.currentMediaItem
    /** 播放失败可观测：限流 429 展示「触发限流，稍后重试」并提供重试入口 */
    val playbackError: StateFlow<String?> = playerConnection.playbackError
    val duration: StateFlow<Long> = playerConnection.duration
    val repeatMode: StateFlow<Int> = playerConnection.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = playerConnection.shuffleModeEnabled
    val queue: StateFlow<List<androidx.media3.common.MediaItem>> = playerConnection.queue

    // 位置轮询（约 500ms 一次）
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    // 是否正在拖拽进度条
    private val _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                if (!_isSeeking.value) {
                    _position.value = playerConnection.currentPosition()
                }
                delay(500)
            }
        }
    }

    fun playPause() = playerConnection.playPause()

    fun clearQueue() = playerConnection.clearQueueItems()

    fun playAtIndex(index: Int) = playerConnection.playAtIndex(index)

    fun removeQueueItemAt(index: Int) = playerConnection.removeQueueItemAt(index)

    fun skipToNext() = playerConnection.skipToNext()

    fun skipToPrevious() = playerConnection.skipToPrevious()

    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

    /** 限流后用户手动重试：清错误并重置恢复链后重播当前曲（无队列上下文则仅清错） */
    fun retryPlayback() {
        val currentId = playerConnection.currentMediaItem.value?.mediaId ?: run {
            playerConnection.clearPlaybackError()
            return
        }
        playerConnection.clearPlaybackError()
        // 重置恢复链 attempted 集合，避免重试后再次失败跳过候选异常
        playerConnection.resetRecovery()
        playerConnection.playAtIndex(playerConnection.queue.value.indexOfFirst { it.mediaId == currentId }.takeIf { it >= 0 } ?: 0)
    }

    fun clearPlaybackError() = playerConnection.clearPlaybackError()

    fun setRepeatMode(mode: Int) = playerConnection.setRepeatMode(mode)

    fun setShuffleModeEnabled(enabled: Boolean) = playerConnection.setShuffleModeEnabled(enabled)

    fun onSeekStart() {
        _isSeeking.value = true
    }

    fun onSeekEnd(positionMs: Long) {
        _isSeeking.value = false
        seekTo(positionMs)
        _position.value = positionMs
        // 歌词进度同步跳转（暂停态下轮询不发，需在此显式更新），钳制语义同轮询
        _lyricPosition.value = positionMs.coerceAtMost(lastLineEndMs)
    }

    // ---------- M2 阶段 1：歌词链路（design.md §3.1/§3.2） ----------

    /** 粘性封面：切歌新曲无 coverUri 时沿用旧值，仅无当前曲才清空（spec 背景契约） */
    private val _stickyCover = MutableStateFlow<String?>(null)
    val stickyCover: StateFlow<String?> = _stickyCover.asStateFlow()

    /** 发给 WebView 的 updateLyrics 载荷 JSON；无词/解析失败也是空行数组载荷（背景照常渲染），仅无当前曲为 null */
    private val _lyricsJson = MutableStateFlow<String?>(null)
    val lyricsJson: StateFlow<String?> = _lyricsJson.asStateFlow()

    /** 当前歌词是否含译文/音译（翻译 FAB 显隐依据，复刻 Web 层语义） */
    private val _hasTranslation = MutableStateFlow(false)
    val hasTranslation: StateFlow<Boolean> = _hasTranslation.asStateFlow()

    /** 已解析 AMLL 行集：info 面板五行歌词小窗数据源（与 lyricsJson 同源同生命周期） */
    private val _parsedLines = MutableStateFlow<List<AmllLyricLine>>(emptyList())
    val parsedLines: StateFlow<List<AmllLyricLine>> = _parsedLines.asStateFlow()

    /** 缓冲中提示位（时间行中央）：Media3 STATE_BUFFERING 直映。
     * 与 Web 层「seek 目标超缓冲区弹 1200ms 提示」语义近似但更简单——原生无 bufferedPosition 上报链路 */
    val isBuffering: StateFlow<Boolean> = playerConnection.playbackState
        .map { it == androidx.media3.common.Player.STATE_BUFFERING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 翻译开关：切换时置空 translated/roman 后重新 toJson 注入（复刻 Web 层 #25 语义） */
    private val _translationEnabled = MutableStateFlow(true)
    val translationEnabled: StateFlow<Boolean> = _translationEnabled.asStateFlow()

    /** 歌词进度：~100ms 节流轮询 + 播完鍗制 min(position, 末句 end)，规避播完全行失活模糊 */
    private val _lyricPosition = MutableStateFlow(0L)
    val lyricPosition: StateFlow<Long> = _lyricPosition.asStateFlow()

    /** 当前曲已映射的 AMLL 行集（翻译开关重建 payload 用）；末句结束时间（ms），无词时 Long.MAX_VALUE 即不钳制 */
    private var currentLines: List<AmllLyricLine> = emptyList()
    private var currentSongId: String? = null
    private var lastLineEndMs: Long = Long.MAX_VALUE

    init {
        startPositionPolling()
        observeCurrentSong()
        startLyricPositionPolling()
    }

    /** 观察当前曲变化 → 反查 Room 取歌词/封面 → 解析映射并发布 payload */
    private fun observeCurrentSong() {
        viewModelScope.launch {
            playerConnection.currentMediaItem
                .map { it?.mediaId }
                .distinctUntilChanged()
                .collect { songId ->
                    currentSongId = songId
                    refreshLyrics(songId)
                }
        }
    }

    private suspend fun refreshLyrics(songId: String?) {
        val song = songId?.let { runCatching { songDao.getById(it) }.getOrNull() }

        // 粘性封面：有新封面即更新；新曲无封面沿用旧值；仅无当前曲才清空
        if (song == null) {
            _stickyCover.value = null
        } else if (!song.coverUri.isNullOrEmpty()) {
            _stickyCover.value = song.coverUri
        }

        // TTML/LRC 解析与映射可能较重（大文件逐词行），移出主线程；结果回主线程赋值，避免跨线程可见性问题
        val synced = withContext(Dispatchers.Default) { LyricsParser.parse(song?.lyrics) }
        currentLines = withContext(Dispatchers.Default) {
            synced?.let { AmllMapper.toAmllLines(it) } ?: emptyList()
        }
        lastLineEndMs = currentLines.maxOfOrNull { it.endTime.toLong() } ?: Long.MAX_VALUE
        _parsedLines.value = currentLines
        _hasTranslation.value = currentLines.any {
            it.translatedLyric.isNotEmpty() || it.romanLyric.isNotEmpty()
        }

        publishPayload(songId)
    }

    /** 按 translationEnabled 重建 payload 并发布（coverUrl 经 file:// → appassets 映射） */
    private fun publishPayload(songId: String?) {
        if (songId.isNullOrEmpty()) {
            _lyricsJson.value = null
            return
        }
        val lines = if (_translationEnabled.value) {
            currentLines
        } else {
            currentLines.map { it.copy(translatedLyric = "", romanLyric = "") }
        }
        val coverUrl = coverUriToAppAssetsUrl(
            uri = _stickyCover.value,
            cacheDirPath = appContext.cacheDir.absolutePath,
        )
        _lyricsJson.value = AmllMapper.toJson(AmllPayload(lines, coverUrl, songId))
    }

    fun toggleTranslation() {
        _translationEnabled.value = !_translationEnabled.value
        publishPayload(currentSongId)
    }

    /** 歌词进度轮询：比 UI 进度条更密的 ~100ms，驱动卡拉OK染色；钳制在末句 endTime 内 */
    private fun startLyricPositionPolling() {
        viewModelScope.launch {
            while (true) {
                if (playerConnection.isPlaying.value && !_isSeeking.value) {
                    _lyricPosition.value = playerConnection.currentPosition().coerceAtMost(lastLineEndMs)
                }
                delay(100)
            }
        }
    }
}

/** 队列页 ViewModel */
@HiltViewModel
class QueueViewModel @Inject constructor(
    val playerConnection: PlayerConnection,
) : ViewModel() {
    val queue: StateFlow<List<androidx.media3.common.MediaItem>> = playerConnection.queue
    val currentMediaItem = playerConnection.currentMediaItem
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
}

package com.muses.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.player.lyric.toAmllLyricLines
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val currentMediaItem = playerConnection.currentMediaItem
    /** 播放失败可观测：限流 429 展示「触发限流，稍后重试」并提供重试入口 */
    val playbackError: StateFlow<String?> = playerConnection.playbackError
    // 时长兜底：播放器未就绪时 duration 为 0，取 DB 的 durationMs 避免冷启动重开进度为 0
    private val _dbDuration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = kotlinx.coroutines.flow.combine(playerConnection.duration, _dbDuration) { playerDur, dbDur ->
        if (playerDur > 0) playerDur else dbDur
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
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

    /** 无参切换：基于最新 StateFlow 值，避免闭包捕获陈旧 repeatMode（沉浸式 WebView 桥接多击竞态） */
    fun toggleRepeat() {
        val cur = repeatMode.value
        val next = if (cur == androidx.media3.common.Player.REPEAT_MODE_ONE) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_ONE
        setRepeatMode(next)
    }

    fun toggleShuffle() {
        setShuffleModeEnabled(!shuffleModeEnabled.value)
    }

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

    /** 已解析 AMLL 行集：五行小窗与完整歌词同源 */
    private val _parsedLines = MutableStateFlow<List<AmllLyricLine>>(emptyList())
    val parsedLines: StateFlow<List<AmllLyricLine>> = _parsedLines.asStateFlow()

    /**  LyricsDocument：供完整版 LyricsPanel 使用 */
    private val _lyricsDocument = MutableStateFlow<LyricsDocument?>(null)
    val lyricsDocument: StateFlow<LyricsDocument?> = _lyricsDocument.asStateFlow()

    /** 当前歌词是否含译文/音译（翻译 FAB 显隐依据） */
    private val _hasTranslation = MutableStateFlow(false)
    val hasTranslation: StateFlow<Boolean> = _hasTranslation.asStateFlow()

    /** 原生前向兼容：保留旧 WebView 的 JSON 载荷（已废弃，空实现供旧测试/归档引用） */
    private val _lyricsJson = MutableStateFlow<String?>(null)
    val lyricsJson: StateFlow<String?> = _lyricsJson.asStateFlow()

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

    /** 观察当前曲变化 → 订阅 Room 实时更新歌词/封面 → 解析映射并发布 payload */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeCurrentSong() {
        viewModelScope.launch {
            playerConnection.currentMediaItem
                .map { it?.mediaId }
                .distinctUntilChanged()
                .flatMapLatest { songId ->
                    currentSongId = songId
                    if (songId == null) flowOf(null)
                    else songDao.observeById(songId)
                }
                .collect { songEntity ->
                    // 兜底封面：扫描未读到内嵌封面时，回退到 Media3 实时 metadata.artworkUri
                    // （对齐 app/NowPlayingUiState mediaMetadata 兜底链路，沉浸页封面缺失修复）
                    val metaArtwork = playerConnection.mediaMetadata.value?.artworkUri?.toString()
                        ?.takeIf { it.isNotBlank() }
                    refreshLyricsWithEntity(songEntity, metaArtwork)
                }
        }
    }

    private suspend fun refreshLyricsWithEntity(
        song: com.muses.player.core.data.db.SongEntity?,
        metadataArtwork: String? = null,
    ) {
        // 时长兜底：DB 时长在播放器未就绪时提供进度分母
        _dbDuration.value = when {
            song == null -> 0L
            song.durationMs > 0 -> song.durationMs
            song.durationSec > 0 -> song.durationSec * 1000
            else -> 0L
        }

        // 粘性封面：有新封面即更新；新曲无 SongEntity 封面 → 沿用 metadata artwork；仅无当前曲才清空
        when {
            song == null -> _stickyCover.value = null
            !song.coverUri.isNullOrEmpty() -> _stickyCover.value = song.coverUri
            !metadataArtwork.isNullOrBlank() -> _stickyCover.value = metadataArtwork
            // 都无：保持旧粘性值（不闪默认底）
        }

        // TTML/LRC 解析与映射可能较重（大文件逐词行），移出主线程；结果回主线程赋值，避免跨线程可见性问题
        val document = withContext(Dispatchers.Default) {
            LyricsParser.parseDocument(song?.lyrics)
        }
        _lyricsDocument.value = document
        currentLines = document?.toAmllLyricLines() ?: emptyList()
        lastLineEndMs = currentLines.maxOfOrNull { it.endTime.toLong() } ?: Long.MAX_VALUE
        _hasTranslation.value = currentLines.any {
            it.translatedLyric.isNotEmpty() || it.romanLyric.isNotEmpty()
        }
        refreshTranslationState()
        if (song?.id == null) {
            _lyricsJson.value = null
        }
    }

    private fun refreshTranslationState() {
        val lines = if (_translationEnabled.value) {
            currentLines
        } else {
            currentLines.map { it.copy(translatedLyric = "", romanLyric = "") }
        }
        _parsedLines.value = lines
        // 兼容旧 WebView JSON：保留但不再用于 UI（coil 直接加载 stickyCover）
        if (currentSongId != null) {
            _lyricsJson.value = "{\"lines\":" + lines.size + "}"
        }
    }

    fun toggleTranslation() {
        _translationEnabled.value = !_translationEnabled.value
        refreshTranslationState()
    }

    /** 歌词进度轮询：比 UI 进度条更密的 ~100ms，驱动卡拉OK染色；钳制在末句 endTime 内
     * 冷启动暂停态也需同步位置，否则重开沉浸页进度为 0（isPlaying=false 时轮询不更新导致） */
    private fun startLyricPositionPolling() {
        viewModelScope.launch {
            // 冷启动立即同步一次，避免暂停态下首帧为 0
            _lyricPosition.value = playerConnection.currentPosition().coerceAtMost(lastLineEndMs)
            while (true) {
                if (!_isSeeking.value) {
                    val pos = playerConnection.currentPosition().coerceAtMost(lastLineEndMs)
                    // 播放态实时更新；暂停态仅在位置变化时更新（避免无谓写入，但保证冷启动后有值）
                    if (playerConnection.isPlaying.value || _lyricPosition.value == 0L || pos != _lyricPosition.value) {
                        _lyricPosition.value = pos
                    }
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

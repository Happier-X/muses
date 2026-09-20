package com.muses.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.LxLyricParser
import com.muses.player.core.model.Song
import com.muses.player.core.model.online.NoOpOnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.model.playback.RepeatMode
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.playback.PlaybackStates
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.player.lyric.toAmllLyricLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 播放队列行（U12：端口只暴露 songId 有序集，展示字段由 VM 按曲库组合） */
data class QueueRow(
    val songId: String,
    val title: String,
    val artist: String?,
)

/**
 * 当前曲的元数据来源。
 *
 * 曲库曲目走 Room；在线曲目**不入库**，只活在 [OnlineTrackSession] 会话表里。
 * 播放页对两者做同一套封面/歌词处理（仅数据来源不同）：
 * 在线曲目的封面/歌词来自洛雪脚本的 `pic` / `lyric` 动作，内存态展示、不写库。
 */
private sealed interface CurrentTrack {
    data class Local(val entity: SongEntity) : CurrentTrack
    data class Online(val song: Song) : CurrentTrack
}

/**
 * 当前曲展示信息（曲库实体 / 在线会话条目归一）。
 *
 * 播放页的标题·艺术家与「是否有歌」判定共用本对象：曲库曲目取 Room，
 * 在线曲目取会话表条目（同様含标题/艺术家），避免播放页只认 Room 而在在线曲目下空白。
 */
data class CurrentSongMeta(
    val songId: String,
    val title: String,
    val artist: String?,
    val isOnline: Boolean,
)

/** 播放页 ViewModel：经 [PlaybackPort] 包装双端播放栈并提供位置轮询 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel constructor(
    val playback: PlaybackPort,
    private val songDao: SongDao,
    /**
     * 在线曲目封面/歌词端口（洛雪脚本 `pic` / `lyric`）。
     * 缺省空实现：宿主未装配在线音源时播放页照常工作，无需判空。
     */
    private val onlineMetadataResolver: OnlineTrackMetadataResolver = NoOpOnlineTrackMetadataResolver,
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playback.isPlaying
    /** 当前曲 id（U12：commonMain 不感知 MediaItem，队列/歌词均以 songId 口径） */
    val currentSongId: StateFlow<String?> = playback.currentSongId
    /** 播放失败可观测：限流 429 展示「触发限流，稍后重试」并提供重试入口 */
    val playbackError: StateFlow<String?> = playback.playbackError
    // 时长兜底：播放器未就绪时 duration 为 0，取 DB 的 durationMs 避免冷启动重开进度为 0
    private val _dbDuration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = combine(playback.duration, _dbDuration) { playerDur, dbDur ->
        if (playerDur > 0) playerDur else dbDur
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** 循环/随机模式：Int 口径保持旧 UI 签名（PlayerControls 参数不变），数值冻结见 [PlaybackStates] */
    val repeatMode: StateFlow<Int> = playback.playerConfig
        .map { if (it.repeatMode == RepeatMode.ONE) PlaybackStates.REPEAT_MODE_ONE else PlaybackStates.REPEAT_MODE_ALL }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackStates.REPEAT_MODE_ALL)
    val shuffleModeEnabled: StateFlow<Boolean> = playback.playerConfig
        .map { it.shuffleEnabled }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 队列展示行（songId → 曲库 title/artist 组合，保持队列顺序） */
    val queueRows: StateFlow<List<QueueRow>> = playback.queueSongIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else songDao.observeByIds(ids).map { list ->
                val byId = list.associateBy { it.id }
                ids.mapNotNull { id -> byId[id]?.let { e -> QueueRow(e.id, e.title, e.artist) } }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前曲实体（曲库实时流；播放页标题/艺术家展示，metaTitle/metaArtist 刮削优先） */
    val currentSong: StateFlow<com.muses.player.core.data.db.SongEntity?> = playback.currentSongId
        .flatMapLatest { songId ->
            if (songId == null) flowOf(null)
            else songDao.observeById(songId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // 位置轮询（约 500ms 一次）
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    // 是否正在拖拽进度条
    private val _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    private fun startPositionPolling() {
        viewModelScope.launch {
            var tick = 0
            // 合并轮询：100ms 一拍，位置每 5 拍更新一次，歌词每拍更新；暂停态降频到 1s
            _position.value = playback.currentPosition()
            _lyricPosition.value = playback.currentPosition().coerceAtMost(lastLineEndMs)
            while (true) {
                val playing = playback.isPlaying.value
                if (!_isSeeking.value) {
                    tick++
                    val pos = playback.currentPosition().coerceAtMost(lastLineEndMs)
                    if (tick % 5 == 0) _position.value = pos
                    if (playing || _lyricPosition.value == 0L || pos != _lyricPosition.value) {
                        _lyricPosition.value = pos
                    }
                }
                delay(if (playing) 100 else 1000)
            }
        }
    }

    fun playPause() = playback.playPause()

    fun clearQueue() = playback.clearQueueItems()

    fun playAtIndex(index: Int) = playback.playAtIndex(index)

    fun removeQueueItemAt(index: Int) = playback.removeQueueItemAt(index)

    fun skipToNext() = playback.skipToNext()

    fun skipToPrevious() = playback.skipToPrevious()

    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)

    /** 限流后用户手动重试：清错误并重置恢复链后重播当前曲（无队列上下文则仅清错） */
    fun retryPlayback() {
        val currentId = playback.currentSongId.value ?: run {
            playback.clearPlaybackError()
            return
        }
        playback.clearPlaybackError()
        // 重置恢复链 attempted 集合，避免重试后再次失败跳过候选异常
        playback.resetRecovery()
        playback.playAtIndex(playback.queueSongIds.value.indexOf(currentId).takeIf { it >= 0 } ?: 0)
    }

    fun clearPlaybackError() = playback.clearPlaybackError()

    /** 无参切换：基于最新 StateFlow 值，避免闭包捕获陈旧 repeatMode（连击竞态） */
    fun toggleRepeat() {
        val cur = playback.playerConfig.value.repeatMode
        playback.setRepeatMode(if (cur == RepeatMode.ONE) RepeatMode.ALL else RepeatMode.ONE)
    }

    fun toggleShuffle() {
        playback.setShuffleEnabled(!playback.playerConfig.value.shuffleEnabled)
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

    /** 当前曲展示信息（标题/艺术家）；同步发布，不等待封面·歌词网络拉取 */
    private val _nowPlayingMeta = MutableStateFlow<CurrentSongMeta?>(null)
    val nowPlayingMeta: StateFlow<CurrentSongMeta?> = _nowPlayingMeta.asStateFlow()

    /** 已解析 AMLL 行集：五行小窗与完整歌词同源 */
    private val _parsedLines = MutableStateFlow<List<AmllLyricLine>>(emptyList())
    val parsedLines: StateFlow<List<AmllLyricLine>> = _parsedLines.asStateFlow()

    /**  LyricsDocument：供完整版 LyricsPanel 使用 */
    private val _lyricsDocument = MutableStateFlow<LyricsDocument?>(null)
    val lyricsDocument: StateFlow<LyricsDocument?> = _lyricsDocument.asStateFlow()

    /** 当前歌词是否含译文/音译（翻译 FAB 显隐依据） */
    private val _hasTranslation = MutableStateFlow(false)
    val hasTranslation: StateFlow<Boolean> = _hasTranslation.asStateFlow()

    /** 缓冲中提示位（时间行中央）：STATE_BUFFERING 直映（状态整型口径冻结，双端同值） */
    val isBuffering: StateFlow<Boolean> = playback.playbackState
        .map { it == PlaybackStates.STATE_BUFFERING }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 翻译开关：切换时置空 translated/roman 后重新 toJson 注入（复刻 Web 层 #25 语义） */
    private val _translationEnabled = MutableStateFlow(true)
    val translationEnabled: StateFlow<Boolean> = _translationEnabled.asStateFlow()

    /** 歌词进度：~100ms 节流轮询 + 播完钳制 min(position, 末句 end)，规避播完全行失活模糊 */
    private val _lyricPosition = MutableStateFlow(0L)
    val lyricPosition: StateFlow<Long> = _lyricPosition.asStateFlow()

    /** 当前曲已映射的 AMLL 行集（翻译开关重建 payload 用）；末句结束时间（ms），无词时 Long.MAX_VALUE 即不钳制 */
    private var currentLines: List<AmllLyricLine> = emptyList()
    private var lastLineEndMs: Long = Long.MAX_VALUE

    init {
        startPositionPolling()
        observeCurrentSong()
    }

    /**
     * 观察当前曲变化 → 组合「曲库实体」与「在线会话条目」为同一份当前曲元数据
     * → 解析封面/歌词并发布给播放页。
     *
     * 在线曲目不入库（见 [OnlineTrackSession]），Room 查不到；`combine` 让迟到的会话登记
     * 也能再推一次值，避免「先观察、后登记」造成封面/歌词永久缺失的竞态。
     */
    private fun observeCurrentSong() {
        viewModelScope.launch {
            playback.currentSongId
                .flatMapLatest { songId -> currentTrackFlow(songId) }
                // collectLatest：切歌时取消上一首尚未完成的封面/歌词拉取
                .collectLatest { track -> refreshCurrentTrack(track) }
        }
    }

    private fun currentTrackFlow(songId: String?): Flow<CurrentTrack?> {
        if (songId == null) return flowOf(null)
        return combine(
            songDao.observeById(songId),
            OnlineTrackSession.observe(songId),
        ) { entity, online ->
            entity?.let { CurrentTrack.Local(it) } ?: online?.let { CurrentTrack.Online(it) }
        }.distinctUntilChanged()
    }

    private suspend fun refreshCurrentTrack(track: CurrentTrack?) {
        // 标题/艺术家先发布：不等封面·歌词的网络拉取，否则在线曲目会先白一下标题
        _nowPlayingMeta.value = when (track) {
            null -> null
            is CurrentTrack.Local -> CurrentSongMeta(
                songId = track.entity.id,
                title = track.entity.title,
                artist = track.entity.artist,
                isOnline = false,
            )
            is CurrentTrack.Online -> CurrentSongMeta(
                songId = track.song.id,
                title = track.song.title,
                artist = track.song.artist,
                isOnline = true,
            )
        }

        // 兜底封面：扫描未读到内嵌封面时，回退到播放器实时 metadata artwork
        // （对齐 app/NowPlayingUiState mediaMetadata 兜底链路，沉浸页封面缺失修复）
        val metadataArtwork = playback.artworkUri.value?.takeIf { it.isNotBlank() }
        when (track) {
            null -> {
                _dbDuration.value = 0L
                _stickyCover.value = null
                applyLyricsDocument(null)
            }
            is CurrentTrack.Local -> refreshLocalTrack(track.entity, metadataArtwork)
            is CurrentTrack.Online -> refreshOnlineTrack(track.song, metadataArtwork)
        }
    }

    /** 曲库曲目：歌词/封面取曲库列（刮削写回链路），metadata artwork 兜底封面 */
    private suspend fun refreshLocalTrack(
        song: SongEntity,
        metadataArtwork: String?,
    ) {
        // 时长兜底：DB 时长在播放器未就绪时提供进度分母
        _dbDuration.value = when {
            song.durationMs > 0 -> song.durationMs
            song.durationSec > 0 -> song.durationSec * 1000
            else -> 0L
        }

        // 粘性封面：有新封面即更新；新曲无 SongEntity 封面 → 沿用 metadata artwork；仅无当前曲才清空
        // 修复：已刮削封面（metaCover 非空）时不回退旧文件封面，避免重刮削后封面被旧 ID3 覆盖
        when {
            !song.coverUri.isNullOrEmpty() -> _stickyCover.value = song.coverUri
            // 刮削标记存在但 coverUri 为空：表示刮削清空封面，不回退旧 metadata，避免复活旧封面
            song.metaCover != null -> _stickyCover.value = null
            !metadataArtwork.isNullOrBlank() -> _stickyCover.value = metadataArtwork
            // 都无：保持旧粘性值（不闪默认底）
        }

        // TTML/LRC 解析与映射可能较重（大文件逐词行），移出主线程；结果回主线程赋值，避免跨线程可见性问题
        val document = withContext(Dispatchers.Default) {
            LyricsParser.parseDocument(song.lyrics)
        }
        applyLyricsDocument(document)
    }

    /**
     * 在线曲目：不落库，封面/歌词走洛雪脚本的 `pic` / `lyric` 动作，**仅在内存态展示**
     * （切歌重新取，不做持久化；与「在线曲目不入库」的整体设计一致）。
     *
     * 封面优先级：脚本 `pic`（数据源权威）→ 搜索结果自带的远程封面（[Song.coverUri]）→ 播放器实时 metadata。
     * 三者都无时保持旧粘性值（与曲库曲目同语义，不闪默认底）。
     *
     * 封面与歌词**串行**拉取：二者共用同一个 QuickJS runtime，脚本 handler 未必并发安全，
     * 串行换取最大兼容性（且脚本未声明动作时端口立即返回 null，无额外开销）。
     */
    private suspend fun refreshOnlineTrack(song: Song, metadataArtwork: String?) {
        _dbDuration.value = song.durationMs

        val ref = OnlineTrackRef.parse(song.path)
        val scriptCover = ref?.let { onlineMetadataResolver.resolveCover(it) }
        val cover = scriptCover?.takeIf { it.isNotBlank() }
            ?: song.coverUri?.takeIf { it.isNotBlank() }
            ?: metadataArtwork
        if (!cover.isNullOrBlank()) _stickyCover.value = cover

        val lyrics = ref?.let { onlineMetadataResolver.resolveLyrics(it) }
        val document = withContext(Dispatchers.Default) {
            lyrics?.let { LxLyricParser.parse(it.lyric, it.tlyric, it.rlyric, it.lxlyric) }
        }
        applyLyricsDocument(document)
    }

    /** 发布歌词文档：同步 AMLL 行集 / 末句结束时间 / 译文标记，并按翻译开关重建 payload */
    private fun applyLyricsDocument(document: LyricsDocument?) {
        _lyricsDocument.value = document
        currentLines = document?.toAmllLyricLines() ?: emptyList()
        lastLineEndMs = currentLines.maxOfOrNull { it.endTime.toLong() } ?: Long.MAX_VALUE
        _hasTranslation.value = currentLines.any {
            it.translatedLyric.isNotEmpty() || it.romanLyric.isNotEmpty()
        }
        refreshTranslationState()
    }

    private fun refreshTranslationState() {
        val lines = if (_translationEnabled.value) {
            currentLines
        } else {
            currentLines.map { it.copy(translatedLyric = "", romanLyric = "") }
        }
        _parsedLines.value = lines
    }

    fun toggleTranslation() {
        _translationEnabled.value = !_translationEnabled.value
        refreshTranslationState()
    }
}

/** 队列页 ViewModel */
class QueueViewModel constructor(
    val playback: PlaybackPort,
) : ViewModel() {
    val queueSongIds: StateFlow<List<String>> = playback.queueSongIds
    val currentSongId: StateFlow<String?> = playback.currentSongId
    val isPlaying: StateFlow<Boolean> = playback.isPlaying
}

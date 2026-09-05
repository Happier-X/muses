package com.muses.player.desktop.playback

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.model.lyrics.OnlineLyricsFailReason
import com.muses.player.core.model.lyrics.OnlineLyricsMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.feature.player.lyric.LyricsParser
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.toAmllLyricLines
import com.muses.player.desktop.DesktopScrapeGraph
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.playback.JvmPlayerPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 播放页歌词态（任务 09-05-desktop-player-lyrics Y2/Y3）。
 *
 * - [lines]：解析后的歌词行（SimpleLyricsPanel 渲染入参）；
 * - [format]：歌词格式（库字段 lyricsFormat / 在线命中 format.wire）；
 * - [source]：歌词来源；[DesktopLyricsState.SOURCE_LIBRARY] 表示库内歌词（刮削写回），
 *   其余为在线命中源 wire 名（amll/kw/tx/wy/kg/mg/lrclib）；null 表示无歌词。
 */
data class DesktopLyricsState(
    val lines: List<AmllLyricLine> = emptyList(),
    val format: String? = null,
    val source: String? = null,
) {
    companion object {
        const val SOURCE_LIBRARY = "library"
    }
}

/** 播放页在线歌词搜索态（Y3）：无库歌词时按钮触发，命中后直接展示（不自动落库） */
sealed interface DesktopLyricsSearchState {
    data object Idle : DesktopLyricsSearchState
    data object Searching : DesktopLyricsSearchState
    data class Failed(val message: String) : DesktopLyricsSearchState
}

/**
 * 桌面播放接线（S3b）：Room 曲库 + JvmPlayerPort + WebDAV 扫描。
 *
 * - 曲库：songDao/sourceDao 直接读库；
 * - 播放：playerPort.enqueue/play；
 * - 扫描：S3b 最小版——按音源 URL 做 PROPFIND 列表并入库（复用 core:webdav Ktor 客户端语义）。
 *   完整扫描（标签解析/增量）随 S5 回归后补，首版只保证建库/扫库/播放链路可用。
 * - 歌词（09-05-desktop-player-lyrics Y2/Y3）：currentSongId 变化 → 读库 lyrics/lyricsFormat
 *   → [LyricsParser.parseDocument] → [lyrics] StateFlow；无库歌词时 [searchOnlineLyrics]
 *   走 DesktopScrapeGraph 的 LyricsMatcher（AMLL+五源+LRCLIB）补充，命中仅内存展示不写库。
 */
class DesktopPlayerHook(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val db: MusesDatabase by lazy { DesktopContainer.database() }
    private val cache: DesktopWebDavAudioCache by lazy { DesktopContainer.audioCache() }

    private var playerPort: JvmPlayerPort? = null

    private val _songs = MutableStateFlow<List<SongEntity>>(emptyList())
    val songs: StateFlow<List<SongEntity>> = _songs.asStateFlow()

    private val _sources = MutableStateFlow<List<SourceEntity>>(emptyList())
    val sources: StateFlow<List<SourceEntity>> = _sources.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongId = MutableStateFlow<String?>(null)
    val currentSongId: StateFlow<String?> = _currentSongId.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _lyrics = MutableStateFlow(DesktopLyricsState())
    val lyrics: StateFlow<DesktopLyricsState> = _lyrics.asStateFlow()

    private val _lyricsSearch = MutableStateFlow<DesktopLyricsSearchState>(DesktopLyricsSearchState.Idle)
    val lyricsSearch: StateFlow<DesktopLyricsSearchState> = _lyricsSearch.asStateFlow()

    /** 曲切换歌词加载 job（切歌取消上一个查询） */
    private var lyricsJob: Job? = null

    /** 在线歌词搜索 job（重复点击/切歌取消上一个查询） */
    private var lyricsSearchJob: Job? = null

    init {
        scope.launch {
            currentSongId.collect { id -> loadLyrics(id) }
        }
    }

    suspend fun ensurePlayer(): JvmPlayerPort {
        playerPort?.let { return it }
        val port = DesktopContainer.playerPort()
        playerPort = port
        // 桥接播放器状态到 UI
        scope.launch { port.isPlaying.collect { _isPlaying.value = it } }
        scope.launch { port.currentSongId.collect { _currentSongId.value = it } }
        scope.launch { port.positionMs.collect { _positionMs.value = it } }
        scope.launch { port.durationMs.collect { _durationMs.value = it } }
        scope.launch { port.volume.collect { _volume.value = it } }
        return port
    }

    fun refresh() {
        scope.launch {
            runCatching {
                _sources.value = db.sourceDao().observeAll().first()
                _songs.value = db.songDao().getAll()
            }.onFailure { e ->
                _status.value = "读取曲库失败：${e.message}"
            }
        }
    }

    fun play(songId: String) {
        scope.launch {
            runCatching {
                val port = ensurePlayer()
                val ids = _songs.value.map { it.id }
                val index = ids.indexOf(songId).coerceAtLeast(0)
                port.enqueue(ids, index)
                port.play()
                _status.value = ""
            }.onFailure { e ->
                _status.value = "播放失败：${e.message}"
            }
        }
    }

    fun togglePlayPause() {
        scope.launch {
            runCatching {
                val port = ensurePlayer()
                if (_isPlaying.value) port.pause() else port.play()
            }.onFailure { e ->
                _status.value = "播放失败：${e.message}"
            }
        }
    }

    fun next() {
        scope.launch { runCatching { ensurePlayer().next() } }
    }

    fun previous() {
        scope.launch { runCatching { ensurePlayer().previous() } }
    }

    fun seekTo(ms: Long) {
        scope.launch { runCatching { ensurePlayer().seekTo(ms) } }
    }

    fun setVolume(volumePercent: Int) {
        scope.launch { runCatching { ensurePlayer().setVolume(volumePercent) } }
    }

    /** Y3：无库歌词时的补充链——LyricsMatcher（AMLL 优先 → 平台五源 → LRCLIB），命中仅内存展示 */
    fun searchOnlineLyrics() {
        val song = _songs.value.firstOrNull { it.id == _currentSongId.value }
        if (song == null) {
            _lyricsSearch.value = DesktopLyricsSearchState.Failed("当前没有正在播放的歌曲")
            return
        }
        lyricsSearchJob?.cancel()
        _lyricsSearch.value = DesktopLyricsSearchState.Searching
        lyricsSearchJob = scope.launch {
            val query = OnlineLyricsQuery(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.albumTitle,
                durationSec = song.durationSec.takeIf { it > 0 }?.toDouble()
                    ?: song.durationMs.takeIf { it > 0 }?.let { it / 1000.0 },
            )
            val next = try {
                when (val result = DesktopScrapeGraph.lyricsMatcher.match(query)) {
                    is OnlineLyricsMatchResult.Ok -> {
                        val document = LyricsParser.parseDocument(result.text)
                        if (document == null || document.lines.isEmpty()) {
                            DesktopLyricsSearchState.Failed("命中歌词解析失败")
                        } else {
                            _lyrics.value = DesktopLyricsState(
                                lines = document.toAmllLyricLines(),
                                format = result.format.wire,
                                source = result.source.wire,
                            )
                            DesktopLyricsSearchState.Idle
                        }
                    }
                    is OnlineLyricsMatchResult.Fail -> DesktopLyricsSearchState.Failed(
                        when (result.reason) {
                            OnlineLyricsFailReason.NETWORK -> "网络异常，请检查网络后重试"
                            OnlineLyricsFailReason.PARSE -> "歌词解析失败"
                            OnlineLyricsFailReason.NO_MATCH -> "未找到匹配歌词"
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DesktopErrorLog.log("DesktopPlayerHook", "在线歌词搜索失败", e)
                DesktopLyricsSearchState.Failed("在线搜索失败：${e.message}")
            }
            if (_lyricsSearch.value is DesktopLyricsSearchState.Searching) {
                _lyricsSearch.value = next
            }
        }
    }

    /** 切歌加载歌词：库字段优先（对齐安卓 song.lyrics 数据链），切歌取消上一个查询 */
    private fun loadLyrics(songId: String?) {
        lyricsJob?.cancel()
        lyricsSearchJob?.cancel()
        _lyricsSearch.value = DesktopLyricsSearchState.Idle
        if (songId.isNullOrBlank()) {
            _lyrics.value = DesktopLyricsState()
            return
        }
        lyricsJob = scope.launch {
            val state = try {
                val song = db.songDao().getById(songId)
                val document = song?.lyrics?.let { LyricsParser.parseDocument(it) }
                DesktopLyricsState(
                    lines = document?.toAmllLyricLines().orEmpty(),
                    format = song?.lyricsFormat,
                    source = if (document != null) DesktopLyricsState.SOURCE_LIBRARY else null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DesktopErrorLog.log("DesktopPlayerHook", "读取歌词失败", e)
                DesktopLyricsState()
            }
            if (_currentSongId.value == songId) {
                _lyrics.value = state
            }
        }
    }
}

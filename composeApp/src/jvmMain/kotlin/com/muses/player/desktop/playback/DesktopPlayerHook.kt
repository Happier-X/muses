package com.muses.player.desktop.playback

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.data.db.SourceEntity
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.playback.JvmPlayerPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面播放接线（S3b）：Room 曲库 + JvmPlayerPort + WebDAV 扫描。
 *
 * - 曲库：songDao/sourceDao 直接读库；
 * - 播放：playerPort.enqueue/play；
 * - 扫描：S3b 最小版——按音源 URL 做 PROPFIND 列表并入库（复用 core:webdav Ktor 客户端语义）。
 *   完整扫描（标签解析/增量）随 S5 回归后补，首版只保证建库/扫库/播放链路可用。
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
}

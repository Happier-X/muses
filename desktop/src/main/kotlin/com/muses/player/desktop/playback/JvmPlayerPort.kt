package com.muses.player.desktop.playback

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.playback.PlayerConfig
import com.muses.player.core.model.playback.RepeatMode
import com.muses.player.core.playback.PlayerPort
import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * S2 桌面播放端口（VLCJ 解码 + 本地队列状态机，实现 commonMain 冻结的 [PlayerPort]）。
 *
 * 解码调用范式（spike.md §4 交接）：
 * - `MediaPlayerFactory("--no-video", "--aout=directsound")`；
 * - `media().play(absolutePath)` 传文件绝对路径，禁止 `File.toURI()`（Windows 畸形 MRL 当 DVD 打开）；
 * - `controls().setTime/pause/play` + 事件 `playing/paused/finished/error/timeChanged/lengthChanged` 桥接 StateFlow。
 *
 * 状态映射：
 * - playing → STATE_READY（播放中）；paused → STATE_READY（暂停，由 [isPlaying] 区分）；
 * - finished → STATE_ENDED（按 repeat 决定重播/下一首）；error → 安全文案 + 失败恢复链。
 *
 * 进度/时长：额外暴露 [positionMs]/[durationMs]/[currentSongId]/[isPlaying] 供 S3 播放页消费；
 * 接口三元组（playbackState/playbackError/playerConfig）保持冻结签名。
 *
 * seek 语义（spike Gate2 结论）：拖动落点为准——[seekTo] 先暂停再 setTime，
 * 若之前在播则落点后恢复播放，消除播放态时钟推进的测量污染。
 *
 * WebDAV：Ktor Range 整文件入 [DesktopWebDavAudioCache]（500MB LRU）后 file:// 播，
 * 不做边播边缓存对等；本地曲目直播绝对路径。
 */
class JvmPlayerPort(
    private val songLookup: suspend (songId: String) -> SongRef?,
    private val sourceLookup: suspend (sourceId: String) -> SourceRef? = { null },
    private val passwordLookup: suspend (sourceId: String) -> String? = { null },
    private val playbackStateRepository: PlaybackStateRepository,
    private val recentPlaysRepository: RecentPlaysRepository,
    private val audioCache: DesktopWebDavAudioCache = DesktopWebDavAudioCache(),
    private val errorLog: (tag: String, msg: String, e: Throwable?) -> Unit = { _, _, _ -> },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val factoryProvider: () -> MediaPlayerFactory = {
        MediaPlayerFactory("--no-video", "--aout=directsound")
    },
) : PlayerPort {

    /** 曲库解析出的播放引用（Song 实体的最小播放子集，避免桌面依赖 :core:data mapper）。 */
    data class SongRef(
        val id: String,
        val sourceId: String,
        val path: String,
        val title: String,
        val artist: String?,
        val album: String?,
        val coverUri: String?,
        val sourceType: SourceType,
    )

    /** 音源引用（WebDAV 基地址 + 登录名；密码经 [passwordLookup] 按需取，不落地）。 */
    data class SourceRef(val id: String, val url: String?, val username: String?)

    // ── PlayerPort 三元组（冻结签名） ──────────────────────

    private val _playbackState = MutableStateFlow(JvmPlaybackStates.STATE_IDLE)
    override val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _playerConfig = MutableStateFlow(PlayerConfig())
    override val playerConfig: StateFlow<PlayerConfig> = _playerConfig.asStateFlow()

    // ── S3 播放页消费的扩展状态 ────────────────────────────

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentSongId = MutableStateFlow<String?>(null)
    val currentSongId: StateFlow<String?> = _currentSongId.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // ── 内部 ───────────────────────────────────────────────

    private val queue = DesktopQueueStateMachine()
    private var repeatMode: RepeatMode = RepeatMode.ALL
    private val attemptedSongIds = LinkedHashSet<String>()
    private var currentRef: SongRef? = null
    private var pausedBySeek = false

    @Volatile private var factory: MediaPlayerFactory? = null
    @Volatile private var player: MediaPlayer? = null

    private var progressJob: Job? = null
    private var persistJob: Job? = null
    private var prepareJob: Job? = null
    private var restoreJob: Job? = null

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    init {
        ensurePlayer()
        scope.launch {
            runCatching {
                val config = playbackStateRepository.readConfig()
                _playerConfig.value = config
                repeatMode = config.repeatMode
                if (config.shuffleEnabled) queue.setShuffleEnabled(true)
            }.onFailure { e ->
                if (e is CancellationException) throw e
                errorLog("JvmPlayerPort", "读取播放配置失败", e)
            }
        }
        restoreJob = scope.launch { restoreFromSnapshot() }
    }

    // ── PlayerPort 实现 ────────────────────────────────────

    override fun play() {
        _playbackError.value = null
        attemptedSongIds.clear()
        val p = player ?: run {
            ensurePlayer()
            player
        } ?: return
        // 从未入队：无事可做（安卓侧同理需先 enqueue）
        if (queue.state().currentSongId == null && currentRef == null) return
        // 暂停恢复：VLCJ 直接 play 即可；ENDED 后 play 重播当前曲
        if (_playbackState.value == JvmPlaybackStates.STATE_ENDED && currentRef != null) {
            startPlayback(currentRef!!, 0L)
            return
        }
        scope.launch { runCatching { p.controls().play() } }
    }

    override fun pause() {
        scope.launch { runCatching { player?.controls()?.pause() } }
    }

    override fun seekTo(ms: Long) {
        val target = ms.coerceAtLeast(0L)
        val p = player ?: return
        val wasPlaying = _isPlaying.value
        scope.launch {
            runCatching {
                // 暂停态落点（spike Gate2 结论）：先暂停消除时钟推进污染，落点后再恢复
                if (wasPlaying) {
                    pausedBySeek = true
                    p.controls().pause()
                    delay(120)
                }
                p.controls().setTime(target)
                _positionMs.value = target
                schedulePersist()
                if (pausedBySeek) {
                    pausedBySeek = false
                    delay(120)
                    p.controls().play()
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                pausedBySeek = false
                errorLog("JvmPlayerPort", "seek 失败", e)
            }
        }
    }

    override fun enqueue(ids: List<String>, index: Int) {
        if (ids.isEmpty()) return
        _playbackError.value = null
        attemptedSongIds.clear()
        queue.enqueue(ids, index, _playerConfig.value.shuffleEnabled)
        val item = queue.state().snapshot.items.getOrNull(queue.state().currentIndex) ?: return
        playSongId(item.songId, 0L)
    }

    override fun setRepeatMode(mode: Int) {
        setRepeatMode(repeatModeFromInt(mode))
    }

    override fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        _playerConfig.value = _playerConfig.value.copy(repeatMode = mode)
        scope.launch {
            runCatching { playbackStateRepository.writeConfig(_playerConfig.value) }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    errorLog("JvmPlayerPort", "写播放配置失败", e)
                }
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        queue.setShuffleEnabled(enabled)
        _playerConfig.value = _playerConfig.value.copy(shuffleEnabled = enabled)
        scope.launch {
            runCatching { playbackStateRepository.writeConfig(_playerConfig.value) }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    errorLog("JvmPlayerPort", "写播放配置失败", e)
                }
            schedulePersist()
        }
    }

    // ── 前台闭环扩展 API（S3 播放页用，非 PlayerPort 冻结签名） ──

    fun next() {
        _playbackError.value = null
        attemptedSongIds.clear()
        val item = queue.step(next = true) ?: return
        playSongId(item.songId, 0L)
    }

    fun previous() {
        _playbackError.value = null
        attemptedSongIds.clear()
        // 进度 >3s 先回到曲首（对齐常见播放器语义），否则上一首
        if ((_positionMs.value) > 3000L && currentRef != null) {
            seekTo(0L)
            return
        }
        val item = queue.step(next = false) ?: return
        playSongId(item.songId, 0L)
    }

    fun setVolume(volumePercent: Int) {
        val v = volumePercent.coerceIn(0, 100)
        _volume.value = v
        scope.launch { runCatching { player?.audio()?.setVolume(v) } }
    }

    fun activeOrderIds(): List<String> = queue.activeOrder().map { it.songId }

    fun currentIndex(): Int = queue.state().currentIndex

    /** 冷启动恢复完成前调用方可用此 Job 等待（S3 首屏/测试用）。 */
    fun restoreJob(): Job? = restoreJob

    fun release() {
        progressJob?.cancel()
        persistJob?.cancel()
        prepareJob?.cancel()
        restoreJob?.cancel()
        runCatching { player?.controls()?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { factory?.release() }
        factory = null
        runCatching { httpClient.close() }
    }

    // ── 播放流水线 ─────────────────────────────────────────

    private fun playSongId(songId: String, startPositionMs: Long) {
        prepareJob?.cancel()
        prepareJob = scope.launch {
            _playbackState.value = JvmPlaybackStates.STATE_BUFFERING
            val ref: SongRef? = try {
                songLookup(songId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorLog("JvmPlayerPort", "查曲库失败 songId=$songId", e)
                null
            }
            if (ref == null) {
                onSongFailed(songId, DesktopPlaybackErrorCopy.FILE_NOT_FOUND)
                return@launch
            }
            val file: File? = try {
                resolvePlayFile(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthFailedException) {
                onSongFailed(songId, DesktopPlaybackErrorCopy.AUTH_FAILED)
                return@launch
            } catch (e: RateLimitedException) {
                // 服务级限流：直接停止等用户手动重试（对齐 RATE_LIMITED_ERROR 语义）
                _playbackError.value = DesktopPlaybackErrorCopy.RATE_LIMITED_ERROR
                _playbackState.value = JvmPlaybackStates.STATE_IDLE
                _isPlaying.value = false
                errorLog("JvmPlayerPort", "WebDAV 限流停止 url=${ref.path}", e)
                return@launch
            } catch (e: Exception) {
                errorLog("JvmPlayerPort", "解析播放文件失败 songId=$songId", e)
                null
            }
            if (file == null || !file.exists() || file.length() <= 0L) {
                onSongFailed(songId, DesktopPlaybackErrorCopy.FILE_NOT_FOUND)
                return@launch
            }
            currentRef = ref
            _currentSongId.value = ref.id
            _durationMs.value = 0L
            _positionMs.value = startPositionMs.coerceAtLeast(0L)
            startPlayback(ref, startPositionMs)
            // 最近播放登记（同曲去重置顶/上限50，对齐 RecentPlaysRepository 语义）
            runCatching {
                val subtitle = listOfNotNull(ref.artist, ref.album).joinToString(" - ")
                recentPlaysRepository.record(
                    com.muses.player.core.model.playback.RecentPlayEntry(
                        songId = ref.id,
                        title = ref.title,
                        subtitle = subtitle,
                        coverUri = ref.coverUri,
                        playedAt = System.currentTimeMillis(),
                    ),
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                errorLog("JvmPlayerPort", "登记最近播放失败", e)
            }
        }
    }

    private fun startPlayback(ref: SongRef, startPositionMs: Long) {
        val p = player ?: run {
            ensurePlayer()
            player
        } ?: run {
            onSongFailed(ref.id, DesktopPlaybackErrorCopy.DEFAULT_ERROR)
            return
        }
        val file = resolveCachedOrLocal(ref)
        if (file == null) {
            onSongFailed(ref.id, DesktopPlaybackErrorCopy.FILE_NOT_FOUND)
            return
        }
        // 绝对路径传法（spike §4 交接）：禁止 File.toURI()，Windows 畸形 MRL 会被当 DVD 打开
        val accepted = runCatching { p.media().play(file.absolutePath) }.getOrDefault(false)
        if (!accepted) {
            onSongFailed(ref.id, DesktopPlaybackErrorCopy.DEFAULT_ERROR)
            return
        }
        if (startPositionMs > 0L) {
            scope.launch {
                delay(600)
                runCatching { p.controls().setTime(startPositionMs) }
            }
        }
        runCatching { p.audio().setVolume(_volume.value) }
    }

    private fun resolveCachedOrLocal(ref: SongRef): File? =
        if (ref.sourceType == SourceType.WEBDAV) audioCache.getCachedFile(ref.path) ?: File(ref.path).takeIf { it.exists() }?.let { fallback ->
            // WebDAV 整文件缓存未命中但本地恰好有同名文件时不做猜测：返回 null 走失败恢复
            if (fallback.isAbsolute && fallback.exists()) fallback else null
        } else File(ref.path).takeIf { it.exists() && it.length() > 0L }

    /** 本地直播 / WebDAV 整文件入缓存（Ktor Range 下载，不做边播边缓存对等）。 */
    private suspend fun resolvePlayFile(ref: SongRef): File = withContext(Dispatchers.IO) {
        if (ref.sourceType != SourceType.WEBDAV) {
            val f = File(ref.path)
            if (!f.exists() || f.length() <= 0L) throw java.io.FileNotFoundException(ref.path)
            return@withContext f
        }
        audioCache.getCachedFile(ref.path)?.let { return@withContext it }
        downloadWebDavToCache(ref)
    }

    private suspend fun downloadWebDavToCache(ref: SongRef): File {
        val source = sourceLookup(ref.sourceId)
        val password: String? = try {
            passwordLookup(ref.sourceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLog("JvmPlayerPort", "取 WebDAV 密码失败", e)
            null
        }
        if (source?.username == null || password == null) {
            throw AuthFailedException("WebDAV 播放缺少认证信息。")
        }
        val credentials = "${source.username}:$password"
        val basic = "Basic " + java.util.Base64.getEncoder()
            .encodeToString(credentials.toByteArray(Charsets.UTF_8))
        val tmp = File.createTempFile("muses-dav-", ".partial")
        try {
            // 整文件下载：优先 Range 分段（网关限流友好），失败回退单次 GET
            val totalSize = probeContentLength(ref.path, basic) ?: -1L
            if (totalSize > 0) {
                downloadRanged(ref.path, basic, tmp, totalSize)
            } else {
                downloadWhole(ref.path, basic, tmp)
            }
            if (!tmp.exists() || tmp.length() <= 0L) throw java.io.IOException("下载为空")
            audioCache.putToCache(ref.path, tmp, null, null)
            return audioCache.getCachedFile(ref.path) ?: throw java.io.IOException("入缓存失败")
        } finally {
            runCatching { if (tmp.exists()) tmp.delete() }
        }
    }

    private suspend fun probeContentLength(url: String, basic: String): Long? {
        return try {
            var length: Long? = null
            httpClient.get(url) {
                header("Authorization", basic)
                header("Range", "bytes=0-0")
            }.let { resp ->
                if (resp.status.value == 401 || resp.status.value == 403) throw AuthFailedException("auth")
                if (resp.status.value == 429) throw RateLimitedException("限流")
                if (resp.status.value == 206) {
                    resp.headers["Content-Range"]?.let { cr ->
                        // 形如 bytes 0-0/12345
                        cr.substringAfterLast('/').toLongOrNull()?.let { length = it }
                    }
                } else if (resp.status.value in 200..299) {
                    resp.headers["Content-Length"]?.toLongOrNull()?.let { length = it }
                }
                runCatching { resp.bodyAsChannel().discard() }
            }
            length
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthFailedException) {
            throw e
        } catch (e: RateLimitedException) {
            throw e
        } catch (e: Exception) {
            errorLog("JvmPlayerPort", "探测长度失败，回退整包下载", e)
            null
        }
    }

    private suspend fun downloadRanged(url: String, basic: String, dest: File, totalSize: Long) {
        val chunk = 512L * 1024L
        RandomAccessFile(dest, "rw").use { raf ->
            raf.setLength(totalSize)
            var offset = 0L
            while (offset < totalSize) {
                val end = minOf(offset + chunk - 1, totalSize - 1)
                httpClient.get(url) {
                    header("Authorization", basic)
                    header("Range", "bytes=$offset-$end")
                }.let { resp ->
                    if (resp.status.value == 401 || resp.status.value == 403) throw AuthFailedException("auth")
                    if (resp.status.value == 429) throw RateLimitedException("限流")
                    if (!resp.status.isSuccess()) throw java.io.IOException("HTTP ${resp.status.value}")
                    val channel = resp.bodyAsChannel()
                    val buf = ByteArray(32 * 1024)
                    raf.seek(offset)
                    while (!channel.isClosedForRead) {
                        val n = channel.readAvailable(buf, 0, buf.size)
                        if (n <= 0) break
                        raf.write(buf, 0, n)
                    }
                }
                offset = end + 1
            }
        }
    }

    private suspend fun downloadWhole(url: String, basic: String, dest: File) {
        httpClient.get(url) {
            header("Authorization", basic)
        }.let { resp ->
            if (resp.status.value == 401 || resp.status.value == 403) throw AuthFailedException("auth")
            if (resp.status.value == 429) throw RateLimitedException("限流")
            if (!resp.status.isSuccess()) throw java.io.IOException("HTTP ${resp.status.value}")
            val channel = resp.bodyAsChannel()
            dest.outputStream().use { out ->
                val buf = ByteArray(32 * 1024)
                while (!channel.isClosedForRead) {
                    val n = channel.readAvailable(buf, 0, buf.size)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
            }
        }
    }

    // ── VLCJ 事件桥接 ──────────────────────────────────────

    private fun ensurePlayer() {
        if (player != null) return
        val f = try {
            factory ?: factoryProvider().also { factory = it }
        } catch (e: Exception) {
            errorLog("JvmPlayerPort", "创建 VLCJ 工厂失败", e)
            _playbackError.value = DesktopPlaybackErrorCopy.DEFAULT_ERROR
            return
        }
        val p = try {
            f.mediaPlayers().newMediaPlayer()
        } catch (e: Exception) {
            errorLog("JvmPlayerPort", "创建 VLCJ 播放器失败", e)
            _playbackError.value = DesktopPlaybackErrorCopy.DEFAULT_ERROR
            return
        }
        p.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                _playbackState.value = JvmPlaybackStates.STATE_READY
                _isPlaying.value = true
                _playbackError.value = null
                startProgressLoop()
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                // 暂停态落点窗口内（seekTo 主动暂停）的 paused 事件不翻转外部状态
                if (pausedBySeek) return
                _playbackState.value = JvmPlaybackStates.STATE_READY
                _isPlaying.value = false
                progressJob?.cancel()
                runCatching { _positionMs.value = mediaPlayer.status().time().coerceAtLeast(0L) }
                schedulePersist()
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
                onFinished()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
                val id = queue.state().currentSongId ?: currentRef?.id
                if (id != null) onSongFailed(id, DesktopPlaybackErrorCopy.NETWORK)
                else {
                    _playbackError.value = DesktopPlaybackErrorCopy.DEFAULT_ERROR
                    _playbackState.value = JvmPlaybackStates.STATE_IDLE
                }
            }

            override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                if (newTime >= 0) _positionMs.value = newTime
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                if (newLength > 0) _durationMs.value = newLength
            }
        })
        runCatching { p.audio().setVolume(_volume.value) }
        player = p
    }

    private fun onFinished() {
        val cur = queue.state()
        val currentId = cur.currentSongId
        if (currentId != null) attemptedSongIds.add(currentId)
        // 单曲循环：重播当前曲
        if (repeatMode == RepeatMode.ONE && currentRef != null) {
            _playbackState.value = JvmPlaybackStates.STATE_READY
            startPlayback(currentRef!!, 0L)
            return
        }
        val order = queue.activeOrder().map { it.songId }
        if (order.isEmpty()) {
            _playbackState.value = JvmPlaybackStates.STATE_ENDED
            schedulePersist()
            return
        }
        val errorIndex = cur.currentIndex
        // 列表循环：播到末尾回绕（step 自带回绕）；单曲队列 + ALL 同样重播
        val nextIndex = if (errorIndex + 1 < order.size) {
            errorIndex + 1
        } else {
            0
        }
        // 全部尝试过（恢复链语义）→ 停止
        if (attemptedSongIds.size >= order.size && order.size > 1) {
            _playbackState.value = JvmPlaybackStates.STATE_ENDED
            attemptedSongIds.clear()
            schedulePersist()
            return
        }
        val item = queue.moveTo(nextIndex)
        if (item == null) {
            _playbackState.value = JvmPlaybackStates.STATE_ENDED
            return
        }
        playSongId(item.songId, 0L)
    }

    /** 单曲失败：登记 attempted → 沿 active order 回绕一次找候选 → 无候选才停止。 */
    private fun onSongFailed(songId: String, copy: String) {
        attemptedSongIds.add(songId)
        val order = queue.activeOrder().map { it.songId }
        val errorIndex = order.indexOf(songId)
        val nextIndex = queue.selectNextCandidate(order, errorIndex, attemptedSongIds)
        if (nextIndex == null) {
            _playbackError.value = DesktopPlaybackErrorCopy.safeCopy(copy)
            _playbackState.value = JvmPlaybackStates.STATE_IDLE
            _isPlaying.value = false
            errorLog("JvmPlayerPort", "播放失败无候选 songId=$songId", null)
            return
        }
        val item = queue.moveTo(nextIndex)
        if (item == null) {
            _playbackError.value = DesktopPlaybackErrorCopy.safeCopy(copy)
            _playbackState.value = JvmPlaybackStates.STATE_IDLE
            return
        }
        errorLog("JvmPlayerPort", "跳过失败曲 songId=$songId -> ${item.songId}", null)
        playSongId(item.songId, 0L)
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            val p = player ?: return@launch
            while (true) {
                delay(500)
                runCatching {
                    val t = p.status().time()
                    if (t >= 0) _positionMs.value = t
                    val len = p.status().length()
                    if (len > 0) _durationMs.value = len
                }
            }
        }
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(500)
            runCatching { persistSnapshotNow() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    errorLog("JvmPlayerPort", "持久化快照失败", e)
                }
        }
    }

    private suspend fun persistSnapshotNow() {
        val cur = queue.state()
        playbackStateRepository.writeSnapshot(
            PlaybackStateRepository.PlaybackSnapshot(
                items = cur.snapshot.items,
                originalOrder = cur.snapshot.originalOrder,
                shuffleOrder = cur.snapshot.shuffleOrder,
                currentIndex = cur.currentIndex,
                positionMs = _positionMs.value.coerceAtLeast(0L),
                currentSongId = cur.currentSongId,
            ),
        )
    }

    private suspend fun restoreFromSnapshot() {
        val snapshot = try {
            playbackStateRepository.readSnapshot() ?: return
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorLog("JvmPlayerPort", "读快照失败", e)
            return
        }
        if (snapshot.items.isEmpty()) return
        // 已被曲库删除的歌曲自然过滤（对齐安卓侧 restoreFromSnapshot）
        val resolvedIds = mutableListOf<String>()
        var resolvedCurrentId = snapshot.currentSongId
        for (item in snapshot.items) {
            val exists = try {
                songLookup(item.songId) != null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (exists) resolvedIds.add(item.songId)
        }
        if (resolvedIds.isEmpty()) return
        if (resolvedCurrentId != null && resolvedCurrentId !in resolvedIds) resolvedCurrentId = null
        val items = resolvedIds.map { com.muses.player.core.model.playback.QueueItem(it) }
        val original = snapshot.originalOrder
            .filter { o -> o.songId in resolvedIds }
            .takeIf { it.isNotEmpty() } ?: items
        val shuffled = snapshot.shuffleOrder?.filter { o -> o.songId in resolvedIds }
        val startIndex = resolvedCurrentId?.let { id -> items.indexOfFirst { it.songId == id } }
            ?.takeIf { it >= 0 } ?: 0
        queue.restore(items, original, shuffled, startIndex, resolvedCurrentId ?: items[startIndex].songId)
        currentRef = try {
            songLookup(items[startIndex].songId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        _currentSongId.value = currentRef?.id
        _positionMs.value = snapshot.positionMs.coerceAtLeast(0L)
        _playbackState.value = JvmPlaybackStates.STATE_IDLE
        // 只恢复不自动播放（对齐安卓侧 playWhenReady=false）
    }

    // ── 二期预留（托盘/SMTC/音频焦点，D2 决策：空实现 + TODO） ──

    /** TODO(二期): 系统托盘（最小化到托盘 + 托盘菜单播放控制）。 */
    fun setTrayVisible(visible: Boolean) {
    }

    /** TODO(二期): Windows SMTC（系统媒体传输控制：任务栏/音量面板展示曲名+封面+按键）。 */
    fun updateSystemMediaTransport(info: String?) {
    }

    /** TODO(二期): 音频焦点（来电/它应用抢占时暂停让路，焦点回归后恢复）。 */
    fun requestAudioFocus(): Boolean = true

    /** TODO(二期): 音频焦点释放。 */
    fun abandonAudioFocus() {
    }

    private class AuthFailedException(message: String) : Exception(message)
    private class RateLimitedException(message: String) : Exception(message)

    companion object {
        /**
         * 默认装配：Room 单例（`<appDataDir>/muses.db`）+ DataStore 持久化 + JVM 缓存/日志目录。
         *
         * - DB：[createJvmDatabase] 单例（S1 底座，不在此重复建库）；
         * - DataStore：[createDataStore] 真实路径（S1 底座）；
         * - 缓存：[PlatformDirs.cacheDir] okio spiller 语义由 [DesktopWebDavAudioCache] 承载；
         * - 崩溃日志：[PlatformDirs.errorLogDir]/crash-latest.txt（见 [DesktopErrorLog]）。
         *
         * 调用方只需供给曲库/音源/密码三查（S3 接线 :core:data 的 SongDao/SourceDao/CredentialsRepository）。
         */
        fun createDefault(
            db: MusesDatabase,
            songLookup: suspend (songId: String) -> SongRef?,
            sourceLookup: suspend (sourceId: String) -> SourceRef? = { null },
            passwordLookup: suspend (sourceId: String) -> String? = { null },
            audioCache: DesktopWebDavAudioCache = DesktopWebDavAudioCache(),
            errorLog: (tag: String, msg: String, e: Throwable?) -> Unit = { tag, msg, e ->
                DesktopErrorLog.log(tag, msg, e)
            },
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ): JvmPlayerPort {
            val dataStore = createDataStore()
            return JvmPlayerPort(
                songLookup = songLookup,
                sourceLookup = sourceLookup,
                passwordLookup = passwordLookup,
                playbackStateRepository = PlaybackStateRepository(dataStore),
                recentPlaysRepository = RecentPlaysRepository(dataStore),
                audioCache = audioCache,
                errorLog = errorLog,
                scope = scope,
            )
        }
    }
}

/** Ktor 响应通道丢弃（探测请求体无人消费时排空连接复用）。 */
private suspend fun io.ktor.utils.io.ByteReadChannel.discard() {
    val buf = ByteArray(8 * 1024)
    while (!isClosedForRead) {
        if (readAvailable(buf, 0, buf.size) <= 0) break
    }
}

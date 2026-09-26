package com.muses.player.desktop.playback

import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.PlayStatsRepository
import com.muses.player.core.data.repository.PlayStatsSessionTracker
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.store.createDataStore
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackResolver
import com.muses.player.core.model.playback.PlayerConfig
import com.muses.player.core.model.playback.RepeatMode
import com.muses.player.core.media.scanner.PlaybackLazyScan
import com.muses.player.core.playback.PlaybackMeta
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
    private val songsExist: (suspend (ids: List<String>) -> Set<String>)? = null,
    // 批量取引用：恢复时一次取回当前曲全量，避免过滤后再单查
    private val songsLookup: (suspend (ids: List<String>) -> Map<String, SongRef>)? = null,
    private val sourceLookup: suspend (sourceId: String) -> SourceRef? = { null },
    private val passwordLookup: suspend (sourceId: String) -> String? = { null },
    private val playbackStateRepository: PlaybackStateRepository,
    private val recentPlaysRepository: RecentPlaysRepository,
    /**
     * 听歌统计埋点内核（可选：测试直构造时可不传，播放行为不受影响）。
     * 装配见 [createDefault]，与 Koin 侧共用同一 DataStore，统计快照双端同源。
     */
    private val playStatsTracker: PlayStatsSessionTracker? = null,
    private val audioCache: DesktopWebDavAudioCache = DesktopWebDavAudioCache(),
    // 默认接桌面日志，避免直构造时静默丢日志；createDefault 同口径
    private val errorLog: (tag: String, msg: String, e: Throwable?) -> Unit = { tag, msg, e ->
        DesktopErrorLog.log(tag, msg, e)
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val factoryProvider: () -> MediaPlayerFactory = { defaultFactory() },
    /**
     * U26 播放懒扫描钩子：startPlayback 接受播放后触发（songId + 已落盘本地文件）。
     * 调用方负责读标签 + 回写库，并回传本次读到的文件标签快照（null = 读取失败/无需处理）；
     * 本端口据此发布 [currentMeta]，对齐安卓 ExoPlayer 解析内嵌标签后经 MediaMetadata
     * 回流的展示口径（未刮削歌曲重播时用上文件侧更新数据）。失败不阻塞播放。
     */
    private val onPlaybackStarted: (suspend (songId: String, localFile: java.io.File) -> PlaybackLazyScan.FileTags?)? = null,
    /**
     * 在线音源直链解析器（洛雪自定义源脚本）。
     * null = 当前构建未启用在线音源（播放在线曲目时报错提示，不崩）。
     */
    private val onlineResolver: OnlineTrackResolver? = null,
) : PlayerPort {

    /** 播放目标：本地文件（LOCAL/WebDAV 缓存）或远程直链（在线音源） */
    private sealed interface PlayTarget {
        data class LocalFile(val file: File) : PlayTarget
        data class RemoteUrl(val url: String) : PlayTarget
    }

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

    /**
     * 当前曲文件实时标签（对齐安卓 Media3 mediaMetadata 直映：VLCJ 不解析内嵌标签，
     * 由懒扫描钩子回传的 [PlaybackLazyScan.FileTags] 映射；上层 mergeNowPlaying 据此
     * 在未刮削歌曲上优先文件侧数据。切歌先清 null，避免上一首残留串台。
     */
    private val _currentMeta = MutableStateFlow<PlaybackMeta?>(null)
    val currentMeta: StateFlow<PlaybackMeta?> = _currentMeta.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // ── 内部 ───────────────────────────────────────────────

    private val queue = DesktopQueueStateMachine()
    private var repeatMode: RepeatMode = RepeatMode.ALL
    private val attemptedSongIds = LinkedHashSet<String>()
    private var currentRef: SongRef? = null
    private var lastPinnedUrl: String? = null
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
        // 听歌统计：播放中按节拍结算已播时长（与安卓 PlaybackService 同口径）
        playStatsTracker?.let { tracker ->
            scope.launch {
                while (true) {
                    delay(PlayStatsSessionTracker.TICK_MS)
                    tracker.checkpoint()
                }
            }
        }
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
            replayCurrent(currentRef!!)
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

    /** U16：按队列位置移除条目（复用状态机 removeSongs；队列内 songId 唯一，按 id 移除等价按 index） */
    fun removeQueueItemAt(index: Int) {
        val songId = queue.activeOrder().getOrNull(index)?.songId ?: return
        queue.removeSongs(setOf(songId))
    }

    /** U20：按 songId 集合清理队列（删音源同步清队列；复用状态机 removeSongs） */
    fun removeFromQueue(songIds: Set<String>) {
        if (songIds.isEmpty()) return
        queue.removeSongs(songIds)
    }

    /** U16：清空播放队列（当前曲播放不受影响，队列态清空） */
    fun clearQueueItems() {
        queue.removeSongs(queue.activeOrder().map { it.songId }.toSet())
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
        lastPinnedUrl?.let { audioCache.release(it) }
        lastPinnedUrl = null
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
            val target: PlayTarget? = try {
                resolvePlayTarget(ref)
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
                errorLog("JvmPlayerPort", "解析播放目标失败 songId=$songId", e)
                null
            }
            if (target == null) {
                onSongFailed(
                    songId,
                    if (ref.sourceType == SourceType.ONLINE) {
                        onlineUnavailableCopy()
                    } else {
                        DesktopPlaybackErrorCopy.FILE_NOT_FOUND
                    },
                )
                return@launch
            }
            currentRef = ref
            // 钉住播放中缓存文件，淘汰跳过；释放上一个（仅 WebDAV 走缓存）
            if (ref.sourceType == SourceType.WEBDAV) {
                audioCache.acquire(ref.path)
                lastPinnedUrl?.takeIf { it != ref.path }?.let { audioCache.release(it) }
                lastPinnedUrl = ref.path
            }
            _currentSongId.value = ref.id
            _currentMeta.value = null
            _durationMs.value = 0L
            _positionMs.value = startPositionMs.coerceAtLeast(0L)
            startPlayback(ref, target, startPositionMs)
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
            // 听歌统计：切歌即登记一次播放并结算上一段时长（VLCJ 起播前 isPlaying 还是旧态，
            // 以本刻取值起段，随后的 playing/paused 事件会再校正）
            playStatsTracker?.let { tracker ->
                scope.launch {
                    tracker.onSongStarted(
                        songId = ref.id,
                        title = ref.title,
                        subtitle = listOfNotNull(ref.artist, ref.album).joinToString(" - "),
                        coverUri = ref.coverUri,
                        isPlaying = _isPlaying.value,
                    )
                }
            }
        }
    }

    private fun startPlayback(ref: SongRef, target: PlayTarget, startPositionMs: Long) {
        val p = player ?: run {
            ensurePlayer()
            player
        } ?: run {
            onSongFailed(ref.id, DesktopPlaybackErrorCopy.DEFAULT_ERROR)
            return
        }
        // 绝对路径传法（spike §4 交接）：禁止 File.toURI()，Windows 畸形 MRL 会被当 DVD 打开
        // 在线音源直接传 HTTP 直链（VLCJ 原生支持网络 MRL）
        val mrl = when (target) {
            is PlayTarget.LocalFile -> target.file.absolutePath
            is PlayTarget.RemoteUrl -> target.url
        }
        val accepted = runCatching { p.media().play(mrl) }.getOrDefault(false)
        if (!accepted) {
            onSongFailed(ref.id, DesktopPlaybackErrorCopy.DEFAULT_ERROR)
            return
        }
        // U26 播放懒扫描：仅本地文件适用（在线曲目无本地文件可读）；
        // 播起来了才触发（读标签 + 回写库由调用方承载，异常内部消化不阻塞播放；
        // 回传的文件标签快照同步发布 currentMeta，对齐安卓 ExoPlayer 内嵌标签解析后
        // onMediaMetadataChanged 回流的展示口径——未刮削歌曲重播时用上文件侧更新数据）
        val localFile = (target as? PlayTarget.LocalFile)?.file
        if (localFile != null) onPlaybackStarted?.let { hook ->
            val songId = ref.id
            scope.launch {
                val tags = runCatching { hook(songId, localFile) }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        errorLog("JvmPlayerPort", "懒扫描钩子失败 songId=$songId", e)
                    }
                    .getOrNull()
                if (tags != null) {
                    _currentMeta.value = PlaybackMeta(
                        title = tags.title,
                        artist = tags.artist,
                        album = tags.album,
                        coverUri = tags.coverUri,
                    )
                }
            }
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

    /**
     * 解析播放目标：
     * - ONLINE：调在线音源解析器异步换取 HTTP 直链（不做长期缓存——直链会过期，
     *   每次播放都重新解析，避免用过期的缓存地址播放）；
     * - 其余源：走既有本地/WebDAV 文件链路。
     *
     * 返回 null 表示不可播（在线未启用/引用损坏/文件不存在），由调用方归入失败链。
     */
    private suspend fun resolvePlayTarget(ref: SongRef): PlayTarget? {
        if (ref.sourceType == SourceType.ONLINE) {
            val resolver = onlineResolver ?: return null
            val onlineRef = OnlineTrackRef.parse(ref.path) ?: return null
            val resolved = resolver.resolve(onlineRef)
            return PlayTarget.RemoteUrl(resolved.url)
        }
        val file = resolvePlayFile(ref)
        return if (file.exists() && file.length() > 0L) PlayTarget.LocalFile(file) else null
    }

    /** 重播当前曲：重新解析播放目标（在线直链会过期，必须重新换取） */
    private fun replayCurrent(ref: SongRef) {
        scope.launch {
            val target = try {
                resolvePlayTarget(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorLog("JvmPlayerPort", "重播解析失败 songId=${ref.id}", e)
                null
            }
            if (target == null) {
                onSongFailed(
                    ref.id,
                    if (ref.sourceType == SourceType.ONLINE) {
                        onlineUnavailableCopy()
                    } else {
                        DesktopPlaybackErrorCopy.FILE_NOT_FOUND
                    },
                )
                return@launch
            }
            startPlayback(ref, target, 0L)
        }
    }

    /** 本地直播 / WebDAV 整文件入缓存（Ktor Range 下载，不做边播边缓存对等）。 */
    private suspend fun resolvePlayFile(ref: SongRef): File = withContext(Dispatchers.IO) {
        // 在线曲目不走文件链路（由 resolvePlayTarget 接管）；此处防御性拦截
        if (ref.sourceType == SourceType.ONLINE) {
            throw java.io.IOException("在线曲目需经 resolvePlayTarget 解析直链")
        }
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

    /**
     * 工厂创建失败收敛：链接期失败（原生库/类缺失）或解析不到 VLC 目录时给安装/配置指引，
     * 其余走通用文案；两种情况都写崩溃日志（不再静默）。
     */
    private fun reportFactoryFailure(e: Throwable) {
        val nativeMissing = e is LinkageError || resolveVlcDir() == null
        val hint = if (nativeMissing) {
            DesktopPlaybackErrorCopy.VLC_MISSING
        } else {
            DesktopPlaybackErrorCopy.DEFAULT_ERROR
        }
        errorLog("JvmPlayerPort", "创建 VLCJ 工厂失败", e)
        _playbackError.value = DesktopPlaybackErrorCopy.safeCopy(hint)
    }

    private fun ensurePlayer() {
        if (player != null) return
        val f = try {
            factory ?: factoryProvider().also { factory = it }
        } catch (e: Exception) {
            reportFactoryFailure(e)
            return
        } catch (e: LinkageError) {
            // VLC 原生库缺失时 VLCJ 抛的是 UnsatisfiedLinkError / NoClassDefFoundError（Error 系，
            // 实测栈顶 MediaPlayerFactory.discoverNativeLibrary → LibVlc.<clinit>）：
            // 原 catch(Exception) 接不住，会把崩溃直接抛给 UI
            reportFactoryFailure(e)
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
                reportStats(isPlaying = true)
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                // 暂停态落点窗口内（seekTo 主动暂停）的 paused 事件不翻转外部状态
                if (pausedBySeek) return
                _playbackState.value = JvmPlaybackStates.STATE_READY
                _isPlaying.value = false
                progressJob?.cancel()
                runCatching { _positionMs.value = mediaPlayer.status().time().coerceAtLeast(0L) }
                schedulePersist()
                reportStats(isPlaying = false)
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
                reportStats(isPlaying = false)
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
                reportStats(isPlaying = false)
                onFinished()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                _isPlaying.value = false
                progressJob?.cancel()
                reportStats(isPlaying = false)
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
            replayCurrent(currentRef!!)
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
    /**
     * 在线曲目不可播的失败文案：未接入解析器（构建未启用在线音源）与解析失败
     * （脚本未加载/源不支持/直链过期/引用损坏）病因不同、指引不同，分别给文案；
     * 两条均在 [DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS] 内，可原样落到 UI
     * （否则会被 onSongFailed 里的 safeCopy 静默兜底成通用文案）。
     */
    private fun onlineUnavailableCopy(): String = onlineUnavailableCopyFor(onlineResolver != null)

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

    /**
     * 听歌统计状态上报（VLCJ 事件线程调用，写盘投递到 [scope]）。
     *
     * 当前曲优先取队列状态机，回退最近一次解析出的引用（播放结束/失败时队列可能已空）。
     */
    private fun reportStats(isPlaying: Boolean) {
        val tracker = playStatsTracker ?: return
        val songId = queue.state().currentSongId ?: currentRef?.id
        scope.launch { tracker.onPlaybackState(songId, isPlaying) }
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
        // 批量存在性：一次查全量替代 N 次逐条（无批量口径时回退逐条）
        val resolvedIds = mutableListOf<String>()
        var resolvedCurrentId = snapshot.currentSongId
        val batch = songsExist?.let { fn ->
            runCatching {
                snapshot.items.map { it.songId }.chunked(900).flatMap { fn(it) }
            }.getOrNull()?.toSet()
        }
        if (batch != null) {
            val order = snapshot.items.map { it.songId }
            resolvedIds.addAll(order.filter { it in batch })
        } else {
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
        // 当前曲引用优先批量口径（已过滤存在性），失败回退单查
        currentRef = try {
            val wanted = items[startIndex].songId
            songsLookup?.let { fn ->
                runCatching { fn(listOf(wanted))[wanted] }.getOrNull()
            } ?: songLookup(wanted)
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

    /**
     * Windows SMTC 已由 composeApp Main.kt 装配的 [com.muses.player.desktop.smtc.SmtcController]
     * 承载（直连 StateFlow + lambda 注入，不经过本端口）；本预留方法保留签名不再接线。
     */
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
        /** 随包内置 VLC 的目录名（位于 jpackage 的 app/resources 下，产物见 scripts/vlc-trim.ps1）。 */
        private const val BUNDLED_VLC_DIR = "vlc"

        /** 仓库开发期便携版（.gitignore 已忽略，仅本地/CI 下载后存在）。 */
        private const val REPO_PORTABLE_VLC = "spike-vlcj/vlc-portable/vlc-3.0.21"

        /** VLC 安装器写入注册表的键（64 位视图 + WOW6432Node 32 位视图）。 */
        private val VLC_REGISTRY_KEYS = listOf(
            "SOFTWARE\\VideoLAN\\VLC",
            "SOFTWARE\\WOW6432Node\\VideoLAN\\VLC",
        )

        private fun isWindows(): Boolean =
            System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

        /** 目录是否可直接喂给 JNA：含 VLC 原生库。本桌面端只出 Windows 包，按 libvlc.dll 判定。 */
        private fun hasLibVlc(dir: File): Boolean = File(dir, "libvlc.dll").isFile

        /**
         * VLC 原生库目录解析（优先级从高到低）：
         * 1. `MUSES_VLC_DIR` 环境变量 / `muses.vlc.dir` 系统属性（含 libvlc.dll 的目录）；
         * 2. 随包内置 `<app resources>/vlc`（发行版内置裁剪后的纯音频 VLC，免装 VLC 桌面版）；
         * 3. 仓库便携版 `spike-vlcj/vlc-portable/vlc-3.0.21`（开发期免安装）；
         * 4. Windows 已安装的 VLC 桌面版（注册表 InstallDir / 约定安装路径 / PATH 上的 vlc.exe）；
         * 5. null = 交给 VLCJ/JNA 按系统路径自行发现。
         */
        fun resolveVlcDir(): File? = vlcDirCandidates().firstOrNull(::hasLibVlc)

        /** 候选目录（按优先级枚举，不做存在性校验）：供 [resolveVlcDir] 与单测共用。 */
        internal fun vlcDirCandidates(): List<File> = buildList {
            explicitVlcDir()?.let(::add)
            bundledVlcDir()?.let(::add)
            repoPortableVlcDir()?.let(::add)
            addAll(installedVlcDirs())
        }

        /** 显式配置：`MUSES_VLC_DIR` 环境变量优先，其次 `muses.vlc.dir` 系统属性。 */
        private fun explicitVlcDir(): File? =
            (System.getenv("MUSES_VLC_DIR")?.takeIf { it.isNotBlank() }
                ?: System.getProperty("muses.vlc.dir")?.takeIf { it.isNotBlank() })
                ?.let(::File)

        /**
         * 随包内置：jpackage 把 `appResourcesRootDir` 落到 app/resources，
         * Compose Multiplatform 运行期经该属性暴露其绝对路径（开发态亦有值，但无 vlc 子目录）。
         */
        private fun bundledVlcDir(): File? =
            System.getProperty("compose.application.resources.dir")
                ?.takeIf { it.isNotBlank() }
                ?.let { File(it, BUNDLED_VLC_DIR) }

        /** 仓库便携版：gradle run 的 cwd 即仓库根；向上 6 层兼容从子模块目录启动。 */
        private fun repoPortableVlcDir(): File? {
            var cursor: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
            repeat(6) {
                val candidate = cursor?.let { File(it, REPO_PORTABLE_VLC) }
                if (candidate != null && hasLibVlc(candidate)) return candidate
                cursor = cursor?.parentFile
            }
            return null
        }

        /**
         * 系统已安装的 VLC 桌面版候选目录：
         * 注册表 InstallDir（非标准安装路径也能命中）→ 约定安装路径（含用户级安装）→ PATH。
         * 非 Windows 返回空：由 VLCJ 自带的 NativeDiscovery 负责其它平台。
         */
        private fun installedVlcDirs(): List<File> {
            if (!isWindows()) return emptyList()
            return buildList {
                registryVlcInstallDir()?.let { add(File(it)) }
                System.getenv("ProgramFiles")?.takeIf { it.isNotBlank() }
                    ?.let { add(File(it, "VideoLAN/VLC")) }
                System.getenv("ProgramFiles(x86)")?.takeIf { it.isNotBlank() }
                    ?.let { add(File(it, "VideoLAN/VLC")) }
                // 用户级安装（无管理员权限时装到 %LOCALAPPDATA%\Programs）
                System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
                    ?.let { add(File(it, "Programs/VideoLAN/VLC")) }
                System.getenv("PATH")?.split(File.pathSeparatorChar)?.forEach { entry ->
                    if (entry.isNotBlank() && File(entry, "vlc.exe").isFile) add(File(entry))
                }
            }
        }

        /**
         * 读注册表 `InstallDir`。JNA 的 Advapi32/WinReg 只在 Windows 可用，
         * 故先过 [isWindows] 再触碰，并用 runCatching 吃掉无权限/键缺失。
         */
        private fun registryVlcInstallDir(): String? {
            if (!isWindows()) return null
            return runCatching {
                val roots = listOf(
                    com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE,
                    com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER,
                )
                VLC_REGISTRY_KEYS.firstNotNullOfOrNull { key ->
                    roots.firstNotNullOfOrNull { root ->
                        runCatching {
                            com.sun.jna.platform.win32.Advapi32Util
                                .registryGetStringValue(root, key, "InstallDir")
                        }.getOrNull()
                    }
                }
            }.getOrNull()
        }

        /**
         * 在线曲目不可播时的文案选择：[resolverAvailable] = 是否接入了在线音源解析器。
         * 未接入（构建未启用在线音源）与解析失败（脚本未加载/源不支持/直链过期/引用损坏）
         * 病因不同、指引不同（去启用音源 vs 去检查脚本），故分给两条文案。
         * 纯函数放 companion 以便单测锁定映射（实例侧经 [onlineUnavailableCopy] 传入解析器状态）。
         */
        internal fun onlineUnavailableCopyFor(resolverAvailable: Boolean): String =
            if (resolverAvailable) {
                DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED
            } else {
                DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING
            }

        /**
         * 默认 VLCJ 工厂：先解析原生库目录并设 `jna.library.path`（VLCJ 经 JNA 加载 libvlc），
         * 再建 `--no-video --aout=directsound` 工厂。
         *
         * JNA 每次 `loadLibrary` 都会重读该属性（NativeLibrary 内部 initPaths），
         * 故即使 SMTC/DPAPI 已先行初始化 JNA，此处设置依然生效（已实测）。
         * 解析不到目录时不设属性，交由 VLCJ 自带 NativeDiscovery 兜底。
         */
        fun defaultFactory(): MediaPlayerFactory {
            val vlcDir = resolveVlcDir()
            if (vlcDir != null) {
                System.setProperty("jna.library.path", vlcDir.absolutePath)
            } else {
                System.clearProperty("jna.library.path")
            }
            return MediaPlayerFactory("--no-video", "--aout=directsound")
        }

        /**
         * 默认装配：Room 单例（`<appDataDir>/muses.db`）+ DataStore 持久化 + JVM 缓存/日志目录。
         *
         * - DB：[createJvmDatabase] 单例（S1 底座，不在此重复建库）；
         * - DataStore：调用方供给单例（同文件多实例会抛 multiple DataStores active；
         *   桌面统一用 DesktopContainer.settingsStore，与凭据/设置/播放状态/最近播放共享）；
         * - 缓存：[PlatformDirs.cacheDir] okio spiller 语义由 [DesktopWebDavAudioCache] 承载；
         * - 崩溃日志：[PlatformDirs.errorLogDir]/crash-latest.txt（见 [DesktopErrorLog]）。
         *
         * 调用方只需供给曲库/音源/密码三查（S3 接线 :core:data 的 SongDao/SourceDao/CredentialsRepository）。
         */
        fun createDefault(
            db: MusesDatabase,
            songLookup: suspend (songId: String) -> SongRef?,
            songsExist: (suspend (ids: List<String>) -> Set<String>)? = null,
            songsLookup: (suspend (ids: List<String>) -> Map<String, SongRef>)? = null,
            sourceLookup: suspend (sourceId: String) -> SourceRef? = { null },
            passwordLookup: suspend (sourceId: String) -> String? = { null },
            audioCache: DesktopWebDavAudioCache = DesktopWebDavAudioCache(),
            errorLog: (tag: String, msg: String, e: Throwable?) -> Unit = { tag, msg, e ->
                DesktopErrorLog.log(tag, msg, e)
            },
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            onPlaybackStarted: (suspend (songId: String, localFile: java.io.File) -> PlaybackLazyScan.FileTags?)? = null,
            dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
                com.muses.player.core.data.store.createDataStore(),
            /** 在线音源直链解析器（洛雪自定义源脚本）；null = 未启用在线音源 */
            onlineResolver: OnlineTrackResolver? = null,
        ): JvmPlayerPort {
            return JvmPlayerPort(
                songLookup = songLookup,
                songsExist = songsExist,
                songsLookup = songsLookup,
                sourceLookup = sourceLookup,
                passwordLookup = passwordLookup,
                playbackStateRepository = PlaybackStateRepository(dataStore),
                recentPlaysRepository = RecentPlaysRepository(dataStore),
                playStatsTracker = PlayStatsSessionTracker(PlayStatsRepository(dataStore)),
                audioCache = audioCache,
                errorLog = errorLog,
                scope = scope,
                onPlaybackStarted = onPlaybackStarted,
                onlineResolver = onlineResolver,
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

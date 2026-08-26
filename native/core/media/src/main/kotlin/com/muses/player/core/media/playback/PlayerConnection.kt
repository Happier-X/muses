@file:Suppress("UnsafeOptInUsageError")

package com.muses.player.core.media.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.media.metadata.TagReader
import com.muses.player.core.media.scanner.CoverCacheWriter
import com.muses.player.core.media.scanner.LocalLibraryScanner
import com.muses.player.core.media.scanner.WebDavLibraryScanner
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.webdav.WebDavAudioCache
import com.muses.player.core.webdav.WebDavClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放连接：经 MediaController 连接 PlaybackService，
 * 暴露播放状态 Flow 供 ViewModel 消费。
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recoveryController: PlaybackRecoveryController,
    private val webDavCache: WebDavAudioCache,
    private val webDavClient: WebDavClient,
    private val webDavLibraryScanner: WebDavLibraryScanner,
    private val songRepository: SongRepository,
) {

    /** 最近一次播放失败的安全文案；用户主动操作后清空（P4 播放页消费） */
    val playbackError: StateFlow<String?> = recoveryController.playbackError

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** WebDAV 预取下载（串行 IO）；play 换队列时取消旧任务 */
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefetchJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = playbackState
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }
    }

    fun connect() {
        if (controller != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        // MediaController 所有方法必须在主线程调用：Future 回调也需投递主线程，
        // 否则 syncState 里的 isPlaying 等直接抛 IllegalStateException（MuMu 实测崩溃）
        controllerFuture?.addListener(
            { mainHandler.post { connectOnMainThread() } },
            java.util.concurrent.Executors.newSingleThreadExecutor(),
        )
    }

    private fun connectOnMainThread() {
        val future = controllerFuture ?: return // disconnect 已发生，丢弃迟到回调
        val connected = runCatching { future.get() }.getOrNull() ?: return
        controller = connected.also { player ->
            player.addListener(playerListener)
            syncState(player)
        }
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.cancel(true)
        controller = null
        controllerFuture = null
    }

    /**
     * 从歌曲列表中选择 songId 开始播放。
     * WebDAV 曲目直接流播：有缓存用 file://，无缓存用 HTTP URL（ExoPlayer 流播）。
     * 后台异步预取+懒扫描标签，不阻塞播放。
     */
    fun play(songId: String, songs: List<com.muses.player.core.model.Song>) {
        // 用户主动切歌：重置恢复链与错误状态（controller.ts 语义）
        recoveryController.reset()
        recoveryController.clearError()

        // 直接播放，不等下载；HTTP 流播由 PlaybackService 的 CacheDataSource 边播边缓存
        applyPlayback(songId, songs)

        // 后台异步：仅当懒扫描标签有活干且未入缓存时才整文件预取（供标签/封面扫描），
        // 否则与流播缓存双倍下载；失败静默不影响播放
        val current = songs.firstOrNull { it.id == songId }
        if (
            current != null
            && current.sourceType == SourceType.WEBDAV
            && current.tagsVersion < WebDavLibraryScanner.TAGS_VERSION
            && webDavCache.getCachedFile(current.path) == null
        ) {
            prefetchJob?.cancel()
            prefetchJob = prefetchScope.launch {
                try {
                    ensureCached(current.path)
                    lazyScanTags(current)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 预取/扫描失败不影响播放
                }
            }
        }
    }

    /**
     * 播放时懒扫描（IO 线程）：缓存文件读标签/封面 + 同目录 .lrc sidecar，upsert 回库。
     * Room Flow 驱动的列表自动刷新为真实标题。幂等：仅对 tagsVersion < TAGS_VERSION 的歌生效。
     * 失败静默保持文件名歌（下次播放重试）。
     */
    private suspend fun lazyScanTags(song: com.muses.player.core.model.Song) {
        if (song.tagsVersion >= WebDavLibraryScanner.TAGS_VERSION) return
        val cached = webDavCache.getCachedFile(song.path) ?: return
        val tags = TagReader.read(cached)
        val sidecar = webDavLibraryScanner.buildSidecarLyricsUrl(song.path)
            ?.let { webDavClient.getString(it) }?.trim()?.takeIf { it.isNotEmpty() }
        val lyrics = tags.lyrics?.trim()?.takeIf { it.isNotEmpty() } ?: sidecar
        val lyricsSource = when {
            tags.lyrics?.trim()?.takeIf { it.isNotEmpty() } != null -> LyricsSource.EMBEDDED
            sidecar != null -> LyricsSource.SIDECAR
            else -> null
        }
        val songId = LocalLibraryScanner.stableSongId(song.sourceId, song.path)
        songRepository.upsert(
            song.copy(
                title = tags.title?.trim()?.takeIf { it.isNotEmpty() } ?: song.title,
                artist = tags.artist ?: song.artist,
                album = tags.album ?: song.album,
                durationMs = if (tags.durationSec > 0) tags.durationSec * 1000L else song.durationMs,
                durationSec = if (tags.durationSec > 0) tags.durationSec else song.durationSec,
                coverUri = tags.coverBytes?.let { CoverCacheWriter.write(context, songId, it) } ?: song.coverUri,
                lyrics = lyrics ?: song.lyrics,
                lyricsSource = lyricsSource ?: song.lyricsSource,
                replayGainTrackDb = tags.replayGainTrackDb ?: song.replayGainTrackDb,
                tagsVersion = WebDavLibraryScanner.TAGS_VERSION,
            ),
        )
    }

    private fun applyPlayback(songId: String, songs: List<com.muses.player.core.model.Song>) {
        val player = controller ?: return
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(resolveUri(song))
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        val index = mediaItems.indexOfFirst { it.mediaId == songId }
        if (index < 0) return

        player.setMediaItems(mediaItems, index, C.TIME_UNSET)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * 解析播放 URI：WebDAV 曲目查缓存命中转 file://；未命中走 HTTP URL（由 OkHttp 认证
     * interceptor 注入 Authorization）。其余源直接用 path。
     *
     * play() 在主线程调用（Media3 契约）；[WebDavAudioCache.getCachedFile] 仅做文件 stat 检查，开销可接受。
     */
    private fun resolveUri(song: com.muses.player.core.model.Song): Uri {
        if (song.sourceType != SourceType.WEBDAV) return Uri.parse(song.path)
        return webDavCache.getCachedFile(song.path)?.let { Uri.fromFile(it) } ?: Uri.parse(song.path)
    }

    /**
     * 整文件下载进播放缓存（IO 线程调用）：命中直接返回；未命中经临时文件下载后 putToCache
     * （顺带预热 LRU，供后续播放零网络）。失败抛出由调用方决定回退策略。
     */
    private suspend fun ensureCached(url: String) {
        if (webDavCache.getCachedFile(url) != null) return
        val tempDir = File(context.cacheDir, "tmp-playback").apply { mkdirs() }
        val temp = File.createTempFile("dl-", ".audio", tempDir)
        try {
            webDavClient.get(url, temp)
            webDavCache.putToCache(url, temp)
        } finally {
            temp.delete()
        }
    }

    fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** 选中并播放队列中第 index 项 */
    fun playAtIndex(index: Int) {
        controller?.seekTo(index, 0)
        controller?.playWhenReady = true
    }

    /** 移除队列中第 index 项 */
    fun removeQueueItemAt(index: Int) {
        controller?.removeMediaItem(index)
    }

    /** 清空队列 */
    fun clearQueueItems() {
        controller?.clearMediaItems()
    }

    fun skipToNext() {
        controller?.let {
            if (it.hasNextMediaItem()) it.seekToNext()
        }
    }

    fun skipToPrevious() {
        controller?.let {
            if (it.currentPosition > 3000) {
                it.seekTo(0)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            }
        }
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun stop() {
        controller?.stop()
    }

    private fun syncState(player: MediaController) {
        _isPlaying.value = player.isPlaying
        _currentMediaItem.value = player.currentMediaItem
        _position.value = player.currentPosition
        _duration.value = if (player.duration > 0) player.duration else 0L
        _playbackState.value = player.playbackState
        _queue.value = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        _repeatMode.value = player.repeatMode
        _shuffleModeEnabled.value = player.shuffleModeEnabled
    }

    /**
     * 位置更新轮询（UI 层调用，约 500ms 一次）。
     * ExoPlayer 的 Player.Listener 不实时推送 position，
     * UI 需主动读取以驱动进度条。
     */
    fun currentPosition(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long {
        val d = controller?.duration ?: 0L
        return if (d > 0) d else 0L
    }
}

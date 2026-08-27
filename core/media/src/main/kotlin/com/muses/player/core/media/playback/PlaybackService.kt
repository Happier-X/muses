package com.muses.player.core.media.playback

import android.app.Notification
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.datasource.cache.SimpleCache
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.tag.AudioTagReader
import com.muses.player.core.media.scanner.LocalLibraryScanner
import com.muses.player.core.media.loudness.LoudnessController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import com.muses.player.core.webdav.StreamingOkHttp
import javax.inject.Inject

/**
 * 播放服务：Media3 MediaSessionService。
 * 持有 ExoPlayer，自动处理通知/媒体按钮/音频焦点/蓝牙断连暂停。
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** Hilt 提供的流播专用客户端：只带 WebDAV 认证 interceptor，不施加 4 rps 限流（见 [StreamingOkHttp]） */
    @Inject
    @StreamingOkHttp
    lateinit var okHttpClient: OkHttpClient

    /** Media3 流播磁盘缓存：探测性重复 Range 请求命中本地不再发网络（防网关限流） */
    @Inject
    lateinit var playbackCache: SimpleCache

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var songDao: SongDao
    @Inject lateinit var playbackStateRepository: PlaybackStateRepository
    @Inject lateinit var recentPlaysRepository: RecentPlaysRepository
    @Inject lateinit var recoveryController: PlaybackRecoveryController
    @Inject lateinit var errorLogStore: ErrorLogStore
    @Inject lateinit var audioTagReader: AudioTagReader
    @Inject lateinit var songRepository: SongRepository

    private var saveJob: kotlinx.coroutines.Job? = null

    // ExoPlayer 强制主线程访问（player-accessed-on-wrong-thread 崩溃防护），
    // 服务生命周期本就在主线程；Room/DataStore 挂起调用内部自行切 IO
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loudnessController: LoudnessController? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        // CacheDataSource 边播边缓存：首次播放仍立即出声，但 ExoPlayer 对未知时长 mp3/flac 的
        // 探测性重复打开会命中本地缓存不再发网络请求，避免触发网关（Cloudflare）限流；
        // 出错时回落上游不阻断播放。file:// 由 DefaultDataSource 外层分派，不经缓存。
        val cacheFactory = CacheDataSource.Factory()
            .setCache(playbackCache)
            .setUpstreamDataSourceFactory(okHttpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val dataSourceFactory = DefaultDataSource.Factory(this, cacheFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // 注：media3 1.11 无 Player.setPreloadItems（相邻预加载 API 在 1.13+），默认不会预加载整队列；
        // 真正触发 429 的是流播 Range 被 4 rps 限流饿死，已通过 @StreamingOkHttp 流播 client 剥离限流解决。
        // 若实测恢复队列（465 首）一次性 prepare 仍发全列请求，再改为「只 prepare 当前曲 + 下一首」分批加载。

        mediaSession = MediaSession.Builder(this, player).build()

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(notificationProvider)

        // 响度均衡：挂在服务侧 ExoPlayer 上（MediaController 无 volume 能力）
        loudnessController = LoudnessController(player, settingsRepository, songDao, serviceScope)
            .also { it.start() }

        // 播放持久化（任务 08-25-native-playback-persistence / P1）
        player.addListener(persistenceListener)
        // ExoPlayer 只能在主线程访问：恢复流程在后台查库，player 操作投递主线程
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        serviceScope.launch {
            val config = playbackStateRepository.readConfig()
            mainHandler.post { applyRestoredConfig(player, config) }
            restoreFromSnapshot(player) { block -> mainHandler.post { block() } }
        }
    }

    /**
     * 冷启动恢复（规格书 = queue.ts loadQueueData + session.ts loadPlaybackSession）：
     * 应用配置 → 按快照重建队列（已被曲库删除的歌曲自然过滤）→ seekTo 上次进度。
     * 只恢复不自动播放（playWhenReady 保持 false）。
     */
    /** 主线程应用恢复的播放配置 */
    private fun applyRestoredConfig(player: Player, config: com.muses.player.core.model.playback.PlayerConfig) {
        player.repeatMode = if (config.repeatMode == com.muses.player.core.model.playback.RepeatMode.ONE) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_ALL
        }
        player.shuffleModeEnabled = config.shuffleEnabled
    }

    /**
     * 冷启动恢复（后台读库；[onMain] 把 ExoPlayer 调用投递回主线程）：
     * 按快照重建队列（已被曲库删除的歌曲自然过滤）→ seekTo 上次进度。
     * 只恢复不自动播放（playWhenReady 保持 false）。
     */
    private suspend fun restoreFromSnapshot(player: Player, onMain: (() -> Unit) -> Unit) {
        val snapshot = playbackStateRepository.readSnapshot() ?: return
        if (snapshot.items.isEmpty()) return

        val resolved = snapshot.items.mapNotNull { songDao.getById(it.songId)?.toDomain() }
        if (resolved.isEmpty()) return

        val startIndex = resolved.indexOfFirst { it.id == snapshot.currentSongId }
            .let { if (it >= 0) it else 0 }
        val mediaItems = resolved.map { song -> buildRestoreMediaItem(song) }
        onMain {
            player.setMediaItems(mediaItems, startIndex, snapshot.positionMs.coerceAtLeast(0))
            player.prepare()
        }
    }

    private fun buildRestoreMediaItem(song: com.muses.player.core.model.Song): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.path.toUri())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build(),
            )
            .build()

    /** 变更节流保存：500ms debounce，避免 seek 拖动高频写盘 */
    private fun scheduleSnapshotSave(player: Player) {
        saveJob?.cancel()
        saveJob = serviceScope.launch {
            kotlinx.coroutines.delay(500)
            saveSnapshotNow(player)
        }
    }

    private suspend fun saveSnapshotNow(player: Player) {
        val items = (0 until player.mediaItemCount).map {
            com.muses.player.core.model.playback.QueueItem(player.getMediaItemAt(it).mediaId)
        }
        playbackStateRepository.writeSnapshot(
            PlaybackStateRepository.PlaybackSnapshot(
                items = items,
                originalOrder = items,
                shuffleOrder = null, // shuffle 序由 Media3 内部管理，恢复时按开关重洗
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition.coerceAtLeast(0),
                currentSongId = player.currentMediaItem?.mediaId,
            ),
        )
    }

    /** 持久化监听：转场/播放态切换/seek 触发节流保存；转场登记最近播放 */
    private val persistenceListener = object : Player.Listener {

        /**
         * 播放失败自动恢复（规格书 = controller.ts 播放失败分支）：
         * 登记失败曲 → 沿 active order 回绕查找未尝试候选 → seek+prepare+play；
         * 无候选才停止并暴露安全文案。
         */
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val player = mediaSession?.player ?: return

            // R2 埋点：播放失败留痕（含限流/恢复链分支），供设置页复制反馈
            errorLogStore.log(
                ErrorLogStore.Level.ERROR,
                "Playback",
                "播放失败：${PlaybackErrorCopy.copyFor(error)}（code=${error.errorCode}）",
                error,
            )

            // 服务级拒绝（限流 429 / 网关故障 5xx）：服务器整体不可用，跳歌只会继续撞墙
            // 并持续触发请求加重限流（实测 465 首队列轮询切歌）——直接停止，等用户手动重试。
            val httpCode = PlaybackErrorCopy.httpResponseCode(error)
            if (httpCode == 429) {
                errorLogStore.log(
                    ErrorLogStore.Level.WARN,
                    "Playback",
                    "触发限流 429（WebDAV 播放）url=${player.currentMediaItem?.localConfiguration?.uri}",
                    error,
                )
                player.stop()
                recoveryController.setError(PlaybackErrorCopy.RATE_LIMITED_RETRY)
                return
            }
            if (httpCode in 500..599) {
                player.stop()
                recoveryController.setError(PlaybackErrorCopy.RATE_LIMITED_ERROR)
                return
            }

            val failedId = player.currentMediaItem?.mediaId
            if (failedId != null) {
                recoveryController.markAttempted(failedId)
            }
            val order = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }
            val candidateIndex = recoveryController.selectNextCandidate(order, player.currentMediaItemIndex)
            if (candidateIndex != null) {
                recoveryController.recordAttempt(order[candidateIndex])
                recoveryController.clearError()
                // 继续恢复时不清媒体会话，避免异步 clear 覆盖下一首刚写入的 metadata
                player.seekTo(candidateIndex, 0)
                player.prepare()
                player.playWhenReady = true
            } else {
                recoveryController.setError(PlaybackErrorCopy.copyFor(error))
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                scheduleSnapshotSave(player)
                val currentId = player.currentMediaItem?.mediaId
                if (currentId != null) {
                    serviceScope.launch {
                        val entity = songDao.getById(currentId)
                        entity?.toDomain()?.let { song ->
                            recentPlaysRepository.record(
                                com.muses.player.core.model.playback.RecentPlayEntry(
                                    songId = song.id,
                                    title = song.title,
                                    subtitle = listOfNotNull(song.artist, song.album)
                                        .filter { it.isNotBlank() }.joinToString(" - "),
                                    coverUri = song.coverUri,
                                    playedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                        // 播放时懒扫描：补齐 tagsVersion<1 的歌曲信息，Room Flow 自动刷新列表
                        // 契约：仅对 FILENAME_TAGS_VERSION(0) 执行，经 SongRepository 唯一入库路径以同步重建派生索引
                        if (entity != null && entity.tagsVersion < LocalLibraryScanner.TAGS_VERSION) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val tagData = audioTagReader.readTagForUpdate(entity.path, entity.id)
                                    if (tagData != null) {
                                        val hasUpdate = !tagData.title.isNullOrBlank() ||
                                            !tagData.artist.isNullOrBlank() ||
                                            !tagData.album.isNullOrBlank() ||
                                            tagData.coverUri != null ||
                                            tagData.durationMs > 0
                                        if (hasUpdate) {
                                            val domain = entity.toDomain().copy(
                                                title = tagData.title?.takeIf { it.isNotBlank() } ?: entity.title,
                                                artist = tagData.artist ?: entity.artist,
                                                album = tagData.album ?: entity.albumTitle,
                                                lyrics = tagData.lyrics ?: entity.lyrics,
                                                coverUri = tagData.coverUri ?: entity.coverUri,
                                                durationMs = tagData.durationMs.coerceAtLeast(entity.durationMs),
                                                durationSec = (tagData.durationMs / 1000).coerceAtLeast(entity.durationSec),
                                                tagsVersion = LocalLibraryScanner.TAGS_VERSION,
                                            )
                                            songRepository.upsert(domain)
                                        } else {
                                            // 无有效标签也标记已处理，避免对无标签文件重复 Range 请求；下次仍显示文件名
                                            val domain = entity.toDomain().copy(tagsVersion = LocalLibraryScanner.TAGS_VERSION)
                                            songRepository.upsert(domain)
                                        }
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    // 静默失败保持 tagsVersion=0 下次重试，不阻塞播放；留痕供设置页排查
                                    errorLogStore.log(ErrorLogStore.Level.WARN, "PlaybackLazyScan", "懒扫描失败 id=${entity.id} path=${entity.path.take(80)}: ${e.message}", e)
                                }
                            }
                        }
                    }
                }
            }
            if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED)
            ) {
                scheduleSnapshotSave(player)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // 销毁前强制落盘一次快照（runBlocking 短超时；服务销毁路径可接受）
        mediaSession?.player?.let { player ->
            runBlocking {
                withTimeoutOrNull(2_000) { saveSnapshotNow(player) }
            }
        }
        // 响度均衡先停，避免释放中的 player 还被回调
        loudnessController?.stop()
        loudnessController = null
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

package com.muses.player.core.media.playback

import android.app.Notification
import android.app.PendingIntent
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
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.tag.AudioTagReader
import com.muses.player.core.media.scanner.LocalLibraryScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import com.muses.player.core.webdav.STREAMING_OKHTTP_QUALIFIER
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

/**
 * 播放服务：Media3 MediaSessionService。
 * 持有 ExoPlayer，自动处理通知/媒体按钮/音频焦点/蓝牙断连暂停。
 *
 * P2a Hilt→Koin：`@AndroidEntryPoint` + `@Inject` 字段改 Koin 懒委托（Service 是 Context，直接 `inject()`）。
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** 流播专用客户端：只带 WebDAV 认证 interceptor，不施加 4 rps 限流（见 [STREAMING_OKHTTP_QUALIFIER]） */
    private val okHttpClient: OkHttpClient by inject(named(STREAMING_OKHTTP_QUALIFIER))

    /** Media3 流播磁盘缓存：探测性重复 Range 请求命中本地不再发网络（防网关限流） */
    private val playbackCache: SimpleCache by inject()

    private val songDao: SongDao by inject()
    private val playbackStateRepository: PlaybackStateRepository by inject()
    private val recentPlaysRepository: RecentPlaysRepository by inject()
    private val recoveryController: PlaybackRecoveryController by inject()
    private val errorLogStore: ErrorLogStore by inject()
    private val audioTagReader: AudioTagReader by inject()
    private val songRepository: SongRepository by inject()
    private val settingsRepository: com.muses.player.core.data.repository.SettingsRepository by inject()

    private var saveJob: kotlinx.coroutines.Job? = null

    // ── 通知歌词模式 ──
    /** 原始元数据（切歌时快照；开启歌词模式后不从 player.currentMediaItem 读，防脏读） */
    private var originalTitle: CharSequence? = null
    private var originalArtist: CharSequence? = null
    /** 标记当前 MediaItem 是否已被歌词模式修改过（防重复 replaceMediaItem 触发持久化循环） */
    private var metadataModified = false
    /** 当前曲歌词行缓存（解析一次，position 轮询只做二分查找） */
    private var lyricsLines: List<com.muses.player.core.lyrics.model.LyricLine>? = null

    // ExoPlayer 强制主线程访问（player-accessed-on-wrong-thread 崩溃防护），
    // 服务生命周期本就在主线程；Room/DataStore 挂起调用内部自行切 IO
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
        // MP3 CBR 时长估算：HTTP 流播（WebDAV）默认 Mp3Extractor 不估算时长（duration=TIME_UNSET），
        // 沉浸页进度条总时长显示 --:-- 且禁用；开 CBR seek flag 后按比特率估算 duration + seek 能力
        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setMp3ExtractorFlags(
                androidx.media3.extractor.mp3.Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING,
            )
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // 注：media3 1.11 无 Player.setPreloadItems（相邻预加载 API 在 1.13+），默认不会预加载整队列；
        // 真正触发 429 的是流播 Range 被 4 rps 限流饿死，已通过流播专用 client（named streamingOkHttp）剥离限流解决。
        // 若实测恢复队列（465 首）一次性 prepare 仍发全列请求，再改为「只 prepare 当前曲 + 下一首」分批加载。

        // 通知卡片点击回应用：Media3 通知的 contentIntent 取自 sessionActivity，
        // 缺省 null 即点击无反应（小米等：别家点卡片回应用，我方无反应即此缺口）。
        // 本版 setSessionActivity 不接受 null，取不到启动意图时保持缺省（行为不变，不崩溃）。
        val sessionBuilder = MediaSession.Builder(this, player)
        buildSessionActivity()?.let { sessionBuilder.setSessionActivity(it) }
        mediaSession = sessionBuilder.build()

        // 09-07 定案：媒体通知口径 = 上面标题、下面艺术家，专辑不进通知（与迷你条窄屏形态一致）；
        // media3 1.11 默认行为相同，此处显式锁定防止后续升级改默认值。MediaMetadata.albumTitle
        // 保留不删——蓝牙/车载（AVRCP）仍需要专辑名。
        val notificationProvider = object : DefaultMediaNotificationProvider(this) {
            override fun getNotificationContentTitle(
                mediaMetadata: androidx.media3.common.MediaMetadata,
            ): CharSequence = mediaMetadata.title ?: ""

            override fun getNotificationContentText(
                mediaMetadata: androidx.media3.common.MediaMetadata,
            ): CharSequence = mediaMetadata.artist ?: ""
        }
        notificationProvider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(notificationProvider)

        // 播放持久化（任务 08-25-native-playback-persistence / P1）
        player.addListener(persistenceListener)
        // 09-07 通知歌词模式：监听开关 + 歌词 + 播放位置，动态替换 MediaMetadata
        startNotificationLyricsMonitoring(player)
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

    // ── 通知歌词模式 ──

    /**
     * 监听 [SettingsRepository.notificationLyricsEnabled]，开启后：
     * - 标题位置显示当前歌词行
     * - 艺术家位置显示「原始标题 - 原始艺术家」
     * 关闭时恢复原始 MediaMetadata。
     */
    private fun startNotificationLyricsMonitoring(player: Player) {
        serviceScope.launch {
            var enabled = false
            // 后台收集开关状态
            launch {
                settingsRepository.notificationLyricsEnabled.collect {
                    enabled = it
                    if (!it) restoreNotificationMetadata(player)
                }
            }
            kotlinx.coroutines.delay(50) // 等首次值到达
            // 位置轮询 + 切歌检测
            var lastSongId: String? = null
            snapshotOriginalMetadata(player)
            parseCurrentSongLyrics(player)
            lastSongId = player.currentMediaItem?.mediaId
            while (true) {
                kotlinx.coroutines.delay(100)
                if (!enabled) {
                    lastSongId = null
                    continue
                }
                val currentId = player.currentMediaItem?.mediaId
                if (currentId != lastSongId) {
                    metadataModified = false
                    snapshotOriginalMetadata(player)
                    parseCurrentSongLyrics(player)
                    lastSongId = currentId
                }
                updateNotificationMetadataWithLyric(player)
            }
        }
    }

    /** 快照原始元数据（只在首次或切歌时调用，防歌词模式下读到脏值） */
    private fun snapshotOriginalMetadata(player: Player) {
        if (metadataModified) return
        val item = player.currentMediaItem ?: return
        originalTitle = item.mediaMetadata.title
        originalArtist = item.mediaMetadata.artist
    }

    /** 从 Room 读取歌词并解析 */
    private suspend fun parseCurrentSongLyrics(player: Player) {
        val songId = player.currentMediaItem?.mediaId ?: run {
            lyricsLines = null
            return
        }
        val song = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            songDao.getById(songId)
        }
        lyricsLines = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.muses.player.feature.player.lyric.LyricsParser.parseDocument(song?.lyrics)?.lines
        }
    }

    /** 根据播放位置更新 MediaMetadata：标题=歌词行，艺术家=原始标题-原始艺术家 */
    private fun updateNotificationMetadataWithLyric(player: Player) {
        val session = mediaSession ?: return
        val lines = lyricsLines
        if (lines.isNullOrEmpty()) {
            if (metadataModified) restoreNotificationMetadata(player)
            return
        }
        val pos = player.currentPosition
        val index = com.muses.player.core.lyrics.model.LyricsDocument(
            lines = lines,
        ).highlightedIndex(pos)
        val lyricLine = index?.let { lines.getOrNull(it)?.text?.trim() }?.takeIf { it.isNotEmpty() }
            ?: return
        val origTitle = originalTitle?.toString() ?: ""
        val origArtist = originalArtist?.toString() ?: ""
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(lyricLine)
            .setArtist("$origTitle - $origArtist")
            .build()
        val current = player.currentMediaItem ?: return
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            current.buildUpon().setMediaMetadata(metadata).build(),
        )
        metadataModified = true
    }

    /** 恢复原始 MediaMetadata（关闭歌词模式或切歌时） */
    private fun restoreNotificationMetadata(player: Player) {
        val origTitle = originalTitle ?: return
        val origArtist = originalArtist
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(origTitle)
            .setArtist(origArtist)
            .build()
        val current = player.currentMediaItem ?: return
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            current.buildUpon().setMediaMetadata(metadata).build(),
        )
        metadataModified = false
        lyricsLines = null
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
                    // 与 applyPlayback 同口径：不传 albumTitle，系统卡片只显示艺术家
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
                // 通知歌词模式：切歌时重置标记，由轮询检测 songId 变化后重新快照+应用
                metadataModified = false
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
                        // 编排收口 U26 共用 [PlaybackLazyScan]；本处只负责读标签（AudioTagReader Range 探测）+ 入库
                        // 封面缺失同样进入（版本已齐也不跳过）：后补内嵌/首次漏读可经 coverBackfill 回填
                        val needsCover = entity != null &&
                            entity.metaCover == null && entity.coverUri.isNullOrBlank()
                        if (entity != null && (entity.tagsVersion < LocalLibraryScanner.TAGS_VERSION || needsCover)) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val tagData = audioTagReader.readTagForUpdate(entity.path, entity.id)
                                    val fileTags = tagData?.let {
                                        com.muses.player.core.media.scanner.PlaybackLazyScan.FileTags(
                                            title = it.title,
                                            artist = it.artist,
                                            album = it.album,
                                            lyrics = it.lyrics,
                                            coverUri = it.coverUri,
                                            durationMs = it.durationMs,
                                        )
                                    }
                                    val song = entity.toDomain()
                                    val merged = com.muses.player.core.media.scanner.PlaybackLazyScan.merge(
                                        song,
                                        fileTags,
                                    )
                                    if (merged != null) {
                                        songRepository.upsert(merged)
                                    } else {
                                        com.muses.player.core.media.scanner.PlaybackLazyScan.coverBackfill(
                                            song,
                                            fileTags?.coverUri,
                                        )?.let { songRepository.upsert(it) }
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

    /**
     * 回应用启动意图（通知卡片/系统卡片点击用）。
     *
     * 经包名取启动意图，不直引 :app 的 MainActivity——core:media 不能反向依赖 app，
     * 且 muses/miui 双 flavor 包名不同，包名口径自动适配。MainActivity 为 singleTask，
     * NEW_TASK 下点击直接把已有任务抬到前台，不会重复建栈。
     */
    private fun buildSessionActivity(): PendingIntent? {
        return runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return null
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }.getOrNull()
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
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

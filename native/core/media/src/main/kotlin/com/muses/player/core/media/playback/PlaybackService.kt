package com.muses.player.core.media.playback

import android.app.Notification
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.media.loudness.LoudnessController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * 播放服务：Media3 MediaSessionService。
 * 持有 ExoPlayer，自动处理通知/媒体按钮/音频焦点/蓝牙断连暂停。
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val okHttpClient by lazy { OkHttpClient.Builder().build() }

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var songDao: SongDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loudnessController: LoudnessController? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        val defaultFactory = DefaultDataSource.Factory(this)
        val dataSourceFactory = DefaultDataSource.Factory(this, okHttpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(mediaSourceFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(android.R.drawable.ic_media_play)
        setMediaNotificationProvider(notificationProvider)

        // 响度均衡：挂在服务侧 ExoPlayer 上（MediaController 无 volume 能力）
        loudnessController = LoudnessController(player, settingsRepository, songDao, serviceScope)
            .also { it.start() }
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

    /**
     * 构建带 Authorization header 的 MediaItem（用于 WebDAV 流播）。
     */
    fun buildWebDavMediaItem(url: String, songId: String, authHeader: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(url.toUri())
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(url.toUri())
                    .build()
            )
            .build()
    }
}

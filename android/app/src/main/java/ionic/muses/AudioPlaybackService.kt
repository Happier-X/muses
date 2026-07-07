package ionic.muses

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.nio.charset.StandardCharsets

class AudioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var currentSongId: String? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification("正在准备播放器"))
        ensurePlayer()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY -> playFromIntent(intent)
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP -> stopPlayback()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun ensurePlayer(): ExoPlayer {
        val existingPlayer = player
        if (existingPlayer != null) {
            return existingPlayer
        }

        val nextPlayer = ExoPlayer.Builder(this).build()
        nextPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (currentSongId == null) {
                    return
                }
                publishState(if (isPlaying) STATUS_PLAYING else STATUS_PAUSED)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopPlayback()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                publishState(STATUS_ERROR, mapPlaybackError(error))
            }
        })

        player = nextPlayer
        mediaSession = MediaSession.Builder(this, nextPlayer).build()
        return nextPlayer
    }

    private fun playFromIntent(intent: Intent) {
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)
        val songId = intent.getStringExtra(EXTRA_SONG_ID)
        val uri = intent.getStringExtra(EXTRA_URI)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "未知歌曲"
        val artist = intent.getStringExtra(EXTRA_ARTIST)
        val album = intent.getStringExtra(EXTRA_ALBUM)

        startForeground(NOTIFICATION_ID, createForegroundNotification(title, artist))

        if (sourceType.isNullOrBlank() || songId.isNullOrBlank() || uri.isNullOrBlank()) {
            publishState(STATUS_ERROR, "播放参数不完整。")
            return
        }

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaId(songId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build(),
            )
            .build()

        try {
            val activePlayer = ensurePlayer()
            currentSongId = songId
            publishState(STATUS_LOADING)
            activePlayer.stop()
            activePlayer.clearMediaItems()
            activePlayer.setMediaSource(createMediaSource(mediaItem, intent))
            activePlayer.prepare()
            activePlayer.play()
        } catch (exception: Exception) {
            publishState(STATUS_ERROR, mapPlaybackError(exception))
        }
    }

    private fun createMediaSource(mediaItem: MediaItem, intent: Intent): ProgressiveMediaSource {
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)
        val dataSourceFactory = if (sourceType == SOURCE_WEBDAV) {
            val username = intent.getStringExtra(EXTRA_USERNAME)
            val password = intent.getStringExtra(EXTRA_PASSWORD)
            if (username == null || password == null) {
                throw IllegalArgumentException("missingCredentials")
            }

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf("Authorization" to "Basic ${encodeBasicAuth(username, password)}"))
            DefaultDataSource.Factory(this, httpDataSourceFactory)
        } else {
            DefaultDataSource.Factory(this)
        }

        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    private fun pausePlayback() {
        player?.pause()
        if (currentSongId != null) {
            publishState(STATUS_PAUSED)
        }
    }

    private fun resumePlayback() {
        val activePlayer = player
        if (activePlayer != null && currentSongId != null) {
            activePlayer.play()
            publishState(STATUS_PLAYING)
        }
    }

    private fun stopPlayback() {
        player?.stop()
        currentSongId = null
        publishState(STATUS_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishState(status: String, errorMessage: String? = null) {
        val snapshot = PlaybackStatus(status, currentSongId, errorMessage)
        lastStatus = snapshot
        sendBroadcast(Intent(ACTION_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, snapshot.status)
            putExtra(EXTRA_SONG_ID, snapshot.currentSongId)
            putExtra(EXTRA_ERROR_MESSAGE, snapshot.errorMessage)
        })
    }

    private fun mapPlaybackError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("missingCredentials", ignoreCase = true) -> "WebDAV 播放缺少认证信息。"
            message.contains("contentUriNotFound", ignoreCase = true) -> "本地音频文件不可访问，请重新扫描或重新授权。"
            message.contains("Permission", ignoreCase = true) || message.contains("EACCES", ignoreCase = true) -> "本地音频文件无访问权限，请重新授权音源目录。"
            message.contains("403") || message.contains("401") -> "WebDAV 认证失败，请检查账号或重新添加音源。"
            message.contains("FileNotFound", ignoreCase = true) || message.contains("No such file", ignoreCase = true) -> "音频文件不存在或已失效，请重新扫描音源。"
            else -> "播放失败，请检查音频文件或网络连接。"
        }
    }

    private fun createForegroundNotification(title: String, artist: String? = null): Notification {
        val subtitle = artist?.takeIf { it.isNotBlank() } ?: "媒体播放服务"
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "播放控制",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "歌曲播放与通知栏控制"
        }
        manager.createNotificationChannel(channel)
    }

    private fun encodeBasicAuth(username: String, password: String): String {
        val rawValue = "$username:$password"
        return Base64.encodeToString(rawValue.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    data class PlaybackStatus(
        val status: String,
        val currentSongId: String?,
        val errorMessage: String?,
    )

    companion object {
        const val ACTION_PLAY = "ionic.muses.audio.PLAY"
        const val NOTIFICATION_CHANNEL_ID = "audio-playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE = "ionic.muses.audio.PAUSE"
        const val ACTION_RESUME = "ionic.muses.audio.RESUME"
        const val ACTION_STOP = "ionic.muses.audio.STOP"
        const val ACTION_STATE_CHANGED = "ionic.muses.audio.STATE_CHANGED"

        const val EXTRA_SOURCE_TYPE = "sourceType"
        const val EXTRA_SONG_ID = "songId"
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM = "album"
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_STATUS = "status"
        const val EXTRA_ERROR_MESSAGE = "errorMessage"

        const val SOURCE_WEBDAV = "webdav"
        const val STATUS_IDLE = "idle"
        const val STATUS_LOADING = "loading"
        const val STATUS_PLAYING = "playing"
        const val STATUS_PAUSED = "paused"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_ERROR = "error"

        @Volatile
        var lastStatus = PlaybackStatus(STATUS_IDLE, null, null)
            private set
    }
}

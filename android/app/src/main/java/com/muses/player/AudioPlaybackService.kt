package com.muses.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

@UnstableApi
class AudioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var currentSongId: String? = null
    private var currentTitle: String = "正在准备播放器"
    private var currentArtist: String? = null
    private val webDavHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    private val webDavAudioCache by lazy { WebDavAudioCache(this, webDavHttpClient) }
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (currentSongId != null) {
                publishState(if (player?.isPlaying == true) STATUS_PLAYING else STATUS_PAUSED)
                scheduleProgressUpdates()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureBootstrapNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setNotificationId(MEDIA_NOTIFICATION_ID)
                .setChannelId(MEDIA_NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.audio_media_notification_channel)
                .build(),
        )
        startForeground(BOOTSTRAP_NOTIFICATION_ID, createBootstrapNotification())
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
            ACTION_SEEK -> seekPlayback(intent.getDoubleExtra(EXTRA_POSITION, 0.0))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRunnable)
        player?.release()
        mediaSession?.release()
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
                if (isPlaying) {
                    scheduleProgressUpdates()
                } else {
                    progressHandler.removeCallbacks(progressRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    progressHandler.removeCallbacks(progressRunnable)
                    if (currentSongId != null) {
                        publishState(STATUS_FINISHED)
                    }
                    return
                }
                if (currentSongId != null) {
                    publishState(if (nextPlayer.isPlaying) STATUS_PLAYING else STATUS_PAUSED)
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

        currentTitle = title
        currentArtist = artist

        if (sourceType.isNullOrBlank() || songId.isNullOrBlank() || uri.isNullOrBlank()) {
            publishState(STATUS_ERROR, "播放参数不完整。")
            return
        }

        try {
            val playbackUri = resolvePlaybackUri(sourceType, uri, intent)
            val mediaItem = MediaItem.Builder()
                .setUri(playbackUri)
                .setMediaId(songId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setAlbumTitle(album)
                        .build(),
                )
                .build()

            val activePlayer = ensurePlayer()
            progressHandler.removeCallbacks(progressRunnable)
            currentSongId = null
            activePlayer.stop()
            activePlayer.clearMediaItems()
            currentSongId = songId
            publishState(STATUS_LOADING)
            activePlayer.setMediaSource(createMediaSource(mediaItem, intent))
            activePlayer.prepare()
            activePlayer.play()
            scheduleProgressUpdates()
        } catch (exception: Exception) {
            publishState(STATUS_ERROR, mapPlaybackError(exception))
        }
    }

    private fun resolvePlaybackUri(sourceType: String, uri: String, intent: Intent): Uri {
        if (sourceType != SOURCE_WEBDAV) {
            return Uri.parse(uri)
        }

        val username = intent.getStringExtra(EXTRA_USERNAME)
        val password = intent.getStringExtra(EXTRA_PASSWORD)
        if (username == null || password == null) {
            throw IllegalArgumentException("missingCredentials")
        }

        val cachedFile = webDavAudioCache.getCachedFile(uri)
        if (cachedFile != null) {
            return Uri.fromFile(cachedFile)
        }

        webDavAudioCache.downloadInBackground(uri, username, password)
        return Uri.parse(uri)
    }

    private fun createMediaSource(mediaItem: MediaItem, intent: Intent): ProgressiveMediaSource {
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)
        val dataSourceFactory = if (sourceType == SOURCE_WEBDAV && mediaItem.localConfiguration?.uri?.scheme?.startsWith("http") == true) {
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
        if (currentSongId == null) {
            stopIdleService()
            return
        }
        player?.pause()
        progressHandler.removeCallbacks(progressRunnable)
        publishState(STATUS_PAUSED)
    }

    private fun resumePlayback() {
        val activePlayer = player
        if (activePlayer == null || currentSongId == null) {
            stopIdleService()
            return
        }
        activePlayer.play()
        publishState(STATUS_PLAYING)
        scheduleProgressUpdates()
    }

    private fun seekPlayback(positionSeconds: Double) {
        val activePlayer = player
        if (activePlayer == null || currentSongId == null) {
            stopIdleService()
            return
        }
        val durationMs = activePlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 }
        val requestedMs = (positionSeconds.coerceAtLeast(0.0) * 1000).toLong()
        val targetMs = durationMs?.let { requestedMs.coerceAtMost(it) } ?: requestedMs
        activePlayer.seekTo(targetMs)
        publishState(if (activePlayer.isPlaying) STATUS_PLAYING else STATUS_PAUSED)
        if (activePlayer.isPlaying) {
            scheduleProgressUpdates()
        }
    }

    private fun stopIdleService() {
        progressHandler.removeCallbacks(progressRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPlayback() {
        progressHandler.removeCallbacks(progressRunnable)
        currentSongId = null
        player?.stop()
        currentTitle = "正在准备播放器"
        currentArtist = null
        publishState(STATUS_STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.postDelayed(progressRunnable, PROGRESS_INTERVAL_MS)
    }

    private fun publishState(status: String, errorMessage: String? = null) {
        val position = currentPositionSeconds()
        val duration = durationSeconds()
        val snapshot = PlaybackStatus(status, currentSongId, errorMessage, position, duration)
        lastStatus = snapshot
        sendBroadcast(Intent(ACTION_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, snapshot.status)
            putExtra(EXTRA_SONG_ID, snapshot.currentSongId)
            putExtra(EXTRA_ERROR_MESSAGE, snapshot.errorMessage)
            putExtra(EXTRA_POSITION, snapshot.position)
            putExtra(EXTRA_DURATION, snapshot.duration)
        })
    }

    private fun currentPositionSeconds(): Double {
        val value = player?.currentPosition ?: 0L
        return if (value > 0) value / 1000.0 else 0.0
    }

    private fun durationSeconds(): Double {
        val value = player?.duration ?: C.TIME_UNSET
        return if (value != C.TIME_UNSET && value > 0) value / 1000.0 else 0.0
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

    private fun createBootstrapNotification(): Notification {
        return NotificationCompat.Builder(this, BOOTSTRAP_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("正在准备播放器")
            .setContentText("媒体播放服务启动中")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureBootstrapNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(BOOTSTRAP_NOTIFICATION_CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            BOOTSTRAP_NOTIFICATION_CHANNEL_ID,
            "后台播放启动",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "保持后台音频播放运行，媒体控制由系统媒体通知提供。"
            setSound(null, null)
            enableVibration(false)
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
        val position: Double,
        val duration: Double,
    )

    companion object {
        const val ACTION_PLAY = "com.muses.player.audio.PLAY"
        const val BOOTSTRAP_NOTIFICATION_CHANNEL_ID = "audio-playback-bootstrap"
        const val MEDIA_NOTIFICATION_CHANNEL_ID = "audio-playback"
        const val BOOTSTRAP_NOTIFICATION_ID = 1001
        const val MEDIA_NOTIFICATION_ID = BOOTSTRAP_NOTIFICATION_ID
        const val ACTION_PAUSE = "com.muses.player.audio.PAUSE"
        const val ACTION_RESUME = "com.muses.player.audio.RESUME"
        const val ACTION_STOP = "com.muses.player.audio.STOP"
        const val ACTION_SEEK = "com.muses.player.audio.SEEK"
        const val ACTION_STATE_CHANGED = "com.muses.player.audio.STATE_CHANGED"

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
        const val EXTRA_POSITION = "position"
        const val EXTRA_DURATION = "duration"

        const val SOURCE_WEBDAV = "webdav"
        const val STATUS_IDLE = "idle"
        const val STATUS_LOADING = "loading"
        const val STATUS_PLAYING = "playing"
        const val STATUS_PAUSED = "paused"
        const val STATUS_FINISHED = "finished"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_ERROR = "error"
        const val PROGRESS_INTERVAL_MS = 750L

        @Volatile
        var lastStatus = PlaybackStatus(STATUS_IDLE, null, null, 0.0, 0.0)
            private set
    }
}

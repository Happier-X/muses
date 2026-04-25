package com.example.muses.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.muses.R
import com.example.muses.data.repository.WebdavConfigManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Background service managing ExoPlayer playback, MediaSession, and notification.
 */
@UnstableApi
class MusicService : MediaLibraryService() {

    companion object {
        private const val TAG = "MusicService"
        const val NOTIFICATION_CHANNEL_ID = "muses_playback"
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(createMediaSourceFactory())
            .build()

        val callback = MusesLibraryCallback()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback)
            .build()

        Log.i(TAG, "Service created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaLibrarySession.release()
        player.release()
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }
 
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and now playing info"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates a DataSource.Factory that:
     * - Handles local content:// URIs via DefaultDataSource (ContentDataSource)
     * - Handles HTTP(S) URIs via OkHttpDataSource with WebDAV Basic auth
     */
    private fun createMediaSourceFactory(): DefaultMediaSourceFactory {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val authHeader = WebdavConfigManager.getBasicAuthHeader(this)
                val request = if (authHeader != null && original.url.scheme in listOf("http", "https")) {
                    original.newBuilder()
                        .header("Authorization", authHeader)
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(this, okHttpFactory)

        return DefaultMediaSourceFactory(dataSourceFactory)
    }

    // --- MediaLibrarySession Callback ---

    private inner class MusesLibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
        }
    }

}

package com.muses.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        /** 解除设备后暂停的去抖窗口 */
        const val DEVICE_REMOVAL_PAUSE_DEBOUNCE_MS = 500L

        /** 窗口剩余阈值，小于等于此值时触发补窗 */
        const val WINDOW_REFILL_THRESHOLD = 2

        /** 输出设备中「拔出即应暂停」的类型集合 */
        val DISRUPTIVE_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_DOCK,
        )

        fun isDisruptiveDeviceRemoved(
            removed: Iterable<AudioPlayerPlugin.RemovedOutputDevice>,
        ): Boolean = removed.any { it.isSink && it.type in DISRUPTIVE_OUTPUT_TYPES }

        @Volatile
        var instance: PlaybackService? = null
            private set

        @Volatile
        var requestUrlsListener: (() -> Unit)? = null
    }

    private var mediaSession: MediaSession? = null
    private var deviceCallbackRegistered = false
    private var pendingRemovalPauseTask: Runnable? = null
    private val removalDebounceHandler = Handler(Looper.getMainLooper())

    // --------------- 原生队列自治 ---------------
    private val playbackQueue = PlaybackQueue()
    @Volatile
    private var pendingResumeAfterRefill: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    // seek 伪 finished 保护窗
    @Volatile private var lastSeekAtMs: Long = 0L
    private val SEEK_GUARD_MS = 1500L

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            try {
                val simplified = removedDevices.map {
                    AudioPlayerPlugin.RemovedOutputDevice(type = it.type, isSink = it.isSink)
                }
                if (!isDisruptiveDeviceRemoved(simplified)) return
                if (mediaSession?.player?.isPlaying != true) return

                pendingRemovalPauseTask?.let { removalDebounceHandler.removeCallbacks(it) }
                pendingRemovalPauseTask = Runnable {
                    pendingRemovalPauseTask = null
                    mediaSession?.player?.pause()
                }
                removalDebounceHandler.postDelayed(
                    pendingRemovalPauseTask!!,
                    DEVICE_REMOVAL_PAUSE_DEBOUNCE_MS,
                )
            } catch (_: Exception) {
                // 回调异常静默
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // handleAudioFocus = true
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        // 防 Doze：同时锁 CPU + WiFi，防止后台 prepare 失败
        player.setWakeMode(C.WAKE_MODE_NETWORK)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // seek 保护窗内的伪 finished 不切歌
                    if (System.currentTimeMillis() - lastSeekAtMs < SEEK_GUARD_MS) {
                        Log.d("PlaybackService", "STATE_ENDED within seek guard, ignore")
                        return
                    }
                    handleAutoAdvanceOnEnded()
                }
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    lastSeekAtMs = System.currentTimeMillis()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .build()

        // 使用 Media3 默认通知提供者，自动处理 startForeground / 通知更新，避免 ANR
        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.mipmap.ic_launcher)
        setMediaNotificationProvider(notificationProvider)

        // 注册设备移除回调
        registerDeviceRemovalCallback()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // 注销设备移除回调
        unregisterDeviceRemovalCallback()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun registerDeviceRemovalCallback() {
        if (deviceCallbackRegistered) return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, removalDebounceHandler)
            deviceCallbackRegistered = true
        }
    }

    private fun unregisterDeviceRemovalCallback() {
        if (!deviceCallbackRegistered) return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            deviceCallbackRegistered = false
        }
    }

    // --------------- 队列自治对外接口 ---------------

    @Synchronized
    fun updateQueueContext(
        tracks: List<PlaybackQueue.Track>,
        windowCurrentIndex: Int,
        repeatModeStr: String,
        hasPreviousOutsideWindow: Boolean,
        hasNextOutsideWindow: Boolean,
        windowResetFromWrap: Boolean = false,
    ) {
        val mode = PlaybackQueue.RepeatMode.fromString(repeatModeStr)
        playbackQueue.replace(tracks, windowCurrentIndex, mode, hasPreviousOutsideWindow, hasNextOutsideWindow)
        requestUrlsIfNeeded()
        if (pendingResumeAfterRefill) {
            pendingResumeAfterRefill = false
            // wrap 场景：JS 已将窗口重置到头部，current 即为下一首，直接播 current
            val resume = if (windowResetFromWrap) {
                playbackQueue.current()
            } else {
                playbackQueue.advanceRaw(false)
            }
            if (resume != null) {
                playFromQueue(resume)
            } else {
                val cur = playbackQueue.current()
                if (cur != null && cur.playable()) {
                    playFromQueue(cur)
                } else if (shouldRequestUrlsForwardWrap()) {
                    pendingResumeAfterRefill = true
                    emitRequestUrls()
                }
            }
        }
    }

    @Synchronized
    private fun handleAutoAdvanceOnEnded(): Boolean {
        val next = playbackQueue.advanceRaw(true)
        if (next == null) {
            if (shouldRequestUrlsForwardWrap()) {
                pendingResumeAfterRefill = true
                emitRequestUrls()
                return true
            }
            return false
        }
        playFromQueue(next)
        return true
    }

    @Synchronized
    private fun playFromQueue(track: PlaybackQueue.Track) {
        val url = track.url ?: return
        if (url.isBlank()) return
        try {
            val uri = Uri.parse(url)
            val mediaItem = MediaItem.Builder()
                .setMediaId(track.songId)
                .setUri(uri)
                .build()
            val player = mediaSession?.player ?: return
            // WebDAV 需 Authorization，单独用带 header 的 MediaSource
            if (!track.authHeader.isNullOrBlank() && url.startsWith("http")) {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(mapOf("Authorization" to track.authHeader!!))
                val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                (player as ExoPlayer).setMediaSource(mediaSource, 0)
            } else {
                player.setMediaItem(mediaItem, 0)
            }
            player.prepare()
            player.playWhenReady = true
            pendingResumeAfterRefill = false
            requestUrlsIfNeeded()
        } catch (e: Exception) {
            Log.e("PlaybackService", "playFromQueue failed", e)
        }
    }

    private fun shouldRequestUrlsForwardWrap(): Boolean {
        if (playbackQueue.hasNextOutsideWindow()) return true
        return playbackQueue.getRepeatMode() == PlaybackQueue.RepeatMode.ALL
                && playbackQueue.hasPreviousOutsideWindow()
    }

    private fun requestUrlsIfNeeded() {
        if (playbackQueue.playableTracksAhead() <= WINDOW_REFILL_THRESHOLD
            && playbackQueue.hasNextOutsideWindow()
        ) {
            emitRequestUrls()
        }
    }

    private fun emitRequestUrls() {
        val listener = requestUrlsListener ?: return
        mainHandler.post { listener.invoke() }
    }

}

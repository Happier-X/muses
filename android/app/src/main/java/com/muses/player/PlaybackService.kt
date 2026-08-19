package com.muses.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        /** 解除设备后暂停的去抖窗口 */
        const val DEVICE_REMOVAL_PAUSE_DEBOUNCE_MS = 500L

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
    }

    private var mediaSession: MediaSession? = null
    private var deviceCallbackRegistered = false
    private var pendingRemovalPauseTask: Runnable? = null
    private val removalDebounceHandler = Handler(Looper.getMainLooper())

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

        mediaSession = MediaSession.Builder(this, player)
            .build()

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
}

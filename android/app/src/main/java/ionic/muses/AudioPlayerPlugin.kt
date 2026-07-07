package ionic.muses

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "AudioPlayer")
class AudioPlayerPlugin : Plugin() {
    private var stateReceiver: BroadcastReceiver? = null

    override fun load() {
        super.load()
        stateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioPlaybackService.ACTION_STATE_CHANGED) {
                    notifyListeners("stateChange", stateFromIntent(intent))
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            stateReceiver,
            IntentFilter(AudioPlaybackService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun handleOnDestroy() {
        stateReceiver?.let { context.unregisterReceiver(it) }
        stateReceiver = null
        super.handleOnDestroy()
    }

    @PluginMethod
    fun play(call: PluginCall) {
        val sourceType = call.getString("sourceType")
        val songId = call.getString("songId")
        val title = call.getString("title") ?: "未知歌曲"

        if (sourceType != "local" && sourceType != "webdav") {
            call.reject("不支持的音源类型。", "invalidSourceType")
            return
        }

        if (songId.isNullOrBlank()) {
            call.reject("缺少歌曲 ID。", "missingSongId")
            return
        }

        val intent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_PLAY
            putExtra(AudioPlaybackService.EXTRA_SOURCE_TYPE, sourceType)
            putExtra(AudioPlaybackService.EXTRA_SONG_ID, songId)
            putExtra(AudioPlaybackService.EXTRA_TITLE, title)
            putExtra(AudioPlaybackService.EXTRA_ARTIST, call.getString("artist"))
            putExtra(AudioPlaybackService.EXTRA_ALBUM, call.getString("album"))
        }

        if (sourceType == "webdav") {
            val url = call.getString("url")
            val username = call.getString("username")
            val password = call.getString("password")
            if (url.isNullOrBlank() || username == null || password == null) {
                call.reject("缺少 WebDAV 播放参数。", "missingWebDavOptions")
                return
            }
            intent.putExtra(AudioPlaybackService.EXTRA_URI, url)
            intent.putExtra(AudioPlaybackService.EXTRA_USERNAME, username)
            intent.putExtra(AudioPlaybackService.EXTRA_PASSWORD, password)
        } else {
            val uri = call.getString("uri")
            if (uri.isNullOrBlank()) {
                call.reject("缺少本地音频地址。", "missingUri")
                return
            }
            intent.putExtra(AudioPlaybackService.EXTRA_URI, uri)
        }

        ContextCompat.startForegroundService(context, intent)
        call.resolve()
    }

    @PluginMethod
    fun pause(call: PluginCall) {
        sendControl(AudioPlaybackService.ACTION_PAUSE)
        call.resolve()
    }

    @PluginMethod
    fun resume(call: PluginCall) {
        sendControl(AudioPlaybackService.ACTION_RESUME)
        call.resolve()
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        sendControl(AudioPlaybackService.ACTION_STOP)
        call.resolve()
    }

    @PluginMethod
    fun seek(call: PluginCall) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_SEEK
                putExtra(AudioPlaybackService.EXTRA_POSITION, call.getDouble("position", 0.0))
            },
        )
        call.resolve()
    }

    @PluginMethod
    fun getState(call: PluginCall) {
        call.resolve(stateFromSnapshot(AudioPlaybackService.lastStatus))
    }

    private fun sendControl(actionName: String) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AudioPlaybackService::class.java).apply { action = actionName },
        )
    }

    private fun stateFromIntent(intent: Intent): JSObject {
        val result = JSObject()
        result.put("status", intent.getStringExtra(AudioPlaybackService.EXTRA_STATUS) ?: AudioPlaybackService.STATUS_IDLE)
        intent.getStringExtra(AudioPlaybackService.EXTRA_SONG_ID)?.let { result.put("currentSongId", it) }
        intent.getStringExtra(AudioPlaybackService.EXTRA_ERROR_MESSAGE)?.let { result.put("errorMessage", it) }
        result.put("position", intent.getDoubleExtra(AudioPlaybackService.EXTRA_POSITION, 0.0))
        result.put("duration", intent.getDoubleExtra(AudioPlaybackService.EXTRA_DURATION, 0.0))
        return result
    }

    private fun stateFromSnapshot(snapshot: AudioPlaybackService.PlaybackStatus): JSObject {
        val result = JSObject()
        result.put("status", snapshot.status)
        snapshot.currentSongId?.let { result.put("currentSongId", it) }
        snapshot.errorMessage?.let { result.put("errorMessage", it) }
        result.put("position", snapshot.position)
        result.put("duration", snapshot.duration)
        return result
    }
}

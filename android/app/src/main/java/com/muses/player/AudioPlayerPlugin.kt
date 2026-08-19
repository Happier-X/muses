package com.muses.player

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@UnstableApi
@CapacitorPlugin(
    name = "AudioPlayer",
    permissions = [
        Permission(
            strings = [Manifest.permission.POST_NOTIFICATIONS],
            alias = "notifications",
        ),
    ],
)
class AudioPlayerPlugin : Plugin() {
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
            removed: Iterable<RemovedOutputDevice>,
        ): Boolean = removed.any { it.isSink && it.type in DISRUPTIVE_OUTPUT_TYPES }
    }

    data class RemovedOutputDevice(val type: Int, val isSink: Boolean)

    // --------------- ExoPlayer ---------------
    private var exoPlayer: ExoPlayer? = null
    private var currentSongId: String? = null
    private var handleAudioFocus = true

    // --------------- HTTP Client ---------------
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .readTimeout(0L, TimeUnit.MILLISECONDS)
            .build()
    }

    private val coverHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10_000L, TimeUnit.MILLISECONDS)
            .readTimeout(15_000L, TimeUnit.MILLISECONDS)
            .callTimeout(20_000L, TimeUnit.MILLISECONDS)
            .build()
    }

    private val audioCache by lazy { WebDavAudioCache(context, httpClient) }

    // --------------- 设备移除暂停 ---------------
    private var deviceCallbackRegistered = false
    private var pendingRemovalPauseTask: Runnable? = null
    private val removalDebounceHandler = Handler(Looper.getMainLooper())

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            try {
                val simplified = removedDevices.map { RemovedOutputDevice(type = it.type, isSink = it.isSink) }
                if (!isDisruptiveDeviceRemoved(simplified)) return
                if (exoPlayer?.isPlaying != true) return

                pendingRemovalPauseTask?.let { removalDebounceHandler.removeCallbacks(it) }
                pendingRemovalPauseTask = Runnable {
                    pendingRemovalPauseTask = null
                    exoPlayer?.pause()
                    emitState()
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

    // --------------- 生命周期 ---------------

    override fun load() {
        super.load()
        registerDeviceRemovalCallback()
        initExoPlayer()
    }

    override fun handleOnDestroy() {
        releaseExoPlayer()
        super.handleOnDestroy()
    }

    private fun registerDeviceRemovalCallback() {
        if (deviceCallbackRegistered) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, removalDebounceHandler)
            deviceCallbackRegistered = true
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initExoPlayer() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                addListener(playerListener)
            }
    }

    private fun rebuildExoPlayer() {
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        val wasPlaying = exoPlayer?.isPlaying == true
        val currentMediaItem = exoPlayer?.currentMediaItem
        
        releaseExoPlayer()
        initExoPlayer()
        
        // 恢复状态
        if (currentMediaItem != null) {
            exoPlayer?.apply {
                setMediaItem(currentMediaItem, currentPosition)
                prepare()
                playWhenReady = wasPlaying
            }
        }
    }

    private fun releaseExoPlayer() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        currentSongId = null
    }

    // --------------- ExoPlayer Listener ---------------

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    emitState()
                }
                Player.STATE_ENDED -> {
                    emitState(status = "finished")
                    // 通知 JS 切下一首
                    notifyPlaybackComplete()
                }
                Player.STATE_BUFFERING -> {
                    emitState(status = "loading")
                }
                Player.STATE_IDLE -> {
                    emitState(status = "idle")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            emitState()
        }

        override fun onPlayerError(error: PlaybackException) {
            emitState(status = "error", errorMessage = error.message ?: "播放失败")
        }
    }

    // --------------- Capacitor Plugin Methods ---------------

    @PluginMethod
    fun load(call: PluginCall) {
        val uri = call.getString("uri") ?: return call.reject("缺少音频地址", "missingUri")
        val songId = call.getString("songId") ?: return call.reject("缺少歌曲标识", "missingSongId")
        val volume = call.getDouble("volume", 1.0) ?: 1.0
        val headers = call.getObject("audioHeaders")

        // 音频焦点设置在 load 时不变，需要通过 setAudioFocus 方法单独设置

        bridge.execute {
            try {
                currentSongId = songId

                // 构建 DataSource.Factory
                val dataSourceFactory = if (headers != null && headers.length() > 0) {
                    val httpHeaders = mutableMapOf<String, String>()
                    for (key in headers.keys()) {
                        headers.getString(key)?.let { httpHeaders[key] = it }
                    }
                    DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(httpHeaders)
                        .setConnectTimeoutMs(15_000)
                        .setReadTimeoutMs(0)
                } else {
                    DefaultHttpDataSource.Factory()
                        .setConnectTimeoutMs(15_000)
                        .setReadTimeoutMs(0)
                }

                // 构建 MediaItem
                val mediaItem = MediaItem.Builder()
                    .setMediaId(songId)
                    .setUri(Uri.parse(uri))
                    .build()

                // 设置并播放
                exoPlayer?.apply {
                    setMediaItem(mediaItem, 0) // 从头播放
                    prepare()
                    playWhenReady = true
                    setVolume(volume.toFloat().coerceIn(0f, 1f))
                }

                emitState(status = "loading")
                call.resolve()
            } catch (e: Exception) {
                call.reject("播放失败: ${e.message}", "playFailed", e)
            }
        }
    }

    @PluginMethod
    fun play(call: PluginCall) {
        exoPlayer?.play()
        emitState()
        call.resolve()
    }

    @PluginMethod
    fun pause(call: PluginCall) {
        exoPlayer?.pause()
        emitState()
        call.resolve()
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        exoPlayer?.stop()
        currentSongId = null
        emitState(status = "stopped")
        call.resolve()
    }

    @PluginMethod
    fun seek(call: PluginCall) {
        val position = call.getDouble("position", 0.0) ?: 0.0
        exoPlayer?.seekTo((position * 1000).toLong())
        emitState()
        call.resolve()
    }

    @PluginMethod
    fun setVolume(call: PluginCall) {
        val volume = call.getDouble("volume", 1.0) ?: 1.0
        exoPlayer?.setVolume(volume.toFloat().coerceIn(0f, 1f))
        call.resolve()
    }

    @PluginMethod
    fun getState(call: PluginCall) {
        val state = buildState()
        call.resolve(state)
    }

    @PluginMethod
    fun setAudioFocus(call: PluginCall) {
        val enabled = call.getBoolean("enabled", true) ?: true
        if (enabled != handleAudioFocus) {
            handleAudioFocus = enabled
            // ExoPlayer 的 audioFocus 设置只能在构建时指定，需要重建
            rebuildExoPlayer()
        }
        call.resolve()
    }

    @PluginMethod
    fun ensureNotificationPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            call.resolve(JSObject().put("granted", true))
            return
        }
        if (getPermissionState("notifications") == PermissionState.GRANTED) {
            call.resolve(JSObject().put("granted", true))
            return
        }
        requestPermissionForAlias("notifications", call, "onNotificationPermissionResult")
    }

    @PermissionCallback
    private fun onNotificationPermissionResult(call: PluginCall) {
        val granted = getPermissionState("notifications") == PermissionState.GRANTED
        call.resolve(JSObject().put("granted", granted))
    }

    // --------------- 封面转换 ---------------

    @PluginMethod
    fun prepareArtworkDataUrl(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrBlank()) {
            call.resolve(JSObject().put("dataUrl", null as String?))
            return
        }

        bridge.execute {
            try {
                val uri = Uri.parse(uriValue)
                openArtworkInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                        ?: throw IllegalArgumentException("unsupportedImage")
                    val outputStream = ByteArrayOutputStream()
                    if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)) {
                        throw IllegalArgumentException("compressFailed")
                    }
                    val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    call.resolve(JSObject().put("dataUrl", "data:image/jpeg;base64,$encoded"))
                } ?: throw IllegalArgumentException("artworkNotFound")
            } catch (exception: Exception) {
                call.resolve(JSObject().put("dataUrl", null as String?))
            }
        }
    }

    @PluginMethod
    fun cacheRemoteCover(call: PluginCall) {
        val url = call.getString("url")
        val cacheKey = call.getString("cacheKey")
        if (url.isNullOrBlank() || cacheKey.isNullOrBlank()) {
            call.resolve(JSObject().put("uri", null as String?))
            return
        }
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            call.resolve(JSObject().put("uri", null as String?))
            return
        }

        bridge.execute {
            try {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Muses/1.0")
                    .get()
                    .build()
                coverHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        call.resolve(JSObject().put("uri", null as String?))
                        return@execute
                    }
                    val body = response.body
                    val contentLength = body.contentLength()
                    if (contentLength > 5L * 1024L * 1024L) {
                        call.resolve(JSObject().put("uri", null as String?))
                        return@execute
                    }
                    val bytes = body.bytes()
                    if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) {
                        call.resolve(JSObject().put("uri", null as String?))
                        return@execute
                    }
                    // 简单魔数校验
                    val isImage =
                        (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) ||
                            (bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) ||
                            (bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[8] == 0x57.toByte()) ||
                            (bytes.size >= 6 && bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte())
                    if (!isImage) {
                        call.resolve(JSObject().put("uri", null as String?))
                        return@execute
                    }
                    val directory = File(context.cacheDir, "covers").apply { mkdirs() }
                    val file = File(directory, "${sha256(cacheKey)}.jpg")
                    file.writeBytes(bytes)
                    call.resolve(JSObject().put("uri", Uri.fromFile(file).toString()))
                }
            } catch (_: Exception) {
                call.resolve(JSObject().put("uri", null as String?))
            }
        }
    }

    // --------------- WebDAV 缓存（保留兼容） ---------------

    @PluginMethod
    fun prepareLocalAudioFile(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrBlank()) {
            call.reject("缺少本地音频地址。", "missingUri")
            return
        }

        if (!uriValue.startsWith("content://")) {
            call.resolve(JSObject().put("uri", uriValue))
            return
        }

        bridge.execute {
            try {
                val songId = call.getString("songId") ?: uriValue
                val preparedUri = copyContentUriToPlaybackCache(Uri.parse(uriValue), songId)
                call.resolve(JSObject().put("uri", preparedUri))
            } catch (exception: Exception) {
                call.reject("本地音频文件不可访问，请重新扫描或重新授权。", "contentUriNotFound", exception)
            }
        }
    }

    @PluginMethod
    fun getCachedWebDavAudioFile(call: PluginCall) {
        val url = call.getString("url")
        if (url.isNullOrBlank()) {
            call.reject("缺少 WebDAV 音频地址。", "missingUrl")
            return
        }

        bridge.execute {
            val cached = audioCache.getCachedFile(url)
            val result = JSObject()
            result.put("uri", cached?.let { Uri.fromFile(it).toString() })
            call.resolve(result)
        }
    }

    @PluginMethod
    fun prefetchWebDavAudioFile(call: PluginCall) {
        val url = call.getString("url")
        val username = call.getString("username")
        val password = call.getString("password")

        if (url.isNullOrBlank()) {
            call.reject("缺少 WebDAV 音频地址。", "missingUrl")
            return
        }
        if (username == null || password == null) {
            call.reject("WebDAV 播放缺少认证信息。", "missingCredentials")
            return
        }

        bridge.execute {
            val cachedFile = audioCache.getCachedFile(url)
            if (cachedFile != null) {
                call.resolve(
                    JSObject()
                        .put("cached", true)
                        .put("started", false),
                )
                return@execute
            }

            val started = audioCache.downloadInBackground(url, username, password)
            call.resolve(
                JSObject()
                    .put("cached", false)
                    .put("started", started),
            )
        }
    }

    // --------------- 辅助方法 ---------------

    private fun buildState(): JSObject {
        val player = exoPlayer
        val state = JSObject()

        if (player == null || currentSongId == null) {
            state.put("status", "idle")
            state.put("position", 0)
            state.put("duration", 0)
            state.put("currentSongId", null as String?)
            return state
        }

        val status = when {
            player.playbackState == Player.STATE_ENDED -> "finished"
            player.playbackState == Player.STATE_BUFFERING -> "loading"
            player.isPlaying -> "playing"
            player.playbackState == Player.STATE_READY -> "paused"
            else -> "idle"
        }

        state.put("status", status)
        state.put("currentSongId", currentSongId)
        state.put("position", player.currentPosition / 1000.0)
        state.put("duration", player.duration / 1000.0)
        state.put("bufferedPosition", player.bufferedPosition / 1000.0)

        return state
    }

    private fun emitState(status: String? = null, errorMessage: String? = null) {
        val state = buildState()
        if (status != null) {
            state.put("status", status)
        }
        if (errorMessage != null) {
            state.put("errorMessage", errorMessage)
        }
        notifyListeners("stateChange", state)
    }

    private fun notifyPlaybackComplete() {
        notifyListeners("playbackComplete", JSObject())
    }

    private fun openArtworkInputStream(uri: Uri): InputStream? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path
                if (path.isNullOrBlank()) null
                else {
                    val file = File(path)
                    if (file.isFile && file.canRead()) FileInputStream(file) else null
                }
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }

    private fun copyContentUriToPlaybackCache(uri: Uri, cacheKey: String): String {
        val cacheDir = File(context.cacheDir, "native-audio-playback").apply { mkdirs() }
        val cachedFile = File(cacheDir, "${sha256(cacheKey)}.audio")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cachedFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("contentUriNotFound")

        if (cachedFile.length() <= 0L) {
            throw IllegalArgumentException("contentUriNotFound")
        }

        return Uri.fromFile(cachedFile).toString()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}

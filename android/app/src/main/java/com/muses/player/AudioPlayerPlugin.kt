package com.muses.player

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
    data class RemovedOutputDevice(val type: Int, val isSink: Boolean)

    // --------------- MediaController ---------------
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var currentSongId: String? = null

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

    // --------------- 生命周期 ---------------

    override fun load() {
        super.load()
        // 播放时再启动前台服务，避免空载 FGS 导致的 ANR（startForegroundService 未及时 startForeground）
        connectMediaController()
        PlaybackService.requestUrlsListener = {
            notifyListeners("requestUrls", JSObject())
        }
    }

    override fun handleOnDestroy() {
        if (PlaybackService.requestUrlsListener != null) {
            PlaybackService.requestUrlsListener = null
        }
        disconnectMediaController()
        super.handleOnDestroy()
    }

    private fun startPlaybackServiceIfNeeded() {
        if (PlaybackService.instance != null) return
        val intent = Intent(context, PlaybackService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // 忽略 ForegroundServiceStartNotAllowed 等异常，MediaController 绑定仍可拉起 Service
            try { context.startService(intent) } catch (_: Exception) {}
        }
    }

    private fun connectMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                // 连接成功，开始监听状态变化
                setupControllerListener()
            } catch (e: Exception) {
                // 连接失败，稍后重试
                android.util.Log.e("AudioPlayer", "MediaController 连接失败", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> emitState()
                    Player.STATE_ENDED -> {
                        emitState(status = "finished")
                        notifyPlaybackComplete()
                    }
                    Player.STATE_BUFFERING -> emitState(status = "loading")
                    Player.STATE_IDLE -> emitState(status = "idle")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPositionPolling()
                } else {
                    stopPositionPolling()
                }
                emitState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                stopPositionPolling()
                emitState(status = "error", errorMessage = error.message ?: "播放失败")
            }
        })
    }

    // --------------- 位置轮询 ---------------
    private var positionPollTimer: android.os.Handler? = null
    private var positionPollRunnable: Runnable? = null
    private val POSITION_POLL_MS = 500L

    private fun startPositionPolling() {
        stopPositionPolling()
        positionPollTimer = android.os.Handler(android.os.Looper.getMainLooper())
        positionPollRunnable = object : Runnable {
            override fun run() {
                if (mediaController?.isPlaying == true) {
                    emitState()
                    positionPollTimer?.postDelayed(this, POSITION_POLL_MS)
                }
            }
        }
        positionPollTimer?.postDelayed(positionPollRunnable!!, POSITION_POLL_MS)
    }

    private fun stopPositionPolling() {
        positionPollRunnable?.let { positionPollTimer?.removeCallbacks(it) }
        positionPollTimer = null
        positionPollRunnable = null
    }

    private fun disconnectMediaController() {
        stopPositionPolling()
        mediaController?.release()
        mediaController = null
        controllerFuture?.cancel(true)
        controllerFuture = null
    }

    // --------------- Capacitor Plugin Methods ---------------

    @PluginMethod
    fun load(call: PluginCall) {
        val uri = call.getString("uri") ?: return call.reject("缺少音频地址", "missingUri")
        val songId = call.getString("songId") ?: return call.reject("缺少歌曲标识", "missingSongId")
        val volume = call.getDouble("volume", 1.0) ?: 1.0
        val headers = call.getObject("audioHeaders")

        bridge.activity.runOnUiThread {
            try {
                startPlaybackServiceIfNeeded()
                currentSongId = songId

                // 构建 DataSource.Factory（带认证头）
                val hasHeaders = headers != null && headers.length() > 0
                if (hasHeaders && headers != null) {
                    val httpHeaders = mutableMapOf<String, String>()
                    for (key in headers.keys()) {
                        headers.getString(key)?.let { httpHeaders[key] = it }
                    }
                    // WebDAV 需要特殊处理：通过 MediaItem 的 RequestMetadata 传递
                    // 这里简化为直接使用 URI（ExoPlayer 会自动处理）
                }

                // 构建 MediaItem
                val mediaItem = MediaItem.Builder()
                    .setMediaId(songId)
                    .setUri(Uri.parse(uri))
                    .build()

                // 通过 MediaController 控制
                mediaController?.apply {
                    setMediaItem(mediaItem, 0)
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
        bridge.activity.runOnUiThread {
            mediaController?.play()
            call.resolve()
        }
    }

    @PluginMethod
    fun pause(call: PluginCall) {
        bridge.activity.runOnUiThread {
            mediaController?.pause()
            call.resolve()
        }
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        bridge.activity.runOnUiThread {
            mediaController?.stop()
            currentSongId = null
            emitState(status = "stopped")
            call.resolve()
        }
    }

    @PluginMethod
    fun seek(call: PluginCall) {
        val position = call.getDouble("position", 0.0) ?: 0.0
        bridge.activity.runOnUiThread {
            mediaController?.seekTo((position * 1000).toLong())
            call.resolve()
        }
    }

    @PluginMethod
    fun setVolume(call: PluginCall) {
        val volume = call.getDouble("volume", 1.0) ?: 1.0
        bridge.activity.runOnUiThread {
            mediaController?.setVolume(volume.toFloat().coerceIn(0f, 1f))
            call.resolve()
        }
    }

    @PluginMethod
    fun getState(call: PluginCall) {
        bridge.activity.runOnUiThread {
            val state = buildState()
            call.resolve(state)
        }
    }

    @PluginMethod
    fun setAudioFocus(call: PluginCall) {
        val enabled = call.getBoolean("enabled", true) ?: true
        // 音频焦点由 PlaybackService 中的 ExoPlayer 管理
        // 这里可以通过重建 ExoPlayer 来切换，但暂时保留默认行为
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

    @PluginMethod
    fun updateQueueContext(call: PluginCall) {
        try {
            val windowTracks = call.getArray("windowTracks")
            val windowCurrentIndex = call.getInt("windowCurrentIndex", -1) ?: -1
            val repeatMode = call.getString("repeatMode") ?: "all"
            val hasPrev = call.getBoolean("hasPreviousOutsideWindow", false) ?: false
            val hasNext = call.getBoolean("hasNextOutsideWindow", false) ?: false
            val windowResetFromWrap = call.getBoolean("windowResetFromWrap", false) ?: false
            val tracks = mutableListOf<PlaybackQueue.Track>()
            if (windowTracks != null) {
                for (i in 0 until windowTracks.length()) {
                    val obj = windowTracks.optJSONObject(i) ?: continue
                    val track = PlaybackQueue.Track()
                    track.songId = obj.optString("songId", "")
                    track.url = obj.optString("url", null)
                    track.authHeader = obj.optString("authHeader", null)?.takeIf { it.isNotBlank() }
                    track.title = obj.optString("title", "")
                    track.artist = obj.optString("artist", "")
                    track.album = obj.optString("album", "")
                    track.coverUrl = obj.optString("coverUrl", "")
                    track.durationMs = obj.optLong("durationMs", 0L)
                    track.playListIndex = obj.optInt("playListIndex", -1)
                    if (track.songId.isNotBlank()) {
                        tracks.add(track)
                    }
                }
            }
            PlaybackService.instance?.updateQueueContext(tracks, windowCurrentIndex, repeatMode, hasPrev, hasNext, windowResetFromWrap)
            call.resolve()
        } catch (e: Exception) {
            call.reject("更新队列失败: ${e.message}", "updateQueueFailed", e)
        }
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
        val controller = mediaController
        val state = JSObject()

        // 优先使用 MediaController 的真实 mediaId，兼容原生队列自治场景（Service直接切歌时 currentSongId 未更新）
        val actualSongId = controller?.currentMediaItem?.mediaId ?: currentSongId
        if (controller == null || actualSongId == null) {
            state.put("status", "idle")
            state.put("position", 0)
            state.put("duration", 0)
            state.put("currentSongId", null as String?)
            return state
        }
        // 同步缓存的 currentSongId，避免后续不一致
        if (actualSongId != currentSongId) {
            currentSongId = actualSongId
        }

        val status = when {
            controller.playbackState == Player.STATE_ENDED -> "finished"
            controller.playbackState == Player.STATE_BUFFERING -> "loading"
            controller.isPlaying -> "playing"
            controller.playbackState == Player.STATE_READY -> "paused"
            else -> "idle"
        }

        state.put("status", status)
        state.put("currentSongId", actualSongId)
        state.put("position", controller.currentPosition / 1000.0)
        state.put("duration", controller.duration / 1000.0)
        state.put("bufferedPosition", controller.bufferedPosition / 1000.0)

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

package com.muses.player

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient

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
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .readTimeout(0L, TimeUnit.MILLISECONDS) // 渐进下载可能持续整首歌
            .build()
    }

    private val audioCache by lazy { WebDavAudioCache(context, httpClient) }

    private val activeProgressiveCancel = AtomicReference<(() -> Unit)?>(null)
    private val activeBufferSongId = AtomicReference<String?>(null)

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

    @PluginMethod
    fun prepareLocalAudioFile(call: PluginCall) {
        val uriValue = call.getString("uri")
        if (uriValue.isNullOrBlank()) {
            call.reject("缺少本地音频地址。", "missingUri")
            return
        }

        if (!uriValue.startsWith("content://")) {
            // 非 content URI 视为已就绪本地文件：上报 full buffer
            val songId = call.getString("songId")
            if (!songId.isNullOrBlank()) {
                emitFullBuffer(songId)
            }
            call.resolve(JSObject().put("uri", uriValue))
            return
        }

        bridge.execute {
            try {
                val songId = call.getString("songId") ?: uriValue
                val preparedUri = copyContentUriToPlaybackCache(Uri.parse(uriValue), songId)
                // 本地拷贝完成后视为全长可 seek
                emitFullBuffer(songId)
                call.resolve(JSObject().put("uri", preparedUri))
            } catch (exception: Exception) {
                call.reject("本地音频文件不可访问，请重新扫描或重新授权。", "contentUriNotFound", exception)
            }
        }
    }

    /**
     * WebDAV 渐进下载到缓存文件；达到可播阈值后返回 file://，
     * 并持续通过 bufferProgress 事件上报已缓冲比例/秒数。
     *
     * 注意：密码仅在原生侧用于下载，不回写到 resolve 结果。
     */
    @PluginMethod
    fun prepareWebDavAudioFile(call: PluginCall) {
        val url = call.getString("url")
        val username = call.getString("username")
        val password = call.getString("password")
        val songId = call.getString("songId")
        val durationHint = call.getDouble("duration")

        if (url.isNullOrBlank()) {
            call.reject("缺少 WebDAV 音频地址。", "missingUrl")
            return
        }
        if (username == null || password == null) {
            call.reject("WebDAV 播放缺少认证信息。", "missingCredentials")
            return
        }
        if (songId.isNullOrBlank()) {
            call.reject("缺少歌曲标识。", "missingSongId")
            return
        }

        // 取消上一首渐进下载，禁止串曲缓冲
        activeProgressiveCancel.getAndSet(null)?.invoke()
        activeBufferSongId.set(songId)
        emitBufferProgress(
            songId = songId,
            bufferedPosition = 0.0,
            duration = durationHint,
            fullyBuffered = false,
            bufferedRatio = 0.0,
        )

        bridge.execute {
            try {
                val handle = audioCache.startProgressiveDownload(
                    url = url,
                    username = username,
                    password = password,
                    onProgress = { snapshot ->
                        if (activeBufferSongId.get() != songId) {
                            return@startProgressiveDownload
                        }
                        val ratio = when {
                            snapshot.fullyBuffered -> 1.0
                            snapshot.contentLength != null && snapshot.contentLength > 0L ->
                                (snapshot.writtenBytes.toDouble() / snapshot.contentLength.toDouble()).coerceIn(0.0, 1.0)
                            else -> null
                        }
                        val bufferedSeconds = if (durationHint != null && durationHint > 0 && ratio != null) {
                            durationHint * ratio
                        } else if (snapshot.fullyBuffered && durationHint != null && durationHint > 0) {
                            durationHint
                        } else {
                            null
                        }
                        emitBufferProgress(
                            songId = songId,
                            bufferedPosition = bufferedSeconds,
                            duration = durationHint,
                            fullyBuffered = snapshot.fullyBuffered,
                            bufferedRatio = ratio,
                        )
                    },
                )

                if (activeBufferSongId.get() != songId) {
                    handle.cancel()
                    call.reject("播放已取消。", "cancelled")
                    return@execute
                }

                activeProgressiveCancel.set(handle.cancel)
                val result = JSObject()
                result.put("uri", handle.fileUri)
                result.put("ready", true)
                call.resolve(result)
            } catch (exception: Exception) {
                if (activeBufferSongId.get() == songId) {
                    activeBufferSongId.compareAndSet(songId, null)
                }
                val message = exception.message.orEmpty()
                when {
                    message.contains("webdavCacheDownloadFailed:401") || message.contains("webdavCacheDownloadFailed:403") ->
                        call.reject("WebDAV 认证失败，请检查账号或重新添加音源。", "authFailed", exception)
                    message.contains("webdavCacheDownloadFailed") ->
                        call.reject("播放失败，请检查音频文件或网络连接。", "downloadFailed", exception)
                    else ->
                        call.reject("播放失败，请检查音频文件或网络连接。", "prepareFailed", exception)
                }
            }
        }
    }

    /** 切歌/停止时取消渐进下载并清空缓冲会话。 */
    @PluginMethod
    fun cancelBufferSession(call: PluginCall) {
        val songId = call.getString("songId")
        activeProgressiveCancel.getAndSet(null)?.invoke()
        if (songId.isNullOrBlank() || activeBufferSongId.get() == songId) {
            activeBufferSongId.set(null)
        }
        call.resolve()
    }

    /**
     * 查询 WebDAV 完整缓存文件 URI。
     * 仅完整目标文件命中；.partial / 未完成下载返回 uri=null。
     * 密码不参与、不回写。
     */
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

    /**
     * 预取下一首 WebDAV：已有完整缓存则 cached=true；否则后台完整下载 started=true。
     * 密码仅用于原生下载边界，不写入 resolve 结果。
     */
    @PluginMethod
    fun prefetchWebDavAudioFile(call: PluginCall) {
        val url = call.getString("url")
        val username = call.getString("username")
        val password = call.getString("password")
        // songId 仅作诊断标识，预取正确性以 URL 缓存会话为准
        @Suppress("UNUSED_VARIABLE")
        val songId = call.getString("songId")

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

    private fun emitFullBuffer(songId: String) {
        activeBufferSongId.set(songId)
        emitBufferProgress(
            songId = songId,
            bufferedPosition = null,
            duration = null,
            fullyBuffered = true,
            bufferedRatio = 1.0,
        )
    }

    private fun emitBufferProgress(
        songId: String,
        bufferedPosition: Double?,
        duration: Double?,
        fullyBuffered: Boolean,
        bufferedRatio: Double?,
    ) {
        val payload = JSObject()
        payload.put("songId", songId)
        if (bufferedPosition != null) {
            payload.put("bufferedPosition", bufferedPosition)
        }
        if (duration != null) {
            payload.put("duration", duration)
        }
        payload.put("fullyBuffered", fullyBuffered)
        if (bufferedRatio != null) {
            payload.put("bufferedRatio", bufferedRatio)
        }
        // Capacitor notifyListeners 需在桥线程安全调用
        bridge.activity.runOnUiThread {
            notifyListeners("bufferProgress", payload)
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

    /**
     * 缓存封面多为 file://.../cache/covers 下的 jpg 文件。
     * 部分机型 ContentResolver 打开 file:// 会失败，优先 FileInputStream；
     * content:// 继续走 ContentResolver。失败 resolve dataUrl=null，由前端强制清空旧封面。
     */
    private fun openArtworkInputStream(uri: Uri): InputStream? {
        return when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path
                if (path.isNullOrBlank()) {
                    null
                } else {
                    val file = File(path)
                    if (file.isFile && file.canRead()) FileInputStream(file) else null
                }
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }

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
}

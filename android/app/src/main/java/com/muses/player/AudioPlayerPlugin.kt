package com.muses.player

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import com.getcapacitor.JSObject
import com.getcapacitor.MessageHandler
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
import okhttp3.Request

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
    private companion object {
        /** 后台自动切歌轮询间隔 */
        const val AUTO_NEXT_POLL_MS = 1000L
        /** 静音防抖窗口：JS 正常切歌 preload 通常在 complete 后数百 ms 内发起 */
        const val AUTO_NEXT_DEBOUNCE_MS = 2500L
        /** 预案触发后的起播确认超时：WebDAV 远程缓冲可能较慢，放宽到 15s */
        const val AUTO_NEXT_VERIFY_MS = 15000L
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .readTimeout(0L, TimeUnit.MILLISECONDS) // 渐进下载可能持续整首歌
            .build()
    }

    /** 封面下载专用：短超时，避免挂死在线匹配 */
    private val coverHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10_000L, TimeUnit.MILLISECONDS)
            .readTimeout(15_000L, TimeUnit.MILLISECONDS)
            .callTimeout(20_000L, TimeUnit.MILLISECONDS)
            .build()
    }

    private val audioCache by lazy { WebDavAudioCache(context, httpClient) }

    private val activeProgressiveCancel = AtomicReference<(() -> Unit)?>(null)
    private val activeBufferSongId = AtomicReference<String?>(null)

    // --------------- 后台自动切歌预案（JS 冻结兜底） ---------------

    private data class AutoNextPlan(
        val songId: String,
        val assetId: String,
        val assetPath: String,
        val isUrl: Boolean,
        val username: String?,
        val password: String?,
        val volume: Double,
        val currentAssetId: String?,
    )

    private data class ResolvedAutoNext(val assetPath: String, val isUrl: Boolean, val remoteUrl: Boolean)

    private var autoNextPlan: AutoNextPlan? = null
    /** 预案已触发、等待 isMusicActive 确认成功；成功发 autoNextStarted，超时发 autoNextFailed */
    private var verifyingPlan: AutoNextPlan? = null
    private var verifyingStartedAt = 0L
    /** JS 上报的期望播放状态；false（暂停/停止）时轮询不触发预案 */
    private var jsExpectedPlaying = false
    /** 防抖起点；0 = 无待确认的静音窗口 */
    private var pendingAutoNextAt = 0L
    private var autoNextHandler: Handler? = null
    private var autoNextTickPosted = false

    override fun load() {
        super.load()
        startAutoNextLoop()
    }

    private fun startAutoNextLoop() {
        if (autoNextHandler != null) {
            return
        }
        autoNextHandler = Handler(Looper.getMainLooper())
        scheduleAutoNextTick()
    }

    private fun scheduleAutoNextTick() {
        val handler = autoNextHandler ?: return
        if (autoNextTickPosted) {
            return
        }
        autoNextTickPosted = true
        handler.postDelayed(
            {
                autoNextTickPosted = false
                tickAutoNext()
            },
            AUTO_NEXT_POLL_MS,
        )
    }

    private fun isMusicActive(): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isMusicActive == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 后台兜底轮询：
     * - 预案已触发（verifying）：isMusicActive 恢复 = 起播成功；超时仍静音 = 失败。
     * - 预案待命且 JS 期望播放：静音进入 2.5s 防抖（给 JS 正常切歌留窗口），仍静音则执行预案。
     * - JS 活跃时正常切歌会让 isMusicActive 恢复或预案被更新，防抖自然重置。
     */
    private fun tickAutoNext() {
        try {
            val now = SystemClock.elapsedRealtime()
            val verifying = verifyingPlan
            if (verifying != null) {
                if (isMusicActive()) {
                    notifyListeners("autoNextStarted", JSObject().put("songId", verifying.songId))
                    verifyingPlan = null
                    verifyingStartedAt = 0L
                } else if (now - verifyingStartedAt >= AUTO_NEXT_VERIFY_MS) {
                    notifyListeners(
                        "autoNextFailed",
                        JSObject().put("songId", verifying.songId).put("reason", "startFailed"),
                    )
                    verifyingPlan = null
                    verifyingStartedAt = 0L
                }
            } else {
                val plan = autoNextPlan
                if (plan != null && jsExpectedPlaying) {
                    if (isMusicActive()) {
                        pendingAutoNextAt = 0L
                    } else {
                        if (pendingAutoNextAt == 0L) {
                            pendingAutoNextAt = now
                        } else if (now - pendingAutoNextAt >= AUTO_NEXT_DEBOUNCE_MS) {
                            pendingAutoNextAt = 0L
                            executeAutoNext(plan)
                        }
                    }
                } else {
                    pendingAutoNextAt = 0L
                }
            }
        } catch (_: Exception) {
            // 轮询异常不打断播放
        }
        scheduleAutoNextTick()
    }

    /** 解析预案播放路径：content:// 拷贝到私有缓存；WebDAV 优先完整缓存，否则远程直链。 */
    private fun resolveAutoNextAsset(plan: AutoNextPlan): ResolvedAutoNext? {
        return try {
            when {
                plan.assetPath.startsWith("content://") -> {
                    val copied = copyContentUriToPlaybackCache(Uri.parse(plan.assetPath), plan.songId)
                    ResolvedAutoNext(copied, true, false)
                }
                plan.username != null &&
                    plan.password != null &&
                    (plan.assetPath.startsWith("http://") || plan.assetPath.startsWith("https://")) -> {
                    val cached = audioCache.getCachedFile(plan.assetPath)
                    if (cached != null) {
                        ResolvedAutoNext(Uri.fromFile(cached).toString(), true, false)
                    } else {
                        ResolvedAutoNext(plan.assetPath, true, true)
                    }
                }
                else -> ResolvedAutoNext(plan.assetPath, plan.isUrl, false)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun executeAutoNext(plan: AutoNextPlan) {
        autoNextPlan = null
        pendingAutoNextAt = 0L

        val resolved = resolveAutoNextAsset(plan)
        if (resolved == null) {
            notifyListeners("autoNextFailed", JSObject().put("songId", plan.songId).put("reason", "resolveFailed"))
            return
        }

        val headers = JSObject()
        if (resolved.remoteUrl && plan.username != null && plan.password != null) {
            headers.put("Authorization", "Basic ${encodeBasicAuth(plan.username, plan.password)}")
        }

        // 顺序调用 capgo 插件：preload（同 asset 已存在时 capgo reject，随后 play 复用现有 asset）→ play
        callNativeAudio(
            "preload",
            JSObject().apply {
                put("assetId", plan.assetId)
                put("assetPath", resolved.assetPath)
                put("isUrl", resolved.isUrl)
                put("audioChannelNum", 1)
                put("volume", plan.volume)
                if (headers.length() > 0) {
                    put("headers", headers)
                }
            },
        )
        callNativeAudio(
            "play",
            JSObject().apply {
                put("assetId", plan.assetId)
                put("volume", plan.volume)
            },
        )
        // 旧 asset 回收：先播后卸；单曲循环（同 assetId）跳过
        if (plan.currentAssetId != null && plan.currentAssetId != plan.assetId) {
            callNativeAudio("unload", JSObject().put("assetId", plan.currentAssetId))
        }

        // 进入验证态：下一轮 tick 用 isMusicActive 确认起播
        verifyingPlan = plan
        verifyingStartedAt = SystemClock.elapsedRealtime()
    }

    /**
     * 原生侧调用 capgo 插件公共方法（@PluginMethod）。
     * 通过反射读取 Bridge.msgHandler 构造 PluginCall（Capacitor 内部字段，异常时静默降级）。
     */
    private fun callNativeAudio(method: String, data: JSObject) {
        try {
            val bridge = getBridge() ?: return
            val msgHandlerField = bridge.javaClass.getDeclaredField("msgHandler")
            msgHandlerField.isAccessible = true
            val msgHandler = msgHandlerField.get(bridge) as? MessageHandler ?: return
            val call = PluginCall(msgHandler, "NativeAudio", PluginCall.CALLBACK_ID_DANGLING, method, data)
            bridge.callPluginMethod("NativeAudio", method, call)
        } catch (_: Exception) {
            // 桥接不可用：放弃预案，JS 心跳 / 回前台对账兜底
            autoNextPlan = null
            verifyingPlan = null
            verifyingStartedAt = 0L
            pendingAutoNextAt = 0L
        }
    }

    /**
     * JS 每次成功起播后注册「下一首预案」；预案更新说明 JS 活跃，切歌由 JS 主导。
     * 密码仅保存在内存，不写日志。
     */
    @PluginMethod
    fun setAutoNext(call: PluginCall) {
        val songId = call.getString("songId") ?: return call.resolve()
        val assetId = call.getString("assetId") ?: return call.resolve()
        val assetPath = call.getString("assetPath") ?: return call.resolve()
        val plan = AutoNextPlan(
            songId = songId,
            assetId = assetId,
            assetPath = assetPath,
            isUrl = call.getBoolean("isUrl", false) == true,
            username = call.getString("username"),
            password = call.getString("password"),
            volume = call.getDouble("volume", 1.0) ?: 1.0,
            currentAssetId = call.getString("currentAssetId"),
        )
        autoNextPlan = plan
        pendingAutoNextAt = 0L
        call.resolve()
    }

    /** 清空预案（停止播放 / 队列尾 / 无下一首）。 */
    @PluginMethod
    fun clearAutoNext(call: PluginCall) {
        autoNextPlan = null
        pendingAutoNextAt = 0L
        call.resolve()
    }

    /** JS 上报期望播放状态；非 playing（暂停/停止）时轮询不触发预案。 */
    @PluginMethod
    fun reportPlaybackStatus(call: PluginCall) {
        val status = call.getString("status")
        jsExpectedPlaying = status == "playing"
        if (!jsExpectedPlaying) {
            pendingAutoNextAt = 0L
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

    private fun encodeBasicAuth(username: String, password: String): String {
        return Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
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

    /**
     * 下载远程封面到 cache/covers/{sha256(cacheKey)}.jpg，返回 file:// URI。
     * 失败 resolve uri=null，不 reject，避免阻塞播放侧在线匹配。
     */
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
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Muses/1.0")
                    .get()
                    .build()
                coverHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        call.resolve(JSObject().put("uri", null as String?))
                        return@execute
                    }
                    // OkHttp 5：Response.body 为非空 ResponseBody
                    val body = response.body
                    // 限制封面体积，防止异常大响应占满缓存
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
                    // 简单魔数校验：JPEG / PNG / WebP / GIF
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

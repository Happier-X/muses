package com.muses.player.core.webdav

import com.muses.player.core.data.log.ErrorLogStore
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** WebDAV 条目（PROPFIND depth 1 返回的单个文件/目录） */
data class WebDavItem(
    val name: String,
    val url: String,
    val isDirectory: Boolean,
    val contentLength: Long = 0L,
    val lastModified: String? = null,
    val eTag: String? = null,
)

/** WebDAV 认证失败异常 */
class WebDavAuthException(message: String) : Exception(message)

/** WebDAV 请求失败异常 */
class WebDavRequestException(val code: Int, message: String) : Exception(message)

/**
 * WebDAV 客户端接口。
 *
 * 移植自旧工程 WebDavPlugin 的 PROPFIND/GET/PUT/DELETE/MOVE + Basic Auth，
 * 阶段 2 完整实现。
 */
interface WebDavClient {
    /** 设置 Basic Auth 凭据（内存持有，不持久化密码到网络层） */
    fun authenticate(username: String, password: String)

    /** 校验服务端连通性（PROPFIND depth 0） */
    suspend fun probe(baseUrl: String): Boolean

    /** PROPFIND depth 1 列出目录内容 */
    suspend fun list(url: String): List<WebDavItem>

    /** 下载文件到本地 */
    suspend fun get(url: String, dest: File): File

    /** 上传文件 */
    suspend fun put(url: String, source: File)

    /** 删除远程文件/目录 */
    suspend fun delete(url: String)

    /** 移动/重命名 */
    suspend fun move(source: String, dest: String)

    /** 下载文件内容为字符串（用于 sidecar 歌词等文本文件） */
    suspend fun getString(url: String): String?
}

internal class OkHttpWebDavClient constructor(
    private val httpClient: OkHttpClient,
    private val authRegistry: WebDavAuthRegistry,
    private val rateLimiter: WebDavRateLimiter,
    private val errorLogStore: ErrorLogStore,
) : WebDavClient {

    /** 显式认证头（authenticate 设置）；空则回落 Registry 按 URL 前缀匹配（播放/懒扫描链路） */
    @Volatile
    private var authHeader: String? = null

    override fun authenticate(username: String, password: String) {
        // 显式 UTF-8：与 WebDavAuthRegistry 一致（OkHttp 默认 ISO-8859-1，非 ASCII 用户名双轨不一致）
        authHeader = Credentials.basic(username, password, Charsets.UTF_8)
    }

    /** 请求认证头：显式优先，否则经 [WebDavAuthRegistry] 按目标 URL 匹配音源凭据 */
    private fun effectiveAuthHeader(url: String): String? =
        authHeader ?: authRegistry.authorizationHeader(url)

    override suspend fun probe(baseUrl: String): Boolean {
        var attempt = 0
        while (true) {
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val body = PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE)
            val request = Request.Builder()
                .url(baseUrl)
                .method("PROPFIND", body)
                .header("Depth", "0")
                .header("Content-Type", "application/xml; charset=utf-8")
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(baseUrl)?.let { header("Authorization", it) } }
                .build()

            try {
                val response = withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }
                if (response.code == 429) {
                    val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                    response.close()
                    if (attempt >= 1) {
                        val ex = IOException("http 429")
                        errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "probe http 429 url=$baseUrl", ex)
                        return false
                    }
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "probe http 429 url=$baseUrl Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                    val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                    try {
                        delay(delayMs)
                    } catch (e: CancellationException) {
                        throw e
                    }
                    attempt++
                    continue
                }
                response.use { res ->
                    return res.code in 200..299
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return false
            }
        }
    }

    override suspend fun list(url: String): List<WebDavItem> {
        var attempt = 0
        while (true) {
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val body = PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", body)
                .header("Depth", "1")
                .header("Content-Type", "application/xml; charset=utf-8")
                .header("Accept", "application/xml, text/xml, */*")
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw e
            }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url", ex)
                    throw ex
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff ${min(retryAfterMs ?: DEFAULT_429_DELAY_MS, MAX_RETRY_AFTER_MS)}ms", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try {
                    delay(delayMs)
                } catch (e: CancellationException) {
                    throw e
                }
                attempt++
                continue
            }

            response.use { res ->
                when (res.code) {
                    401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${res.code}）")
                    in 200..299 -> {
                        val bytes = res.body.bytes()
                        val contentType = res.header("Content-Type")
                        return parsePropfindResponse(bytes, contentType, url)
                    }
                    else -> throw WebDavRequestException(res.code, "PROPFIND 失败（HTTP ${res.code}）")
                }
            }
        }
    }

    override suspend fun get(url: String, dest: File): File {
        var attempt = 0
        while (true) {
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val request = Request.Builder()
                .url(url)
                .get()
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url", ex)
                    throw ex
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try {
                    delay(delayMs)
                } catch (e: CancellationException) {
                    throw e
                }
                attempt++
                continue
            }

            response.use { res ->
                when (res.code) {
                    401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${res.code}）")
                    in 200..299 -> {
                        dest.parentFile?.mkdirs()
                        res.body.byteStream().use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        return dest
                    }
                    else -> throw WebDavRequestException(res.code, "下载失败（HTTP ${res.code}）")
                }
            }
        }
    }

    override suspend fun put(url: String, source: File) {
        var attempt = 0
        while (true) {
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val requestBody = source.readBytes().toRequestBody("application/octet-stream".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url", ex)
                    throw ex
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try { delay(delayMs) } catch (e: CancellationException) { throw e }
                attempt++
                continue
            }

            response.use { res ->
                when (res.code) {
                    401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${res.code}）")
                    !in 200..299 -> throw WebDavRequestException(res.code, "上传失败（HTTP ${res.code}）")
                    else -> return
                }
            }
        }
    }

    override suspend fun delete(url: String) {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val request = Request.Builder()
                .url(url)
                .delete()
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) { throw e }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url", ex)
                    throw ex
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try { delay(delayMs) } catch (e: CancellationException) { throw e }
                attempt++
                continue
            }

            response.use { res ->
                when (res.code) {
                    401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${res.code}）")
                    !in 200..299 -> throw WebDavRequestException(res.code, "删除失败（HTTP ${res.code}）")
                    else -> return
                }
            }
        }
    }

    override suspend fun move(source: String, dest: String) {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val request = Request.Builder()
                .url(source)
                .method("MOVE", null)
                .header("Destination", dest)
                .header("Overwrite", "T")
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(dest)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) { throw e }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$source -> $dest", ex)
                    throw ex
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$source -> $dest Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try { delay(delayMs) } catch (e: CancellationException) { throw e }
                attempt++
                continue
            }

            response.use { res ->
                when (res.code) {
                    401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${res.code}）")
                    !in 200..299 -> throw WebDavRequestException(res.code, "移动失败（HTTP ${res.code}）")
                    else -> return
                }
            }
        }
    }

    override suspend fun getString(url: String): String? {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "text/plain, */*")
                .header(RATE_LIMIT_MARKER, "1")
                .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
                .build()

            val response = try {
                withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return null
            }

            if (response.code == 429) {
                val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
                response.close()
                if (attempt >= 1) {
                    val ex = IOException("http 429")
                    errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url", ex)
                    // sidecar 场景静默回 null，不抛阻断懒扫描
                    return null
                }
                errorLogStore.log(ErrorLogStore.Level.WARN, "WebDavClient", "http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                val delayMs = (retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS)
                try { delay(delayMs) } catch (e: CancellationException) { throw e }
                attempt++
                continue
            }

            return response.use { res ->
                if (!res.isSuccessful) return@use null
                val bytes = res.body.bytes()
                if (bytes.isEmpty()) return@use null
                decodeResponseBody(bytes, res.header("Content-Type"))
                    .takeIf { it.isNotBlank() }
            }
        }
    }

    // ── PROPFIND XML 解析 ──────────────────────────────────────────

    /**
     * 解析 PROPFIND depth 1 响应。
     * 使用 XmlPullParser 逐事件解析，避免完整 DOM 构建。
     * 返回的列表只包含子项（depth 1），跳过目录自身（depth 0）。
     */
    private fun parsePropfindResponse(
        bytes: ByteArray,
        contentType: String?,
        requestUrl: String,
    ): List<WebDavItem> {
        val text = decodeResponseBody(bytes, contentType)
        val items = mutableListOf<WebDavItem>()
        val baseUrl = normalizeUrl(requestUrl)

        val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(text.reader())

        // 跟踪当前 response 块内的属性
        var currentHref: String? = null
        var currentIsDirectory = false
        var currentContentLength = 0L
        var currentLastModified: String? = null
        var currentETag: String? = null

        // 标签状态追踪
        var inResponse = false
        var inHref = false
        var inProp = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    val localTag = parser.name.lowercase().substringAfterLast(':')
                    currentTag = localTag
                    when {
                        localTag == "response" -> {
                            inResponse = true
                            currentHref = null
                            currentIsDirectory = false
                            currentContentLength = 0L
                            currentLastModified = null
                            currentETag = null
                        }
                        localTag == "href" && inResponse -> inHref = true
                        localTag == "prop" && inResponse -> inProp = true
                        localTag == "collection" && inProp -> currentIsDirectory = true
                    }
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> {
                    val textContent = parser.text?.trim() ?: ""
                    if (textContent.isEmpty()) {
                        eventType = parser.next()
                        continue
                    }
                    when {
                        inHref && inResponse -> currentHref = textContent
                        inProp && inResponse -> {
                            when (currentTag) {
                                "getcontentlength" -> currentContentLength = textContent.toLongOrNull() ?: 0L
                                "getlastmodified" -> currentLastModified = textContent
                                "getetag" -> currentETag = textContent
                            }
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val localTag = parser.name.lowercase().substringAfterLast(':')
                    when {
                        localTag == "href" -> inHref = false
                        localTag == "prop" -> inProp = false
                        localTag == "response" -> {
                            inResponse = false
                            currentHref?.let { rawHref ->
                                val name = extractNameFromHref(rawHref)
                                if (name.isNotEmpty() && name != ".") {
                                    val resolvedUrl = resolveHref(rawHref, baseUrl)
                                    // 跳过目录自身（depth 0 条目 = 请求 URL 本身）
                                    if (resolvedUrl.trimEnd('/') != baseUrl.trimEnd('/')) {
                                        items.add(
                                            WebDavItem(
                                                name = name,
                                                url = resolvedUrl,
                                                isDirectory = currentIsDirectory,
                                                contentLength = currentContentLength,
                                                lastModified = currentLastModified,
                                                eTag = currentETag,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return items
    }

    private fun extractNameFromHref(href: String): String {
        val decoded = runCatching {
            java.net.URLDecoder.decode(href, "UTF-8")
        }.getOrDefault(href)
        return decoded.trimEnd('/').substringAfterLast('/')
    }

    private fun normalizeUrl(url: String): String {
        return url.trimEnd('/')
    }

    private fun resolveHref(href: String, baseUrl: String): String {
        val decoded = runCatching {
            java.net.URLDecoder.decode(href, "UTF-8")
        }.getOrDefault(href)

        return when {
            decoded.startsWith("http://") || decoded.startsWith("https://") -> decoded
            // 绝对路径：从 baseUrl 的 scheme+host+port 构造完整 URL
            decoded.startsWith("/") -> {
                val scheme = baseUrl.substringBefore("://")
                val hostPort = baseUrl.substringAfter("://").substringBefore("/")
                "$scheme://$hostPort$decoded"
            }
            else -> "$baseUrl/$decoded"
        }
    }

    private fun decodeResponseBody(bytes: ByteArray, contentType: String?): String {
        if (bytes.isEmpty()) return ""

        val headerCharset = parseHeaderCharset(contentType)
        if (headerCharset != null) {
            return String(bytes, headerCharset)
        }

        val utf8Text = String(bytes, StandardCharsets.UTF_8)
        val xmlCharset = XML_ENCODING_PATTERN.find(utf8Text)
            ?.groupValues?.getOrNull(1)
            ?.trim('\'', '"')
            ?.takeIf { it.isNotBlank() }
            ?.let { name -> runCatching { java.nio.charset.Charset.forName(name) }.getOrNull() }

        if (xmlCharset != null && xmlCharset != StandardCharsets.UTF_8) {
            return String(bytes, xmlCharset)
        }

        if (utf8Text.contains('\uFFFD')) {
            return runCatching { String(bytes, java.nio.charset.Charset.forName("GBK")) }
                .getOrDefault(utf8Text)
        }

        return utf8Text
    }

    private fun parseHeaderCharset(contentType: String?): java.nio.charset.Charset? {
        if (contentType.isNullOrBlank()) return null
        return CHARSET_PATTERN.find(contentType)
            ?.groupValues?.getOrNull(1)
            ?.let { name -> runCatching { java.nio.charset.Charset.forName(name.trim()) }.getOrNull() }
    }

    private companion object {
        const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8" ?><d:propfind xmlns:d="DAV:"><d:allprop /></d:propfind>"""
        val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
        val CHARSET_PATTERN = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
        val XML_ENCODING_PATTERN = Regex("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        /** 内部标记头：已在协程层限流的请求告知 OkHttp 层跳过二次限流 */
        const val RATE_LIMIT_MARKER = "X-Muses-Rate-Limited"

        /** 429 退避上限 8s（对齐 ScrapeHttp） */
        const val MAX_RETRY_AFTER_MS: Long = 8000L

        /** 无 Retry-After 时默认退避 1s */
        const val DEFAULT_429_DELAY_MS: Long = 1000L

        /**
         * 解析 Retry-After 头（秒数或 HTTP-date）。
         * 复用 ScrapeHttp.parseRetryAfterMs 语义，copy 至本类避免跨模块依赖 core:scrape。
         */
        fun parseRetryAfterMs(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim()
            trimmed.toLongOrNull()?.let { sec ->
                return (sec * 1000L).coerceAtLeast(0L)
            }
            return try {
                val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                val dateTime = ZonedDateTime.parse(trimmed, formatter)
                val diff = dateTime.toInstant().toEpochMilli() - System.currentTimeMillis()
                diff.coerceAtLeast(0L)
            } catch (_: DateTimeParseException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
}

package com.muses.player.core.webdav

import com.muses.player.core.data.log.ErrorLogStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
 *
 * P2c 说明：传输层由 OkHttp 换为 Ktor-client（CIO）；`get/put` 的 `java.io.File`
 * 签名保持不变（`WebDavAudioCache`/写回链路留守 Android，见 R3 偏离记录），
 * 搬入 commonMain 待 core:data 仓库 KMP 化后再议。
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

/**
 * P2c Ktor 实现（原 OkHttpWebDavClient 逐方法平移，契约冻结）：
 * - 429 骨架：acquire → 执行 → 解析 Retry-After → delay(min,8s) → 重试 1 次 → 二次抛 `http 429`；
 * - 401/403 → WebDavAuthException；其他非 2xx → WebDavRequestException（文案逐字保留）；
 * - 内部 `X-Muses-Rate-Limited` marker 头不再外发（Ktor 请求不经过 OkHttp 拦截器链，
 *   旧 marker 仅用于 OkHttp 层跳过二次限流，外发会污染服务端）。
 */
internal class KtorWebDavClient constructor(
    private val httpClient: HttpClient = defaultWebDavHttpClient(),
    private val authRegistry: WebDavAuthRegistry,
    private val rateLimiter: WebDavRateLimiter,
    private val errorLogStore: ErrorLogStore,
) : WebDavClient {

    /** 显式认证头（authenticate 设置）；空则回落 Registry 按 URL 前缀匹配（播放/懒扫描链路） */
    @Volatile
    private var authHeader: String? = null

    override fun authenticate(username: String, password: String) {
        // 显式 UTF-8：与 WebDavAuthRegistry 一致（非 ASCII 用户名双轨一致）
        authHeader = basicHeader(username, password)
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
            val response = try {
                httpClient.request(encodeUrl(baseUrl)) {
                    method = HttpMethod("PROPFIND")
                    header("Depth", "0")
                    effectiveAuthHeader(baseUrl)?.let { header("Authorization", it) }
                    // P2c-fix：contentType()+setBody(String) 会被 CIO 以 Content-Length: 0 空发
                    // （OpenList 回 400）；改 ByteArrayContent 自带类型（同 put）。
                    setBody(ByteArrayContent(PROPFIND_BODY.toByteArray(Charsets.UTF_8), ContentType.parse("application/xml; charset=utf-8")))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return false
            }
            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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
            runCatching { response.bodyAsText() }
            return response.status.value in 200..299
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
            val response = try {
                httpClient.request(encodeUrl(url)) {
                    method = HttpMethod("PROPFIND")
                    header("Depth", "1")
                    header("Accept", "application/xml, text/xml, */*")
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                    // P2c-fix：同上，ByteArrayContent 保 body 必达。
                    setBody(ByteArrayContent(PROPFIND_BODY.toByteArray(Charsets.UTF_8), ContentType.parse("application/xml; charset=utf-8")))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            when (response.status.value) {
                401, 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                in 200..299 -> {
                    val bytes = response.readRawBytes()
                    return parsePropfindResponse(bytes, response.headers["Content-Type"], url)
                }
                else -> {
                    // P2c-diag 临时：记录 400 响应体与请求指纹定位 OpenList 拒因（脱敏：Authorization 只记有无）
                    val respBody = runCatching { response.bodyAsText().take(500) }.getOrDefault("<unreadable>")
                    errorLogStore.log(
                        ErrorLogStore.Level.ERROR, "WebDavDiag",
                        "PROPFIND url=$url depth=1 hasAuth=${response.call.request.headers["Authorization"] != null} " +
                            "respContentType=${response.headers["Content-Type"]} respBody=$respBody", null,
                    )
                    throw WebDavRequestException(response.status.value, "PROPFIND 失败（HTTP ${response.status.value}）")
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
            val response = try {
                httpClient.get(encodeUrl(url)) {
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            when (response.status.value) {
                401, 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                in 200..299 -> {
                    dest.parentFile?.mkdirs()
                    streamToFile(response, dest)
                    return dest
                }
                else -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavRequestException(response.status.value, "下载失败（HTTP ${response.status.value}）")
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
            val response = try {
                httpClient.request(encodeUrl(url)) {
                    method = HttpMethod.Put
                    // ByteArrayContent 自带 contentType：避免 contentType()+setBody(ByteArray)
                    // 被 DefaultTransform 二次包裹（MockEngine 下断言体时需精确类型）
                    setBody(ByteArrayContent(source.readBytes(), ContentType.Application.OctetStream))
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            when (response.status.value) {
                401, 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                !in 200..299 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavRequestException(response.status.value, "上传失败（HTTP ${response.status.value}）")
                }
                else -> {
                    runCatching { response.bodyAsText() }
                    return
                }
            }
        }
    }

    override suspend fun delete(url: String) {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val response = try {
                httpClient.delete(encodeUrl(url)) {
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) { throw e }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            when (response.status.value) {
                401, 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                !in 200..299 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavRequestException(response.status.value, "删除失败（HTTP ${response.status.value}）")
                }
                else -> {
                    runCatching { response.bodyAsText() }
                    return
                }
            }
        }
    }

    override suspend fun move(source: String, dest: String) {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val response = try {
                httpClient.request(encodeUrl(source)) {
                    method = HttpMethod("MOVE")
                    header("Destination", encodeUrl(dest))
                    header("Overwrite", "T")
                    effectiveAuthHeader(dest)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) { throw e }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            when (response.status.value) {
                401, 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                !in 200..299 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavRequestException(response.status.value, "移动失败（HTTP ${response.status.value}）")
                }
                else -> {
                    runCatching { response.bodyAsText() }
                    return
                }
            }
        }
    }

    override suspend fun getString(url: String): String? {
        var attempt = 0
        while (true) {
            try { rateLimiter.acquire() } catch (e: CancellationException) { throw e }
            val response = try {
                httpClient.get(encodeUrl(url)) {
                    header("Accept", "text/plain, */*")
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return null
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
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

            if (!response.status.isSuccess()) return null
            val bytes = response.readRawBytes()
            if (bytes.isEmpty()) return null
            return decodeResponseBody(bytes, response.headers["Content-Type"])
                .takeIf { it.isNotBlank() }
        }
    }

    /** 流式落盘（原 `byteStream.copyTo` 语义；大音频文件不全量进内存）。 */
    private suspend fun streamToFile(response: HttpResponse, dest: File) {
        val channel = response.bodyAsChannel()
        dest.outputStream().use { out ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (!channel.isClosedForRead) {
                val n = channel.readAvailable(buf, 0, buf.size)
                if (n == -1) break
                out.write(buf, 0, n)
            }
        }
    }

    // ── PROPFIND XML 解析 ──────────────────────────────────────────

    /**
     * 解析 PROPFIND depth 1 响应（P2c：XmlPullParser → 手写 multistatus 解析，
     * S0 结论：xmlutil 1.0.x 与 Kotlin 2.4.10 搭配未经验证 + 本形状极窄，手写零依赖更稳）。
     *
     * 语义冻结：命名空间前缀容忍（`d:` 或无）、标签大小写不敏感、只取子项跳过目录自身（depth 0）、
     * href 百分号解码 + XML 转义还原、`<collection/>` 判目录、缺字段回退零值。
     * 返回的列表只包含子项（depth 1），跳过目录自身（depth 0）。
     */
    internal fun parsePropfindResponse(
        bytes: ByteArray,
        contentType: String?,
        requestUrl: String,
    ): List<WebDavItem> {
        val text = decodeResponseBody(bytes, contentType)
        val items = mutableListOf<WebDavItem>()
        val baseUrl = normalizeUrl(requestUrl)

        for (block in RESPONSE_BLOCK.findAll(text)) {
            val inner = block.groupValues[1]
            val rawHref = HREF_VALUE.find(inner)?.groupValues?.get(1)?.trim()?.let(::unescapeXml)?.trim()
            if (rawHref.isNullOrEmpty()) continue
            val isDirectory = COLLECTION_TAG.containsMatchIn(inner)
            val contentLength =
                PROP_TEXT("getcontentlength").find(inner)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
            val lastModified =
                PROP_TEXT("getlastmodified").find(inner)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            val eTag =
                PROP_TEXT("getetag").find(inner)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

            val name = extractNameFromHref(rawHref)
            if (name.isEmpty() || name == ".") continue
            val resolvedUrl = resolveHref(rawHref, baseUrl)
            // 跳过目录自身（depth 0 条目 = 请求 URL 本身）
            if (resolvedUrl.trimEnd('/') == baseUrl.trimEnd('/')) continue
            items.add(
                WebDavItem(
                    name = name,
                    url = resolvedUrl,
                    isDirectory = isDirectory,
                    contentLength = contentLength,
                    lastModified = lastModified,
                    eTag = eTag,
                ),
            )
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

    companion object {
        private const val HEX_DIGITS = "0123456789ABCDEF"

        /**
         * P2c-fix：OkHttp 会把 URL 中的非 ASCII 百分号编码后发送，Ktor 原样发 UTF-8
         * 原字节（OpenList 回 400）。此处对齐 OkHttp：仅编码 >127 的码点（UTF-8 %XX 大写），
         * 已有 %XX 原样保留防 double-encode；凭据匹配/解析仍用原始 url，不受影响。
         */
        internal fun encodeUrl(raw: String): String {
            if (raw.none { it.code > 127 }) return raw
            val out = StringBuilder(raw.length)
            var i = 0
            while (i < raw.length) {
                val cp = Character.codePointAt(raw, i)
                if (cp > 127) {
                    for (b in String(Character.toChars(cp)).toByteArray(Charsets.UTF_8)) {
                        out.append('%').append(HEX_DIGITS[(b.toInt() ushr 4) and 0xF])
                            .append(HEX_DIGITS[b.toInt() and 0xF])
                    }
                } else {
                    out.append(raw[i])
                }
                i += Character.charCount(cp)
            }
            return out.toString()
        }
        const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8" ?><d:propfind xmlns:d="DAV:"><d:allprop /></d:propfind>"""
        val CHARSET_PATTERN = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
        val XML_ENCODING_PATTERN = Regex("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        /** 429 退避上限 8s（对齐 ScrapeHttp） */
        const val MAX_RETRY_AFTER_MS: Long = 8000L

        /** 无 Retry-After 时默认退避 1s */
        const val DEFAULT_429_DELAY_MS: Long = 1000L

        // ── 手写 multistatus 解析：标签名前缀/大小写容忍 ──
        private val RESPONSE_BLOCK =
            Regex("<(?:\\w+:)?response\\b[^>]*>(.*?)</(?:\\w+:)?response>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val HREF_VALUE =
            Regex("<(?:\\w+:)?href\\b[^>]*>(.*?)</(?:\\w+:)?href>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val COLLECTION_TAG =
            Regex("<(?:\\w+:)?collection\\b\\s*/?>", RegexOption.IGNORE_CASE)
        private fun PROP_TEXT(local: String) =
            Regex("<(?:\\w+:)?$local\\b[^>]*>(.*?)</(?:\\w+:)?$local>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        /** XmlPullParser 原生做的实体还原（href 含 & / 中文转义时必须；含数字引用，还原语义与原生对等）。 */
        private fun unescapeXml(value: String): String =
            value.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace(NUMERIC_REF_DEC) {
                    runCatching { it.groupValues[1].toInt().toChar().toString() }.getOrDefault(it.value)
                }
                .replace(NUMERIC_REF_HEX) {
                    runCatching { it.groupValues[1].toInt(16).toChar().toString() }.getOrDefault(it.value)
                }

        private val NUMERIC_REF_DEC = Regex("&#(\\d+);")
        private val NUMERIC_REF_HEX = Regex("&#x([0-9a-fA-F]+);")

        /**
         * Basic 认证头（原 `Credentials.basic(user, pass, UTF_8)` 语义的纯函数版，
         * P2c 去 okhttp3：`Basic base64(user:pass)`，显式 UTF-8）。
         */
        @OptIn(ExperimentalEncodingApi::class)
        fun basicHeader(username: String?, password: String): String {
            val credential = "${username ?: ""}:$password".toByteArray(Charsets.UTF_8)
            return "Basic " + Base64.encode(credential)
        }

        /** 默认 CIO 客户端（原 NAS 超时 15 连接/30 读/60 写对齐：socket 覆盖读写双向 inactivity）。 */
        fun defaultWebDavHttpClient(): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 30_000L
            }
        }

        /**
         * 解析 Retry-After 头（秒数或 HTTP-date）。
         * 复用 ScrapeHttp.parseRetryAfterMs 语义，copy 至本类避免跨模块依赖 core:scrape
         * （:core:scrape 反向依赖 :core:webdav，引用即成环）。
         * P2c：HTTP-date 改手动纯算术解析，与 common 侧实现逐字节一致。
         */
        fun parseRetryAfterMs(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim()
            trimmed.toLongOrNull()?.let { sec ->
                return (sec * 1000L).coerceAtLeast(0L)
            }
            return parseHttpDateToEpochMs(trimmed)?.let { epochMs ->
                (epochMs - System.currentTimeMillis()).coerceAtLeast(0L)
            }
        }

        private val HTTP_DATE_PATTERN =
            // 日允许 1-2 位（java.time RFC_1123 输出天数不补零，如 "Fri, 4 Sep 2026"）
            Regex("""^[A-Za-z]{3}, (\d{1,2}) ([A-Za-z]{3}) (\d{4}) (\d{2}):(\d{2}):(\d{2}) (GMT|UTC|[+-]\d{4})$""")

        private val MONTHS = mapOf(
            "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
            "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12,
        )

        /** RFC1123 HTTP-date → epoch 毫秒；格式不符返回 null（调用方回退默认延迟）。 */
        private fun parseHttpDateToEpochMs(value: String): Long? {
            val match = HTTP_DATE_PATTERN.matchEntire(value.trim()) ?: return null
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val month = MONTHS[match.groupValues[2]] ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: return null
            val hour = match.groupValues[4].toIntOrNull() ?: return null
            val minute = match.groupValues[5].toIntOrNull() ?: return null
            val second = match.groupValues[6].toIntOrNull() ?: return null
            if (day !in 1..31 || hour > 23 || minute > 59 || second > 60) return null
            val days = daysSinceEpoch(year, month, day) ?: return null
            val zoneMs = parseZoneOffsetMs(match.groupValues[7]) ?: return null
            return days * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1000L - zoneMs
        }

        /** GMT/UTC → 0；±HHMM → 对应毫秒偏移（与 common 侧 ScrapeHttp 逐字节一致）。 */
        private fun parseZoneOffsetMs(zone: String): Long? {
            if (zone == "GMT" || zone == "UTC") return 0L
            if (zone.length != 5) return null
            val sign = when (zone[0]) {
                '+' -> 1L
                '-' -> -1L
                else -> return null
            }
            val hh = zone.substring(1, 3).toIntOrNull() ?: return null
            val mm = zone.substring(3, 5).toIntOrNull() ?: return null
            if (hh > 23 || mm > 59) return null
            return sign * (hh * 3_600_000L + mm * 60_000L)
        }

        /** Howard Hinnant days_from_civil（纯算术，无平台库依赖）。 */
        private fun daysSinceEpoch(y: Int, m: Int, d: Int): Long? {
            if (m !in 1..12 || y < 1970 || y > 2100) return null
            val yAdj = if (m <= 2) y - 1 else y
            val era = (if (yAdj >= 0) yAdj else yAdj - 399) / 400
            val yoe = yAdj - era * 400
            val mp = (m + 9) % 12
            val doy = (153 * mp + 2) / 5 + d - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097L + doe - 719468L
        }
    }
}

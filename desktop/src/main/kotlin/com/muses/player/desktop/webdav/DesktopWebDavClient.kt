package com.muses.player.desktop.webdav

import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavAuthException
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import com.muses.player.core.webdav.WebDavRateLimiter
import com.muses.player.core.webdav.WebDavRequestException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * 桌面 WebDAV 客户端（W4 桌面装配，任务 09-05-scrape-kmp R2「桌面复用或对齐实现」）。
 *
 * `:core:webdav` 是安卓库形态（KtorWebDavClient 留守），桌面 JVM 无法消费；
 * 本实现按 commonMain [WebDavClient] 接口对齐其契约（语义冻结，请求骨架逐方法对齐）：
 * - 429：每请求前 `rateLimiter.acquire()`（共享 4 rps 桶）→ 解析 Retry-After（秒/HTTP-date，
 *   复用 commonMain [ScrapeHttp.parseRetryAfterMs]）→ delay(min(计算值, 8s)) → 重试 1 次 →
 *   二次 429 抛 `IOException("http 429")` 并 `ErrorLogStore.log(WARN)`；
 * - 401/403 → [WebDavAuthException]；其他非 2xx → [WebDavRequestException]（文案逐字保留）；
 * - PROPFIND 解析：手写 multistatus 正则（前缀/大小写容忍、只取子项、href 解码、
 *   `<collection/>` 判目录、数字实体还原、charset 嗅探 + GBK 兜底），与 KtorWebDavClient 一致；
 * - URL 编码：仅对 >127 码点做 UTF-8 %XX 大写（防 double-encode，对齐 OkHttp 行为）；
 * - Basic 认证：显式 UTF-8（`DesktopWebDavBrowseLoader.basicHeader` 同语义）。
 *
 * 浏览页 [DesktopWebDavBrowseLoader] 的请求/解析逻辑已收敛至本实现（`parseEntries` 转发）。
 */
class DesktopWebDavClient(
    private val httpClient: HttpClient = defaultDesktopWebDavHttpClient(),
    private val rateLimiter: WebDavRateLimiter = WebDavRateLimiter(),
    private val errorLogStore: ErrorLogStore? = null,
) : WebDavClient {

    /** 显式认证头（authenticate 设置）；空则回落 registry 按 URL 前缀匹配（预留，桌面当前无 registry） */
    @Volatile
    private var authHeader: String? = null

    /** 可选 URL 前缀 → Basic 头回退表（多音源场景显式 authenticate 之外的兜底；最长前缀匹配） */
    @Volatile
    private var registryHeaders: List<Pair<String, String>> = emptyList()

    override fun authenticate(username: String, password: String) {
        authHeader = basicHeader(username, password)
    }

    /** 按 URL 前缀注册凭据（懒扫描/写回链多音源兜底，对齐 WebDavAuthRegistry.refresh 语义的轻量版） */
    fun registerCredentials(baseUrl: String, username: String?, password: String) {
        synchronized(this) {
            val header = basicHeader(username, password)
            val next = registryHeaders.filterNot { it.first == baseUrl } + (baseUrl to header)
            registryHeaders = next.sortedByDescending { it.first.length }
        }
    }

    /** 请求认证头：显式优先，否则按目标 URL 最长前缀匹配 */
    private fun effectiveAuthHeader(url: String): String? =
        authHeader ?: registryHeaders.firstOrNull { url.startsWith(it.first) }?.second

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
                    // ByteArrayContent 自带类型：contentType()+setBody(String) 会被 CIO 以
                    // Content-Length: 0 空发（OpenList 回 400），对齐 KtorWebDavClient 的 P2c-fix
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
                    warn("probe http 429 url=$baseUrl", IOException("http 429"))
                    return false
                }
                warn("probe http 429 url=$baseUrl Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
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
                    setBody(ByteArrayContent(PROPFIND_BODY.toByteArray(Charsets.UTF_8), ContentType.parse("application/xml; charset=utf-8")))
                }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
                if (attempt >= 1) {
                    warn("http 429 url=$url", IOException("http 429"))
                    throw IOException("http 429")
                }
                warn("http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
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
                    runCatching { response.bodyAsText() }
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
                    warn("http 429 url=$url", IOException("http 429"))
                    throw IOException("http 429")
                }
                warn("http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
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
                    // ByteArrayContent 自带 contentType：避免 DefaultTransform 二次包裹
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
                    warn("http 429 url=$url", IOException("http 429"))
                    throw IOException("http 429")
                }
                warn("http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
                } catch (e: CancellationException) {
                    throw e
                }
                attempt++
                continue
            }

            when {
                response.status.value == 401 || response.status.value == 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                response.status.value !in 200..299 -> {
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
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val response = try {
                httpClient.delete(encodeUrl(url)) {
                    effectiveAuthHeader(url)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
                if (attempt >= 1) {
                    warn("http 429 url=$url", IOException("http 429"))
                    throw IOException("http 429")
                }
                warn("http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
                } catch (e: CancellationException) {
                    throw e
                }
                attempt++
                continue
            }

            when {
                response.status.value == 401 || response.status.value == 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                response.status.value !in 200..299 -> {
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
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            val response = try {
                httpClient.request(encodeUrl(source)) {
                    method = HttpMethod("MOVE")
                    header("Destination", encodeUrl(dest))
                    header("Overwrite", "T")
                    effectiveAuthHeader(dest)?.let { header("Authorization", it) }
                }
            } catch (e: CancellationException) {
                throw e
            }

            if (response.status.value == 429) {
                val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"])
                runCatching { response.bodyAsText() }
                if (attempt >= 1) {
                    warn("http 429 url=$source -> $dest", IOException("http 429"))
                    throw IOException("http 429")
                }
                warn("http 429 url=$source -> $dest Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
                } catch (e: CancellationException) {
                    throw e
                }
                attempt++
                continue
            }

            when {
                response.status.value == 401 || response.status.value == 403 -> {
                    runCatching { response.bodyAsText() }
                    throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.status.value}）")
                }
                response.status.value !in 200..299 -> {
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
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
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
                    // sidecar 场景静默回 null，不抛阻断（对齐 KtorWebDavClient.getString）
                    warn("http 429 url=$url", IOException("http 429"))
                    return null
                }
                warn("http 429 url=$url Retry-After=${retryAfterMs ?: "null"} -> backoff", null)
                try {
                    delay((retryAfterMs ?: DEFAULT_429_DELAY_MS).coerceIn(0L, MAX_RETRY_AFTER_MS))
                } catch (e: CancellationException) {
                    throw e
                }
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

    // ── 内部工具 ──────────────────────────────────────────────

    private fun warn(message: String, e: Throwable?) {
        try {
            errorLogStore?.log(ErrorLogStore.Level.WARN, "WebDavClient", message, e)
        } catch (_: Exception) {
            // 日志失败不阻断请求链路
        }
    }

    /** 流式落盘（大音频文件不全量进内存，对齐 KtorWebDavClient.streamToFile）。 */
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

    // ── PROPFIND XML 解析（与 KtorWebDavClient.parsePropfindResponse 同语义）──

    /**
     * 解析 PROPFIND depth 1 响应：命名空间前缀容忍、标签大小写不敏感、只取子项跳过自身、
     * href 百分号解码 + XML 转义还原、`<collection/>` 判目录、缺字段回退零值。
     */
    internal fun parsePropfindResponse(
        bytes: ByteArray,
        contentType: String?,
        requestUrl: String,
    ): List<WebDavItem> {
        val text = decodeResponseBody(bytes, contentType)
        val items = mutableListOf<WebDavItem>()
        val baseUrl = requestUrl.trimEnd('/')

        for (block in RESPONSE_BLOCK.findAll(text)) {
            val inner = block.groupValues[1]
            val rawHref = HREF_VALUE.find(inner)?.groupValues?.get(1)?.trim()?.let(::unescapeXml)?.trim()
            if (rawHref.isNullOrEmpty()) continue
            val isDirectory = COLLECTION_TAG.containsMatchIn(inner)
            val contentLength =
                propText("getcontentlength").find(inner)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
            val lastModified =
                propText("getlastmodified").find(inner)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            val eTag =
                propText("getetag").find(inner)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

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
        val decoded = runCatching { java.net.URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        return decoded.trimEnd('/').substringAfterLast('/')
    }

    private fun resolveHref(href: String, baseUrl: String): String {
        val decoded = runCatching { java.net.URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        return when {
            decoded.startsWith("http://") || decoded.startsWith("https://") -> decoded
            decoded.startsWith("/") -> {
                val scheme = baseUrl.substringBefore("://")
                val hostPort = baseUrl.substringAfter("://").substringBefore('/')
                "$scheme://$hostPort$decoded"
            }
            else -> "$baseUrl/$decoded"
        }
    }

    private fun decodeResponseBody(bytes: ByteArray, contentType: String?): String {
        if (bytes.isEmpty()) return ""

        parseHeaderCharset(contentType)?.let { return String(bytes, it) }

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
        private const val PROPFIND_BODY =
            """<?xml version="1.0" encoding="utf-8" ?><d:propfind xmlns:d="DAV:"><d:allprop /></d:propfind>"""
        private val CHARSET_PATTERN = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
        private val XML_ENCODING_PATTERN = Regex("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        /** 429 退避上限 8s（对齐 ScrapeHttp / KtorWebDavClient） */
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
        private fun propText(local: String) =
            Regex("<(?:\\w+:)?$local\\b[^>]*>(.*?)</(?:\\w+:)?$local>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        /** XmlPullParser 原生做的实体还原（含数字引用，还原语义与原生对等）。 */
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

        /** Basic 认证头：`Basic base64(user:pass)`，显式 UTF-8（对齐 KtorWebDavClient.basicHeader）。 */
        @OptIn(ExperimentalEncodingApi::class)
        fun basicHeader(username: String?, password: String): String {
            val credential = "${username ?: ""}:$password".toByteArray(Charsets.UTF_8)
            return "Basic " + Base64.encode(credential)
        }

        /** 默认 CIO 客户端（NAS 超时 15 连接/30 读，对齐 KtorWebDavClient.defaultWebDavHttpClient）。 */
        fun defaultDesktopWebDavHttpClient(): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 30_000L
            }
        }

        /**
         * Retry-After 头解析（秒数或 HTTP-date）：复用 commonMain [ScrapeHttp.parseRetryAfterMs]
         * （public，双端一致），不再第三份 copy。
         */
        fun parseRetryAfterMs(value: String?): Long? = ScrapeHttp.parseRetryAfterMs(value)

        /**
         * URL 编码：仅对 >127 的码点做 UTF-8 %XX 大写（已有 %XX 原样保留防 double-encode），
         * 对齐 KtorWebDavClient.encodeUrl（OkHttp 发送行为）。
         */
        fun encodeUrl(raw: String): String {
            if (raw.none { it.code > 127 }) return raw
            val hexDigits = "0123456789ABCDEF"
            val out = StringBuilder(raw.length)
            var i = 0
            while (i < raw.length) {
                val cp = Character.codePointAt(raw, i)
                if (cp > 127) {
                    for (b in String(Character.toChars(cp)).toByteArray(Charsets.UTF_8)) {
                        out.append('%').append(hexDigits[(b.toInt() ushr 4) and 0xF])
                            .append(hexDigits[b.toInt() and 0xF])
                    }
                } else {
                    out.append(raw[i])
                }
                i += Character.charCount(cp)
            }
            return out.toString()
        }
    }
}

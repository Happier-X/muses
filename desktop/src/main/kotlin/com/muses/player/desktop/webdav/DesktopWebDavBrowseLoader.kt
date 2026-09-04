package com.muses.player.desktop.webdav

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import java.net.URLDecoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 桌面 WebDAV 目录浏览加载器（浏览页共用化的桌面业务侧）。
 *
 * 约束：
 * - 不依赖 `:core:webdav`（该模块是安卓库形态，桌面 JVM 无法消费）；
 * - PROPFIND 解析语义与安卓 `KtorWebDavClient.parsePropfindResponse` 对齐：
 *   前缀/大小写容忍、只取子项跳过自身、href 解码、`<collection/>` 判目录；
 * - 路径工具（normalize/parent）与 `core:webdav.WebDavUtils` 同语义，桌面侧本地实现避免跨模块依赖。
 */
object DesktopWebDavBrowseLoader {

    /** 桌面浏览条目（调用方映射为共用 `WebDavBrowseItem`）。 */
    data class Entry(
        val name: String,
        val url: String,
        val isDirectory: Boolean,
        val contentLength: Long = 0L,
        val lastModified: String? = null,
    )

    private const val PROPFIND_BODY =
        """<?xml version="1.0" encoding="utf-8" ?><d:propfind xmlns:d="DAV:"><d:allprop /></d:propfind>"""

    private val RESPONSE_BLOCK =
        Regex("<(?:\\w+:)?response\\b[^>]*>(.*?)</(?:\\w+:)?response>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val HREF_VALUE =
        Regex("<(?:\\w+:)?href\\b[^>]*>(.*?)</(?:\\w+:)?href>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val COLLECTION_TAG =
        Regex("<(?:\\w+:)?collection\\b\\s*/?>", RegexOption.IGNORE_CASE)

    private fun propText(local: String) =
        Regex("<(?:\\w+:)?$local\\b[^>]*>(.*?)</(?:\\w+:)?$local>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    @Volatile
    private var httpClient: HttpClient? = null

    private fun client(): HttpClient =
        httpClient ?: synchronized(this) {
            httpClient ?: HttpClient(CIO).also { httpClient = it }
        }

    /** PROPFIND depth 1 列出目录内容（仅返回目录项，调用方无需二次过滤）。 */
    suspend fun listDirectories(
        serverUrl: String,
        path: String,
        username: String,
        password: String,
    ): List<Entry> {
        val url = buildUrl(serverUrl, path)
        val response = client().request(url) {
            method = HttpMethod("PROPFIND")
            header("Depth", "1")
            header("Accept", "application/xml, text/xml, */*")
            header("Authorization", basicHeader(username, password))
            setBody(ByteArrayContent(PROPFIND_BODY.toByteArray(Charsets.UTF_8), ContentType.parse("application/xml; charset=utf-8")))
        }
        when (response.status.value) {
            401, 403 -> throw IllegalStateException("WebDAV 认证失败（HTTP ${response.status.value}）")
            in 200..299 -> Unit
            else -> throw IllegalStateException("PROPFIND 失败（HTTP ${response.status.value}）")
        }
        val bytes = response.readRawBytes()
        return parseEntries(String(bytes, Charsets.UTF_8), url)
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
    }

    internal fun parseEntries(text: String, requestUrl: String): List<Entry> {
        val items = mutableListOf<Entry>()
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
            val name = extractName(rawHref)
            if (name.isEmpty() || name == ".") continue
            val resolvedUrl = resolveHref(rawHref, baseUrl)
            if (resolvedUrl.trimEnd('/') == baseUrl.trimEnd('/')) continue
            items.add(
                Entry(
                    name = name,
                    url = resolvedUrl,
                    isDirectory = isDirectory,
                    contentLength = contentLength,
                    lastModified = lastModified,
                ),
            )
        }
        return items
    }

    private fun extractName(href: String): String {
        val decoded = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        return decoded.trimEnd('/').substringAfterLast('/')
    }

    private fun resolveHref(href: String, baseUrl: String): String {
        val decoded = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
        return when {
            decoded.startsWith("http://") || decoded.startsWith("https://") -> decoded
            decoded.startsWith("/") -> {
                val scheme = baseUrl.substringBefore("://")
                val hostPort = baseUrl.substringAfter("://").substringBefore("/")
                "$scheme://$hostPort$decoded"
            }
            else -> "$baseUrl/$decoded"
        }
    }

    private fun unescapeXml(value: String): String =
        value.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

    @OptIn(ExperimentalEncodingApi::class)
    private fun basicHeader(username: String?, password: String): String {
        val credential = "${username ?: ""}:$password".toByteArray(Charsets.UTF_8)
        return "Basic " + Base64.encode(credential)
    }

    /** 与 `core:webdav.buildWebDavUrl` 同语义（server 去尾斜杠 + 编码 path）。 */
    fun buildUrl(serverUrl: String, path: String): String {
        val trimmedServer = serverUrl.trim().trimEnd('/')
        return "$trimmedServer${encodePath(normalizePath(path))}"
    }

    /** 与 `core:webdav.normalizeWebDavPath` 同语义。 */
    fun normalizePath(path: String): String {
        val raw = if (path.isBlank()) "/" else path.trim()
        val withLeading = if (withLeading(raw)) raw else "/$raw"
        val collapsed = withLeading.replace(Regex("/+"), "/")
        return if (collapsed.length > 1 && collapsed.endsWith("/")) collapsed.dropLast(1) else collapsed
    }

    private fun withLeading(raw: String) = raw.startsWith("/")

    /** 与 `core:webdav.getParentWebDavPath` 同语义（根返回 null）。 */
    fun parentPath(path: String): String? {
        val normalized = normalizePath(path)
        if (normalized == "/") return null
        val parent = normalized.substring(0, normalized.lastIndexOf('/'))
        return parent.ifEmpty { "/" }
    }

    private fun encodePath(path: String): String {
        if (path == "/") return "/"
        val encoded = path.split("/").filter { it.isNotEmpty() }
            .joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        return "/$encoded"
    }
}

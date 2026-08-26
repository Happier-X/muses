package com.muses.player.core.webdav

import android.util.Base64
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
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

@Singleton
internal class OkHttpWebDavClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val authRegistry: WebDavAuthRegistry,
) : WebDavClient {

    /** 显式认证头（authenticate 设置）；空则回落 Registry 按 URL 前缀匹配（播放/懒扫描链路） */
    @Volatile
    private var authHeader: String? = null

    override fun authenticate(username: String, password: String) {
        authHeader = Credentials.basic(username, password)
    }

    /** 请求认证头：显式优先，否则经 [WebDavAuthRegistry] 按目标 URL 匹配音源凭据 */
    private fun effectiveAuthHeader(url: String): String? =
        authHeader ?: authRegistry.authorizationHeader(url)

    override suspend fun probe(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE)
            val request = Request.Builder()
                .url(baseUrl)
                .method("PROPFIND", body)
                .header("Depth", "0")
                .header("Content-Type", "application/xml; charset=utf-8")
                .apply { effectiveAuthHeader(baseUrl)?.let { header("Authorization", it) } }
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.code in 200..299
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun list(url: String): List<WebDavItem> = withContext(Dispatchers.IO) {
        val body = PROPFIND_BODY.toRequestBody(XML_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", body)
            .header("Depth", "1")
            .header("Content-Type", "application/xml; charset=utf-8")
            .header("Accept", "application/xml, text/xml, */*")
            .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
            .build()

        httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.code}）")
                in 200..299 -> {
                    val bytes = response.body.bytes()
                    val contentType = response.header("Content-Type")
                    parsePropfindResponse(bytes, contentType, url)
                }
                else -> throw WebDavRequestException(response.code, "PROPFIND 失败（HTTP ${response.code}）")
            }
        }
    }

    override suspend fun get(url: String, dest: File): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
            .build()

        httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.code}）")
                in 200..299 -> {
                    dest.parentFile?.mkdirs()
                    response.body.byteStream().use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    dest
                }
                else -> throw WebDavRequestException(response.code, "下载失败（HTTP ${response.code}）")
            }
        }
    }

    override suspend fun put(url: String, source: File) = withContext(Dispatchers.IO) {
        val requestBody = source.readBytes().toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
            .build()

        httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.code}）")
                !in 200..299 -> throw WebDavRequestException(response.code, "上传失败（HTTP ${response.code}）")
            }
        }
    }

    override suspend fun delete(url: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .delete()
            .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
            .build()

        httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.code}）")
                !in 200..299 -> throw WebDavRequestException(response.code, "删除失败（HTTP ${response.code}）")
            }
        }
    }

    override suspend fun move(source: String, dest: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(source)
            .method("MOVE", null)
            .header("Destination", dest)
            .header("Overwrite", "T")
            .apply { effectiveAuthHeader(dest)?.let { header("Authorization", it) } }
            .build()

        httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw WebDavAuthException("WebDAV 认证失败（HTTP ${response.code}）")
                !in 200..299 -> throw WebDavRequestException(response.code, "移动失败（HTTP ${response.code}）")
            }
        }
    }

    override suspend fun getString(url: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "text/plain, */*")
            .apply { effectiveAuthHeader(url)?.let { header("Authorization", it) } }
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.body.bytes()
                if (bytes.isEmpty()) return@use null
                decodeResponseBody(bytes, response.header("Content-Type"))
                    .takeIf { it.isNotBlank() }
            }
        }.getOrNull()
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
    }
}

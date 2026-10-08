package com.muses.player.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/** 独立客户端，避免修改音乐音源的认证信息；不跨重定向发送同步凭据。 */
class WebDavSyncTransport(
    private val client: OkHttpClient = OkHttpClient.Builder().followRedirects(false)
        .followSslRedirects(false).retryOnConnectionFailure(false)
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build(),
) {
    companion object {
        const val MAX_SNAPSHOT_BYTES = 20 * 1024 * 1024
        val DEVICE_FILE = Regex("device-([a-f0-9-]{36})\\.json")
    }

    data class RemoteFile(val name: String, val deviceId: String)
    data class Document(val body: String, val etag: String?)

    fun root(config: WebDavSyncConfig): HttpUrl {
        val base = config.url.trim().toHttpUrl()
        require(base.username.isEmpty() && base.password.isEmpty() && base.query == null && base.fragment == null) {
            "请填写不含账号、参数或片段的 WebDAV 地址"
        }
        val segments = config.directory.trim().trim('/').split('/')
        require(segments.isNotEmpty() && segments.all { it.isNotBlank() && it != "." && it != ".." && '\\' !in it && '%' !in it }) {
            "请输入有效的同步目录"
        }
        val builder = base.newBuilder()
        if (!base.encodedPath.endsWith('/')) builder.addPathSegment("")
        segments.forEach { builder.addPathSegment(it) }
        return builder.addPathSegment("").build()
    }

    private fun request(url: HttpUrl, config: WebDavSyncConfig, password: String) = Request.Builder()
        .url(url).header("Authorization", Credentials.basic(config.username, password, Charsets.UTF_8))

    private fun check(code: Int) {
        when (code) {
            401, 403 -> error("WebDAV 认证或权限不足，请检查账号和应用密码")
            409 -> error("同步目录的上级目录不存在，请先创建上级目录")
            412 -> error("远端数据已变化，请重新同步")
            429 -> error("WebDAV 请求过于频繁，请稍后重试")
            in 300..399 -> error("WebDAV 地址发生重定向，请填写最终地址")
            !in 200..299 -> error("WebDAV 请求失败（HTTP $code）")
        }
    }

    suspend fun list(config: WebDavSyncConfig, password: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val root = root(config)
        client.newCall(request(root, config, password).method("MKCOL", ByteArray(0).toRequestBody()).build()).execute().use {
            if (it.code != 405) check(it.code)
        }
        val xml = client.newCall(request(root, config, password).header("Depth", "1")
            .method("PROPFIND", """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>""".toRequestBody("application/xml".toMediaType())).build())
            .execute().use { response -> check(response.code); readLimited(response, 1024 * 1024) }
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true)) { "WebDAV 目录响应格式异常" }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; isExpandEntityReferences = false }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val nodes = doc.getElementsByTagNameNS("DAV:", "href")
        val result = (0 until nodes.length).mapNotNull { index ->
            val url = root.resolve(nodes.item(index).textContent) ?: return@mapNotNull null
            if (url.scheme != root.scheme || url.host != root.host || url.port != root.port ||
                url.pathSegments.dropLast(1) != root.pathSegments.dropLast(1)) return@mapNotNull null
            val name = url.pathSegments.last()
            val match = DEVICE_FILE.matchEntire(name) ?: return@mapNotNull null
            RemoteFile(name, match.groupValues[1])
        }.distinctBy { it.deviceId }
        require(result.size <= 100) { "同步设备文件过多，请检查同步目录" }
        result
    }

    suspend fun read(config: WebDavSyncConfig, password: String, name: String): Document? = withContext(Dispatchers.IO) {
        require(DEVICE_FILE.matches(name))
        client.newCall(request(root(config).newBuilder().addPathSegment(name).build(), config, password).get().build()).execute().use {
            if (it.code == 404) null else {
                check(it.code)
                Document(readLimited(it, MAX_SNAPSHOT_BYTES), it.header("ETag"))
            }
        }
    }

    suspend fun write(config: WebDavSyncConfig, password: String, name: String, body: String, old: Document?) = withContext(Dispatchers.IO) {
        require(DEVICE_FILE.matches(name))
        require(body.toByteArray().size <= MAX_SNAPSHOT_BYTES) { "同步数据过大" }
        val builder = request(root(config).newBuilder().addPathSegment(name).build(), config, password)
        if (old == null) builder.header("If-None-Match", "*")
        else old.etag?.takeUnless { it.startsWith("W/") }?.let { builder.header("If-Match", it) }
        client.newCall(builder.put(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()).execute().use { check(it.code) }
        val saved = read(config, password, name) ?: error("尚未确认远端同步数据完整，请重试")
        check(saved.body == body) { "远端同步数据校验失败，请重试" }
    }

    private fun readLimited(response: okhttp3.Response, max: Int): String {
        val body = response.body
        require(body.contentLength() <= max) { "WebDAV 响应过大" }
        val out = ByteArrayOutputStream()
        body.byteStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(out.size() + count <= max) { "WebDAV 响应过大" }
                out.write(buffer, 0, count)
            }
        }
        return out.toString("UTF-8")
    }
}

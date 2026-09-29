package com.muses.player.desktop.playback

import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

/** 将带认证的 WebDAV 音频按请求转发给 VLC，使未缓存歌曲无需等待整曲下载。 */
internal class DesktopWebDavStreamProxy(private val cache: DesktopWebDavAudioCache) {
    private data class Stream(val token: String, val url: String, val authorization: String)

    private val active = AtomicReference<Stream?>(null)
    private val failureStatus = AtomicReference<Int?>(null)
    private var server: HttpServer? = null
    private var executor: ExecutorService? = null

    fun lastFailureStatus(): Int? = failureStatus.get()

    @Synchronized
    fun open(url: String, authorization: String): String {
        val http = server ?: HttpServer.create(
            InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0),
            0,
        ).also { created ->
            created.createContext("/audio") { exchange -> handle(exchange) }
            val workers = Executors.newCachedThreadPool { task ->
                Thread(task, "muses-webdav-stream").apply { isDaemon = true }
            }
            created.executor = workers
            executor = workers
            created.start()
            server = created
        }
        val stream = Stream(UUID.randomUUID().toString(), url, authorization)
        failureStatus.set(null)
        active.set(stream)
        return "http://127.0.0.1:${http.address.port}/audio/${stream.token}"
    }

    fun clear() {
        active.set(null)
        failureStatus.set(null)
    }

    @Synchronized
    fun close() {
        active.set(null)
        server?.stop(0)
        server = null
        executor?.shutdownNow()
        executor = null
    }

    private fun handle(exchange: HttpExchange) {
        val stream = active.get()
        if (stream == null || exchange.requestURI.path != "/audio/${stream.token}") {
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
            return
        }
        val method = exchange.requestMethod
        if (method != "GET" && method != "HEAD") {
            exchange.sendResponseHeaders(405, -1)
            exchange.close()
            return
        }
        var upstream: HttpURLConnection? = null
        var cacheFile: File? = null
        try {
            upstream = (URI(stream.url).toURL().openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Authorization", stream.authorization)
                exchange.requestHeaders.getFirst("Range")?.let { setRequestProperty("Range", it) }
                exchange.requestHeaders.getFirst("If-Range")?.let { setRequestProperty("If-Range", it) }
            }
            val status = upstream.responseCode
            if (status >= 400 && active.get() == stream) failureStatus.set(status)
            for (name in listOf("Content-Type", "Content-Range", "Accept-Ranges", "ETag", "Last-Modified")) {
                upstream.getHeaderField(name)?.let { exchange.responseHeaders.set(name, it) }
            }
            val length = upstream.getHeaderFieldLong("Content-Length", -1L)
            if (method == "HEAD" || status == 304 || status == 416 || status >= 400) {
                exchange.sendResponseHeaders(status, -1)
                return
            }
            exchange.sendResponseHeaders(status, if (length > 0) length else 0)
            val canCache = method == "GET" && status == 200 && length in 1..cache.maxCacheBytes()
                && exchange.requestHeaders.getFirst("Range") == null
            if (canCache) cacheFile = runCatching {
                File.createTempFile("muses-dav-stream-", ".partial")
            }.getOrNull()
            var copied = 0L
            upstream.inputStream.use { input ->
                exchange.responseBody.use { output ->
                    cacheFile?.outputStream().use { cacheOutput ->
                        val buffer = ByteArray(64 * 1024)
                        while (active.get() == stream) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            cacheOutput?.write(buffer, 0, count)
                            copied += count
                        }
                    }
                }
            }
            if (cacheFile != null && copied == length && active.get() == stream) {
                runCatching { cache.putToCache(stream.url, cacheFile) }
            }
        } catch (_: Exception) {
            // VLC 关闭旧连接或切歌时会主动断开；错误由 VLC 的 error 事件处理。
            if (active.get() == stream) failureStatus.set(502)
            runCatching { exchange.sendResponseHeaders(502, -1) }
        } finally {
            upstream?.disconnect()
            cacheFile?.delete()
            exchange.close()
        }
    }
}

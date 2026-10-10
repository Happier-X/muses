package com.muses.player.desktop.playback

import com.muses.player.desktop.cache.DesktopWebDavAudioCache
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopWebDavMetadataTest {
    @Test fun `远端版本改变或没有版本时不能复用旧音频`() {
        val directory = Files.createTempDirectory("dav-metadata-test").toFile()
        var etag: String? = "\"v1\""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/song.mp3") { exchange ->
            etag?.let { exchange.responseHeaders.set("ETag", it) }
            exchange.responseHeaders.set("Content-Length", "3")
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()
        val proxy = DesktopWebDavStreamProxy(DesktopWebDavAudioCache(directory))
        try {
            val url = "http://127.0.0.1:${server.address.port}/song.mp3"
            val cache = DesktopWebDavAudioCache(directory)
            val audio = java.io.File(directory, "original.mp3").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            cache.putToCache(url, audio, "\"v1\"")
            assertNotNull(proxy.validatedCache(url, "Basic test"))
            etag = "\"v2\""
            assertNull(proxy.validatedCache(url, "Basic test"))
            etag = null
            assertNull(proxy.validatedCache(url, "Basic test"))
            // 核对失效不删除已保存的音频副本，离线仍可使用。
            assertTrue(cache.getCachedFile(url)?.isFile == true)
        } finally { proxy.close(); server.stop(0); directory.deleteRecursively() }
    }
}

package com.muses.player.core.lxsdk.store

import com.muses.player.core.lxsdk.LxException
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.lxsdk.LxScriptEngine
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GdApiScriptTest {
    private suspend fun withEngine(
        response: (HttpRequestData) -> String,
        block: suspend (LxScriptEngine) -> Unit,
    ) {
        val script = BundledLxScripts.load().single { it.id == "builtin-gd-api" }
        val client = HttpClient(MockEngine { request ->
            respond(response(request), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val engine = LxScriptEngine(script.source, LxCryptoJvm(), LxHttpClient(client))
        try {
            engine.load()
            block(engine)
        } finally {
            engine.close()
            client.close()
        }
    }

    @Test fun `各平台原曲ID及所选音质正确映射到官方参数`() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        withEngine({ request ->
            requests += request
            val bitrate = when (request.url.parameters["br"]) {
                "740" -> "918"
                "999" -> "2317"
                else -> request.url.parameters["br"]
            }
            """{"url":"https://cdn.test/audio","br":$bitrate}"""
        }) { engine ->
            val platforms = mapOf("wy" to "netease", "kw" to "kuwo", "tx" to "tencent")
            val qualities = mapOf(LxQuality.Q_128K to "128", LxQuality.Q_192K to "192",
                LxQuality.Q_320K to "320", LxQuality.FLAC to "740", LxQuality.FLAC_24BIT to "999")
            for ((platform, source) in platforms) {
                for ((quality, bitrate) in qualities) {
                    assertEquals("https://cdn.test/audio",
                        engine.getMusicUrl(platform, quality, """{"songmid":"1&2"}""").url)
                    val url = requests.last().url
                    assertEquals("music-api.gdstudio.xyz", url.host)
                    assertEquals("/api.php", url.encodedPath)
                    assertEquals("url", url.parameters["types"])
                    assertEquals(source, url.parameters["source"])
                    assertEquals("1&2", url.parameters["id"])
                    assertEquals(bitrate, url.parameters["br"])
                }
            }
        }
        assertEquals(15, requests.size)
    }

    @Test fun `接口自动降档或缺少音质时拒绝结果且不在脚本内重试`() = runBlocking {
        for (body in listOf(
            """{"url":"https://cdn.test/audio.mp3","br":320}""",
            """{"url":"https://cdn.test/audio.flac"}""",
        )) {
            var requests = 0
            withEngine({ requests++; body }) { engine ->
                assertFailsWith<LxException> { engine.getMusicUrl("wy", LxQuality.FLAC, """{"id":1}""") }
            }
            assertEquals(1, requests)
        }
    }

    @Test fun `歌词及封面使用各自ID且保留译文`() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        withEngine({ request ->
            requests += request
            if (request.url.parameters["types"] == "lyric")
                """{"lyric":"[00:01.00]原文","tlyric":"[00:01.00]译文"}"""
            else """{"url":"https://cdn.test/cover.jpg"}"""
        }) { engine ->
            val info = """{"songmid":"1","lyric_id":"2","pic_id":"3"}"""
            val lyrics = engine.getLyric("wy", info)
            assertEquals("[00:01.00]原文", lyrics.lyric)
            assertEquals("[00:01.00]译文", lyrics.tlyric)
            assertEquals("2", requests.last().url.parameters["id"])
            assertEquals("https://cdn.test/cover.jpg", engine.getPic("wy", info).url)
            assertEquals("3", requests.last().url.parameters["id"])
            assertEquals("500", requests.last().url.parameters["size"])
        }
    }

    @Test fun `错误页和缺少专辑图ID均不误取地址`() = runBlocking {
        var requests = 0
        withEngine({ requests++; "<html>https://cdn.test/错误页</html>" }) { engine ->
            assertFailsWith<LxException> { engine.getMusicUrl("wy", LxQuality.FLAC, """{"id":1}""") }
            assertFailsWith<LxException> { engine.getPic("wy", """{"id":1}""") }
        }
        assertEquals(1, requests)
    }

    @Test fun `请求预算耗尽后停止调用接口`() = runBlocking {
        var requests = 0
        withEngine({ requests++; """{"url":"https://cdn.test/audio","br":128}""" }) { engine ->
            repeat(50) { engine.getMusicUrl("wy", LxQuality.Q_128K, """{"id":1}""") }
            val failure = assertFailsWith<LxException> {
                engine.getMusicUrl("wy", LxQuality.Q_128K, """{"id":1}""")
            }
            assertTrue(failure.message.orEmpty().contains("频率"))
        }
        assertEquals(50, requests)
    }
}

package com.muses.player.core.network

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProviderRequestGateTest {
    @Test fun `文本与歌词跨域共用来源队列和间隔`() = runTest {
        val starts = mutableListOf<Long>()
        val gate = ProviderRequestGate(500) { testScheduler.currentTime }
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
            starts += testScheduler.currentTime
            delay(700)
            respond("成功")
            }
        }))
        try {
            val text = ScrapeHttp(client, WebDavRateLimiter.Unlimited, requestGate = gate)
            val lyrics = LyricsHttp(client, requestGate = gate)
            val first = async { text.getText("https://c.y.qq.com/search") }
            val second = async { lyrics.postText("https://u.y.qq.com/lyric", "{}") }
            assertEquals("成功", first.await())
            assertEquals("成功", second.await())
            assertEquals(listOf(0L, 700L), starts)
        } finally { client.close() }
    }

    @Test fun `不同来源可以并发且同源快速请求保持间隔`() = runTest {
        val gate = ProviderRequestGate(500) { testScheduler.currentTime }
        val starts = mutableListOf<Pair<String, Long>>()
        suspend fun request(url: String) = gate.withSource(url) {
            beforeRequest()
            starts += providerRequestKey(url) to testScheduler.currentTime
            delay(100)
        }
        val first = async { request("https://music.163.com/a") }
        val second = async { request("https://c.y.qq.com/a") }
        first.await(); second.await()
        request("https://interface3.music.163.com/b")
        assertEquals(listOf("wy" to 0L, "tx" to 0L, "wy" to 500L), starts)
    }

    @Test fun `429重试也遵守来源间隔`() = runTest {
        val starts = mutableListOf<Long>()
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
            starts += testScheduler.currentTime
            if (starts.size == 1) respond("限流", HttpStatusCode.TooManyRequests, headersOf("Retry-After", "0"))
            else respond("成功")
            }
        }))
        try {
            val text = ScrapeHttp(client, WebDavRateLimiter.Unlimited,
                requestGate = ProviderRequestGate(500) { testScheduler.currentTime })
            assertEquals("成功", text.getText("https://music.163.com/a"))
            assertEquals(listOf(0L, 500L), starts)
        } finally { client.close() }
    }

    @Test fun `取消排队不阻塞后续请求也不占用额外间隔`() = runTest {
        val gate = ProviderRequestGate(500) { testScheduler.currentTime }
        val release = CompletableDeferred<Unit>()
        val first = launch { gate.withSource("https://music.163.com/a") { beforeRequest(); release.await() } }
        runCurrent()
        val canceled = launch { gate.withSource("https://music.163.com/b") { beforeRequest(); error("不应执行") } }
        runCurrent()
        canceled.cancel(); canceled.join()
        val third = async { gate.withSource("https://music.163.com/c") { beforeRequest(); testScheduler.currentTime } }
        release.complete(Unit)
        first.join()
        assertEquals(500L, third.await())
    }

    @Test fun `MusicBrainz即使通用间隔更短也遵守每秒一次`() = runTest {
        val gate = ProviderRequestGate(0) { testScheduler.currentTime }
        val starts = mutableListOf<Long>()
        repeat(3) {
            gate.withSource("https://musicbrainz.org/ws/2/recording/?query=$it") {
                beforeRequest(); starts += testScheduler.currentTime
            }
        }
        assertEquals(listOf(0L, 1000L, 2000L), starts)
    }

    @Test fun `域名分组覆盖图片域名且不误合并相似域名`() {
        assertEquals("wy", providerRequestKey("https://p1.music.126.net/cover.jpg"))
        assertEquals("tx", providerRequestKey("https://y.gtimg.cn/cover.jpg"))
        assertEquals("gdstudio", providerRequestKey("https://music-api.gdstudio.xyz/api.php"))
        assertTrue(providerRequestKey("https://music.163.com.example.org/a") != "wy")
    }
}

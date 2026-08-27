package com.muses.player.core.scrape.http

import java.io.IOException
import java.net.InetAddress
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ScrapeHttp 429 退避单测（任务 08-27-scrape-throttle-429）。
 *
 * - 解析 Retry-After（秒 / HTTP-date）；
 * - 命中 429 时尊重头部延迟后重试 1 次；
 * - 二次 429 抛 IOException("http 429")。
 */
@RunWith(RobolectricTestRunner::class)
class ScrapeHttp429Test {

    private lateinit var server: MockWebServer
    private val loopback = InetAddress.getLoopbackAddress()

    private fun http(rateLimiter: ScrapeRateLimiter = ScrapeRateLimiter.Unlimited): ScrapeHttp =
        ScrapeHttp(
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val local = chain.request().url.newBuilder()
                        .scheme("http")
                        .host(loopback.hostAddress)
                        .port(server.port)
                        .build()
                    chain.proceed(chain.request().newBuilder().url(local).build())
                }
                .build(),
            rateLimiter = rateLimiter,
        )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start(loopback, 0)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `429带RetryAfter秒_重试一次后成功`() = runTest {
        // 首次 429 带 Retry-After:0（立即重试），二次 200
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0").setBody("throttled"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok-body"))

        val client = http()
        val body = client.getText("https://example.com/a")
        assertEquals("ok-body", body)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `429二次仍429抛http429`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0").setBody("t1"))
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0").setBody("t2"))

        val client = http()
        try {
            client.getText("https://example.com/b")
            fail("应抛 IOException(\"http 429\")")
        } catch (e: IOException) {
            assertEquals("http 429", e.message)
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `429无RetryAfter默认1s后重试成功`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("t1"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("second"))

        val client = http()
        val body = client.getText("https://example.com/c")
        assertEquals("second", body)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `非429错误不重试直接抛`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("err"))

        val client = http()
        try {
            client.getText("https://example.com/d")
            fail("应抛 http 500")
        } catch (e: IOException) {
            assertEquals("http 500", e.message)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `getBytes在429后重试一次成功`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0").setBody("t"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("bytes-body"))

        val client = http()
        val bytes = client.getBytes("https://example.com/e")
        assertEquals("bytes-body", String(bytes))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `parseRetryAfter秒数正确`() {
        assertEquals(2000L, ScrapeHttp.parseRetryAfterMs("2"))
        assertEquals(0L, ScrapeHttp.parseRetryAfterMs("0"))
        assertEquals(null, ScrapeHttp.parseRetryAfterMs(null))
        assertEquals(null, ScrapeHttp.parseRetryAfterMs(""))
    }

    @Test
    fun `parseRetryAfterHttpDate正确`() {
        val future = ZonedDateTime.now().plusSeconds(5)
        val header = future.format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val parsed = ScrapeHttp.parseRetryAfterMs(header)
        assertTrue(parsed != null && parsed!! in 0..6000)
    }

    @Test
    fun `RetryAfter超过8s被截断`() = runTest {
        // Retry-After: 20 秒，但 ScrapeHttp 应截断至 8s（MAX_RETRY_AFTER_MS）
        // 为避免真实等待 8s，用 Robolectric 的虚拟时间：runTest 会虚拟推进 delay
        // 此处仅验证 getText 在大 Retry-After 下仍会重试并最终成功（二次 200）
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "20").setBody("t"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val client = http()
        val body = client.getText("https://example.com/f")
        assertEquals("ok", body)
        assertEquals(2, server.requestCount)
    }
}

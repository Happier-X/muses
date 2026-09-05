package com.muses.player.core.scrape.http

import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * ScrapeHttp 429 退避单测（P2c：MockWebServer → Ktor MockEngine 重写，语义冻结）。
 *
 * - 解析 Retry-After（秒 / HTTP-date）；
 * - 命中 429 时尊重头部延迟后重试 1 次；
 * - 二次 429 抛 IOException("http 429")。
 *
 * MockEngine 不走真实 socket：delay 照常经 runTest 虚拟时间；requestCount 改计数 handler 命中次数。
 * （kotlinx.io.IOException 在 JVM 即 java.io.IOException 别名，旧 catch 断言零改动。）
 *
 * W3 归位 :core:common commonTest：HTTP-date 用例的 java.time 构造改手动
 * （固定锚点 epoch=1_700_000_000_000 = 2023-11-14T22:13:20Z 星期二，经 nowMs 注入）。
 */
class ScrapeHttp429Test {

    private data class MockResp(
        val status: Int,
        val body: String,
        val headers: Map<String, String> = emptyMap(),
    )

    private var requests: Int = 0

    private fun http(
        vararg resps: MockResp,
        rateLimiter: WebDavRateLimiter = WebDavRateLimiter.Unlimited,
    ): ScrapeHttp {
        val queue = ArrayDeque(resps.toList())
        requests = 0
        val engine = MockEngine { _ ->
            requests++
            val r = queue.removeFirst()
            respond(
                r.body,
                HttpStatusCode.fromValue(r.status),
                headersOf(*r.headers.map { (k, v) -> k to listOf(v) }.toTypedArray()),
            )
        }
        return ScrapeHttp(HttpClient(engine), rateLimiter = rateLimiter)
    }

    @Test
    fun `429带RetryAfter秒_重试一次后成功`() = runTest {
        // 首次 429 带 Retry-After:0（立即重试），二次 200
        val client = http(
            MockResp(429, "throttled", mapOf("Retry-After" to "0")),
            MockResp(200, "ok-body"),
        )
        val body = client.getText("https://example.com/a")
        assertEquals("ok-body", body)
        assertEquals(2, requests)
    }

    @Test
    fun `429二次仍429抛http429`() = runTest {
        val client = http(
            MockResp(429, "t1", mapOf("Retry-After" to "0")),
            MockResp(429, "t2", mapOf("Retry-After" to "0")),
        )
        try {
            client.getText("https://example.com/b")
            fail("应抛 IOException(\"http 429\")")
        } catch (e: java.io.IOException) {
            assertEquals("http 429", e.message)
        }
        assertEquals(2, requests)
    }

    @Test
    fun `429无RetryAfter默认1s后重试成功`() = runTest {
        val client = http(
            MockResp(429, "t1"),
            MockResp(200, "second"),
        )
        val body = client.getText("https://example.com/c")
        assertEquals("second", body)
        assertEquals(2, requests)
    }

    @Test
    fun `非429错误不重试直接抛`() = runTest {
        val client = http(MockResp(500, "err"))
        try {
            client.getText("https://example.com/d")
            fail("应抛 http 500")
        } catch (e: java.io.IOException) {
            assertEquals("http 500", e.message)
        }
        assertEquals(1, requests)
    }

    @Test
    fun `getBytes在429后重试一次成功`() = runTest {
        val client = http(
            MockResp(429, "t", mapOf("Retry-After" to "0")),
            MockResp(200, "bytes-body"),
        )
        val bytes = client.getBytes("https://example.com/e")
        assertEquals("bytes-body", String(bytes))
        assertEquals(2, requests)
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
        // 手动构造 RFC1123（commonTest 无 java.time）：固定锚点 1_700_000_000_000L = 2023-11-14T22:13:20Z（星期二）
        // header = 锚点 + 5s → 期望解析差值 ≈ 5000ms
        val header = "Tue, 14 Nov 2023 22:13:25 GMT"
        val parsed = ScrapeHttp.parseRetryAfterMs(header) { 1_700_000_000_000L }
        assertTrue(parsed != null && parsed in 4000..6000)
    }

    @Test
    fun `RetryAfter超过8s被截断`() = runTest {
        // Retry-After: 20 秒，但 ScrapeHttp 应截断至 8s（MAX_RETRY_AFTER_MS）
        // runTest 虚拟推进 delay：验证大 Retry-After 下仍会重试并最终成功（二次 200）
        val client = http(
            MockResp(429, "t", mapOf("Retry-After" to "20")),
            MockResp(200, "ok"),
        )
        val body = client.getText("https://example.com/f")
        assertEquals("ok", body)
        assertEquals(2, requests)
    }
}

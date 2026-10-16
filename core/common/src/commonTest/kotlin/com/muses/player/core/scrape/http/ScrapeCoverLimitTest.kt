package com.muses.player.core.scrape.http

import com.muses.player.core.network.ProviderRequestGate
import com.muses.player.core.scrape.writeback.HttpCoverBytesFetcher
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrapeCoverLimitTest {
    private fun http(client: HttpClient) = ScrapeHttp(client,
        rateLimiter = WebDavRateLimiter.Unlimited, requestGate = ProviderRequestGate(intervalMs = 0))

    @Test
    fun `声明超限时不等待响应体并按封面获取失败处理`() = runTest {
        val body = ByteChannel(autoFlush = true)
        val client = HttpClient(MockEngine {
            respond(body, HttpStatusCode.OK,
                headersOf("Content-Length", (ScrapeHttp.MAX_COVER_BYTES.toLong() + 1).toString()))
        })
        try {
            assertNull(HttpCoverBytesFetcher(http(client)).fetch("https://cover.test/large"))
            assertTrue(body.isClosedForRead)
        } finally { client.close(); body.cancel(null) }
    }

    @Test
    fun `无长度或声明偏小仍在实际超限时停止且不等待结束`() = runTest {
        for (length in listOf(null, "1")) {
            val body = ByteChannel(autoFlush = true)
            body.writeFully(byteArrayOf(1, 2, 3, 4, 5))
            val client = HttpClient(MockEngine {
                respond(body, HttpStatusCode.OK,
                    if (length == null) headersOf() else headersOf("Content-Length", length))
            })
            try {
                assertFailsWith<java.io.IOException> {
                    http(client).getBytes("https://cover.test/overflow", maxBytes = 4)
                }
                assertTrue(body.isClosedForRead)
            } finally { client.close(); body.cancel(null) }
        }
    }

    @Test
    fun `恰好上限与空响应正常返回`() = runTest {
        for (bytes in listOf(byteArrayOf(1, 2, 3, 4), byteArrayOf())) {
            val client = HttpClient(MockEngine { respond(ByteReadChannel(bytes), HttpStatusCode.OK) })
            try {
                assertTrue(bytes.contentEquals(http(client).getBytes("https://cover.test/exact", maxBytes = 4)))
            } finally { client.close() }
        }
    }

    @Test
    fun `错误响应不等待或读取无界错误正文`() = runTest {
        val body = ByteChannel(autoFlush = true)
        val client = HttpClient(MockEngine { respond(body, HttpStatusCode.InternalServerError) })
        try {
            val error = assertFailsWith<java.io.IOException> {
                http(client).getBytes("https://cover.test/error", maxBytes = 4)
            }
            assertEquals("http 500", error.message)
            assertTrue(body.isClosedForRead)
        } finally { client.close(); body.cancel(null) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `取消下载会释放响应和来源许可且不折叠为普通失败`() = runTest {
        val body = ByteChannel(autoFlush = true)
        var requests = 0
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                requests++
                if (requests == 1) respond(body, HttpStatusCode.OK)
                else respond(ByteReadChannel(byteArrayOf(7)), HttpStatusCode.OK)
            }
        }))
        try {
            val fetcher = HttpCoverBytesFetcher(http(client))
            val pending = async { fetcher.fetch("https://cover.test/cancel") }
            runCurrent()
            assertEquals(1, requests)
            pending.cancel()
            assertFailsWith<CancellationException> { pending.await() }
            assertTrue(body.isClosedForRead)
            assertTrue(byteArrayOf(7).contentEquals(fetcher.fetch("https://cover.test/next")))
            assertEquals(2, requests)
        } finally { client.close(); body.cancel(null) }
    }
}

package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.MatchConfidence
import com.muses.player.core.model.scrape.OnlineTextMatchFailReason
import com.muses.player.core.model.scrape.OnlineTextMatchResult
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.provider.KwProvider
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 规格 = src/features/metadata/match.ts matchOnlineTextMeta 主流程 + providers/kw.ts 解析
 * （P2c：kw 段 MockWebServer → Ktor MockEngine；纯 FakeProvider 段本就无需 Robolectric，
 * 整类去 Robolectric，MockEngine 按序应答，requestCount 改计数器）。
 *
 * W2 上收注：`com.muses.player.core.scrape.http.ScrapeRateLimiter`（core:webdav 限流器的
 * 兼容别名，本轮已删除）→ 直接引用 `com.muses.player.core.webdav.WebDavRateLimiter`，编译期等价。
 */
class TextMetaMatcherTest {

    // ── fake provider ───────────────────────────────────────

    private class FakeProvider(
        override val id: OnlineTextSource,
        val result: () -> TextMetaHit? = { null },
    ) : TextMetaProvider {
        var calls: Int = 0

        override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
            calls++
            return result()
        }
    }

    private fun query(
        songId: String = "s1",
        title: String = "Love Story",
        artist: String? = null,
        album: String? = null,
        path: String? = null,
    ) = OnlineTextQuery(songId = songId, title = title, artist = artist, album = album, path = path)

    @Test
    fun `title空白归no-match`() = runTest {
        val matcher = TextMetaMatcher(emptyList())
        val result = matcher.match(query(title = "   "))
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NO_MATCH), result)
    }

    @Test
    fun `无需补缺时not-needed早退`() = runTest {
        val provider = FakeProvider(OnlineTextSource.KW)
        val matcher = TextMetaMatcher(listOf(provider))
        // artist/album 齐备且非弱 title → not-needed
        val result = matcher.match(query(artist = "A", album = "B"))
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NOT_NEEDED), result)
        assertEquals(0, provider.calls)
    }

    @Test
    fun `链序逐源尝试_命中即返回且带来源与置信度`() = runTest {
        val kwMiss = FakeProvider(OnlineTextSource.KW)
        val txHit = FakeProvider(OnlineTextSource.TX) {
            TextMetaHit(title = "Love Story", artist = "Taylor Swift", source = OnlineTextSource.KG)
        }
        val wyNever = FakeProvider(OnlineTextSource.WY)
        val matcher = TextMetaMatcher(listOf(kwMiss, txHit, wyNever))

        val result = matcher.match(query())
        assertTrue(result is OnlineTextMatchResult.Ok)
        result as OnlineTextMatchResult.Ok
        // 来源改写为命中 provider 的 id
        assertEquals(OnlineTextSource.TX, result.hit.source)
        // title exact + artist 命中 → high
        assertEquals(MatchConfidence.HIGH, result.confidence)
        assertEquals(1, kwMiss.calls)
        assertEquals(1, txHit.calls)
        assertEquals(0, wyNever.calls)
    }

    @Test
    fun `命中但对缺口无用则继续下一源`() = runTest {
        // 查询已有 artist/album、弱 title，但 hit.title 不相关 → 不算有效命中
        val badHit = FakeProvider(OnlineTextSource.KW) {
            TextMetaHit(title = "Unrelated Song", artist = "X", source = OnlineTextSource.KW)
        }
        val goodHit = FakeProvider(OnlineTextSource.TX) {
            TextMetaHit(title = "Shape of You (Live)", artist = "Ed", source = OnlineTextSource.TX)
        }
        val matcher = TextMetaMatcher(listOf(badHit, goodHit))
        val result = matcher.match(
            query(
                title = "Shape of You",
                path = "/music/Shape of You.mp3",
                artist = "Ed Sheeran",
                album = "Divide",
            ),
        )
        // 第一个源命中但对缺口无用被跳过，第二个源命中后停止
        assertTrue(result is OnlineTextMatchResult.Ok)
        assertEquals(1, badHit.calls)
    }

    @Test
    fun `无命中写负缓存并归no-match`() = runTest {
        val miss = FakeProvider(OnlineTextSource.KW) {
            // artist/album 齐备 + 弱 title：不相关 hit.title 对缺口无用
            TextMetaHit(title = "Unrelated", artist = "Someone", source = OnlineTextSource.KW)
        }
        val matcher = TextMetaMatcher(listOf(miss))
        val q = query(title = "Shape of You", path = "/music/Shape of You.mp3", artist = "Ed Sheeran", album = "Divide")
        val first = matcher.match(q)
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NO_MATCH), first)
        assertEquals(1, matcher.negativeCache.size())

        // 同 songId + 同 queryKey 再次匹配：负缓存短路，不再调用 provider
        val second = matcher.match(q)
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NO_MATCH), second)
        assertEquals(1, miss.calls)
    }

    @Test
    fun `网络异常归network不写负缓存以支持重试`() = runTest {
        val failing = FakeProvider(OnlineTextSource.KW) { throw kotlinx.io.IOException("http 500") }
        val matcher = TextMetaMatcher(listOf(failing))
        val result = matcher.match(query())
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK), result)
        // 任务 08-27：NETWORK（含 429）不入负缓存，由限流器控频，支持稍后重试
        assertEquals(0, matcher.negativeCache.size())
    }

    @Test
    fun `queryKey变化不短路旧负缓存`() = runTest {
        val miss = FakeProvider(OnlineTextSource.KW)
        val matcher = TextMetaMatcher(listOf(miss))
        matcher.match(query(songId = "s1", title = "A"))
        // 同 songId 但不同 query（album 变化）→ 负缓存不命中
        matcher.match(query(songId = "s1", title = "A", album = "B"))
        assertEquals(2, miss.calls)
    }

    // ── kw provider JSON 解析（MockEngine 按序应答）───────────────

    private data class MockResp(val status: Int, val body: String)

    private class Harness(vararg resps: MockResp) {
        private val queue = ArrayDeque(resps.toList())
        val requests = mutableListOf<HttpRequestData>()
        val http = ScrapeHttp(
            HttpClient(
                MockEngine { req ->
                    requests += req
                    val r = queue.removeFirst()
                    respond(r.body, HttpStatusCode.fromValue(r.status))
                },
            ),
            rateLimiter = WebDavRateLimiter.Unlimited,
        )
    }

    private fun ok(body: String) = MockResp(200, body)

    @Test
    fun `kw解析abslist字段并pickBestHit`() = runTest {
        val h = Harness(
            ok(
                """
                {"abslist":[
                  {"SONGNAME":" Love Story ","ARTIST":"Taylor Swift","ALBUM":"Fearless"},
                  {"SONGNAME":"Other","ARTIST":"","ALBUM":""},
                  {"SONGNAME":"No Artist No Album"}
                ]}
                """.trimIndent(),
            ),
        )

        val provider = KwProvider(h.http)
        val hit = provider.search(query(title = "Love Story", artist = "Taylor"))

        assertTrue(hit != null)
        assertEquals("Love Story", hit?.title)
        assertEquals("Taylor Swift", hit?.artist)
        assertEquals("Fearless", hit?.album)
        // 第二条 artist/album 均空被过滤；仅剩一条候选
        assertEquals(1, h.requests.size)
    }

    @Test
    fun `kw响应非JSON返回null不抛错`() = runTest {
        val h = Harness(ok("<html>oops</html>"))
        val provider = KwProvider(h.http)
        assertNull(provider.search(query(title = "Love Story")))
    }

    @Test
    fun `kw非2xx向上抛错由matcher归network`() = runTest {
        val h = Harness(MockResp(500, "err"))
        val matcher = TextMetaMatcher(listOf(KwProvider(h.http)))
        val result = matcher.match(query())
        assertEquals(OnlineTextMatchResult.Fail(OnlineTextMatchFailReason.NETWORK), result)
    }
}

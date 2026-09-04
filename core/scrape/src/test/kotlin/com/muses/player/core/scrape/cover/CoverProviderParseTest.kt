package com.muses.player.core.scrape.cover

import com.muses.player.core.scrape.cover.provider.ItunesCoverProvider
import com.muses.player.core.scrape.cover.provider.WyCoverProvider
import com.muses.player.core.scrape.http.ScrapeHttp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 规格 = src/features/cover/providers/itunes.ts 与 wy.ts 的 JSON 解析
 * （P2c：MockWebServer → Ktor MockEngine；MockEngine 拦截全部 host，
 * 硬编码远端 host 无需重写，队列按请求顺序应答；requestCount 改计数器）。
 */
class CoverProviderParseTest {

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
            rateLimiter = com.muses.player.core.scrape.http.ScrapeRateLimiter.Unlimited,
        )
    }

    private fun ok(body: String) = MockResp(200, body)
    private fun err(status: Int, body: String) = MockResp(status, body)

    // ── itunes ──────────────────────────────────────────────

    @Test
    fun `itunes解析results并放大artwork到600`() = runTest {
        val h = Harness(
            ok(
                """
                {"resultCount":2,"results":[
                  {"trackName":"Love Story","artistName":"Taylor Swift","collectionName":"Fearless",
                   "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music/100x100bb.jpg"},
                  {"trackName":"Other Song","artistName":"Nobody","collectionName":"X",
                   "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/X/100x100bb.jpg"}
                ]}
                """.trimIndent(),
            ),
        )

        val provider = ItunesCoverProvider(h.http)
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story", artist = "Taylor"))

        // 最佳命中为第一条（title 精确 + artist 命中），100x100 放大为 600x600
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/600x600bb.jpg",
            url,
        )
    }

    @Test
    fun `itunes无artwork的结果被过滤后返回null`() = runTest {
        val h = Harness(ok("""{"resultCount":1,"results":[{"trackName":"Love Story"}]}"""))

        val provider = ItunesCoverProvider(h.http)
        assertNull(provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story")))
    }

    @Test
    fun `enlargeItunesArtworkUrl仅替换第一处尺寸段`() {
        // \d+x\d+([a-z]*)\. 非全局：只替换第一处
        assertEquals(
            "https://a.com/600x600bb.jpg?fallback=200x200bb.jpg",
            com.muses.player.core.scrape.cover.provider.enlargeItunesArtworkUrl(
                "https://a.com/100x100bb.jpg?fallback=200x200bb.jpg",
            ),
        )
    }

    // ── wy ─────────────────────────────────────────────────

    @Test
    fun `wy搜索加详情两跳取album_picUrl且http升https`() = runTest {
        val h = Harness(
            // 第一跳：公开搜索
            ok(
                """
                {"result":{"songs":[
                  {"id":123,"name":"Love Story","artists":[{"name":"Taylor Swift"}],
                   "album":{"name":"Fearless"}},
                  {"id":0,"name":"Invalid"}
                ]}}
                """.trimIndent(),
            ),
            // 第二跳：song/detail 返回 http:// 封面 → 应升级 https
            ok(
                """
                {"songs":[{"album":{"picUrl":"http://p1.music.126.net/abc.jpg"}}]}
                """.trimIndent(),
            ),
        )

        val provider = WyCoverProvider(h.http)
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story", artist = "Taylor"))

        assertEquals("https://p1.music.126.net/abc.jpg", url)
        assertEquals(2, h.requests.size)

        // 第一跳：搜索；第二跳：详情 ids=[123]（数字无引号，整体 encode）
        val searchPath = h.requests[0].url.encodedPath
        assertTrue(searchPath.contains("search/get/web"))
        val detailUrl = h.requests[1].url.toString()
        assertTrue(detailUrl.contains("song/detail"))
        assertTrue(detailUrl.contains("ids=%5B123%5D"))
    }

    @Test
    fun `wy详情失败重试下一条候选`() = runTest {
        val h = Harness(
            ok(
                """{"result":{"songs":[
                  {"id":1,"name":"Love Story"},{"id":2,"name":"Love Story live"}
                ]}}""",
            ),
            // 第一条详情 500 → 失败；第二条详情返回封面
            err(500, "err"),
            ok("""{"songs":[{"album":{"picUrl":"https://p2.music.126.net/def.jpg"}}]}"""),
        )

        val provider = WyCoverProvider(h.http)
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story"))

        assertEquals("https://p2.music.126.net/def.jpg", url)
        assertEquals(3, h.requests.size)
    }
}

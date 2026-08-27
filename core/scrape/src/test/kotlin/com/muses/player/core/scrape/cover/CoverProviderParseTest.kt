package com.muses.player.core.scrape.cover

import com.muses.player.core.scrape.cover.provider.ItunesCoverProvider
import com.muses.player.core.scrape.cover.provider.WyCoverProvider
import com.muses.player.core.scrape.http.ScrapeHttp
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 规格 = src/features/cover/providers/itunes.ts 与 wy.ts 的 JSON 解析（MockWebServer） */
@RunWith(RobolectricTestRunner::class)
class CoverProviderParseTest {

    private lateinit var server: MockWebServer
    private val loopback = InetAddress.getLoopbackAddress()

    /** 把硬编码的 itunes.apple.com / music.163.com 重写到本地 mock server（http + loopback IPv4） */
    private fun httpFor(server: MockWebServer): ScrapeHttp =
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
            rateLimiter = com.muses.player.core.scrape.http.ScrapeRateLimiter.Unlimited,
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

    // ── itunes ──────────────────────────────────────────────

    @Test
    fun `itunes解析results并放大artwork到600`() = runTest {
        server.enqueue(
            MockResponse().setBody(
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

        val provider = ItunesCoverProvider(httpFor(server))
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story", artist = "Taylor"))

        // 最佳命中为第一条（title 精确 + artist 命中），100x100 放大为 600x600
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/600x600bb.jpg",
            url,
        )
    }

    @Test
    fun `itunes无artwork的结果被过滤后返回null`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"resultCount":1,"results":[{"trackName":"Love Story"}]}"""),
        )

        val provider = ItunesCoverProvider(httpFor(server))
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
        // 第一跳：公开搜索
        server.enqueue(
            MockResponse().setBody(
                """
                {"result":{"songs":[
                  {"id":123,"name":"Love Story","artists":[{"name":"Taylor Swift"}],
                   "album":{"name":"Fearless"}},
                  {"id":0,"name":"Invalid"}
                ]}}
                """.trimIndent(),
            ),
        )
        // 第二跳：song/detail 返回 http:// 封面 → 应升级 https
        server.enqueue(
            MockResponse().setBody(
                """
                {"songs":[{"album":{"picUrl":"http://p1.music.126.net/abc.jpg"}}]}
                """.trimIndent(),
            ),
        )

        val provider = WyCoverProvider(httpFor(server))
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story", artist = "Taylor"))

        assertEquals("https://p1.music.126.net/abc.jpg", url)
        assertEquals(2, server.requestCount)

        // 第一跳：搜索；第二跳：详情 ids=[123]（数字无引号，整体 encode）
        val searchPath = server.takeRequest().requestUrl?.encodedPath.orEmpty()
        assertTrue(searchPath.contains("search/get/web"))
        val detailUrl = server.takeRequest().requestUrl?.toString().orEmpty()
        assertTrue(detailUrl.contains("song/detail"))
        assertTrue(detailUrl.contains("ids=%5B123%5D"))
    }

    @Test
    fun `wy详情失败重试下一条候选`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"result":{"songs":[
                  {"id":1,"name":"Love Story"},{"id":2,"name":"Love Story live"}
                ]}}""",
            ),
        )
        // 第一条详情 500 → 失败；第二条详情返回封面
        server.enqueue(MockResponse().setResponseCode(500).setBody("err"))
        server.enqueue(
            MockResponse().setBody("""{"songs":[{"album":{"picUrl":"https://p2.music.126.net/def.jpg"}}]}"""),
        )

        val provider = WyCoverProvider(httpFor(server))
        val url = provider.searchCoverUrl(OnlineCoverQuery(songId = "s1", title = "Love Story"))

        assertEquals("https://p2.music.126.net/def.jpg", url)
        assertEquals(3, server.requestCount)
    }
}

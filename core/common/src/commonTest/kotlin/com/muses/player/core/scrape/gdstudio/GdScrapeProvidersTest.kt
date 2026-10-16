package com.muses.player.core.scrape.gdstudio

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.editmeta.*
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import com.muses.player.core.network.ProviderRequestGate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.ContinuationInterceptor
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GdScrapeProvidersTest {
    private val rows = """[
        {"id":"1","name":"测试歌曲 (Live)","artist":["测试歌手"],"album":"现场版","pic_id":"11","lyric_id":"12","source":"netease"},
        {"id":"2","name":"测试歌曲","artist":["测试歌手"],"album":"测试专辑","pic_id":"21","lyric_id":"22","source":"netease"}
    ]"""
    private val query = EditCloudMetaQuery("本地歌曲", "测试歌曲", "测试歌手", "测试专辑")

    private suspend fun withClient(
        enabled: suspend () -> Boolean = { true },
        searchBody: String = rows,
        block: suspend (GdScrapeClient, List<HttpRequestData>) -> Unit,
    ) {
        val requests = mutableListOf<HttpRequestData>()
        val dispatcher = currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
        val http = HttpClient(MockEngine(MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler { request ->
            requests += request
            respond(when (request.url.parameters["types"]) {
                "search" -> searchBody
                "pic" -> """{"url":"https://cdn.test/封面.jpg"}"""
                else -> """{"lyric":"[00:01.00]原文","tlyric":"[00:01.00]译文"}"""
            })
            }
        }))
        try { block(GdScrapeClient(ScrapeHttp(http, rateLimiter = WebDavRateLimiter.Unlimited, requestGate = ProviderRequestGate(0)), enabled), requests) }
        finally { http.close() }
    }

    @Test fun `审核搜索三个维度共用结果并标明GD来源`() = runTest {
        withClient { client, requests ->
            val search = EditCloudMetaSearch(listOf(GdTextProvider(client)), listOf(GdCoverProvider(client)), listOf(GdLyricsPort(client)))
            val result = search.search(query, SearchOptions(platform = CloudPlatformId.GD, lyricsPlatform = CloudLyricsPlatformId.GD))
            assertEquals(2, result.text.items.size)
            assertTrue(result.text.items.all { it.source == OnlineTextSource.GD })
            assertEquals(OnlineCoverSource.GD, result.cover.items.single().source)
            assertEquals("gdstudio", result.lyrics.items.single().source)
            assertEquals("[00:01.00]译文", result.lyrics.items.single().translationText)
            assertEquals(1, requests.count { it.url.parameters["types"] == "search" })
            assertEquals("21", requests.single { it.url.parameters["types"] == "pic" }.url.parameters["id"])
            assertEquals("22", requests.single { it.url.parameters["types"] == "lyric" }.url.parameters["id"])
            assertTrue(requests.none { it.url.parameters["types"] == "url" })
        }
    }

    @Test fun `同名翻唱现场版和缺少歌手均不会误取歌词封面`() = runTest {
        for (body in listOf(
            """[{"id":"1","name":"测试歌曲","artist":["其他歌手"],"pic_id":"11"}]""",
            """[{"id":"1","name":"测试歌曲 (Live)","artist":["测试歌手"],"pic_id":"11"}]""",
        )) withClient(searchBody = body) { client, requests ->
            assertNull(GdLyricsPort(client).searchLyrics(query))
            assertNull(GdCoverProvider(client).searchCoverUrl(OnlineCoverQuery(query.songId, query.title, query.artist)))
            assertTrue(requests.all { it.url.parameters["types"] == "search" })
        }
        withClient { client, requests ->
            assertNull(GdLyricsPort(client).searchLyrics(query.copy(artist = null)))
            assertTrue(requests.isEmpty())
        }
    }

    @Test fun `关闭内置音源后缓存也不参与刮削`() = runTest {
        var enabled = true
        withClient(enabled = { enabled }) { client, requests ->
            assertEquals(2, GdTextProvider(client).searchCandidates(OnlineTextQuery(query.songId, query.title, artist = query.artist)).size)
            enabled = false
            assertTrue(GdTextProvider(client).searchCandidates(OnlineTextQuery(query.songId, query.title, artist = query.artist)).isEmpty())
            assertNull(GdLyricsPort(client).searchLyrics(query))
            assertEquals(1, requests.size)
        }
    }

    @Test fun `明确选择其他来源时不会调用GD接口`() = runTest {
        withClient { client, requests ->
            val result = EditCloudMetaSearch(listOf(GdTextProvider(client)), listOf(GdCoverProvider(client)), listOf(GdLyricsPort(client)))
                .search(query, SearchOptions(platform = CloudPlatformId.WY, lyricsPlatform = CloudLyricsPlatformId.WY))
            assertTrue(result.text.items.isEmpty())
            assertTrue(result.cover.items.isEmpty())
            assertTrue(result.lyrics.items.isEmpty())
            assertTrue(requests.isEmpty())
        }
    }

    @Test fun `接口错误返回可重试的来源失败`() = runTest {
        withClient(searchBody = """{"error":"接口不可用"}""") { client, _ ->
            val result = EditCloudMetaSearch(listOf(GdTextProvider(client)), emptyList()).search(query)
            assertEquals(EditDimStatus.NETWORK, result.text.status)
            assertEquals(listOf("gdstudio"), result.text.failedSources)
        }
    }
}

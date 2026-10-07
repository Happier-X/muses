package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchPage
import com.muses.player.core.search.OnlineSearchProvider
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.OnlineSearchService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AiRecommendRequestTest {
    @Test
    fun 不足二十首也只生成一轮() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            respond("""{"choices":[{"message":{"content":"[{\"name\":\"海阔天空\",\"artist\":\"Beyond\"}]"}}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(provider {
                listOf(OnlineSearchResult("wy", "1", "海阔天空", "Beyond", null, null, null, "{}"))
            })))
            val service = AiRecommendService(AiChatClient(client), matcher)
            val result = service.recommend(
                LibraryProfile(1, emptyList(), emptyList(), emptyList(), emptyList(), emptyMap()),
                AiRecommendConfig("https://example.com/v1", "test", "fake-key"),
            )
            assertEquals(1, result.matched)
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun 单首搜索卡住时结束匹配() = runTest {
        val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(provider { awaitCancellation() })))
        val result = matcher.match(listOf(AiSongSuggestion("海阔天空", "Beyond")))
        assertEquals(0, result.matched)
        assertEquals(3_000, testScheduler.currentTime)
    }

    @Test
    fun 外部取消不会变成空结果() = runTest {
        val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(provider {
            throw CancellationException("取消推荐")
        })))
        assertFailsWith<CancellationException> {
            matcher.match(listOf(AiSongSuggestion("海阔天空", "Beyond")))
        }
    }

    @Test
    fun 网易匹配失败后使用其他平台同歌手同版本() = runTest {
        val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(
            provider { listOf(song("wy", "其他歌手"), song("wy", "Beyond", "海阔天空 (Live)")) },
            provider("tx") { listOf(song("tx", "Beyond")) },
        )))
        assertEquals("tx", matcher.match(listOf(AiSongSuggestion("海阔天空", "Beyond")))
            .tracks.single().result.platform)
    }

    @Test
    fun 网易超时也会回退且其他平台失败不影响结果() = runTest {
        val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(
            provider { awaitCancellation() },
            provider("kw") { error("接口不可用") },
            provider("tx") { listOf(song("tx", "Beyond")) },
        )))
        assertEquals("tx", matcher.match(listOf(AiSongSuggestion("海阔天空", "Beyond")))
            .tracks.single().result.platform)
        assertEquals(3_000, testScheduler.currentTime)
    }

    @Test
    fun 网易匹配成功后不请求其他平台() = runTest {
        var fallbackRequests = 0
        val matcher = AiSuggestionMatcher(OnlineSearchService(listOf(
            provider { listOf(song("wy", "Beyond")) },
            provider("tx") { fallbackRequests++; listOf(song("tx", "Beyond")) },
        )))
        assertEquals("wy", matcher.match(listOf(AiSongSuggestion("海阔天空", "Beyond")))
            .tracks.single().result.platform)
        assertEquals(0, fallbackRequests)
    }

    private fun song(platform: String, artist: String, name: String = "海阔天空") =
        OnlineSearchResult(platform, "1", name, artist, null, null, null, "{}")

    private fun provider(platformId: String = "wy", results: suspend () -> List<OnlineSearchResult>) = object : OnlineSearchProvider {
        override val platform = platformId
        override val displayName = "网易云音乐"
        override suspend fun search(keyword: String, page: Int, pageSize: Int) =
            OnlineSearchPage(platform, keyword, page, results(), false)
    }
}

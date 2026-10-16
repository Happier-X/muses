package com.muses.player.core.scrape.editmeta

import com.muses.player.core.lyrics.amll.AmllIndexRepository
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.model.lyrics.AmllFailReason
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.AmllMatchResult
import com.muses.player.core.network.ProviderFailureKind
import com.muses.player.core.network.ProviderRequestGate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AmllScrapeRetryTest {
    private val index = """{"rawLyricFile":"测试.ttml","metadata":[["musicName",["曲名"]],["artists",["歌手"]]]}"""
    private val query = EditCloudMetaQuery("歌曲", "曲名", "歌手")

    @Test fun `诊断重新下载TTML且不覆盖播放正缓存`() = runTest {
        var downloads = 0
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { downloads++; respond(if (downloads == 1) "<tt>原歌词</tt>" else "<tt>诊断歌词</tt>") }
        }))
        try {
            val amll = AmllTtmlDbClient(LyricsHttp(client, requestGate = ProviderRequestGate(0)), AmllIndexRepository { index })
            val playback = AmllMatchQuery("歌曲", "曲名", "歌手")
            val original = amll.match(playback)
            assertIs<AmllMatchResult.Ok>(original)
            val search = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll)))
            val rows = mutableListOf<SourceDiagnosticResult>()
            search.diagnose(query, onResult = rows::add)
            assertEquals(SourceDiagnosticStatus.OK, rows.single().status)
            assertEquals(2, downloads)
            val cached = assertIs<AmllMatchResult.Ok>(amll.match(playback))
            assertEquals(original.ttml, cached.ttml)
            assertEquals(original.rawLyricFile, cached.rawLyricFile)
            assertEquals(2, downloads)
        } finally { client.close() }
    }

    @Test fun `刮削跳过播放查询的失败缓存并可立即恢复索引请求`() = runTest {
        var indexRequests = 0
        var downloads = 0
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                if (request.url.encodedPath == "/index") {
                    indexRequests++
                    if (indexRequests == 1) respond("暂不可用", HttpStatusCode.ServiceUnavailable) else respond(index)
                } else {
                    downloads++
                    respond("<tt></tt>")
                }
            }
        }))
        try {
            val http = LyricsHttp(client, requestGate = ProviderRequestGate(0))
            val amll = AmllTtmlDbClient(http, AmllIndexRepository { http.getText("https://example.com/index") })
            val playbackQuery = AmllMatchQuery("歌曲", "曲名", "歌手")
            assertEquals(AmllMatchResult.Fail(AmllFailReason.NETWORK), amll.match(playbackQuery))
            assertEquals(AmllMatchResult.Fail(AmllFailReason.NETWORK), amll.match(playbackQuery))
            assertEquals(1, indexRequests) // 普通播放仍命中原有失败缓存。
            val result = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll))).search(query)
            assertEquals(EditDimStatus.OK, result.lyrics.status)
            assertEquals("<tt></tt>", result.lyrics.items.single().text)
            assertEquals(2, indexRequests)
            assertEquals(1, downloads)
            assertIs<AmllMatchResult.Ok>(amll.match(playbackQuery))
            assertEquals(1, downloads) // 成功缓存仍可复用。
        } finally { client.close() }
    }

    @Test fun `失败重试真正请求TTML并保留新的HTTP失败分类`() = runTest {
        var downloads = 0
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                downloads++
                when (downloads) {
                    1 -> respond("限流", HttpStatusCode.TooManyRequests)
                    2 -> respond("暂不可用", HttpStatusCode.ServiceUnavailable)
                    else -> respond("<tt></tt>")
                }
            }
        }))
        try {
            val amll = AmllTtmlDbClient(LyricsHttp(client, requestGate = ProviderRequestGate(0)), AmllIndexRepository { index })
            val search = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll)))
            val limited = search.search(query)
            assertEquals(ProviderFailureKind.RATE_LIMITED, limited.lyrics.sourceFailures.single().kind)
            assertEquals(429, limited.lyrics.sourceFailures.single().httpStatus)
            val failed = search.retryFailed(query, limited)
            assertEquals(EditDimStatus.NETWORK, failed.lyrics.status)
            assertEquals(ProviderFailureKind.SERVER, failed.lyrics.sourceFailures.single().kind)
            assertEquals(503, failed.lyrics.sourceFailures.single().httpStatus)
            val recovered = search.retryFailed(query, failed)
            assertEquals(EditDimStatus.OK, recovered.lyrics.status)
            assertTrue(recovered.lyrics.failedSources.isEmpty())
            assertEquals(3, downloads)
        } finally { client.close() }
    }

    @Test fun `无效TTML按响应异常报告且重试可跳过解析失败缓存`() = runTest {
        var downloads = 0
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                downloads++
                respond(if (downloads <= 2) "<html>无效歌词</html>" else "<tt></tt>")
            }
        }))
        try {
            val amll = AmllTtmlDbClient(LyricsHttp(client, requestGate = ProviderRequestGate(0)), AmllIndexRepository { index })
            val search = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll)))
            assertEquals(AmllMatchResult.Fail(AmllFailReason.PARSE), amll.match(AmllMatchQuery("歌曲", "曲名", "歌手")))
            val failed = search.search(query)
            assertEquals(ProviderFailureKind.RESPONSE, failed.lyrics.sourceFailures.single().kind)
            assertEquals(listOf("amll"), failed.lyrics.failedSources)
            val recovered = search.retryFailed(query, failed)
            assertEquals(EditDimStatus.OK, recovered.lyrics.status)
            assertEquals(3, downloads)
        } finally { client.close() }
    }

    @Test fun `手动刮削失败不会向播放查询新增失败缓存`() = runTest {
        for (networkFailure in listOf(true, false)) {
            var downloads = 0
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler {
                    downloads++
                    if (downloads > 1) respond("<tt></tt>")
                    else if (networkFailure) respond("暂不可用", HttpStatusCode.ServiceUnavailable)
                    else respond("<html>无效歌词</html>")
                }
            }))
            try {
                val amll = AmllTtmlDbClient(LyricsHttp(client, requestGate = ProviderRequestGate(0)), AmllIndexRepository { index })
                val result = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll))).search(query)
                assertEquals(EditDimStatus.NETWORK, result.lyrics.status)
                assertIs<AmllMatchResult.Ok>(amll.match(AmllMatchQuery("歌曲", "曲名", "歌手")))
                assertEquals(2, downloads)
            } finally { client.close() }
        }
    }

    @Test fun `端口区分未收录网络响应和中止结果`() = runTest {
        val client = HttpClient(MockEngine { error("本测试不应联网") })
        try {
            for (reason in AmllFailReason.entries) {
                val amll = object : AmllTtmlDbClient(LyricsHttp(client), AmllIndexRepository { error("不加载索引") }) {
                    override suspend fun match(query: AmllMatchQuery): AmllMatchResult {
                        assertTrue(query.bypassNegativeCache)
                        return AmllMatchResult.Fail(reason)
                    }
                }
                val result = EditCloudMetaSearch(emptyList(), emptyList(), listOf(AmllLyricsPort(amll))).search(query)
                when (reason) {
                    AmllFailReason.NO_MATCH -> {
                        assertEquals(EditDimStatus.NO_MATCH, result.lyrics.status)
                        assertTrue(result.lyrics.failedSources.isEmpty())
                    }
                    AmllFailReason.NETWORK -> assertEquals(ProviderFailureKind.NETWORK, result.lyrics.sourceFailures.single().kind)
                    AmllFailReason.PARSE -> assertEquals(ProviderFailureKind.RESPONSE, result.lyrics.sourceFailures.single().kind)
                    AmllFailReason.ABORTED -> assertEquals(EditDimStatus.ABORTED, result.lyrics.status)
                }
            }
        } finally { client.close() }
    }
}

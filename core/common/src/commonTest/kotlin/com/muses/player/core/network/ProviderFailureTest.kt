package com.muses.player.core.network

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.EditDimStatus
import com.muses.player.core.scrape.editmeta.LyricsHit
import com.muses.player.core.scrape.editmeta.LyricsSearchPort
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.model.scrape.OnlineTextQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProviderFailureTest {
    @Test fun `HTTP失败按状态分类且未知错误不伪装成无匹配`() {
        for ((status, kind) in listOf(401 to ProviderFailureKind.AUTH, 403 to ProviderFailureKind.AUTH,
            429 to ProviderFailureKind.RATE_LIMITED, 408 to ProviderFailureKind.TIMEOUT,
            504 to ProviderFailureKind.TIMEOUT, 503 to ProviderFailureKind.SERVER, 404 to ProviderFailureKind.RESPONSE)) {
            assertEquals(kind, providerFailureKind(ProviderHttpException(status)))
        }
        assertEquals(ProviderFailureKind.RESPONSE, providerFailureKind(SerializationException("无效响应")))
        assertEquals(ProviderFailureKind.NETWORK, providerFailureKind(IOException("网络中断")))
    }

    @Test fun `歌词内部吞掉HTTP错误后审核仍报告失败并保留其他候选`() = runTest {
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { respond("敏感响应正文", HttpStatusCode.Forbidden) }
        }))
        try {
            val http = LyricsHttp(client, requestGate = ProviderRequestGate(0))
            val port = object : LyricsSearchPort {
                override val id = "wy"
                override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
                    runCatching { http.getText("https://music.163.com/a") }
                    return null
                }
            }
            val text = object : TextMetaProvider {
                override val id = OnlineTextSource.KW
                override suspend fun search(query: OnlineTextQuery) = TextMetaHit("歌曲", source = id)
            }
            val result = EditCloudMetaSearch(listOf(text), emptyList(), listOf(port))
                .search(EditCloudMetaQuery("歌曲", "歌曲"))
            assertEquals(EditDimStatus.OK, result.text.status)
            assertEquals(EditDimStatus.NETWORK, result.lyrics.status)
            assertEquals(ProviderFailureKind.AUTH, result.lyrics.sourceFailures.single().kind)
            assertEquals(403, result.lyrics.sourceFailures.single().httpStatus)
            assertEquals(listOf("wy"), result.lyrics.failedSources)
            assertTrue("敏感响应正文" !in result.lyrics.sourceFailures.toString())
        } finally { client.close() }
    }

    @Test fun `备用接口成功后不显示已恢复的错误且查询之间不串失败`() = runTest {
        assertEquals("成功", captureProviderFailures {
            runCatching { observeProviderRequest("https://music.163.com/a") { throw ProviderHttpException(503) } }
            "成功"
        })
        assertEquals(null, captureProviderFailures<String?> { null })
    }

    @Test fun `LRCLIB精确查询404是未收录而非来源失败`() = runTest {
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { respond("未找到", HttpStatusCode.NotFound) }
        }))
        try {
            val http = LyricsHttp(client, requestGate = ProviderRequestGate(0))
            val result = captureProviderFailures<String?> {
                runCatching { http.getJson("https://lrclib.net/api/get?track_name=歌曲") }
                null
            }
            assertEquals(null, result)
        } finally { client.close() }
    }

    @Test fun `响应解析失败可追溯且取消不会变成来源失败`() = runTest {
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { respond("{无效JSON") }
        }))
        try {
            val http = LyricsHttp(client, requestGate = ProviderRequestGate(0))
            assertFailsWith<SerializationException> {
                captureProviderFailures<String?> { runCatching { http.getJson("https://music.163.com/a") }; null }
            }
            assertFailsWith<CancellationException> {
                captureProviderFailures<String?> { observeProviderRequest("https://music.163.com/a") { throw CancellationException() } }
            }
        } finally { client.close() }
    }
}

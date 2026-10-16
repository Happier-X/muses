package com.muses.player.core.lyrics

import com.muses.player.core.lyrics.amll.AmllIndexRepository
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.lrclib.searchLrclibLyrics
import com.muses.player.core.lyrics.provider.searchKgLyrics
import com.muses.player.core.lyrics.provider.searchKwLyrics
import com.muses.player.core.lyrics.provider.searchMgLyrics
import com.muses.player.core.lyrics.provider.searchTxLyrics
import com.muses.player.core.lyrics.provider.searchWyLyrics
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.AmllMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.network.ProviderRequestGate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LyricsProviderCancellationTest {
    private fun query(source: String? = null, identity: String = "{}") = OnlineLyricsQuery(
        "歌曲", "曲名", "歌手", trackRef = source?.let { OnlineTrackRef(it, identity, "来源") },
    )

    private data class CancelCase(
        val name: String,
        val query: OnlineLyricsQuery,
        val before: List<String> = emptyList(),
        val search: suspend (LyricsHttp, OnlineLyricsQuery) -> OnlineLyricsProviderHit?,
    )

    @Test fun `各平台搜索与歌词下载中的取消均向上传播且不继续请求`() = runTest {
        val cases = listOf(
            CancelCase("网易搜索", query(), search = ::searchWyLyrics),
            CancelCase("网易逐字", query("wy", """{"songmid":123}"""), search = ::searchWyLyrics),
            CancelCase("网易行级", query("wy", """{"songmid":123}"""), listOf("{}"), ::searchWyLyrics),
            CancelCase("QQ 搜索", query(), search = ::searchTxLyrics),
            CancelCase("QQ 逐字", query("tx", """{"songmid":"mid","songid":123}"""), search = ::searchTxLyrics),
            CancelCase("QQ 行级", query("tx", """{"songmid":"mid"}"""), search = ::searchTxLyrics),
            CancelCase("酷狗搜索", query(), search = ::searchKgLyrics),
            CancelCase("酷狗歌词搜索", query("kg", """{"hash":"hash"}"""), search = ::searchKgLyrics),
            CancelCase("酷狗下载", query("kg", """{"hash":"hash"}"""),
                listOf("""{"candidates":[{"id":"歌词","accesskey":"测试"}]}"""), ::searchKgLyrics),
            CancelCase("酷我搜索", query(), search = ::searchKwLyrics),
            CancelCase("酷我歌词", query("kw", """{"songmid":"123"}"""), search = ::searchKwLyrics),
            CancelCase("咪咕搜索", query(), search = ::searchMgLyrics),
            CancelCase("咪咕下载", query(), listOf(
                """{"musics":[{"songName":"曲名","singerName":"歌手","lrcUrl":"https://example.com/歌词.lrc"}]}""",
            ), ::searchMgLyrics),
            CancelCase("LRCLIB 精确", query(), search = ::searchLrclibLyrics),
            CancelCase("LRCLIB 检索", query(), listOf("{}"), ::searchLrclibLyrics),
        )
        for (case in cases) {
            var calls = 0
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler {
                    val index = calls++
                    if (index == case.before.size) throw CancellationException(case.name)
                    respond(case.before.getOrNull(index) ?: "{}")
                }
            }))
            try {
                val http = LyricsHttp(client, requestGate = ProviderRequestGate(0))
                assertFailsWith<CancellationException>(case.name) { case.search(http, case.query) }
                assertEquals(case.before.size + 1, calls, case.name)
            } finally { client.close() }
        }
    }

    @Test fun `网易QQ与LRCLIB普通接口错误仍允许备用接口成功`() = runTest {
        for ((search, request) in listOf(
            ::searchWyLyrics to query("wy", """{"songmid":123}"""),
            ::searchTxLyrics to query("tx", """{"songmid":"mid","songid":123}"""),
            ::searchLrclibLyrics to query(),
        )) {
            var calls = 0
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    calls++
                    if (calls == 1) respond("暂不可用", HttpStatusCode.ServiceUnavailable)
                    else if (request.url.host == "lrclib.net") respond(
                        """[{"trackName":"曲名","artistName":"歌手","syncedLyrics":"[00:01.00]备用歌词"}]""",
                    )
                    else respond("""{"lrc":{"lyric":"[00:01.00]备用歌词"},"lyric":"[00:01.00]备用歌词"}""")
                }
            }))
            try {
                val hit = search(LyricsHttp(client, requestGate = ProviderRequestGate(0)), request)
                assertEquals("[00:01.00]备用歌词", hit?.text)
                assertEquals(2, calls)
            } finally { client.close() }
        }
    }

    @Test fun `AMLL索引与歌词取消均不污染同曲负缓存`() = runTest {
        for (cancelIndex in listOf(true, false)) {
            var loads = 0
            var downloads = 0
            val repository = AmllIndexRepository {
                loads++
                if (cancelIndex && loads == 1) throw CancellationException("取消索引加载")
                """{"rawLyricFile":"测试.ttml","metadata":[["musicName",["曲名"]],["artists",["歌手"]]]}"""
            }
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler {
                    downloads++
                    if (!cancelIndex && downloads == 1) throw CancellationException("取消歌词下载")
                    respond("<tt></tt>")
                }
            }))
            try {
                val amll = AmllTtmlDbClient(LyricsHttp(client, requestGate = ProviderRequestGate(0)), repository)
                val query = AmllMatchQuery("歌曲", "曲名", "歌手")
                assertFailsWith<CancellationException> { amll.match(query) }
                assertIs<AmllMatchResult.Ok>(amll.match(query))
                assertEquals(if (cancelIndex) 2 else 1, loads)
                assertEquals(if (cancelIndex) 1 else 2, downloads)
            } finally { client.close() }
        }
    }
}

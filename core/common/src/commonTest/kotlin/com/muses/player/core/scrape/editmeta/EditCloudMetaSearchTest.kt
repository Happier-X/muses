package com.muses.player.core.scrape.editmeta

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.text.TextMetaProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 规格 = src/features/editMeta/searchEditCloudMeta.ts 编排主流程（fake provider 注入） */
class EditCloudMetaSearchTest {

    private class FakeText(
        override val id: OnlineTextSource,
        val result: (OnlineTextQuery) -> TextMetaHit? = { null },
    ) : TextMetaProvider {
        var calls = 0
        override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
            calls++
            return result(query)
        }
    }

    private class FakeCover(
        override val id: OnlineCoverSource,
        val result: (OnlineCoverQuery) -> String? = { null },
    ) : CoverProvider {
        var calls = 0
        override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
            calls++
            return result(query)
        }
    }

    private class FakeLyrics(
        override val id: String,
        val result: (EditCloudMetaQuery) -> LyricsHit? = { null },
    ) : LyricsSearchPort {
        var calls = 0
        override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? {
            calls++
            return result(query)
        }
    }

    private fun query() = EditCloudMetaQuery(songId = "s1", title = "Love Story", artist = "Taylor")

    @Test
    fun `title或songId为空_三维全空no-match`() = runTest {
        val search = EditCloudMetaSearch(emptyList(), emptyList())
        val result = search.search(EditCloudMetaQuery(songId = "", title = "  "))
        assertEquals(EditDimStatus.NO_MATCH, result.text.status)
        assertEquals(EditDimStatus.NO_MATCH, result.cover.status)
        assertEquals(EditDimStatus.NO_MATCH, result.lyrics.status)
    }

    @Test
    fun `文本维度_去重按分排序并截断max`() = runTest {
        val hitHigh = TextMetaHit(title = "Love Story", artist = "Taylor", source = OnlineTextSource.KW)
        val hitDup = TextMetaHit(title = "love story ", artist = "taylor", source = OnlineTextSource.KW) // normalize 后同 key
        val hitLow = TextMetaHit(album = "Fearless", source = OnlineTextSource.TX)
        val textProviders = listOf(
            FakeText(OnlineTextSource.KW) { hitHigh },
            FakeText(OnlineTextSource.TX) { hitDup },
            FakeText(OnlineTextSource.WY) { hitLow },
            FakeText(OnlineTextSource.KG) { null },
        )
        val search = EditCloudMetaSearch(textProviders, emptyList())
        val result = search.search(query(), SearchOptions(maxCandidates = 1))

        // 命中收集时 source 改写为 provider.id；去重后仅剩 high/low，截断 max=1 取高分
        assertEquals(EditDimStatus.OK, result.text.status)
        assertEquals(1, result.text.items.size)
        assertTrue(result.text.items.single().artist == "Taylor")
        // 无命中源不计 network
        assertEquals(4, textProviders.sumOf { it.calls })
    }

    @Test
    fun `文本维度_全provider异常归network`() = runTest {
        val failing = object : TextMetaProvider {
            override val id = OnlineTextSource.KW
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? = throw IllegalStateException("boom")
        }
        val search = EditCloudMetaSearch(listOf(failing), emptyList())
        val result = search.search(query())
        assertEquals(EditDimStatus.NETWORK, result.text.status)
        assertTrue(result.text.items.isEmpty())
    }

    @Test
    fun `封面维度_url校验_小写去重_满max提前停止`() = runTest {
        val covers = listOf(
            FakeCover(OnlineCoverSource.ITUNES) { "https://a.com/600x600bb.jpg" },
            FakeCover(OnlineCoverSource.KW) { "HTTPS://A.COM/600x600bb.jpg" }, // 小写去重命中
            FakeCover(OnlineCoverSource.TX) { "ftp://bad.com/x.jpg" },          // 非 http(s) 跳过
            FakeCover(OnlineCoverSource.WY) { "https://b.com/c.jpg" },
            FakeCover(OnlineCoverSource.KG) { "https://c.com/d.jpg" },
        )
        val search = EditCloudMetaSearch(emptyList(), covers)
        val result = search.search(query(), SearchOptions(maxCandidates = 2))

        assertEquals(EditDimStatus.OK, result.cover.status)
        assertEquals(listOf("https://a.com/600x600bb.jpg", "https://b.com/c.jpg"), result.cover.items.map { it.remoteUrl })
        // 满.max 后后续 provider 不再调用（kg 未被调用）
        assertEquals(0, covers[4].calls)
    }

    @Test
    fun `歌词维度_amll始终参与且不随平台过滤_ttml粗排优先`() = runTest {
        val amll = FakeLyrics("amll") { LyricsHit(text = "<tt>ttml</tt>", format = "ttml") }
        val wy = FakeLyrics("wy") { LyricsHit(text = "[00:01.00]lrc", format = "lrc") }
        val lrclib = FakeLyrics("lrclib") { LyricsHit(text = "[00:01.00]lib", format = "lrc") }
        val search = EditCloudMetaSearch(emptyList(), emptyList(), lyricsPorts = listOf(amll, wy, lrclib))

        // 平台限定 wy：amll 仍参与；lrclib 不参与
        val result = search.search(query(), SearchOptions(lyricsPlatform = CloudLyricsPlatformId.WY))
        assertEquals(EditDimStatus.OK, result.lyrics.status)
        // ttml 粗排在前
        assertEquals("amll", result.lyrics.items.first().source)
        assertEquals(2, result.lyrics.items.size)
        assertEquals(0, lrclib.calls)
    }

    @Test
    fun `歌词维度_重复候选按source_format_前120字去重`() = runTest {
        val sameText = "[00:01.00]same line"
        // 同源同格式同前缀 → 去重
        val p1 = FakeLyrics("wy") { LyricsHit(text = sameText, format = "lrc") }
        var callCount = 0
        val p1b = FakeLyrics("wy") { callCount++; if (callCount > 1) LyricsHit(text = sameText + " tail", format = "lrc") else LyricsHit(text = sameText, format = "lrc") }
        // 跨源同文：Web key 含 source → 不去重
        val p2 = FakeLyrics("kw") { LyricsHit(text = sameText, format = "lrc") }
        val search = EditCloudMetaSearch(emptyList(), emptyList(), lyricsPorts = listOf(p1, p1b, p2))
        val result = search.search(query())
        assertEquals(2, result.lyrics.items.size)
        assertEquals(setOf("wy", "kw"), result.lyrics.items.map { it.source }.toSet())
    }

    @Test
    fun `中止信号_检查点抛出后维度标记aborted`() = runTest {
        val signal = AbortSignal { true }
        val provider = FakeText(OnlineTextSource.KW) { TextMetaHit(title = "x", source = OnlineTextSource.KW) }
        val search = EditCloudMetaSearch(listOf(provider), emptyList())
        val result = search.search(query(), signal = signal)
        assertEquals(EditDimStatus.ABORTED, result.text.status)
        // 检查点在 provider 调用前 → 未实际调用
        assertEquals(0, provider.calls)
    }

    @Test
    fun `维度过滤_只搜指定维度其余为空`() = runTest {
        val cover = FakeCover(OnlineCoverSource.KW) { "https://a.com/x.jpg" }
        val search = EditCloudMetaSearch(emptyList(), listOf(cover))
        val result = search.search(query(), SearchOptions(dimensions = setOf(EditDimKey.TEXT)))
        assertEquals(0, cover.calls)
        assertEquals(EditDimStatus.NO_MATCH, result.cover.status)
    }

    @Test
    fun `平台过滤_itunes无文本provider_封面仅itunes`() = runTest {
        val kwText = FakeText(OnlineTextSource.KW) { TextMetaHit(title = "x", source = OnlineTextSource.KW) }
        val kwCover = FakeCover(OnlineCoverSource.KW) { "https://k.com/x.jpg" }
        val itunesCover = FakeCover(OnlineCoverSource.ITUNES) { "https://i.com/x.jpg" }
        val search = EditCloudMetaSearch(listOf(kwText), listOf(kwCover, itunesCover))

        val result = search.search(query(), SearchOptions(platform = CloudPlatformId.ITUNES))
        assertEquals(0, kwText.calls)   // itunes 无文本源
        assertEquals(0, kwCover.calls)  // 平台过滤排除 kw
        assertEquals(
            listOf("https://i.com/x.jpg"),
            result.cover.items.map { it.remoteUrl },
        )
    }
}

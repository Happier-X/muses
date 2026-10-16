package com.muses.player.core.scrape.editmeta

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.text.TextMetaProvider
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

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

    @Test fun `诊断单项超时后继续下一项且耗时包含等待`() = runTest {
        val slow = object : LyricsSearchPort {
            override val id = "wy"
            override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? { delay(60_000); return null }
        }
        val engine = EditCloudMetaSearch(emptyList(), emptyList(), listOf(slow, FakeLyrics("tx") { LyricsHit("歌词", "lrc") }))
        val rows = mutableListOf<SourceDiagnosticResult>()
        engine.diagnose(query(), nowMs = { testScheduler.currentTime }, onResult = rows::add)
        assertEquals(com.muses.player.core.network.ProviderFailureKind.TIMEOUT, rows.first().failure?.kind)
        assertEquals(30_000L, rows.first().elapsedMs)
        assertEquals(SourceDiagnosticStatus.OK, rows.last().status)
    }

    @Test fun `诊断只列装配能力并按来源筛选且保留AMLL`() {
        val engine = EditCloudMetaSearch(listOf(FakeText(OnlineTextSource.WY), FakeText(OnlineTextSource.TX)),
            listOf(FakeCover(OnlineCoverSource.LRCAPI)), listOf(FakeLyrics("amll"), FakeLyrics("wy"), FakeLyrics("tx")))
        assertEquals(listOf(SourceDiagnosticTarget("lrcapi", EditDimKey.COVER),
            SourceDiagnosticTarget("amll", EditDimKey.LYRICS), SourceDiagnosticTarget("wy", EditDimKey.LYRICS)),
            engine.diagnosticTargets(SearchOptions(platform = CloudPlatformId.LRCAPI, lyricsPlatform = CloudLyricsPlatformId.WY)))
    }

    @Test fun `诊断失败隔离并保留结构化HTTP错误和耗时`() = runTest {
        val engine = EditCloudMetaSearch(listOf(FakeText(OnlineTextSource.WY) { throw com.muses.player.core.network.ProviderHttpException(403) },
            FakeText(OnlineTextSource.TX) { TextMetaHit("结果", source = OnlineTextSource.TX) }), emptyList(), listOf(FakeLyrics("wy")))
        var clock = 0L
        val rows = mutableListOf<SourceDiagnosticResult>()
        engine.diagnose(query(), nowMs = { clock.also { clock += 17 } }, onResult = rows::add)
        assertEquals(listOf(SourceDiagnosticStatus.FAILED, SourceDiagnosticStatus.OK, SourceDiagnosticStatus.NO_MATCH), rows.map { it.status })
        assertEquals(com.muses.player.core.network.ProviderFailureKind.AUTH, rows.first().failure?.kind)
        assertEquals(403, rows.first().failure?.httpStatus)
        assertTrue(rows.all { it.elapsedMs == 17L })
    }

    @Test fun `封面诊断必须验证图片而不能仅凭地址成功`() = runTest {
        val cover = FakeCover(OnlineCoverSource.WY) { "https://example.com/image" }
        val rows = mutableListOf<SourceDiagnosticResult>()
        EditCloudMetaSearch(emptyList(), listOf(cover)).diagnose(query(), onResult = rows::add)
        assertEquals(SourceDiagnosticStatus.UNVERIFIED, rows.single().status)
        rows.clear()
        EditCloudMetaSearch(emptyList(), listOf(cover), diagnosticCoverProbe = {
            throw com.muses.player.core.network.ProviderResponseException()
        }).diagnose(query(), onResult = rows::add)
        assertEquals(SourceDiagnosticStatus.FAILED, rows.single().status)
        assertEquals(com.muses.player.core.network.ProviderFailureKind.RESPONSE, rows.single().failure?.kind)
        assertTrue(rows.single().imageChecked)
        rows.clear()
        EditCloudMetaSearch(emptyList(), listOf(cover), diagnosticCoverProbe = {}).diagnose(query(), onResult = rows::add)
        assertEquals(SourceDiagnosticStatus.OK, rows.single().status)
        assertTrue(rows.single().imageChecked)
    }

    @Test fun `诊断取消不吞掉且后续来源不启动`() = runTest {
        val remaining = FakeText(OnlineTextSource.TX)
        val engine = EditCloudMetaSearch(listOf(FakeText(OnlineTextSource.WY) { throw CancellationException("停止") }, remaining), emptyList())
        val rows = mutableListOf<SourceDiagnosticResult>()
        assertFailsWith<CancellationException> { engine.diagnose(query(), onResult = rows::add) }
        assertEquals(0, remaining.calls)
        assertTrue(rows.isEmpty())
    }

    @Test fun `重试仅请求失败来源对应维度并追加候选`() = runTest {
        var fail = true
        val wyText = FakeText(OnlineTextSource.WY) { TextMetaHit("已有候选", source = OnlineTextSource.WY) }
        val kwText = FakeText(OnlineTextSource.KW) {
            if (fail) error("暂不可用")
            TextMetaHit("恢复候选", source = OnlineTextSource.KW)
        }
        val wyCover = FakeCover(OnlineCoverSource.WY) {
            if (fail) error("暂不可用")
            "https://example.com/cover.jpg"
        }
        val amll = FakeLyrics("amll") { LyricsHit("已有歌词", "ttml") }
        val wyLyrics = FakeLyrics("wy") { LyricsHit("已有歌词", "lrc") }
        val search = EditCloudMetaSearch(listOf(wyText, kwText), listOf(wyCover), listOf(amll, wyLyrics))
        val original = search.search(query())
        fail = false
        val recovered = search.retryFailed(query(), original)
        assertEquals(listOf("已有候选", "恢复候选"), recovered.text.items.map { it.title })
        assertEquals(original.text.defaultIndex, recovered.text.defaultIndex)
        assertEquals(EditDimStatus.OK, recovered.cover.status)
        assertTrue(recovered.text.failedSources.isEmpty())
        assertTrue(recovered.cover.sourceFailures.isEmpty())
        assertEquals(original.lyrics, recovered.lyrics)
        assertEquals(1, wyText.calls)
        assertEquals(2, kwText.calls)
        assertEquals(2, wyCover.calls)
        assertEquals(1, amll.calls)
        assertEquals(1, wyLyrics.calls)
        // 已恢复后再次重试不会访问任何来源。
        assertEquals(recovered, search.retryFailed(query(), recovered))
        assertEquals(2, kwText.calls)
    }

    @Test fun `重试后无匹配清除旧错误仍失败则保留新的失败类型`() = runTest {
        var attempts = 0
        val source = FakeText(OnlineTextSource.WY) {
            attempts++
            when (attempts) {
                1 -> throw com.muses.player.core.network.ProviderHttpException(503)
                2 -> throw com.muses.player.core.network.ProviderHttpException(429)
                else -> null
            }
        }
        val search = EditCloudMetaSearch(listOf(source), emptyList())
        val failed = search.search(query())
        val limited = search.retryFailed(query(), failed)
        assertEquals(429, limited.text.sourceFailures.single().httpStatus)
        val empty = search.retryFailed(query(), limited)
        assertEquals(EditDimStatus.NO_MATCH, empty.text.status)
        assertTrue(empty.text.failedSources.isEmpty())
    }

    @Test fun `版本一致候选优先且冲突候选仍可人工选择`() = runTest {
        val provider = object : TextMetaProvider {
            override val id = OnlineTextSource.WY
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? = null
            override suspend fun searchCandidates(query: OnlineTextQuery) =
                listOf("Love Story (Remix)", "Love Story", "Love Story (Live)").map {
                    TextMetaHit(it, "Taylor", "Fearless", id)
                }
        }
        val search = EditCloudMetaSearch(listOf(provider), emptyList())
        val result = search.search(query().copy(title = "Love Story (Live)"))
        assertEquals(listOf("Love Story (Live)", "Love Story", "Love Story (Remix)"), result.text.items.map { it.title })
        val unmarked = search.search(query())
        assertEquals("Love Story", unmarked.text.items.first().title)
        assertEquals(3, unmarked.text.items.size)
    }

    @Test fun `完整的错误身份不能压过缺专辑的正确候选且同名目录条目不误去重`() = runTest {
        val provider = object : TextMetaProvider {
            override val id = OnlineTextSource.WY
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? = null
            override suspend fun searchCandidates(query: OnlineTextQuery) = listOf(
                TextMetaHit("Love Story", "Taylor Swift", "Fearless", id, 180.0, candidateId = "wy:错误歌手"),
                TextMetaHit("Love Story", "Taylor", null, id, 180.0, candidateId = "wy:正确录音"),
                TextMetaHit("Love Story", "Taylor", "Fearless", id, 210.0, candidateId = "wy:较长剪辑"),
                TextMetaHit("Love Story", "Taylor", null, id, 180.0, candidateId = "wy:其他目录条目"),
            )
        }
        val result = EditCloudMetaSearch(listOf(provider), emptyList()).search(query().copy(durationSec = 180.0))
        assertEquals(listOf("wy:正确录音", "wy:其他目录条目"), result.text.items.take(2).map { it.candidateId })
        assertEquals(4, result.text.items.size)
    }

    @Test fun `同源多个版本保留且传递时长`() = runTest {
        val provider = object : TextMetaProvider {
            override val id = OnlineTextSource.WY
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? = error("审核不能走单候选接口")
            override suspend fun searchCandidates(query: OnlineTextQuery): List<TextMetaHit> {
                assertEquals(180.0, query.durationSec)
                return listOf("Love Story", "Love Story (Live)").map {
                    TextMetaHit(it, "Taylor", "Fearless", id)
                }
            }
        }
        val result = EditCloudMetaSearch(listOf(provider), emptyList()).search(query().copy(durationSec = 180.0))
        assertEquals(2, result.text.items.size)
    }

    @Test fun `部分来源失败保留成功候选和失败来源`() = runTest {
        val result = EditCloudMetaSearch(
            listOf(FakeText(OnlineTextSource.WY) { TextMetaHit("Love Story", source = OnlineTextSource.WY) },
                FakeText(OnlineTextSource.KW) { error("请求失败") }),
            listOf(FakeCover(OnlineCoverSource.TX) { error("请求失败") }),
            listOf(FakeLyrics("amll") { error("请求失败") }, FakeLyrics("wy") { LyricsHit("歌词", "lrc") }),
        ).search(query())
        assertEquals(EditDimStatus.OK, result.text.status)
        assertEquals(1, result.text.items.size)
        assertEquals(listOf("kw"), result.text.failedSources)
        assertEquals(listOf("tx"), result.cover.failedSources)
        assertEquals(listOf("amll"), result.lyrics.failedSources)
        assertEquals(1, result.lyrics.items.size)
    }

    @Test fun `普通取消在三个维度均向上传播`() = runTest {
        val searches = listOf(
            EditCloudMetaSearch(listOf(FakeText(OnlineTextSource.WY) { throw CancellationException() }), emptyList()),
            EditCloudMetaSearch(emptyList(), listOf(FakeCover(OnlineCoverSource.WY) { throw CancellationException() })),
            EditCloudMetaSearch(emptyList(), emptyList(), listOf(FakeLyrics("amll") { throw CancellationException() })),
            EditCloudMetaSearch(emptyList(), emptyList(), listOf(FakeLyrics("wy") { throw CancellationException() })),
        )
        for (search in searches) assertFailsWith<CancellationException> { search.search(query()) }
    }

    @Test fun `来源超时后继续其他来源并记录缺失`() = runTest {
        val slow = object : TextMetaProvider {
            override val id = OnlineTextSource.KW
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? { delay(60_000); return null }
        }
        val good = FakeText(OnlineTextSource.WY) { TextMetaHit("Love Story", source = OnlineTextSource.WY) }
        val result = EditCloudMetaSearch(listOf(slow, good), emptyList()).search(query())
        assertEquals(1, result.text.items.size)
        assertEquals(listOf("kw"), result.text.failedSources)
    }

    @Test fun `封面和歌词无命中时去掉本地专辑重试`() = runTest {
        val cover = FakeCover(OnlineCoverSource.WY) { if (it.album == null) "https://example.com/cover.jpg" else null }
        val lyrics = FakeLyrics("wy") { if (it.album == null) LyricsHit("歌词", "lrc") else null }
        val result = EditCloudMetaSearch(emptyList(), listOf(cover), listOf(lyrics)).search(query().copy(album = "我的音乐"))
        assertEquals(2, cover.calls)
        assertEquals(2, lyrics.calls)
        assertEquals(1, result.cover.items.size)
        assertEquals(1, result.lyrics.items.size)
    }

    @Test fun `歌词前段相同但后段或译文不同仍保留`() = runTest {
        val prefix = "歌词".repeat(100)
        val ports = listOf(
            FakeLyrics("wy") { LyricsHit(prefix + "结尾一", "lrc", "译文一") },
            FakeLyrics("wy") { LyricsHit(prefix + "结尾二", "lrc", "译文一") },
            FakeLyrics("wy") { LyricsHit(prefix + "结尾一", "lrc", "译文二") },
        )
        assertEquals(3, EditCloudMetaSearch(emptyList(), emptyList(), ports).search(query()).lyrics.items.size)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `三维搜索并行等待而非耗时相加`() = runTest {
        val search = EditCloudMetaSearch(
            listOf(object : TextMetaProvider {
                override val id = OnlineTextSource.WY
                override suspend fun search(query: OnlineTextQuery): TextMetaHit? { delay(1000); return null }
            }),
            listOf(object : CoverProvider {
                override val id = OnlineCoverSource.WY
                override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? { delay(1000); return null }
            }),
            listOf(object : LyricsSearchPort {
                override val id = "wy"
                override suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit? { delay(1000); return null }
            }),
        )
        search.search(query())
        assertEquals(1000L, testScheduler.currentTime)
    }

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
    fun `歌词维度_完全相同内容去重且保留跨来源候选`() = runTest {
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

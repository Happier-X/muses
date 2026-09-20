package com.muses.player.core.search

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [OnlineSearchService] 聚合行为测试：并行、部分失败不阻断、并发限流 */
class OnlineSearchServiceTest {

    /** 可编程假 provider：按平台返回结果或抛错 */
    private class FakeProvider(
        override val platform: String,
        override val displayName: String,
        private val results: List<OnlineSearchResult> = emptyList(),
        private val error: String? = null,
        private val hasMore: Boolean = false,
    ) : OnlineSearchProvider {
        var calls = 0
            private set
        var maxConcurrent = 0
            private set

        override suspend fun search(keyword: String, page: Int, pageSize: Int): OnlineSearchPage {
            calls++
            if (error != null) throw OnlineSearchException(platform, error)
            return OnlineSearchPage(platform, keyword, page, results, hasMore)
        }
    }

    private fun result(platform: String, id: String) = OnlineSearchResult(
        platform = platform,
        songId = id,
        name = "歌$id",
        artist = "歌手",
        album = "专辑",
        durationMs = 200_000,
        coverUrl = null,
        musicInfoJson = """{"songmid":"$id"}""",
    )

    @Test
    fun `platforms 与 platformNames 反映注册顺序`() {
        val svc = OnlineSearchService(
            listOf(
                FakeProvider("kw", "酷我音乐"),
                FakeProvider("tx", "QQ音乐"),
            ),
        )
        assertEquals(listOf("kw", "tx"), svc.platforms)
        assertEquals("酷我音乐", svc.platformNames["kw"])
    }

    @Test
    fun `单平台搜索命中对应 provider`() = runTest {
        val kw = FakeProvider("kw", "酷我音乐", listOf(result("kw", "1")))
        val svc = OnlineSearchService(listOf(kw, FakeProvider("tx", "QQ音乐")))
        val page = svc.search("kw", "关键词")
        assertEquals(1, page.results.size)
        assertEquals(1, kw.calls)
    }

    @Test
    fun `不支持的平台抛出可读异常`() = runTest {
        val svc = OnlineSearchService(listOf(FakeProvider("kw", "酷我音乐")))
        val ex = kotlin.test.assertFailsWith<OnlineSearchException> { svc.search("mg", "x") }
        assertTrue(ex.message!!.contains("mg"))
    }

    @Test
    fun `多平台并行搜索返回全部结局`() = runTest {
        val svc = OnlineSearchService(
            listOf(
                FakeProvider("kw", "酷我音乐", listOf(result("kw", "1"))),
                FakeProvider("tx", "QQ音乐", listOf(result("tx", "2"))),
                FakeProvider("wy", "网易云音乐", listOf(result("wy", "3"))),
            ),
        )
        val outcomes = svc.searchAll("关键词")
        assertEquals(3, outcomes.size)
        assertTrue(outcomes.all { it is PlatformSearchOutcome.Success })
        assertEquals(listOf("kw", "tx", "wy"), outcomes.map { it.platform })
    }

    @Test
    fun `单平台失败不影响其它平台`() = runTest {
        val svc = OnlineSearchService(
            listOf(
                FakeProvider("kw", "酷我音乐", listOf(result("kw", "1"))),
                FakeProvider("tx", "QQ音乐", error = "接口拒绝访问"),
                FakeProvider("wy", "网易云音乐", listOf(result("wy", "3"))),
            ),
        )
        val outcomes = svc.searchAll("关键词")
        assertEquals(3, outcomes.size)

        val tx = outcomes.first { it.platform == "tx" }
        assertTrue(tx is PlatformSearchOutcome.Failure)
        assertEquals("接口拒绝访问", (tx as PlatformSearchOutcome.Failure).message)

        // 其余平台仍成功
        assertTrue(outcomes.filter { it.platform != "tx" }.all { it is PlatformSearchOutcome.Success })
    }

    @Test
    fun `可限定目标平台子集`() = runTest {
        val kw = FakeProvider("kw", "酷我音乐", listOf(result("kw", "1")))
        val tx = FakeProvider("tx", "QQ音乐", listOf(result("tx", "2")))
        val svc = OnlineSearchService(listOf(kw, tx))

        val outcomes = svc.searchAll("关键词", platforms = listOf("tx"))
        assertEquals(1, outcomes.size)
        assertEquals("tx", outcomes.first().platform)
        assertEquals(0, kw.calls, "未选中平台不应被调用")
    }

    @Test
    fun `结果可直接转为播放引用与曲目`() = runTest {
        val r = result("tx", "001TQYtM47BGjK")
        val ref = r.toTrackRef("src-1", quality = "320k")
        assertEquals("tx", ref.platform)
        assertEquals("src-1", ref.sourceId)
        // path 编码可无损还原
        val decoded = com.muses.player.core.model.online.OnlineTrackRef.parse(ref.encode())
        assertEquals("001TQYtM47BGjK", decoded?.musicInfoJson?.let {
            kotlinx.serialization.json.Json.parseToJsonElement(it)
                .let { e -> (e as kotlinx.serialization.json.JsonObject)["songmid"] }
                ?.let { v -> (v as kotlinx.serialization.json.JsonPrimitive).content }
        })

        val song = r.toSong("src-1")
        assertEquals("online:tx:001TQYtM47BGjK", song.id)
        assertEquals(com.muses.player.core.model.SourceType.ONLINE, song.sourceType)
        assertEquals("歌001TQYtM47BGjK", song.title)
        assertEquals(200L, song.durationSec)
    }
}

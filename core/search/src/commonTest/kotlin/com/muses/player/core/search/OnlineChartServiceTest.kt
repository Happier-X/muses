package com.muses.player.core.search

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlineChartServiceTest {
    private class FakeProvider(
        private val songs: List<OnlineSearchResult>,
        private val repeatPage: Boolean = false,
        private val failurePage: Int? = null,
    ) : OnlineChartProvider {
        override val platform = "tx"
        override val displayName = "测试平台"
        val requestedPages = mutableListOf<Int>()
        override suspend fun charts() = emptyList<OnlineChart>()
        override suspend fun chartSongs(chartId: String, page: Int, pageSize: Int): OnlineChartPage {
            requestedPages.add(page)
            if (page == failurePage) throw OnlineChartException(platform, "请求失败")
            val offset = if (repeatPage) 0 else (page - 1) * pageSize
            val results = songs.drop(offset).take(pageSize)
            return OnlineChartPage(platform, chartId, page, results, repeatPage || offset + results.size < songs.size)
        }
    }

    private fun songs(count: Int) = List(count) { index ->
        OnlineSearchResult("tx", index.toString(), "歌曲 $index", null, null, null, null, "{}")
    }

    @Test
    fun `超过三百首的榜单完整获取并保留排名`() = runTest {
        val expected = songs(350)
        val provider = FakeProvider(expected)
        assertEquals(expected, OnlineChartService(listOf(provider)).allChartSongs("tx", "1"))
        assertEquals(listOf(1, 2, 3, 4), provider.requestedPages)
    }

    @Test
    fun `不足一页的榜单只请求一次`() = runTest {
        val provider = FakeProvider(songs(20))
        assertEquals(20, provider.allChartSongs("1").size)
        assertEquals(listOf(1), provider.requestedPages)
    }

    @Test
    fun `重复页不会无限请求或被当作完整榜单`() = runTest {
        val provider = FakeProvider(songs(100), repeatPage = true)
        assertFailsWith<OnlineChartException> { provider.allChartSongs("1") }
        assertEquals(listOf(1, 2), provider.requestedPages)
    }

    @Test
    fun `后续页失败不会返回不完整榜单`() = runTest {
        val provider = FakeProvider(songs(200), failurePage = 2)
        assertFailsWith<OnlineChartException> { provider.allChartSongs("1") }
    }
}

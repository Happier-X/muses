package com.muses.player.feature.home

import com.muses.player.core.search.OnlinePlaylist
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeaturedPlaylistCacheTest {
    private val items = listOf(OnlinePlaylist("wy", "1", "歌单", "https://example.com/cover.jpg"))

    @Test
    fun 当天下拉更新歌单信息但保留选择和顺序() {
        val selected = listOf("3", "1", "2").map { OnlinePlaylist("wy", it, "旧标题", "") }
        val candidates = listOf("1", "2", "3", "4").map { OnlinePlaylist("wy", it, "新标题 $it", "") }
        val result = selectDailyFeaturedPlaylists(candidates, CachedFeaturedPlaylists(selected, 1000L, "2026-10-09"), "2026-10-09")

        assertEquals(listOf("3", "1", "2"), result.map { it.id })
        assertEquals(listOf("新标题 3", "新标题 1", "新标题 2"), result.map { it.title })
    }

    @Test
    fun 隔天重新选择且候选重复时只保留不同歌单() {
        val candidates = listOf("4", "4", "5", "6").map { OnlinePlaylist("wy", it, "新歌单", "") }
        val result = selectDailyFeaturedPlaylists(candidates, CachedFeaturedPlaylists(items, 1000L, "2026-10-08"), "2026-10-09")
        assertEquals(setOf("4", "5", "6"), result.map { it.id }.toSet())
        assertEquals(3, result.size)
    }

    @Test
    fun sameDaySelectionSurvivesOldSixHourLimit() {
        val cache = CachedFeaturedPlaylists(items, 1000L, "2026-10-08")
        assertTrue(cache.isFresh(1000L + 7 * 60 * 60 * 1000L, "2026-10-08"))
    }

    @Test
    fun nextDayRefreshesEvenWithinSixHours() {
        val cache = CachedFeaturedPlaylists(items, 1000L, "2026-10-08")
        assertFalse(cache.isFresh(2000L, "2026-10-09"))
    }

    @Test
    fun oldUndatedAndEmptyCachesRefresh() {
        assertFalse(CachedFeaturedPlaylists(items, 1000L).isFresh(2000L, "2026-10-08"))
        assertFalse(CachedFeaturedPlaylists(emptyList(), 1000L, "2026-10-08").isFresh(2000L, "2026-10-08"))
    }
}

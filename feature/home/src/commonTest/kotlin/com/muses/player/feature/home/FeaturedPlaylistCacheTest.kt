package com.muses.player.feature.home

import com.muses.player.core.search.OnlinePlaylist
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeaturedPlaylistCacheTest {
    private val items = listOf(OnlinePlaylist("wy", "1", "歌单", "https://example.com/cover.jpg"))

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

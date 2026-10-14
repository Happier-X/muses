package com.muses.player.feature.home

import com.muses.player.core.search.OnlinePlaylist
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeaturedPlaylistCacheTest {
    private val items = listOf(OnlinePlaylist("wy", "1", "歌单", "https://example.com/cover.jpg"))

    @Test fun 推荐歌单在有效期内使用缓存且不受日期变化影响() {
        val cache = CachedFeaturedPlaylists(items, 1000L, day = "旧日期", recommended = true)
        assertTrue(cache.isFresh(2000L))
        assertFalse(cache.isFresh(1000L + 6 * 60 * 60 * 1000L))
    }

    @Test fun 旧每日精选缓存仅能作为失败回退不能阻止加载推荐目录() {
        assertFalse(CachedFeaturedPlaylists(items, 1000L, day = "2026-10-14").isFresh(2000L))
        assertFalse(CachedFeaturedPlaylists(items, 1000L).isFresh(2000L))
    }

    @Test fun 空缓存及未来时间必须重新加载() {
        assertFalse(CachedFeaturedPlaylists(emptyList(), 1000L, recommended = true).isFresh(2000L))
        assertFalse(CachedFeaturedPlaylists(items, 2000L, recommended = true).isFresh(1000L))
    }
}

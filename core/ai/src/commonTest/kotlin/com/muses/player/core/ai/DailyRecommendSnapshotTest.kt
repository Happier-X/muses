package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyRecommendSnapshotTest {
    private val profile = LibraryProfile(
        2, emptyList(), emptyList(), emptyList(), emptyList(),
        mapOf("晴天".normalizeForMatch() to setOf("周杰伦".normalizeForMatch())),
    )

    @Test
    fun 同名不同歌手不会误删() {
        assertTrue(profile.containsSong("晴天 (Live)", "周杰伦"))
        assertFalse(profile.containsSong("晴天", "其他歌手"))
        assertTrue(profile.containsSong("晴天", null))
    }

    @Test
    fun 当天缓存恢复播放字段并排除刚入库歌曲() {
        val result = AiRecommendResult(
            listOf(
                track("晴天", "周杰伦"),
                track("海阔天空", "Beyond"),
            ),
            2, emptyList(),
        )
        val serialized = DailyRecommendSnapshot.encode("2026-09-25", result)
        val restored = DailyRecommendSnapshot.decode(serialized, "2026-09-25", profile)
        assertEquals(1, restored?.matched)
        assertEquals("海阔天空", restored?.tracks?.single()?.result?.name)
        assertEquals("{}", restored?.tracks?.single()?.result?.musicInfoJson)
        assertNull(DailyRecommendSnapshot.decode(serialized, "2026-09-26", profile))
        assertNull(DailyRecommendSnapshot.decode("已损坏", "2026-09-25", profile))
    }

    @Test
    fun 已显示推荐在歌曲进入曲库后会被剔除() {
        val result = AiRecommendResult(
            listOf(track("光年之外", "邓紫棋"), track("海阔天空", "Beyond")),
            2, emptyList(),
        )
        val webDavProfile = LibraryProfile(
            1, emptyList(), emptyList(), emptyList(), emptyList(),
            mapOf("光年之外".normalizeForMatch() to setOf("邓紫棋".normalizeForMatch())),
        )

        val filtered = result.excludingOwnedSongs(webDavProfile)

        assertEquals(listOf("海阔天空"), filtered.tracks.map { it.result.name })
    }

    private fun track(name: String, artist: String): AiRecommendedTrack = AiRecommendedTrack(
        AiSongSuggestion(name, artist, "推荐理由"),
        OnlineSearchResult("wy", name, name, artist, null, null, null, "{}"),
    )
}

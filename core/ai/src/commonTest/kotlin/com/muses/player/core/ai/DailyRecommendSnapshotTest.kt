package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.data.db.SongEntity
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
    fun 当天全部入库后缓存仍然有效且次日才过期() {
        val result = AiRecommendResult(listOf(track("晴天", "周杰伦")), 1, emptyList())
        val snapshot = DailyRecommendSnapshot.encode("2026-10-07", result)
        assertEquals(0, DailyRecommendSnapshot.decode(snapshot, "2026-10-07", profile)?.matched)
        assertEquals(1, DailyRecommendSnapshot.decode(snapshot, "2026-10-07")?.matched)
        assertNull(DailyRecommendSnapshot.decode(snapshot, "2026-10-08"))
    }

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

    @Test
    fun 未读取标签的本地与WebDAV文件名按歌手和歌名去重() {
        val songs = listOf(
            song("1", "WEBDAV", "Marshmello - Alone"),
            song("2", "LOCAL", "01 - 周杰伦 — 晴天"),
            song("3", "ONLINE", "Beyond - 海阔天空"),
            song("4", "LOCAL", "甲 - 乙").copy(artist = "丙", tagsVersion = 1),
        )
        val owned = LibraryProfile(songs.size, emptyList(), emptyList(), emptyList(), emptyList(), buildOwnedSongIndex(songs))
        assertTrue(owned.containsSong("Alone", "Marshmello"))
        assertTrue(owned.containsSong("晴天", "周杰伦"))
        assertFalse(owned.containsSong("Alone", "其他歌手"))
        assertFalse(owned.containsSong("海阔天空", "Beyond"))
        assertFalse(owned.containsSong("乙", "甲"))
        val result = AiRecommendResult(
            listOf(track("Alone", "Marshmello"), track("晴天", "周杰伦"), track("海阔天空", "Beyond")),
            3, emptyList(),
        )
        assertEquals(listOf("海阔天空"), result.excludingOwnedSongs(owned).tracks.map { it.result.name })
        assertEquals(1, DailyRecommendSnapshot.decode(DailyRecommendSnapshot.encode("2026-09-27", result), "2026-09-27", owned)?.matched)
    }

    private fun song(id: String, sourceType: String, title: String) = SongEntity(
        id = id, sourceId = sourceType, sourceType = sourceType, path = title, title = title,
    )

    private fun track(name: String, artist: String): AiRecommendedTrack = AiRecommendedTrack(
        AiSongSuggestion(name, artist, "推荐理由"),
        OnlineSearchResult("wy", name, name, artist, null, null, null, "{}"),
    )
}

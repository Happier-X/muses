package com.muses.player.core.ai

import com.muses.player.core.data.repository.PlayStats
import com.muses.player.core.data.repository.SongPlayStat
import com.muses.player.core.model.playback.RecentOnlineTrack
import com.muses.player.core.model.playback.RecentPlayEntry
import com.muses.player.core.search.OnlineSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalRecommendationTest {
    private val now = 1_800_000_000_000L
    private fun recent(id: String, title: String, artist: String, days: Int = 0) = RecentPlayEntry(
        id, title, "$artist - 专辑", playedAt = now - days * 86_400_000L,
        onlineTrack = RecentOnlineTrack("lx", "本机引用", artist),
    )
    private fun stat(id: String, title: String, artist: String, count: Int) =
        SongPlayStat(id, title, "$artist - 专辑", playCount = count)
    private fun track(title: String, artist: String) = AiRecommendedTrack(
        AiSongSuggestion(title, artist), OnlineSearchResult("wy", title, title, artist, null, null, null, "{}"),
    )
    private fun profile() = LibraryProfile(1, emptyList(), emptyList(), emptyList(), listOf("曲库歌曲 - 曲库歌手"))

    @Test fun LX近期播放参与本机权重且次数封顶() {
        val history = listOf(recent("new", "新歌", "近期歌手"), recent("old", "旧歌", "旧歌手", 100))
        val stats = PlayStats(songs = mapOf("new" to stat("new", "新歌", "近期歌手", 2),
            "old" to stat("old", "旧歌", "旧歌手", 10000)))
        val taste = buildLocalListeningTaste(emptyList(), history, stats, now)
        assertTrue(taste.artistWeights.getValue("近期歌手") > taste.artistWeights.getValue("旧歌手"))
        val capped = stats.copy(songs = stats.songs + ("old" to stat("old", "旧歌", "旧歌手", 25)))
        assertEquals(taste.artistWeights, buildLocalListeningTaste(emptyList(), history, capped, now).artistWeights)
        assertEquals(setOf("近期歌手"), taste.recentSongs["新歌"])
        assertFalse("旧歌" in taste.recentSongs)
    }

    @Test fun 跨平台同曲去重未知歌手不污染本机画像() {
        val single = recent("kw:1", "作品", "歌手")
        val expected = buildLocalListeningTaste(emptyList(), listOf(single), PlayStats.Empty, now)
        val actual = buildLocalListeningTaste(emptyList(), listOf(single, single.copy(songId = "wy:1"),
            recent("bad", "占位歌", "未知艺术家")), PlayStats.Empty, now)
        assertEquals(expected, actual)
    }

    @Test fun 行为索引不进入外部AI提示词() {
        val basic = profile()
        val withHistory = basic.copy(artistWeights = mapOf("私密歌手" to 100.0), knownArtists = setOf("私密歌手"),
            recentSongs = mapOf("私密歌曲" to setOf("私密歌手")))
        assertEquals(basic.toPromptText(), withHistory.toPromptText())
        assertFalse(withHistory.toPromptText().contains("私密"))
        assertEquals(AiRecommendService.buildUserPrompt(basic, 20), AiRecommendService.buildUserPrompt(withHistory, 20))
    }

    @Test fun 跨平台旧高频不会覆盖最近听过的同一作品() {
        val taste = buildLocalListeningTaste(emptyList(), listOf(recent("new", "同一作品", "歌手")),
            PlayStats(songs = mapOf("old" to stat("old", "同一作品", "歌手", 25))), now)
        assertEquals(setOf("歌手"), taste.recentSongs["同一作品"])
        assertEquals(1, taste.artistWeights.size)
    }

    @Test fun 只有专辑的副标题不会作为歌手参与排序() {
        val entry = RecentPlayEntry("no-artist", "作品", "只有专辑", playedAt = now)
        val taste = buildLocalListeningTaste(emptyList(), listOf(entry), PlayStats.Empty, now)
        assertTrue(taste.artistWeights.isEmpty())
        assertTrue(taste.recentSongs.isEmpty())
    }

    @Test fun 默认二十首分配十四首熟悉歌手六首发现且限制歌手数量() {
        val known = (1..6).map { "熟悉歌手$it" }.toSet()
        val tracks = known.flatMap { artist -> (1..5).map { track("$artist 的作品$it", artist) } } +
            (1..8).map { track("新作品$it", "新歌手$it") }
        val profile = profile().copy(knownArtists = known,
            artistWeights = known.associateWith { if (it == "熟悉歌手6") 10.0 else 1.0 })
        val selected = balanceRecommendations(tracks, profile, 20)
        assertEquals(20, selected.size)
        assertEquals(14, selected.count { it.result.artist in known })
        assertEquals(6, selected.count { it.result.artist !in known })
        assertTrue(selected.groupingBy { it.result.artist }.eachCount().values.all { it <= 3 })
        assertEquals("熟悉歌手6", selected.first().result.artist)
        assertTrue(selected[2].result.artist !in known)
    }

    @Test fun 不足比例从剩余候选补足但不放宽同歌手上限() {
        val oneArtist = (1..10).map { track("作品$it", "同一歌手") }
        assertEquals(3, balanceRecommendations(oneArtist, profile(), 20).size)
        val onlyDiscovery = (1..20).map { track("作品$it", "新歌手$it") }
        assertEquals(20, balanceRecommendations(onlyDiscovery, profile(), 20).size)
    }

    @Test fun 新生成与缓存再次排序都过滤已拥有和最近听过且不会误删同名不同歌手() {
        val profile = profile().copy(ownedSongs = mapOf("已拥有" to setOf("歌手")),
            recentSongs = mapOf("听过" to setOf("歌手")))
        val result = AiRecommendResult(listOf(track("已拥有", "歌手"), track("听过", "歌手"),
            track("听过", "其他歌手"), track("新作品", "歌手")), 4, emptyList())
        val selected = result.personalized(profile)
        assertEquals(listOf("其他歌手", "歌手"), selected.tracks.map { it.result.artist })
        assertEquals(selected, selected.personalized(profile))
    }
}

package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendPoolTest {
    private val profile = LibraryProfile(1, emptyList(), emptyList(), emptyList(), emptyList())
    private fun track(name: String, artist: String = "歌手", platform: String = "wy") = AiRecommendedTrack(
        AiSongSuggestion(name, artist, "推荐理由"),
        OnlineSearchResult(platform, name, name, artist, null, null, null, "{}"))
    private fun result(vararg tracks: AiRecommendedTrack) = AiRecommendResult(tracks.toList(), tracks.size, emptyList())

    @Test
    fun 跨天迁移旧缓存并保存喜欢和无感反馈() {
        val legacy = DailyRecommendSnapshot.encode("2020-01-01", result(track("歌曲甲"), track("歌曲乙"), track("歌曲丙")))
        val pool = RecommendPoolSnapshot.decode(legacy).respond("歌曲甲", "歌手", true).respond("歌曲乙", "歌手", false)
        val restored = RecommendPoolSnapshot.decode(RecommendPoolSnapshot.encode(pool))
        assertEquals(listOf("歌曲丙"), restored.result.tracks.map { it.result.name })
        assertEquals(listOf(true, false), restored.feedback.map { it.liked })
        assertEquals("{}", restored.result.tracks.single().result.musicInfoJson)
    }

    @Test
    fun 无感在补歌请求期间发生也不会被跨平台候选放回() {
        val pool = RecommendPool(result(track("歌曲甲"), track("歌曲乙"))).respond("歌曲甲", "歌手", false)
        val merged = pool.merge(result(track("歌曲甲", platform = "tx"), track("歌曲丙")), profile)
        assertEquals(listOf("歌曲乙", "歌曲丙"), merged.result.tracks.map { it.result.name })
        assertTrue(merged.profile(profile).containsSong("歌曲甲", "歌手"))
    }

    @Test
    fun 只补空位保留顺序去重且不超过二十首() {
        val pool = RecommendPool(AiRecommendResult((1..19).map { track("歌曲$it") }, 19, emptyList()))
        val merged = pool.merge(result(track("歌曲1", platform = "tx"), track("歌曲20"), track("歌曲21")), profile)
        assertEquals((1..20).map { "歌曲$it" }, merged.result.tracks.map { it.result.name })
    }

    @Test
    fun 成功下载覆盖无感并作为喜欢画像同名不同歌手仍可推荐() {
        val pool = RecommendPool(result(track("歌曲甲"))).respond("歌曲甲", "歌手", false)
            .respond("歌曲甲", "歌手", true)
        assertEquals(1, pool.feedback.size)
        assertTrue(pool.feedback.single().liked)
        val personalized = pool.profile(profile)
        assertTrue(personalized.artistWeights.getValue("歌手") > 0)
        assertTrue(personalized.sampleTracks.contains("歌曲甲 - 歌手"))
        assertEquals(1, pool.merge(result(track("歌曲甲", "独立艺人")), profile).result.matched)
    }

    @Test
    fun 新入库歌曲剔除而未下载未反馈歌曲保持显示() {
        val pool = RecommendPool(result(track("歌曲甲"), track("歌曲乙")))
        val owned = profile.copy(ownedSongs = mapOf("歌曲甲" to setOf("歌手")))
        assertEquals(listOf("歌曲乙"), pool.merge(result(), owned).result.tracks.map { it.result.name })
        assertEquals(2, pool.merge(result(), profile).result.matched)
    }
}

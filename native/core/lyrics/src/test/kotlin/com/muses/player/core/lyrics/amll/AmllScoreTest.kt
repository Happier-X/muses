package com.muses.player.core.lyrics.amll

import com.muses.player.core.model.lyrics.AmllIndexEntry
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.scrape.MatchConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规格 = src/features/lyrics/score.ts 评分权重/分级 + amllTtmlDb.ts 索引解析与候选选择 */
class AmllScoreTest {

    private fun entry(
        name: String,
        artists: List<String> = listOf("Taylor Swift"),
        album: String? = "Fearless",
        duration: Double? = null,
    ) = AmllIndexEntry(musicName = name, artists = artists, album = album, durationSec = duration, rawLyricFile = "$name.ttml")

    @Test
    fun `scoreTitle分级`() {
        assertEquals(TitleMatchLevel.EXACT, scoreTitle("Love Story", "love story").level)
        assertEquals(ScoreWeights.TITLE_EXACT, scoreTitle("Love Story", "love story").score)
        // Web normalizeText 会剥掉 (Live) 等后缀词 → 两边 normalize 相等即 EXACT
        assertEquals(TitleMatchLevel.EXACT, scoreTitle("Love Story (Live)", "Love Story").level)
        // 真正的包含关系：候选为查询子串
        assertEquals(TitleMatchLevel.CONTAINS, scoreTitle("Love Story Deluxe Edition", "Love Story").level)
        assertEquals(TitleMatchLevel.NONE, scoreTitle("Love Story", "Unrelated").level)
    }

    @Test
    fun `classifyMatch_时长偏差超5秒降low`() {
        val q = AmllMatchQuery(songId = "s1", title = "Song", artist = "A B", durationSec = 200.0)
        val e = entry("Song", artists = listOf("A B"), duration = 210.0) // 偏差 10s
        val tm = scoreTitle("Song", "Song")
        assertEquals(MatchConfidence.LOW, classifyMatch(q, e, tm, 25))
    }

    @Test
    fun `classifyMatch_exact且artist命中为high`() {
        val q = AmllMatchQuery(songId = "s1", title = "Song", artist = "Taylor Swift")
        val e = entry("Song")
        val tm = scoreTitle("Song", "Song")
        assertEquals(25, scoreArtists("Taylor Swift", listOf("Taylor Swift")))
        assertEquals(MatchConfidence.HIGH, classifyMatch(q, e, tm, 25))
    }

    @Test
    fun `classifyMatch_contains需artist命中才high`() {
        val q = AmllMatchQuery(songId = "s1", title = "Love Story Live", artist = "Someone Else")
        val e = entry("Love Story")
        val tm = scoreTitle(q.title, e.musicName)
        assertEquals(MatchConfidence.LOW, classifyMatch(q, e, tm, scoreArtists(q.artist, e.artists)))
    }

    @Test
    fun `findBestMatch_低于最低分过滤并取最高`() {
        val index = listOf(entry("Unrelated"), entry("Love Story"), entry("Love Story Deluxe"))
        val best = findBestMatch(
            AmllMatchQuery(songId = "s1", title = "Love Story"),
            index,
        )
        assertTrue(best != null)
        // exact（100）+ artist（25）= 125 高于 contains 分支
        assertEquals("Love Story", best?.entry?.musicName)
        assertEquals(MatchConfidence.HIGH, best?.confidence)

        assertNull(findBestMatch(AmllMatchQuery(songId = "s1", title = "Totally Different"), index))
    }

    @Test
    fun `parseIndexLine宽松解析jsonl`() {
        val line = """{"metadata":[["musicName",["Love Story"]],["artists",["Taylor Swift","Ed"]],["album",["Fearless"]]],"rawLyricFile":"ls.ttml"}"""
        val e = parseIndexLine(line)
        assertEquals("Love Story", e?.musicName)
        assertEquals(listOf("Taylor Swift", "Ed"), e?.artists)
        assertEquals("Fearless", e?.album)
        assertNull(parseIndexLine("""{"rawLyricFile":"x.ttml"}"""))   // 缺 musicName
        assertNull(parseIndexLine("not json"))
        assertNull(parseIndexLine(""))
    }

    @Test
    fun `搜索索引_exact与trigram与short桶`() {
        val entries = listOf(
            entry("Love Story"),
            entry("爱"),
            entry("Unrelated Song"),
        )
        val index = createSearchIndex(entries)

        // exact 命中
        assertEquals(1, index.selectCandidates("love story").size)
        // trigram：包含 "lov" 桶 → 命中第一条
        assertTrue(index.selectCandidates("My Love Story").any { it.musicName == "Love Story" })
        // 短标题 short: 桶（长度 <3）
        assertTrue(index.selectCandidates("爱").any { it.musicName == "爱" })
        // 无关标题零候选或不含目标
        val unrelated = index.selectCandidates("Completely Different Words Here")
        assertTrue(unrelated.none { it.musicName == "爱" })
    }
}

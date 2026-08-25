package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.MatchConfidence
import org.junit.Assert.assertEquals
import org.junit.Test

/** 规格 = src/features/metadata/util.ts classifyTextMetaConfidence（child4 R4-2）逐条规则 */
class TextMetaConfidenceTest {

    @Test
    fun `title或query为空归low`() {
        assertEquals(MatchConfidence.LOW, classifyTextMetaConfidence(null, "A", "T", "A"))
        assertEquals(MatchConfidence.LOW, classifyTextMetaConfidence("T", "A", null, "A"))
        assertEquals(MatchConfidence.LOW, classifyTextMetaConfidence("", "A", "T", "A"))
        assertEquals(MatchConfidence.LOW, classifyTextMetaConfidence("T", "A", "", null))
    }

    @Test
    fun `title不相关归low`() {
        assertEquals(MatchConfidence.LOW, classifyTextMetaConfidence("ABC", "A", "XYZ", "A"))
    }

    @Test
    fun `title exact 且无查询歌手信息归high`() {
        // 无 artist 信息（JS !qArtist）
        assertEquals(MatchConfidence.HIGH, classifyTextMetaConfidence("Love Story", null, "Love Story", null))
        assertEquals(MatchConfidence.HIGH, classifyTextMetaConfidence("Love Story", null, "love story ", ""))
    }

    @Test
    fun `title exact 且 artist 命中归high`() {
        assertEquals(
            MatchConfidence.HIGH,
            classifyTextMetaConfidence("Love Story", "Taylor Swift", "love story", "Taylor Swift"),
        )
        // 互相包含也算 artist 命中
        assertEquals(
            MatchConfidence.HIGH,
            classifyTextMetaConfidence("Love Story", "Swift", "love story", "Taylor Swift"),
        )
    }

    @Test
    fun `title exact 但 artist 不命中归low`() {
        assertEquals(
            MatchConfidence.LOW,
            classifyTextMetaConfidence("Love Story", "Someone Else", "Love Story", "Taylor Swift"),
        )
    }

    @Test
    fun `title contains 无 artist 信息归low`() {
        assertEquals(
            MatchConfidence.LOW,
            classifyTextMetaConfidence("Love Story Deluxe", null, "Love Story", ""),
        )
    }

    @Test
    fun `title contains 且 artist 命中归high`() {
        assertEquals(
            MatchConfidence.HIGH,
            classifyTextMetaConfidence("Love Story (Live)", "Taylor Swift", "Love Story", "taylor swift"),
        )
    }

    @Test
    fun `title contains 但 artist 不命中归low`() {
        assertEquals(
            MatchConfidence.LOW,
            classifyTextMetaConfidence("Love Story (Live)", "Cover Man", "Love Story", "Taylor Swift"),
        )
    }
}

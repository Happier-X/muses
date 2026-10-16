package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TextMatchEvidenceTest {
    private fun evidence(query: String, candidate: String) = evaluateTextMatch(
        TextMetaHit(candidate, "歌手", "专辑", OnlineTextSource.WY),
        OnlineTextQuery("歌曲", query, artist = "歌手", album = "专辑"),
    )

    @Test fun `中英文现场标记视为同一版本`() {
        val result = evidence("歌曲（现场版）", "歌曲 (Live)")
        assertTrue(result.reasons.any { it == "版本标记一致（现场版）" })
        assertTrue("标题主体一致" in result.reasons)
        assertTrue(result.warnings.none { "版本" in it })
    }

    @Test fun `明确的版本冲突可见且降低排序`() {
        val same = evidence("歌曲 (Live)", "歌曲 (Live)")
        val conflict = evidence("歌曲 (Live)", "歌曲 (Remix)")
        assertTrue(conflict.warnings.any { "版本标记不同" in it })
        assertTrue(same.score > conflict.score)
        assertFalse(same.requiresReview)
        assertTrue(conflict.requiresReview)
    }

    @Test fun `没有标记时不推断为录音室版`() {
        assertTrue(evidence("歌曲", "歌曲 (Live)").warnings.any { "搜索词未标明版本" in it })
        assertTrue(evidence("歌曲 (Live)", "歌曲").warnings.any { "候选未标明版本" in it })
        assertTrue(evidence("歌曲", "歌曲").warnings.isEmpty())
        assertTrue(evidence("歌曲", "歌曲 (Live)").requiresReview)
        assertTrue(evidence("歌曲 (Live)", "歌曲").requiresReview)
    }

    @Test fun `普通歌名中的单词与子串不作为版本`() {
        for (title in listOf("Live Forever", "Long Live", "Deliver", "Remixed Memories", "现场", "重制人生", "歌曲 (Live Forever)", "歌曲 - Live Forever", "歌曲（现场的你）")) {
            val result = evidence(title, title)
            assertTrue(result.reasons.none { "版本标记" in it }, title)
            assertTrue(result.warnings.isEmpty(), title)
            assertFalse(result.requiresReview, title)
        }
    }

    @Test fun `正常歌名中的版本单词不会被删掉后声称一致`() {
        val result = evidence("Live Forever", "Forever")
        assertTrue("标题主体一致" !in result.reasons)
        assertTrue("标题主体相近" in result.reasons)
    }

    @Test fun `识别分隔符中文后缀和全角括号`() {
        for ((query, candidate) in listOf(
            "歌曲 (Remastered)" to "歌曲 - 2024 Remaster",
            "歌曲 (Acoustic)" to "歌曲【不插电版】",
            "歌曲 (Instrumental)" to "歌曲伴奏版",
            "歌曲 (Demo)" to "歌曲（样带版）",
            "歌曲 (Live)" to "歌曲 (Live at Wembley)",
        )) assertTrue(evidence(query, candidate).reasons.any { "版本标记一致" in it }, candidate)
    }

    @Test fun `不同组合版本不能视为一致`() {
        val result = evidence("歌曲 (Live Acoustic)", "歌曲 (Live)")
        assertTrue(result.warnings.any { "版本标记不同" in it })
    }

    @Test fun `字段缺失不声称一致且显示真实差异`() {
        val result = evaluateTextMatch(
            TextMetaHit("另一首歌", "另一位歌手", source = OnlineTextSource.WY),
            OnlineTextQuery("歌曲", "歌曲", artist = "歌手", album = "专辑"),
        )
        assertTrue("标题主体不同" in result.warnings)
        assertTrue(result.reasons.none { "专辑" in it })
        val empty = evaluateTextMatch(TextMetaHit(source = OnlineTextSource.WY), OnlineTextQuery("歌曲", "歌曲"))
        assertTrue(empty.reasons.isEmpty())
        assertTrue(empty.warnings.isEmpty())
    }

    @Test fun `身份冲突需要人工确认但单独专辑差异不阻止预选`() {
        val query = OnlineTextQuery("歌曲", "曲名", artist = "甲", album = "旧专辑")
        assertTrue(evaluateTextMatch(TextMetaHit("另一首", "甲", source = OnlineTextSource.WY), query).requiresReview)
        assertTrue(evaluateTextMatch(TextMetaHit("曲名", "乙", source = OnlineTextSource.WY), query).requiresReview)
        val albumOnly = evaluateTextMatch(TextMetaHit("曲名", "甲", "新专辑", OnlineTextSource.WY), query)
        assertFalse(albumOnly.requiresReview)
        assertTrue("专辑不同" in albumOnly.warnings)
    }
}

package com.muses.player.feature.scrape

import com.muses.player.core.ai.AiScrapeChoice
import com.muses.player.core.ai.AiScrapeDecision
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.editmeta.EditDimResult
import com.muses.player.core.scrape.editmeta.EditDimStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrapeTextSelectionTest {
    private fun review() = ScrapeReviewState.Review(
        song = Song("歌曲", "来源", "/歌曲.mp3", "本地标题", "本地歌手", "本地专辑"),
        text = EditDimResult(EditDimStatus.OK, listOf(
            TextMetaHit("标题甲", "歌手甲", "专辑甲", OnlineTextSource.KW),
            TextMetaHit("标题乙", "歌手乙", "专辑乙", OnlineTextSource.WY),
            TextMetaHit("标题丙", "歌手丙", null, OnlineTextSource.KG),
        )),
        cover = EditDimResult(EditDimStatus.NO_MATCH, emptyList()),
        lyrics = EditDimResult(EditDimStatus.NO_MATCH, emptyList()),
        checkedFields = setOf("title", "artist", "album", "cover", "lyrics"),
    )

    @Test fun `毫秒时长参与默认选择且明显不符时不预勾选`() {
        val song = review().song.copy(durationMs = 180000, durationSec = 0)
        val keyword = ReviewKeyword(song.title, song.artist.orEmpty(), song.album.orEmpty())
        val hit = TextMetaHit(song.title, song.artist, "新专辑", OnlineTextSource.WY, durationSec = 210.0)
        assertTrue(defaultTextCheckedFields(song, hit, keyword).isEmpty())
        assertEquals(setOf("album"), defaultTextCheckedFields(song, hit.copy(durationSec = 181.0), keyword))
    }

    @Test fun `全选跳过空白候选并支持手改补全`() {
        val state = review().copy(text = EditDimResult(EditDimStatus.OK, listOf(
            TextMetaHit(null, "", "  ", OnlineTextSource.KW),
        )))
        assertTrue(selectableFields(state).isEmpty())
        assertEquals(listOf("title", "artist"), selectableFields(state.copy(
            editTitle = "手改标题", editArtist = "手改歌手",
        )))
        assertEquals(listOf("album"), selectableFields(state.copy(editAlbum = "手改专辑")))
    }

    @Test fun `三个字段可组合不同来源且各自保留来源记录`() {
        val result = review().withTextFieldCandidate(ReviewTextField.TITLE, 1)
            .withTextFieldCandidate(ReviewTextField.ARTIST, 2)
        assertEquals("标题乙", result.resolvedTitle())
        assertEquals("歌手丙", result.resolvedArtist())
        assertEquals("专辑甲", result.resolvedAlbum())
        assertEquals(OnlineTextSource.WY, result.fieldHit(ReviewTextField.TITLE)?.source)
        assertEquals(OnlineTextSource.KG, result.fieldHit(ReviewTextField.ARTIST)?.source)
        assertEquals(OnlineTextSource.KW, result.fieldHit(ReviewTextField.ALBUM)?.source)
        assertEquals(review().checkedFields, result.checkedFields)
    }

    @Test fun `有身份或版本疑点的候选不预勾选且专辑差异可以预选`() {
        val song = review().song
        val keyword = ReviewKeyword("本地标题", "本地歌手", "本地专辑")
        for (hit in listOf(
            TextMetaHit("无关曲目", "本地歌手", "新专辑", OnlineTextSource.KW),
            TextMetaHit("本地标题", "另一人", "新专辑", OnlineTextSource.KW),
            TextMetaHit("本地标题 (Live)", "本地歌手", "新专辑", OnlineTextSource.KW),
        )) assertTrue(defaultTextCheckedFields(song, hit, keyword).isEmpty())
        assertEquals(setOf("album"), defaultTextCheckedFields(song,
            TextMetaHit("本地标题", "本地歌手", "新专辑", OnlineTextSource.KW), keyword))
    }

    @Test fun `AI不能预勾选冲突候选但保留手选字段及其勾选`() {
        val original = review().copy(keyword = ReviewKeyword("标题甲", "歌手甲"))
            .withTextFieldCandidate(ReviewTextField.ALBUM, 0)
            .copy(editTitle = "手改标题", checkedFields = setOf("title", "album", "cover"))
        val result = original.withAiSelection(AiScrapeDecision(
            AiScrapeChoice(1, "选择存在疑点的候选"), AiScrapeChoice(null, "不更改歌词"),
        ))
        assertEquals(1, result.selectedTextIndex)
        assertEquals(setOf("title", "album", "cover"), result.checkedFields)
        assertEquals("手改标题", result.resolvedTitle())
        assertEquals("专辑甲", result.resolvedAlbum())
        assertFalse("artist" in result.checkedFields)
    }

    @Test fun `切换字段只清除自己的覆写并保留其他手改`() {
        val result = review().copy(editTitle = "手改标题", editArtist = "手改歌手", editAlbum = "手改专辑")
            .withTextFieldCandidate(ReviewTextField.ARTIST, 1)
        assertEquals("手改标题", result.resolvedTitle())
        assertEquals("歌手乙", result.resolvedArtist())
        assertEquals("手改专辑", result.resolvedAlbum())
        assertEquals(null, result.editArtist)
    }

    @Test fun `越界和无值候选不会覆盖原选择`() {
        val original = review().copy(editAlbum = "手改专辑")
        assertEquals(original, original.withTextFieldCandidate(ReviewTextField.ALBUM, 2))
        assertEquals(original, original.withTextFieldCandidate(ReviewTextField.TITLE, -1))
        assertEquals(original, original.withTextFieldCandidate(ReviewTextField.TITLE, 99))
    }

    @Test fun `选择与本地相同的值只取消该字段勾选`() {
        val original = review()
        val same = original.copy(text = original.text.copy(items = original.text.items +
            TextMetaHit("本地标题", source = OnlineTextSource.TX)))
        val result = same.withTextFieldCandidate(ReviewTextField.TITLE, 3)
        assertFalse("title" in result.checkedFields)
        assertTrue("artist" in result.checkedFields)
        assertTrue("cover" in result.checkedFields)
    }

    @Test fun `再次使用AI仍保留手选来源和手动取消的勾选`() {
        val original = review().withTextFieldCandidate(ReviewTextField.ALBUM, 1)
            .copy(checkedFields = setOf("artist"))
        val result = original.withAiSelection(AiScrapeDecision(
            AiScrapeChoice(2, "选择另一候选"), AiScrapeChoice(null, "不更改歌词"),
        ))
        assertEquals("专辑乙", result.resolvedAlbum())
        assertFalse("album" in result.checkedFields)
        assertEquals("标题丙", result.resolvedTitle())
        assertEquals(OnlineTextSource.WY, result.fieldHit(ReviewTextField.ALBUM)?.source)
    }
}

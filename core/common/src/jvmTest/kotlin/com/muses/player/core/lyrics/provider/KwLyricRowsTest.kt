package com.muses.player.core.lyrics.provider

import com.muses.player.core.lyrics.parser.LrcLyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KwLyricRowsTest {
    private val rows = listOf(
        LrcLine(0.0, "示例歌曲"),
        LrcLine(46.95, "以下歌词翻译由文曲大模型提供"),
        LrcLine(46.95, "First line"), LrcLine(50.41, "第一行译文"),
        LrcLine(50.41, "Second line"), LrcLine(53.15, "第二行译文"),
        LrcLine(53.15, "Last line"), LrcLine(56.74, "末行译文"),
    )

    @Test
    fun 结束时间标记的译文配到原文起点() {
        val hit = parseKwLyricRows(rows)!!
        val document = LrcLyricsParser.parse(hit.text, translation = hit.translationText.orEmpty())
        assertEquals(4, document.lines.size)
        assertNull(document.lines[0].translation)
        assertEquals("第一行译文", document.lines[1].translation)
        assertEquals(46950L, document.lines[1].timeMs)
        assertEquals("第二行译文", document.lines[2].translation)
        assertEquals("末行译文", document.lines[3].translation)
    }

    @Test
    fun 空白翻译声明仍按连续配对还原() {
        val changed = rows.toMutableList().apply { this[1] = this[1].copy(text = "   ") }
        val hit = parseKwLyricRows(changed)!!
        val document = LrcLyricsParser.parse(hit.text, translation = hit.translationText.orEmpty())
        assertEquals(4, document.lines.size)
        assertEquals("第一行译文", document.lines[1].translation)
        assertEquals("末行译文", document.lines[3].translation)
    }

    @Test
    fun 非连续配对的双语歌词保持原样() {
        val changed = rows.toMutableList().apply { this[3] = this[3].copy(time = 49.0) }
        val hit = parseKwLyricRows(changed)!!
        assertNull(hit.translationText)
        assertEquals(linesToLrc(changed), hit.text)
    }
}

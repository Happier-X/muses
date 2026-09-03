package com.muses.player.core.lyrics.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QRC 逐字解析单测：真实形态为三元 timing `(start,duration,0)`，
 * 每行 `[行头](timing)字(timing)字…`，字在 timing 之后。
 */
class QQMusicQrcLyricsParserTest {

    @Test
    fun `三元 timing 逐字切分且行尾字不丢`() {
        // 截图《为何我》真实形态
        val raw = "[42320,230](42320,230,0)虽(42550,150,0)然(42700,460,0)少(43160,1810,0)了"
        val doc = QQMusicQrcLyricsParser.parse(raw)
        assertEquals(1, doc.lines.size)
        val line = doc.lines.first()
        assertEquals("虽然少了", line.text)
        assertFalse(line.text.contains("("))
        assertEquals(4, line.syllables.size)
        assertEquals("虽", line.syllables[0].text)
        assertEquals(42320L, line.syllables[0].startTimeMs)
        assertEquals("了", line.syllables[3].text)
    }

    @Test
    fun `单 timing 行正文不丢`() {
        val doc = QQMusicQrcLyricsParser.parse("[0,500](0,500,0)Hello World")
        assertEquals(1, doc.lines.size)
        assertEquals("Hello World", doc.lines.first().text)
        assertEquals(1, doc.lines.first().syllables.size)
    }

    @Test
    fun `二元 timing 老形态兼容`() {
        val doc = QQMusicQrcLyricsParser.parse("[0,500](0,500)Hi")
        assertEquals(1, doc.lines.size)
        assertEquals("Hi", doc.lines.first().text)
        assertTrue(doc.lines.first().syllables.isNotEmpty())
    }

    @Test
    fun `多行各自切分`() {
        val raw = "[1000,500](1000,250,0)你(1250,250,0)好\n[2000,500](2000,500,0)世界"
        val doc = QQMusicQrcLyricsParser.parse(raw)
        assertEquals(2, doc.lines.size)
        assertEquals("你好", doc.lines[0].text)
        assertEquals("世界", doc.lines[1].text)
    }
}

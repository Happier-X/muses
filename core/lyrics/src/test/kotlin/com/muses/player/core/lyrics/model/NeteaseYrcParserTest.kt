package com.muses.player.core.lyrics.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** YRC 解析单测：行头形态 + 无行头纯 timing 行（网易解密后每字一行）。 */
class NeteaseYrcParserTest {

    @Test
    fun `行头加三元 timing 逐字切分`() {
        val raw = "[42320,230](42320,230,0)虽(42550,150,0)然(42700,460,0)少(43160,1810,0)了"
        val lines = NeteaseLyricParser.parseYrc(raw)
        assertEquals(1, lines.size)
        assertEquals("虽然少了", lines.first().text)
        assertFalse(lines.first().text.contains("("))
        assertEquals(4, lines.first().syllables.size)
        assertEquals("虽", lines.first().syllables[0].text)
        assertEquals(42320L, lines.first().syllables[0].startTimeMs)
        assertEquals("了", lines.first().syllables[3].text)
    }

    @Test
    fun `无行头纯 timing 行也组行`() {
        // 《为何我》库内实际形态
        val raw = "(123270,370,0)浮\n(123640,1180,0)现\n(124820,1590,0)了"
        val lines = NeteaseLyricParser.parseYrc(raw)
        assertEquals(3, lines.size)
        assertEquals("浮", lines[0].text)
        assertFalse(lines[0].text.contains("("))
        assertEquals(123270L, lines[0].timeMs)
        assertEquals("现", lines[1].text)
        assertEquals("了", lines[2].text)
    }

    @Test
    fun `纯文本行不误食`() {
        val lines = NeteaseLyricParser.parseYrc("浮现了妳的天空\n[123270,370]纯行")
        assertEquals(1, lines.size)
        assertEquals("纯行", lines.first().text)
    }
}

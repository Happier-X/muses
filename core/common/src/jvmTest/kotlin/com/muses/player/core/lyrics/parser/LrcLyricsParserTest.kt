package com.muses.player.core.lyrics.parser

import com.muses.player.feature.player.lyric.LyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcLyricsParserTest {
    @Test
    fun 同时间戳双语歌词合并为原文与译文() {
        val document = LyricsParser.parseDocument(
            """
            [00:00.00]bad guy - Billie Eilish
            [00:00.00]QQ音乐享有本翻译作品的著作权
            [00:14.43]White shirt now red my bloody nose
            [00:14.43]血流不止的鼻子染红了我的白衬衫
            [00:17.92]Sleepin you are on your tippy toes
            [00:17.92]你会踮着脚尖趁我安睡时
            [00:21.43]Creepin around like no one knows
            """.trimIndent(),
        )!!

        assertEquals(5, document.lines.size)
        assertNull(document.lines[0].translation)
        assertEquals("QQ音乐享有本翻译作品的著作权", document.lines[1].text)
        assertEquals("White shirt now red my bloody nose", document.lines[2].text)
        assertEquals("血流不止的鼻子染红了我的白衬衫", document.lines[2].translation)
        assertEquals(3490L, document.lines[2].durationMs)
        assertEquals("你会踮着脚尖趁我安睡时", document.lines[3].translation)
        assertNull(document.lines[4].translation)
    }

    @Test
    fun 同时间戳同语言歌词保留为两行() {
        val document = LrcLyricsParser.parse(
            "[00:01.00]First singer\n[00:01.00]Second singer\n[00:03.00]Next line",
        )

        assertEquals(3, document.lines.size)
        assertNull(document.lines[0].translation)
        assertEquals("Second singer", document.lines[1].text)
    }

    @Test
    fun 中文原文与拉丁字母注音不误判为译文() {
        val document = LrcLyricsParser.parse(
            "[00:01.00]你好世界\n[00:01.00]ni hao shi jie",
        )

        assertEquals(2, document.lines.size)
        assertNull(document.lines[0].translation)
    }

    @Test
    fun 分开提供的译文仍按原有时间轴解析() {
        val document = LrcLyricsParser.parse(
            lrc = "[00:01.00]Hello\n[00:03.00]World",
            translation = "[00:01.00]你好\n[00:03.00]世界",
        )

        assertEquals(2, document.lines.size)
        assertEquals("你好", document.lines[0].translation)
        assertEquals("世界", document.lines[1].translation)
    }
}

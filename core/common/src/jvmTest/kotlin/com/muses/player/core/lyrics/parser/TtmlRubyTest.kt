package com.muses.player.core.lyrics.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtmlRubyTest {
    @Test
    fun 注音容器保留主字且按注音推导词时间() {
        val document = TtmlLyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:tts="http://www.w3.org/ns/ttml#styling">
              <body><div><p begin="1.000" end="4.000"><span begin="1.000" end="1.500">你</span><span tts:ruby="container"><span tts:ruby="base">好</span><span tts:ruby="textContainer"><span tts:ruby="text" begin="1.600" end="2.000">ha</span><span tts:ruby="text" begin="2.500" end="3.000">o</span></span></span></p></div></body>
            </tt>
        """.trimIndent())
        val line = document.lines.single()
        assertEquals("你好", line.text)
        assertEquals(2, line.syllables.size)
        val word = line.syllables.last()
        assertEquals(1600L, word.startTimeMs)
        assertEquals(3000L, word.endTimeMs)
        assertEquals(listOf("ha", "o"), word.ruby.map { it.text })
        assertEquals(listOf(1600L, 2500L), word.ruby.map { it.startTimeMs })
        assertTrue(line.romanizationSyllables.isEmpty())
    }

    @Test
    fun 无时间注音不能混入正文() {
        val document = TtmlLyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:tts="http://www.w3.org/ns/ttml#styling">
              <body><div><p begin="1.000" end="4.000"><span tts:ruby="container"><span tts:ruby="base">好</span><span tts:ruby="textContainer"><span tts:ruby="text">hao</span></span></span></p></div></body>
            </tt>
        """.trimIndent())
        assertEquals("好", document.lines.single().text)
        assertTrue(document.lines.single().syllables.isEmpty())
    }
}

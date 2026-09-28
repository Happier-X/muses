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

    @Test
    fun 附加翻译和纯文本音译按行标识关联() {
        val document = TtmlLyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://itunes.apple.com/lyric-ttml-internal">
              <head><iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal">
                <translations>
                  <translation xml:lang="en"><text for="L1">Wrong language</text></translation>
                  <translation xml:lang="zh-Hans-CN"><text for="L1">你好</text></translation>
                </translations>
                <transliterations><transliteration xml:lang="ja-Latn"><text for="L1">konnichiwa</text></transliteration></transliterations>
              </iTunesMetadata></head>
              <body><div><p begin="1.000" end="3.000" itunes:key="L1"><span begin="1.000" end="3.000">こんにちは</span></p></div></body>
            </tt>
        """.trimIndent())
        val line = document.lines.single()
        assertEquals("こんにちは", line.text)
        assertEquals("你好", line.translation)
        assertEquals("konnichiwa", line.romanization)
    }

    @Test
    fun 逐词音译不混入正文且无时间注音继承词时间() {
        val document = TtmlLyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:tts="http://www.w3.org/ns/ttml#styling"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body><div><p begin="1.000" end="3.000">
                <span begin="1.000" end="2.000">君<span ttm:role="x-roman">kimi</span></span>
                <span tts:ruby="container" begin="2.000" end="3.000">
                  <span tts:ruby="base">色</span>
                  <span tts:ruby="textContainer"><span tts:ruby="text">いろ</span></span>
                </span>
              </p></div></body>
            </tt>
        """.trimIndent())
        val line = document.lines.single()
        assertEquals("君色", line.text)
        assertEquals("kimi", line.romanization)
        assertEquals("いろ", line.syllables.last().ruby.single().text)
        assertEquals(2000L, line.syllables.last().ruby.single().startTimeMs)
    }

    @Test
    fun 附加逐词音译沿用原词时间且行内翻译不混入正文() {
        val document = TtmlLyricsParser.parse("""
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://itunes.apple.com/lyric-ttml-internal"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <head><iTunesMetadata xmlns="http://music.apple.com/lyric-ttml-internal">
                <transliterations><transliteration xml:lang="ja-Latn"><text for="L2"><span>ki</span><span>mi</span></text></transliteration></transliterations>
              </iTunesMetadata></head>
              <body><div><p begin="1.000" end="3.000" itunes:key="L2"><span begin="1.000" end="2.000">き</span><span begin="2.000" end="3.000">み</span><span ttm:role="x-translation" xml:lang="zh-Hans">你</span></p></div></body>
            </tt>
        """.trimIndent())
        val line = document.lines.single()
        assertEquals("きみ", line.text)
        assertEquals("你", line.translation)
        assertEquals("ki mi", line.romanization)
        assertEquals(listOf(1000L, 2000L), line.romanizationSyllables.map { it.startTimeMs })
    }
}

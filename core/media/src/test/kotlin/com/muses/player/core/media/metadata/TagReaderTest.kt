package com.muses.player.core.media.metadata

import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 标签解析测试：内存构造 ID3v2.4 / VorbisComment（FLAC/OGG 用）两种标签，不依赖二进制 fixture */
class TagReaderTest {

    private fun id3Tag(
        title: String? = null,
        artist: String? = null,
        album: String? = null,
    ): ID3v24Tag = ID3v24Tag().apply {
        title?.let { setField(org.jaudiotagger.tag.FieldKey.TITLE, it) }
        artist?.let { setField(org.jaudiotagger.tag.FieldKey.ARTIST, it) }
        album?.let { setField(org.jaudiotagger.tag.FieldKey.ALBUM, it) }
    }

    @Test
    fun `ID3v2 基本字段解析`() {
        val tags = TagReader.parse(id3Tag(title = "晴天", artist = "周杰伦", album = "叶惠美"))
        assertEquals("晴天", tags.title)
        assertEquals("周杰伦", tags.artist)
        assertEquals("叶惠美", tags.album)
        assertNull(tags.lyrics)
    }

    @Test
    fun `Vorbis 注释基本字段解析`() {
        val tag = VorbisCommentTag().apply {
            setField(org.jaudiotagger.tag.FieldKey.TITLE, "Test Song")
            setField(org.jaudiotagger.tag.FieldKey.ARTIST, "Tester")
            setField(org.jaudiotagger.tag.FieldKey.ALBUM, "Album X")
        }
        val tags = TagReader.parse(tag)
        assertEquals("Test Song", tags.title)
        assertEquals("Tester", tags.artist)
        assertEquals("Album X", tags.album)
    }

    @Test
    fun `空标签返回空结果`() {
        val tags = TagReader.parse(ID3v24Tag())
        assertNull(tags.title)
        assertNull(tags.artist)
        assertNull(tags.album)
    }

    @Test
    fun `内嵌 AMLL TTML 不因缺少方括号被丢弃`() {
        val lyrics = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:00:01.000" end="00:00:02.000">测试歌词</p></div></body></tt>"""
        val tag = ID3v24Tag().apply {
            setField(org.jaudiotagger.tag.FieldKey.LYRICS, lyrics)
        }
        val actual = TagReader.parse(tag).lyrics
        assertEquals(lyrics, actual)
        val document = com.muses.player.feature.player.lyric.LyricsParser.parseDocument(actual)
        assertEquals("测试歌词", document?.lines?.single()?.text)
    }

    @Test
    fun `Vorbis 内嵌带命名空间的 TTML 可以读取`() {
        val lyrics = """<?xml version="1.0"?><ttml:tt xmlns:ttml="http://www.w3.org/ns/ttml"></ttml:tt>"""
        val tag = VorbisCommentTag().apply {
            setField(org.jaudiotagger.tag.FieldKey.LYRICS, lyrics)
        }
        assertEquals(lyrics, TagReader.parse(tag).lyrics)
    }
}

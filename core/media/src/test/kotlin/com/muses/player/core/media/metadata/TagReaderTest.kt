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
        assertNull(tags.replayGainTrackDb)
    }

    // ---- ReplayGain 解析 ----

    @Test
    fun `ReplayGain dB 字符串解析`() {
        assertEquals(-6.54, TagReader.parseReplayGainDbString("-6.54 dB")!!, 1e-9)
        assertEquals(1.2, TagReader.parseReplayGainDbString("+1.2")!!, 1e-9)
        assertEquals(-3.0, TagReader.parseReplayGainDbString("-3 db")!!, 1e-9)
        assertNull(TagReader.parseReplayGainDbString("not-a-number"))
        assertNull(TagReader.parseReplayGainDbString(""))
        assertNull(TagReader.parseReplayGainDbString(null))
    }

    @Test
    fun `R128 Q7点8 整数按除以256换算`() {
        // Opus 常见 R128_TRACK_GAIN = -1697 → -6.63dB 左右
        val q78 = TagReader.normalizeReplayGainDbValue(-1697.0)!!
        assertEquals(-6.62890625, q78, 1e-9)
        // 常规 dB 直接透传
        assertEquals(-8.0, TagReader.normalizeReplayGainDbValue(-8.0)!!, 1e-9)
        // 超出合理区间且换算后仍非法 → 丢弃
        assertNull(TagReader.normalizeReplayGainDbValue(Double.NaN))
        assertNull(TagReader.normalizeReplayGainDbValue(999999.0))
    }

    @Test
    fun `从 Vorbis 标签读取 ReplayGain track gain`() {
        val tag = VorbisCommentTag().apply {
            setField("REPLAYGAIN_TRACK_GAIN", "-7.89 dB")
        }
        val db = TagReader.parseReplayGainTrackDb(tag)!!
        assertEquals(-7.89, db, 1e-9)
    }

    @Test
    fun `无 RG 标签时返回 null 而非假增益`() {
        assertNull(TagReader.parseReplayGainTrackDb(id3Tag(title = "t")))
    }
}

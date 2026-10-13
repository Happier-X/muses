package com.muses.player.feature.sources

import com.muses.player.core.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryLyricsSearchTest {
    private fun song(lyrics: String?) = Song("1", "local", "test.flac", "歌曲", lyrics = lyrics)

    @Test
    fun `逐字时间标记不妨碍完整歌词匹配`() {
        val match = assertNotNull(matchLibraryLyrics(song("[00:12.00]<00:12.00>我<00:12.50>不<00:13.00>难过"), "我不难过"))
        assertEquals("我不难过", match.snippet)
        assertEquals("我不难过", match.snippet.substring(match.matchStart, match.matchEnd))
    }

    @Test
    fun `逐字歌词保留括号中的正文`() {
        val match = assertNotNull(matchLibraryLyrics(song("[12000,2000](12000,500,0)我(12500,500,0)不难过 [真的]"), "我不难过"))
        assertEquals("我不难过 [真的]", match.snippet)
    }

    @Test
    fun `TTML逐字歌词合并并解码实体但不搜索元数据`() {
        val lyrics = """<tt><head><metadata>测试作者</metadata></head><body><div><p><span>你</span><span> &amp; </span><span>&#25105;</span></p></div></body></tt>"""
        val match = assertNotNull(matchLibraryLyrics(song(lyrics), "你 & 我"))
        assertEquals("你 & 我", match.snippet)
        assertNull(matchLibraryLyrics(song(lyrics), "测试作者"))
    }

    @Test
    fun `TTML缩进不拆散逐字歌词而段落仍独立`() {
        val lyrics = "<tt><body><p><span>我</span>\n  <span>不难过</span></p>\n<p>下一句</p></body></tt>"
        assertNotNull(matchLibraryLyrics(song(lyrics), "我不难过"))
        assertNull(matchLibraryLyrics(song(lyrics), "不难过下一句"))
    }

    @Test
    fun `歌词头和时间戳不会被当作正文命中`() {
        val lyrics = "[ar:孙燕姿]\n[ti:我不难过]\n[00:12.00]又站在你家的门口"
        assertNull(matchLibraryLyrics(song(lyrics), "孙燕姿"))
        assertNull(matchLibraryLyrics(song(lyrics), "00:12"))
        assertNotNull(matchLibraryLyrics(song(lyrics), "你家的门口"))
    }

    @Test
    fun `忽略大小写及重复空白`() {
        assertNotNull(matchLibraryLyrics(song("[00:01.00]I   Miss YOU"), "miss you"))
        assertNull(matchLibraryLyrics(song(null), "歌词"))
        assertNull(matchLibraryLyrics(song("正文"), "  "))
    }

    @Test
    fun `长段落围绕命中位置展示`() {
        val match = assertNotNull(matchLibraryLyrics(song("前".repeat(200) + "记得的歌词" + "后".repeat(200)), "记得的歌词"))
        assertEquals("记得的歌词", match.snippet.substring(match.matchStart, match.matchEnd))
        assertTrue(match.snippet.length < 100)
        assertTrue(match.snippet.startsWith("…"))
    }
}

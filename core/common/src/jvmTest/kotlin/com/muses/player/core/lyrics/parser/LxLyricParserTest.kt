package com.muses.player.core.lyrics.parser

import com.muses.player.core.lyrics.model.LyricQuality
import com.muses.player.core.lyrics.model.LyricSource
import com.muses.player.core.lyrics.model.LyricTimingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 洛雪歌词解析单测：四字段 → LyricsDocument。
 *
 * 覆盖真实各源的两种逐字时间基准（绝对 / 相对行首）、翻译与罗马音对齐、
 * 行级 LRC 回退、以及逐字时间回退的单调修正。
 */
class LxLyricParserTest {

    @Test
    fun `逐字绝对基准（酷我酷狗形态）逐字切分正确`() {
        val doc = LxLyricParser.parse(
            lyric = "[00:12.340]你好",
            lxlyric = "[00:12.340]<12340,120>你<12460,180>好",
        )
        assertNotNull(doc)
        assertEquals(LyricSource.LxMusic, doc!!.source)
        assertEquals(LyricQuality.WordSynchronized, doc.quality)
        assertEquals(1, doc.lines.size)

        val line = doc.lines.first()
        assertEquals("你好", line.text)
        assertFalse(line.text.contains("<"))
        assertEquals(12340L, line.timeMs)
        assertEquals(300L, line.durationMs)
        assertEquals(LyricTimingKind.Precise, line.timingKind)
        assertEquals(2, line.syllables.size)
        assertEquals(12340L, line.syllables[0].startTimeMs)
        assertEquals(12460L, line.syllables[0].endTimeMs)
        assertEquals(12460L, line.syllables[1].startTimeMs)
        assertEquals(12640L, line.syllables[1].endTimeMs)
    }

    @Test
    fun `逐字相对基准（咪咕形态）按行首平移`() {
        val doc = LxLyricParser.parse(
            lyric = "[00:12.340]你好",
            lxlyric = "[00:12.340]<0,120>你<120,180>好",
        )
        assertNotNull(doc)
        val line = doc!!.lines.first()
        assertEquals(12340L, line.timeMs)
        // 相对基准 → 首字落到行首时间，而非 0
        assertEquals(12340L, line.syllables[0].startTimeMs)
        assertEquals(12460L, line.syllables[1].startTimeMs)
        assertEquals(12640L, line.syllables[1].endTimeMs)
    }

    @Test
    fun `逐字时间回退时钳到前字结束（单调修正）`() {
        val doc = LxLyricParser.parse(lxlyric = "[00:01.000]<1000,500>你<1200,100>好")
        assertNotNull(doc)
        val syllables = doc!!.lines.first().syllables
        // 第二个字原始 start=1200 早于首字 end=1500 → 钳到 1500
        assertEquals(1500L, syllables[1].startTimeMs)
        assertEquals(1600L, syllables[1].endTimeMs)
    }

    @Test
    fun `翻译与罗马音按时间对齐到主行`() {
        val doc = LxLyricParser.parse(
            lyric = "[00:12.340]你好\n[00:15.000]世界",
            lxlyric = "[00:12.340]<12340,300>你<12640,360>好\n[00:15.000]<15000,400>世<15400,400>界",
            tlyric = "[00:12.340]Hello\n[00:15.000]World",
            rlyric = "[00:12.340]ni hao\n[00:15.000]shi jie",
        )
        assertNotNull(doc)
        assertEquals("Hello", doc!!.lines[0].translation)
        assertEquals("ni hao", doc.lines[0].romanization)
        assertEquals("World", doc.lines[1].translation)
        assertEquals("shi jie", doc.lines[1].romanization)
    }

    @Test
    fun `无 lxlyric 时回退行级 LRC 且关闭伪逐字`() {
        val doc = LxLyricParser.parse(lyric = "[00:01.000]第一行\n[00:03.000]第二行")
        assertNotNull(doc)
        assertEquals(LyricQuality.LineSynchronized, doc!!.quality)
        assertFalse(doc.pseudoTimingAllowed)
        assertEquals(2, doc.lines.size)
        assertEquals("第一行", doc.lines[0].text)
        assertTrue(doc.lines[0].syllables.isEmpty())
        assertEquals(1000L, doc.lines[0].timeMs)
    }

    @Test
    fun `lyric 字段残留逐字标记时同样按逐字解析`() {
        val doc = LxLyricParser.parse(lyric = "[00:01.000]<1000,100>你<1100,100>好")
        assertNotNull(doc)
        assertEquals(LyricQuality.WordSynchronized, doc!!.quality)
        val line = doc.lines.first()
        assertEquals("你好", line.text)
        assertEquals(2, line.syllables.size)
        assertEquals(1000L, line.syllables[0].startTimeMs)
    }

    @Test
    fun `逐字行与纯文本行混排各自成行`() {
        val doc = LxLyricParser.parse(
            lxlyric = "[00:01.000]<1000,100>你<1100,100>好\n[00:03.000]纯文本行",
        )
        assertNotNull(doc)
        assertEquals(2, doc!!.lines.size)
        assertEquals(2, doc.lines[0].syllables.size)
        assertEquals(0, doc.lines[1].syllables.size)
        assertEquals("纯文本行", doc.lines[1].text)
    }

    @Test
    fun `第三方脚本返回酷狗 KRC 原始格式时按 KRC 解析`() {
        // 酷狗 KRC 明文形态：行头 [start,duration]，字前 <offset,duration,0>
        val doc = LxLyricParser.parse(lyric = "[0,1000]<0,350,0>你<350,650,0>好")
        assertNotNull(doc)
        assertEquals(LyricSource.LxMusic, doc!!.source)
        assertEquals(LyricQuality.WordSynchronized, doc.quality)
        assertEquals(1, doc.lines.size)
        assertEquals("你好", doc.lines.first().text)
        assertFalse(doc.lines.first().text.contains("<"))
        assertEquals(2, doc.lines.first().syllables.size)
    }

    @Test
    fun `第三方脚本返回 QQ QRC 原始格式时按 QRC 解析`() {
        val doc = LxLyricParser.parse(lyric = "[42320,230](42320,230,0)虽(42550,150,0)然")
        assertNotNull(doc)
        assertEquals(LyricSource.LxMusic, doc!!.source)
        assertEquals("虽然", doc.lines.first().text)
        assertFalse(doc.lines.first().text.contains("("))
    }

    @Test
    fun `空输入返回 null`() {
        assertNull(LxLyricParser.parse())
        assertNull(LxLyricParser.parse(lyric = "", tlyric = "  ", lxlyric = null))
        assertNull(LxLyricParser.parse(lyric = "没有时间标签的文本"))
    }
}

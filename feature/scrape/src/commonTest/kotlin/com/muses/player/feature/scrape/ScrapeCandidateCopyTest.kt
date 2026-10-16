package com.muses.player.feature.scrape

import com.muses.player.core.scrape.editmeta.EditLyricsCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrapeCandidateCopyTest {
    private fun preview(text: String, format: String = "lrc") =
        lyricsCandidatePreview(EditLyricsCandidate(text, format, "测试来源", translationText = "另一份译文"))

    @Test fun `逐行短预览去掉时间码和文件信息且最多三行`() {
        val result = preview("[ti:歌名]\n[ar:歌手]\n[00:01]第一句\n[00:02]第二句\n[00:03]第三句\n[00:04]第四句")
        assertEquals("逐行歌词", result.kind)
        assertEquals("第一句\n第二句\n第三句", result.snippet)
        assertTrue(result.text.orEmpty().endsWith("第四句"))
        assertFalse(result.text.orEmpty().contains("另一份译文"))
    }

    @Test fun `纯文本保留歌词正文并跳过空行和元信息`() {
        val result = preview("[ti:歌名]\n\n第一句\n\n第二句\n第三句\n第四句")
        assertEquals("纯文本歌词", result.kind)
        assertEquals("第一句\n第二句\n第三句", result.snippet)
    }

    @Test fun `逐字格式展示文字而不是音节时间码`() {
        for ((format, raw) in listOf(
            "yrc" to "[1000,1000](1000,500,0)你(1500,500,0)好",
            "qrc" to "[1000,1000](1000,500,0)你(1500,500,0)好",
            "krc" to "[1000,1000]<0,500,0>你<500,500,0>好",
        )) {
            val result = preview(raw, format)
            assertEquals("逐字歌词", result.kind, format)
            assertEquals("你好", result.snippet, format)
        }
    }

    @Test fun `TTML正文可读并按真实音节区分逐行和逐字`() {
        val start = "<tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div>"
        val end = "</div></body></tt>"
        val line = preview(start + "<p begin=\"00:01.000\" end=\"00:03.000\">你好</p>" + end, "ttml")
        assertEquals("逐行歌词", line.kind)
        assertEquals("你好", line.snippet)
        val word = preview(start + "<p begin=\"00:01.000\" end=\"00:03.000\"><span begin=\"00:01.000\" end=\"00:02.000\">你</span><span begin=\"00:02.000\" end=\"00:03.000\">好</span></p>" + end, "ttml")
        assertEquals("逐字歌词", word.kind)
        assertEquals("你好", word.snippet)
    }

    @Test fun `无效XML不回退成带标签的歌词短预览`() {
        val result = preview("<tt><p>[00:01]正文</p></tt>", "ttml")
        assertEquals("格式待核对", result.kind)
        assertNull(result.text)
        assertFalse(result.snippet.contains("<"))
    }

    @Test fun `实体声明和超长内容不进入解析`() {
        assertNull(preview("<!DOCTYPE tt [<!ENTITY x SYSTEM 'file:///不会读取'>]><tt>&x;</tt>", "ttml").text)
        assertEquals("歌词较长", preview("字".repeat(256 * 1024 + 1)).kind)
        assertTrue(preview("字".repeat(200)).snippet.length <= 101)
    }

    @Test fun `QRC容器缺失歌词属性不误作LRC且有效容器仍可预览`() {
        val invalid = preview("<QrcInfos>[00:01]正文</QrcInfos>", "qrc")
        assertEquals("格式待核对", invalid.kind)
        assertNull(invalid.text)
        val valid = preview("<QrcInfos><LyricInfo LyricContent=\"[1000,1000](1000,500,0)你(1500,500,0)好\"/></QrcInfos>", "qrc")
        assertEquals("逐字歌词", valid.kind)
        assertEquals("你好", valid.snippet)
    }
}

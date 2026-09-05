package com.muses.player.core.lyrics.provider.qrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 规格 = src/features/lyrics/providers/qrc.ts + @applemusic-like-lyrics/lyric eqrc。
 * 密文向量由同版本 AMLL 库 encryptQrcHex 生成（node /tmp/genqrc.cjs）。
 */
class QrcDecoderTest {

    /** 真实向量：encryptQrcHex 生成的两行 QRC（含 zlib 压缩与 3DES 加密全链路） */
    private val cipherHex =
        "a6e0d4c95b8eeb9d9c3df309f31af834cdcc30ca50c381e5711a47805ffd3ce611fcc9ba2e50e2f3885376bcdb9bbf8ca9137e33146afa0b"
    private val expectedPlain =
        "[0,500](0,500,0)Hello World\n[1000,800](1000,800,0)Second line"

    @Test
    fun `解密真实向量_经zlib还原明文`() {
        val xml = QrcDecoder.decryptHex(cipherHex)
        assertTrue(xml != null && xml.contains("Hello World"))
        // 明文行时间轴直接透传（extractLyricContent 无 XML 包裹时原样返回）
        assertEquals(expectedPlain, QrcDecoder.extractLyricContent(xml!!.trim()))
        // decryptToPlain：合法逐字格式 → 返回明文
        assertEquals(expectedPlain, QrcDecoder.decryptToPlain(cipherHex))
    }

    @Test
    fun `非法输入返回null`() {
        assertNull(QrcDecoder.decryptToPlain(""))
        assertNull(QrcDecoder.decryptToPlain("xyz"))           // 非 hex
        assertNull(QrcDecoder.decryptToPlain("abc"))           // 奇数长度
        assertNull(QrcDecoder.decryptToPlain("aabbcc"))        // 非 8 字节倍数
        assertNull(QrcDecoder.decryptToPlain("00".repeat(8)))  // 合法长度但解压失败
    }

    @Test
    fun `looksLikeWordLevelBracket判定`() {
        assertTrue(QrcDecoder.looksLikeWordLevelBracket("[0,500](0,500,0)Hi"))
        assertTrue(QrcDecoder.looksLikeWordLevelBracket("{\"x\":1}\n[10,20](10,20,0)Hi"))
        assertTrue(!QrcDecoder.looksLikeWordLevelBracket("[00:01.00]plain lrc"))
        assertTrue(!QrcDecoder.looksLikeWordLevelBracket(""))
    }

    @Test
    fun `extractLyricContent从XML抽取LyricContent属性并反转义`() {
        val xml = "<QrcInfos><QrcHeadInfo LyricContent=\"[0,100](0,100,0)&quot;Hi&quot;&#10;next\"/></QrcInfos>"
        val extracted = QrcDecoder.extractLyricContent(xml)
        assertEquals("[0,100](0,100,0)\"Hi\"\nnext", extracted)
    }
}

package com.muses.player.core.media.metadata

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 标签写入测试。
 *
 * 说明：jaudiotagger 无法凭空创建音频文件，这里在内存中合成「ID3 无标签的最小合法 MP3」
 * （若干个 MPEG-1 Layer III 128kbps/44.1kHz 静音帧，仅帧头有效即可满足解析器），
 * 再走 TagWriter 写入 → TagReader 读回验证闭环。
 * m4a 容器需完整 MP4 box 结构，无法低成本合成，故不覆盖（异常路径以不存在的文件代替）。
 */
class TagWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** MPEG-1 Layer III / 44.1kHz / 128kbps / 无 padding / 立体声：帧长 417 字节 */
    private fun synthesizeMinimalMp3(frameCount: Int = 64): ByteArray {
        val frame = ByteArray(417)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte() // MPEG1, Layer III, 无 CRC
        frame[2] = 0x90.toByte() // 128kbps, 44.1kHz, 无 padding
        frame[3] = 0x00.toByte() // 立体声等模式位全零
        return ByteArray(frameCount * frame.size).also { out ->
            for (i in 0 until frameCount) {
                frame.copyInto(out, i * frame.size)
            }
        }
    }

    private fun newMp3(): File {
        val file = File(tmp.root, "sample.mp3")
        file.writeBytes(synthesizeMinimalMp3())
        return file
    }

    @Test
    fun `MP3 写入基本标签并读回验证`() {
        val file = newMp3()
        // 前置：合成样本可被解析器识别
        TagReader.read(file)

        val result = TagWriter.write(
            file,
            TagWriter.TagWriteRequest(title = "晴天", artist = "周杰伦", album = "叶惠美"),
        )
        assertTrue("写入应成功：${result.message}", result.ok)
        assertEquals("晴天", TagReader.read(file).title)
    }

    @Test
    fun `MP3 写入歌词与封面并读回验证`() {
        val file = newMp3()
        val coverBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()) // 最小 JPEG 骨架
        val lyrics = "[00:01.00]测试歌词"

        val result = TagWriter.write(
            file,
            TagWriter.TagWriteRequest(lyrics = lyrics, coverBytes = coverBytes),
        )
        assertTrue("写入应成功：${result.message}", result.ok)
        val tags = TagReader.read(file)
        assertEquals(lyrics, tags.lyrics)
        assertNotNull(tags.coverBytes)
        assertArrayEquals(coverBytes, tags.coverBytes)
    }

    @Test
    fun `clearLyrics 清空已有歌词`() {
        val file = newMp3()
        assertTrue(TagWriter.write(file, TagWriter.TagWriteRequest(lyrics = "旧歌词")).ok)

        assertTrue(TagWriter.write(file, TagWriter.TagWriteRequest(clearLyrics = true)).ok)
        assertEquals(null, TagReader.read(file).lyrics)
    }

    @Test
    fun `null 字段不修改既有值`() {
        val file = newMp3()
        assertTrue(TagWriter.write(file, TagWriter.TagWriteRequest(title = "原标题")).ok)

        assertTrue(TagWriter.write(file, TagWriter.TagWriteRequest(artist = "新艺术家")).ok)
        val tags = TagReader.read(file)
        assertEquals("原标题", tags.title)
        assertEquals("新艺术家", tags.artist)
    }

    @Test
    fun `不存在的文件返回失败结果且不抛异常`() {
        val missing = File(tmp.root, "missing.mp3")
        val result = TagWriter.write(missing, TagWriter.TagWriteRequest(title = "x"))
        assertFalse(result.ok)
        // m4a 等其他容器的格式兼容性失败同样折叠为 write_failed，此处以文件不存在覆盖该分类语义
        assertEquals("write_failed", result.code)
        assertNotNull(result.message)
    }
}

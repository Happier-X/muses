package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class VerifiedTagWriteTest {
    /** 构造 MPEG 帧，不使用用户的歌曲作为测试素材。 */
    private fun audio(): File = File.createTempFile("tag-roundtrip-", ".mp3").apply {
        outputStream().use { output ->
            val frame = ByteArray(417)
            frame[0] = 0xff.toByte(); frame[1] = 0xfb.toByte(); frame[2] = 0x90.toByte()
            repeat(500) { output.write(frame) }
        }
    }

    @Test fun `真实 MP3 写入后可回读 TTML 并原子保存`() = runTest {
        val file = audio()
        try {
            val lyrics = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01" end="00:02">测试</p></div></body></tt>"""
            val result = LocalAudioTagFileWriter(JaudiotaggerTagPort).write(
                Song("test", "local", file.absolutePath, "旧标题"),
                ScrapeChanges(title = "新标题", artist = "测试歌手", lyrics = lyrics), null)
            assertTrue(result.ok, result.message)
            val actual = JaudiotaggerTagPort.readTags(file)!!
            assertEquals("新标题", actual.title)
            assertEquals(lyrics, actual.lyrics)
            val cleared = JaudiotaggerTagPort.writeAndVerify(file, ScrapeChanges(artist = "", lyrics = ""), null)
            assertTrue(cleared.ok, cleared.message)
            assertTrue(JaudiotaggerTagPort.readTags(file)?.lyrics.isNullOrBlank())
        } finally { file.delete() }
    }

    @Test fun `原子替换被拒时回退为原地覆盖写入`() = runTest {
        val file = audio()
        try {
            // 安卓外部存储（SAF 授权目录）不允许 rename 不是自己创建的文件，报 EPERM；
            // 此时必须改写目标文件本身，否则本地文件音源的刮削回写全部失败。
            val writer = LocalAudioTagFileWriter(JaudiotaggerTagPort) { _, _ ->
                throw java.io.IOException("Operation not permitted")
            }
            val result = writer.write(
                Song("test", "local", file.absolutePath, "旧标题"),
                ScrapeChanges(title = "新标题", artist = "测试歌手"), null)
            assertTrue(result.ok, result.message)
            val actual = JaudiotaggerTagPort.readTags(file)!!
            assertEquals("新标题", actual.title)
            assertEquals("测试歌手", actual.artist)
        } finally { file.delete() }
    }

    @Test fun `封面不可用时不修改原音频`() = runTest {
        val file = audio()
        try {
            val original = file.readBytes()
            val result = LocalAudioTagFileWriter(JaudiotaggerTagPort).write(
                Song("test", "local", file.absolutePath, "旧标题"),
                ScrapeChanges(title = "新标题", coverRemoteUrl = "https://example.com/cover.jpg"), null)
            assertTrue(!result.ok)
            assertTrue(original.contentEquals(file.readBytes()))
        } finally { file.delete() }
    }
}

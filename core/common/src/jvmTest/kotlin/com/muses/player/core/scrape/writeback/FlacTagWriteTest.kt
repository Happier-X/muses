package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FlacTagWriteTest {
    // 素材由 FFmpeg 生成：0.2 秒、44.1 kHz、440 Hz 正弦音，封面为 8×8 蓝色图片。
    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/metadata/$name")).use { it.readBytes() }

    /** 跳过 FLAC 元数据块，比较编码音频帧，确保回写没有重新编码或损坏音频。 */
    private fun frames(file: File): ByteArray {
        val bytes = file.readBytes()
        assertEquals("fLaC", String(bytes, 0, 4, Charsets.US_ASCII))
        var offset = 4
        while (true) {
            val last = bytes[offset].toInt() and 0x80 != 0
            val length = ((bytes[offset + 1].toInt() and 0xff) shl 16) or
                ((bytes[offset + 2].toInt() and 0xff) shl 8) or (bytes[offset + 3].toInt() and 0xff)
            offset += 4 + length
            if (last) return bytes.copyOfRange(offset, bytes.size)
        }
    }

    @Test fun `FLAC 内嵌中文标签歌词封面并保留音频帧`() = runTest {
        verifyWrite("cover.png", "[00:01.00]雨过后的风景\n[00:02.00]中文歌词")
    }

    @Test fun `FLAC 内嵌 TTML 歌词与 JPEG 封面`() = runTest {
        verifyWrite("cover.jpg", """<tt xmlns="http://www.w3.org/ns/ttml"><body><div><p begin="00:01" end="00:02">雨过后的风景</p></div></body></tt>""")
    }

    private suspend fun verifyWrite(coverName: String, lyrics: String) {
        val file = File.createTempFile("flac-tags-", ".flac")
        try {
            file.writeBytes(resource("sine.flac"))
            val originalFrames = frames(file)
            val cover = resource(coverName)
            val writer = LocalAudioTagFileWriter(JaudiotaggerTagPort)
            val song = Song("flac-test", "local", file.absolutePath, "旧标题")
            val result = writer.write(song, ScrapeChanges(
                title = "雨过后的风景", artist = "Dizzy Dizzo (蔡诗芸)", album = "黑色彩虹",
                lyrics = lyrics, coverRemoteUrl = "https://example.com/cover.png",
            ), cover)
            assertTrue(result.ok, result.message)
            val actual = checkNotNull(JaudiotaggerTagPort.readTags(file))
            assertEquals("雨过后的风景", actual.title)
            assertEquals("Dizzy Dizzo (蔡诗芸)", actual.artist)
            assertEquals("黑色彩虹", actual.album)
            assertEquals(lyrics, actual.lyrics)
            assertContentEquals(cover, actual.cover)
            assertContentEquals(originalFrames, frames(file))

            val cleared = writer.write(song, ScrapeChanges(title = "", lyrics = "", coverUri = ""), null)
            assertTrue(cleared.ok, cleared.message)
            val remaining = checkNotNull(JaudiotaggerTagPort.readTags(file))
            assertTrue(remaining.title.isNullOrEmpty())
            assertTrue(remaining.lyrics.isNullOrEmpty())
            assertTrue(remaining.cover?.isNotEmpty() != true)
            assertEquals(actual.artist, remaining.artist)
            assertEquals(actual.album, remaining.album)
            assertContentEquals(originalFrames, frames(file))
        } finally { file.delete() }
    }
}

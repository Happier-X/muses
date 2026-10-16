package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.scrape.ports.JaudiotaggerTagPort
import com.muses.player.core.scrape.ports.TagPort
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test fun `无法识别的封面在标签写入前拒绝且不修改原音频`() = runTest {
        val file = audio()
        try {
            val original = file.readBytes()
            for (bytes in listOf("<html>错误页</html>".toByteArray(), byteArrayOf(1, 2, 3),
                byteArrayOf(0xff.toByte(), 0xd8.toByte()), byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47))) {
                val result = JaudiotaggerTagPort.writeTags(file,
                    ScrapeChanges(title = "新标题", coverUri = "https://example.com/cover"), bytes)
                assertFalse(result.ok)
                assertEquals("cover_failed", result.code)
                assertTrue(original.contentEquals(file.readBytes()))
            }
        } finally { file.delete() }
    }

    @Test fun `标签处理期间原文件被修改时保留外部内容且不替换`() = runTest {
        for (sameMetadata in listOf(false, true)) {
            val file = audio()
            try {
                val modified = file.lastModified()
                var external = byteArrayOf()
                var replaced = false
                val port = object : TagPort by JaudiotaggerTagPort {
                    override fun writeTags(staged: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
                        val result = JaudiotaggerTagPort.writeTags(staged, changes, coverBytes)
                        external = file.readBytes().let { bytes ->
                            if (sameMetadata) bytes.apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
                            else bytes + byteArrayOf(1, 2, 3)
                        }
                        file.writeBytes(external)
                        if (sameMetadata) {
                            assertTrue(file.setLastModified(modified))
                            assertEquals(modified, file.lastModified())
                        }
                        return result
                    }
                }
                val writer = LocalAudioTagFileWriter(port) { _, _ -> replaced = true }
                val result = writer.write(Song("test", "local", file.absolutePath, "旧标题"),
                    ScrapeChanges(title = "新标题"), null)
                assertFalse(result.ok)
                assertEquals("file_changed", result.code)
                assertFalse(replaced)
                assertTrue(external.contentEquals(file.readBytes()))
            } finally { file.delete() }
        }
    }

    @Test fun `原子替换失败期间外部改动不会被回退覆盖或备份还原`() = runTest {
        val file = audio()
        try {
            var external = byteArrayOf()
            val writer = LocalAudioTagFileWriter(JaudiotaggerTagPort) { _, original ->
                external = original.readBytes() + byteArrayOf(4, 5, 6)
                original.writeBytes(external)
                throw java.io.IOException("模拟原子替换失败")
            }
            val result = writer.write(Song("test", "local", file.absolutePath, "旧标题"),
                ScrapeChanges(title = "新标题"), null)
            assertFalse(result.ok)
            assertEquals("file_changed", result.code)
            assertTrue(external.contentEquals(file.readBytes()))
        } finally { file.delete() }
    }
    @Test fun `审核后同大小同时间戳的修改在标签写入前拒绝`() = runTest {
        val file = audio()
        try {
            val song = Song("test", "local", file.absolutePath, "旧标题")
            val baseline = requireNotNull(readLocalAudioRevision(song))
            val modified = file.lastModified()
            val external = file.readBytes().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
            file.writeBytes(external)
            assertTrue(file.setLastModified(modified))
            var tagWrites = 0
            val port = object : TagPort by JaudiotaggerTagPort {
                override fun writeTags(file: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
                    tagWrites++
                    return JaudiotaggerTagPort.writeTags(file, changes, coverBytes)
                }
            }
            val result = LocalAudioTagFileWriter(port).writeReviewed(song, ScrapeChanges(title = "新标题"), null, baseline)
            assertEquals("review_changed", result.code)
            assertEquals(0, tagWrites)
            assertTrue(external.contentEquals(file.readBytes()))
        } finally { file.delete() }
    }

    @Test fun `审核基准未变可正常写入并回读`() = runTest {
        val file = audio()
        try {
            val song = Song("test", "local", file.toURI().toString(), "旧标题")
            val result = LocalAudioTagFileWriter(JaudiotaggerTagPort).writeReviewed(song,
                ScrapeChanges(title = "已审核标题"), null, requireNotNull(readLocalAudioRevision(song)))
            assertTrue(result.ok, result.message)
            assertEquals("已审核标题", JaudiotaggerTagPort.readTags(file)?.title)
        } finally { file.delete() }
    }

    @Test fun `已审核文件消失视为审核冲突`() = runTest {
        val file = audio()
        try {
            val song = Song("test", "local", file.absolutePath, "旧标题")
            val baseline = requireNotNull(readLocalAudioRevision(song))
            assertTrue(file.delete())
            val result = LocalAudioTagFileWriter(JaudiotaggerTagPort).writeReviewed(song, ScrapeChanges(title = "新标题"), null, baseline)
            assertEquals("review_changed", result.code)
            assertFalse(file.exists())
        } finally { file.delete() }
    }

    @Test fun `真实主封面快照可在原展示地址失效后逐字节恢复`() = runTest {
        val file = audio()
        val directory = java.nio.file.Files.createTempDirectory("muses-real-cover-").toFile()
        try {
            val bytes = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a0ioAAAAASUVORK5CYII=")
            val seeded = JaudiotaggerTagPort.writeAndVerify(file, ScrapeChanges(title = "原标签", coverUri = "已经失效的地址"), bytes)
            assertTrue(seeded.ok, seeded.message)
            val song = Song("test", "local", file.absolutePath, "曲库标题")
            val writer = LocalAudioTagFileWriter(JaudiotaggerTagPort)
            val captured = requireNotNull(writer.captureSnapshot(song))
            assertTrue(captured.coverKnown)
            kotlin.test.assertContentEquals(bytes, captured.tags.cover)
            val snapshots = CoverSnapshotStore(directory)
            val id = snapshots.save(requireNotNull(captured.tags.cover))
            assertTrue(writer.write(song, ScrapeChanges(coverUri = ""), null).ok)
            assertTrue(writer.captureSnapshot(song)?.coverKnown == true)
            assertEquals(null, writer.captureSnapshot(song)?.tags?.cover)
            val result = writer.write(song, ScrapeChanges(coverUri = "snapshot:$id"), snapshots.read(id))
            assertTrue(result.ok, result.message)
            kotlin.test.assertContentEquals(bytes, JaudiotaggerTagPort.readTags(file)?.cover)
        } finally { file.delete(); directory.deleteRecursively() }
    }

}

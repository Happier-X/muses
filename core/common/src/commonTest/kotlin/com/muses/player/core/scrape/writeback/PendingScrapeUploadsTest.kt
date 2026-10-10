package com.muses.player.core.scrape.writeback

import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PendingScrapeUploadsTest {
    private class Server(var bytes: ByteArray, var failPut: Boolean = false, var version: String? = "\"version\"") : WebDavClient {
        var puts = 0
        override fun authenticate(username: String, password: String) = Unit
        override suspend fun probe(baseUrl: String) = true
        override suspend fun list(url: String) = emptyList<WebDavItem>()
        override suspend fun strongETag(url: String) = version
        override suspend fun putIfMatch(url: String, source: File, eTag: String?) {
            assertEquals("\"version\"", eTag)
            put(url, source)
        }
        override suspend fun get(url: String, dest: File): File = dest.apply { writeBytes(bytes) }
        override suspend fun put(url: String, source: File) {
            puts++
            if (failPut) error("服务器写入失败")
            bytes = source.readBytes()
        }
        override suspend fun delete(url: String) = error("禁止删除远端文件")
        override suspend fun move(source: String, dest: String) = error("禁止移动远端文件")
        override suspend fun getString(url: String): String? = null
    }

    @Test fun failedUploadSurvivesRestartAndRetriesPreparedFile() = runBlocking {
        val root = Files.createTempDirectory("pending-scrape-test").toFile()
        try {
            val original = File(root, "original.mp3").apply { writeText("原始音频") }
            val prepared = File(root, "prepared.mp3").apply { writeText("已内嵌信息的音频") }
            val directory = File(root, "persistent")
            val store = PendingScrapeUploads(directory)
            val task = store.prepare("song", "source", "https://example.test/dav", "https://example.test/dav/song.mp3",
                "歌曲", prepared, audioFileHash(original))
            val server = Server(original.readBytes(), failPut = true)
            assertFalse(store.upload(task, server))
            assertContentEquals(prepared.readBytes(), store.file(task).readBytes())
            val restored = PendingScrapeUploads(directory)
            assertEquals(1, restored.tasks.value.size)
            assertNotNull(restored.tasks.value.single().error)
            server.failPut = false
            assertTrue(restored.upload(restored.tasks.value.single(), server))
            assertContentEquals(prepared.readBytes(), server.bytes)
            assertTrue(restored.tasks.value.isEmpty())
            assertTrue(PendingScrapeUploads(directory).tasks.value.isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun changedRemoteFileIsPreservedAndAlreadyUploadedFileDoesNotRepeatPut() = runBlocking {
        val root = Files.createTempDirectory("pending-scrape-test").toFile()
        try {
            val original = File(root, "original.flac").apply { writeText("原始") }
            val prepared = File(root, "prepared.flac").apply { writeText("处理完成") }
            val store = PendingScrapeUploads(File(root, "persistent"))
            val task = store.prepare("song", "source", "https://example.test", "https://example.test/song.flac",
                "歌曲", prepared, audioFileHash(original))
            val server = Server("另一台设备修改后的音频".toByteArray())
            assertFalse(store.upload(task, server))
            assertEquals(0, server.puts)
            assertTrue(store.tasks.value.single().error.orEmpty().contains("远端文件已经变化"))
            assertTrue(store.file(task).isFile)
            server.bytes = prepared.readBytes()
            assertTrue(store.upload(task, server))
            assertEquals(0, server.puts)
        } finally { root.deleteRecursively() }
    }

    @Test fun corruptLocalFileIsNeverUploaded() = runBlocking {
        val root = Files.createTempDirectory("pending-scrape-test").toFile()
        try {
            val audio = File(root, "audio.wav").apply { writeText("音频") }
            val store = PendingScrapeUploads(File(root, "persistent"))
            val task = store.prepare("song", "source", "https://example.test", "https://example.test/song.wav",
                "歌曲", audio, audioFileHash(audio))
            store.file(task).writeText("损坏")
            val server = Server(audio.readBytes())
            assertFalse(store.upload(task, server))
            assertEquals(0, server.puts)
            assertEquals(1, store.tasks.value.size)
        } finally { root.deleteRecursively() }
    }

    @Test fun serverWithoutVersionKeepsLocalFileForExport() = runBlocking {
        val root = Files.createTempDirectory("pending-scrape-test").toFile()
        try {
            val original = File(root, "original.mp3").apply { writeText("原始") }
            val prepared = File(root, "prepared.mp3").apply { writeText("新的内嵌信息") }
            val store = PendingScrapeUploads(File(root, "persistent"))
            val task = store.prepare("song", "source", "https://example.test", "https://example.test/song.mp3",
                "歌曲", prepared, audioFileHash(original))
            val server = Server(original.readBytes(), version = null)
            assertFalse(store.upload(task, server))
            assertEquals(0, server.puts)
            assertContentEquals(prepared.readBytes(), store.file(task).readBytes())
            assertTrue(store.tasks.value.single().error.orEmpty().contains("导出"))
        } finally { root.deleteRecursively() }
    }
}

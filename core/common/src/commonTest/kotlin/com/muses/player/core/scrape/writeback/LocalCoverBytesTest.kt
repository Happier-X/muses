package com.muses.player.core.scrape.writeback

import com.muses.player.core.scrape.http.ScrapeHttp
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LocalCoverBytesTest {
    @Test
    fun `路径和两种文件地址均能读取包含中文空格及特殊字符的封面`() = runTest {
        val directory = java.nio.file.Files.createTempDirectory("local-cover-").toFile()
        val file = File(directory, "中文 封面 #%+.png")
        try {
            val bytes = byteArrayOf(1, 2, 3, 4)
            file.writeBytes(bytes)
            val uri = file.toURI().toASCIIString()
            for (location in listOf(file.absolutePath, uri, uri.replace("file:/", "file:///"))) {
                assertTrue(bytes.contentEquals(readLocalCoverBytes(location, maxBytes = 4)), location)
            }
        } finally { file.delete(); directory.delete() }
    }

    @Test
    fun `超过上限拒绝读取且空文件正常返回`() = runTest {
        val file = File.createTempFile("local-cover-", ".png")
        try {
            assertTrue(byteArrayOf().contentEquals(readLocalCoverBytes(file.absolutePath, maxBytes = 4)))
            file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            assertNull(readLocalCoverBytes(file.absolutePath, maxBytes = 4))
            RandomAccessFile(file, "rw").use { it.setLength(ScrapeHttp.MAX_COVER_BYTES.toLong() + 1) }
            assertNull(readLocalCoverBytes(file.toURI().toASCIIString()))
        } finally { file.delete() }
    }

    @Test
    fun `失效路径目录及损坏文件地址返回不可用`() = runTest {
        val directory = java.nio.file.Files.createTempDirectory("local-cover-").toFile()
        try {
            assertNull(readLocalCoverBytes(File(directory, "不存在.png").absolutePath))
            assertNull(readLocalCoverBytes(directory.absolutePath))
            assertNull(readLocalCoverBytes("file:///未编码 空格.png"))
            assertNull(readLocalCoverBytes("file:相对路径.png"))
        } finally { directory.delete() }
    }
}

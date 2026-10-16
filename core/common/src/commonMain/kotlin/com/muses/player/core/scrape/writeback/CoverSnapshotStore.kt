package com.muses.player.core.scrape.writeback

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 主封面内容寻址存储；不删除仍可能被记录引用的数据，容量满时拒绝新快照。 */
class CoverSnapshotStore(private val directory: File, private val maxTotalBytes: Long = 256L * 1024 * 1024) {
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun target(id: String): File {
        require(Regex("[0-9a-f]{64}").matches(id)) { "封面快照标识无效" }
        return File(directory, id)
    }
    suspend fun save(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        require(bytes.isNotEmpty() && bytes.size <= 20 * 1024 * 1024) { "主封面快照超出大小上限" }
        synchronized(this@CoverSnapshotStore) {
            check(directory.isDirectory || directory.mkdirs()) { "无法创建封面快照目录" }
            val id = hash(bytes)
            val file = target(id)
            if (file.isFile && hash(file.readBytes()) == id) return@synchronized id
            val used = directory.listFiles().orEmpty().sumOf { it.length() }
            check(used + bytes.size <= maxTotalBytes) { "封面快照空间已满" }
            val temporary = File.createTempFile("snapshot-", ".tmp", directory)
            try {
                java.io.FileOutputStream(temporary).use { it.write(bytes); it.fd.sync() }
                check(hash(temporary.readBytes()) == id) { "封面快照校验失败" }
                java.nio.file.Files.move(temporary.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } finally { temporary.delete() }
            id
        }
    }
    suspend fun read(id: String): ByteArray = withContext(Dispatchers.IO) {
        val file = target(id)
        check(file.isFile && file.length() in 1..(20L * 1024 * 1024)) { "封面快照不存在或大小无效" }
        file.readBytes().also { check(hash(it) == id) { "封面快照已损坏" } }
    }
    fun location(id: String): String = target(id).toURI().toString()
}

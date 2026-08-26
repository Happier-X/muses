package com.muses.player.core.media.scanner

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

/**
 * 封面缓存落盘小组件：Local / WebDAV 两个扫描器共用。
 * 落盘路径 cache/covers/<sha256(songKey)>.jpg，返回安全 file:// URI。
 */
object CoverCacheWriter {

    /** 封面落盘；失败返回 null（封面缺失不阻塞扫描） */
    fun write(context: Context, cacheKey: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val directory = File(context.cacheDir, "covers").apply { mkdirs() }
            val file = File(directory, "${sha256(cacheKey)}.jpg")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    /** 十六进制小写 SHA-256（歌曲稳定 ID 与封面缓存名共用同一哈希实现） */
    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

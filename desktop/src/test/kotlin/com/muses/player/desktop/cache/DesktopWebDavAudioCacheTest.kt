package com.muses.player.desktop.cache

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * S2 桌面 WebDAV 缓存单测（隔离 tmp 目录，不污染真实缓存）。
 */
class DesktopWebDavAudioCacheTest {

    private fun newCache(): Pair<DesktopWebDavAudioCache, File> {
        val tmp = Files.createTempDirectory("muses-dav-cache").toFile()
        return DesktopWebDavAudioCache(tmp) to tmp
    }

    private fun srcFile(dir: File, name: String, bytes: ByteArray): File {
        val f = File(dir, name)
        f.writeBytes(bytes)
        return f
    }

    @Test
    fun 未命中返回空命中后可读() {
        val (cache, tmp) = newCache()
        try {
            assertNull(cache.getCachedFile("https://dav.example.com/a.mp3"))
            val src = srcFile(tmp, "a.mp3", ByteArray(128) { it.toByte() })
            cache.putToCache("https://dav.example.com/a.mp3", src, "etag1", "lm1")
            val hit = cache.getCachedFile("https://dav.example.com/a.mp3")
            assertNotNull(hit)
            assertEquals(128, hit.length().toInt())
            assertEquals("etag1", cache.getCachedMeta("https://dav.example.com/a.mp3")?.eTag)
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun 空文件与后缀白名单() {
        val (cache, tmp) = newCache()
        try {
            val empty = srcFile(tmp, "e.mp3", ByteArray(0))
            cache.putToCache("https://dav.example.com/e.mp3", empty)
            assertNull(cache.getCachedFile("https://dav.example.com/e.mp3"))
            val src = srcFile(tmp, "x.bin", ByteArray(16) { 1 })
            cache.putToCache("https://dav.example.com/无后缀路径", src)
            assertNotNull(cache.getCachedFile("https://dav.example.com/无后缀路径"))
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    fun 同名前缀元信息关联() {
        val (cache, tmp) = newCache()
        try {
            val src = srcFile(tmp, "m.flac", ByteArray(64) { 2 })
            cache.putToCache("https://dav.example.com/m.flac", src, "e9", "lm9")
            // 二次命中不丢 meta（touchAccessTime 只改行2）
            cache.getCachedFile("https://dav.example.com/m.flac")
            assertEquals("e9", cache.getCachedMeta("https://dav.example.com/m.flac")?.eTag)
        } finally {
            tmp.deleteRecursively()
        }
    }
}

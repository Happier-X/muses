package com.muses.player.desktop.cache

import com.muses.player.core.data.platform.PlatformDirs
import java.io.File
import java.security.MessageDigest

/**
 * S2 桌面 WebDAV 音频磁盘缓存（对齐 `DiskWebDavAudioCache` 语义，不依赖安卓 Context/Uri）。
 *
 * - 落盘 `<cacheDir>/webdav-cache/<sha256(url)>.<ext>`，ext 取 URL 尾段后缀白名单否则 `audio`；
 * - 每个缓存文件旁存同名前缀 `.meta`（行0=eTag，行1=lastModified，行2=lastAccessMs）；
 * - LRU 500MB：超限按 lastAccess 升序淘汰，同步删 `.meta`；
 * - `.partial`/`.tmp`/空文件一律视为未命中。
 *
 * 二期不做 CacheDataSource 边播边缓存对等：首版整文件入缓存后 file:// 播。
 */
class DesktopWebDavAudioCache(
    private val rootDir: File = File(PlatformDirs.cacheDir(), CACHE_DIR),
) {
    companion object {
        const val CACHE_DIR = "webdav-cache"
        const val MAX_CACHE_BYTES = 500L * 1024L * 1024L
    }

    data class CacheMeta(val eTag: String?, val lastModified: String?)

    private fun cacheDir(): File = rootDir.apply { mkdirs() }

    fun getCachedFile(url: String): File? {
        val file = cacheFile(url)
        if (!file.exists() || file.length() <= 0L) return null
        if (file.name.endsWith(".partial") || file.name.endsWith(".tmp")) return null
        touchAccessTime(url)
        return file
    }

    fun getCachedMeta(url: String): CacheMeta? {
        val metaFile = metaFile(url)
        if (!metaFile.exists()) return null
        return runCatching {
            val lines = metaFile.readLines()
            CacheMeta(
                eTag = lines.getOrNull(0)?.takeIf { it.isNotEmpty() },
                lastModified = lines.getOrNull(1)?.takeIf { it.isNotEmpty() },
            )
        }.getOrNull()
    }

    fun putToCache(url: String, file: File, eTag: String? = null, lastModified: String? = null) {
        if (!file.exists() || file.length() <= 0L) return
        val target = cacheFile(url)
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()
        file.copyTo(target, overwrite = true)
        target.setLastModified(System.currentTimeMillis())
        writeMeta(url, eTag, lastModified)
        trimToLimit()
    }

    fun maxCacheBytes(): Long = MAX_CACHE_BYTES

    fun currentCacheSize(): Long = cacheDir().listFiles()
        ?.filter { it.isFile && !it.name.endsWith(".meta") }
        ?.sumOf { it.length() } ?: 0L

    fun clear() {
        cacheDir().listFiles()?.forEach { it.delete() }
    }

    /**
     * 单条失效（W4 桌面装配，任务 09-05-scrape-kmp）：刮削写回成功后删除该 URL 的
     * 缓存文件与 `.meta`，对齐安卓 `AudioTagReader.invalidate` 语义（避免播放读到旧音频）。
     * URL 未命中时为 no-op；全程不抛异常（失效失败不影响写回主流程）。
     */
    fun invalidate(url: String) {
        runCatching {
            cacheFile(url).delete()
            metaFile(url).delete()
        }
    }

    private fun trimToLimit() {
        val dir = cacheDir()
        val files = dir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".meta") && !it.name.endsWith(".tmp") && !it.name.endsWith(".partial") }
            ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_CACHE_BYTES) return
        val sorted = files.sortedBy { file ->
            runCatching {
                File(dir, file.nameWithoutExtension + ".meta").readLines().getOrNull(2)?.toLongOrNull() ?: 0L
            }.getOrDefault(0L)
        }
        for (file in sorted) {
            if (totalSize <= MAX_CACHE_BYTES) break
            val fileSize = file.length()
            if (file.delete()) totalSize -= fileSize
            File(dir, file.nameWithoutExtension + ".meta").delete()
        }
    }

    private fun cacheFile(url: String): File {
        val extension = url.substringAfterLast('/', "").substringAfterLast('.', "")
            .lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "audio"
        return File(cacheDir(), "${sha256(url)}.$extension")
    }

    private fun metaFile(url: String): File = File(cacheDir(), "${sha256(url)}.meta")

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun writeMeta(url: String, eTag: String?, lastModified: String?) {
        val metaFile = metaFile(url)
        metaFile.parentFile?.mkdirs()
        val now = System.currentTimeMillis().toString()
        metaFile.writeText(listOf(eTag.orEmpty(), lastModified.orEmpty(), now).joinToString("\n"))
    }

    private fun touchAccessTime(url: String) {
        val metaFile = metaFile(url)
        if (!metaFile.exists()) {
            writeMeta(url, null, null)
            return
        }
        runCatching {
            val lines = metaFile.readLines().toMutableList()
            while (lines.size < 3) lines.add("")
            lines[2] = System.currentTimeMillis().toString()
            metaFile.writeText(lines.joinToString("\n"))
        }
    }
}

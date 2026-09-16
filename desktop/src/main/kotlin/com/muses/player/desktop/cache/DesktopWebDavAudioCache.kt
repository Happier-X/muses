package com.muses.player.desktop.cache

import com.muses.player.core.data.platform.PlatformDirs
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

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

    // 原子计数器：避免每次 currentCacheSize 都遍历文件；-1 表示尚未初始化
    private val cachedBytes = AtomicLong(-1L)
    // 播放中保护：已钉住的 URL 不参与淘汰，避免播到一半被删
    private val pinned = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /** 钉住播放中文件（切歌时调用，淘汰跳过） */
    fun acquire(url: String) { pinned.add(url) }

    /** 释放旧播放文件 */
    fun release(url: String) { pinned.remove(url) }

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
        // 原子落盘：先写 .partial 再重命名，避免播放读到半截文件
        val partial = File(target.parentFile, "${target.name}.partial")
        runCatching { partial.delete() }
        file.copyTo(partial, overwrite = true)
        val oldSize = if (target.exists()) target.length() else 0L
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            // 回退：重命名失败则直接覆盖拷贝
            partial.copyTo(target, overwrite = true)
            runCatching { partial.delete() }
        }
        target.setLastModified(System.currentTimeMillis())
        writeMeta(url, eTag, lastModified)
        if (cachedBytes.get() >= 0) cachedBytes.addAndGet(target.length() - oldSize)
        trimToLimit()
    }

    fun maxCacheBytes(): Long {
        // 动态降档：可用空间不足时按四分之一限流，保底 128MB，上限 500MB
        val usable = runCatching { rootDir.usableSpace }.getOrDefault(MAX_CACHE_BYTES)
        if (usable <= 0) return MAX_CACHE_BYTES
        return minOf(MAX_CACHE_BYTES, maxOf(128L * 1024L * 1024L, usable / 4))
    }

    fun currentCacheSize(): Long {
        cachedBytes.get().takeIf { it >= 0 }?.let { return it }
        val size = cacheDir().listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".meta") }
            ?.sumOf { it.length() } ?: 0L
        cachedBytes.set(size)
        return size
    }

    fun clear() {
        cacheDir().listFiles()?.forEach { it.delete() }
        cachedBytes.set(0L)
    }

    /**
     * 单条失效（W4 桌面装配，任务 09-05-scrape-kmp）：刮削写回成功后删除该 URL 的
     * 缓存文件与 `.meta`，对齐安卓 `AudioTagReader.invalidate` 语义（避免播放读到旧音频）。
     * URL 未命中时为 no-op；全程不抛异常（失效失败不影响写回主流程）。
     */
    fun invalidate(url: String) {
        runCatching {
            val target = cacheFile(url)
            val size = if (target.exists()) target.length() else 0L
            target.delete()
            metaFile(url).delete()
            if (cachedBytes.get() >= 0) cachedBytes.addAndGet(-size)
        }
    }

    private fun trimToLimit() {
        val limit = maxCacheBytes()
        val total = currentCacheSize()
        if (total <= limit) return
        val dir = cacheDir()
        val files = dir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".meta") && !it.name.endsWith(".tmp") && !it.name.endsWith(".partial") }
            ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= limit) return
        // 钉住文件先排除；若全部被钉则直接返回不删
        val pinnedNames = pinned.map { sha256(it) }.toSet()
        val candidates = files.filter { f -> pinnedNames.none { f.name.startsWith(it) } }
            .sortedBy { file ->
                runCatching {
                    File(dir, file.nameWithoutExtension + ".meta").readLines().getOrNull(2)?.toLongOrNull() ?: 0L
                }.getOrDefault(0L)
            }
        for (file in candidates) {
            if (totalSize <= limit) break
            val fileSize = file.length()
            if (file.delete()) totalSize -= fileSize
            File(dir, file.nameWithoutExtension + ".meta").delete()
        }
        cachedBytes.set(totalSize)
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

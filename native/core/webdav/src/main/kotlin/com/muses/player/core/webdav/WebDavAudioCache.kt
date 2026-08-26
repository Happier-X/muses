package com.muses.player.core.webdav

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * WebDAV 音频磁盘缓存抽象。
 *
 * 播放流播命中判断与库扫描下载预热共用；测试可注入内存 fake（无需 Android 环境）。
 */
interface WebDavAudioCache {
    /** 返回完整缓存文件；.partial / .tmp / 空文件一律视为未命中 */
    fun getCachedFile(url: String): File?

    /** 写入缓存（从已下载的本地文件搬过来）；调用方需自行删除临时文件 */
    fun putToCache(url: String, file: File, eTag: String? = null, lastModified: String? = null)
}

/**
 * [WebDavAudioCache] 磁盘实现。
 *
 * - 按 URL 做 key，文件存 `context.cacheDir/webdav-cache/`；
 * - ETag / Last-Modified 校验用于条件请求（下载时由调用方传入）；
 * - LRU 淘汰：超限时按 lastAccess 时间升序删除；
 * - 每个缓存文件旁存 `.meta` 文件记录 eTag / lastModified / lastAccess。
 */
@Singleton
class DiskWebDavAudioCache @Inject constructor(
    @ApplicationContext private val context: Context,
) : WebDavAudioCache {
    private val cacheDir: File
        get() = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }

    /**
     * 返回完整缓存文件；.partial / .tmp / 空文件一律视为未命中。
     */
    override fun getCachedFile(url: String): File? {
        val file = cacheFile(url)
        if (!file.exists() || file.length() <= 0L) return null
        if (file.name.endsWith(".partial") || file.name.endsWith(".tmp")) return null
        touchAccessTime(url)
        return file
    }

    /**
     * 获取缓存元信息（ETag / Last-Modified），用于条件请求。
     */
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

    /**
     * 写入缓存（从已下载的本地文件搬过来）。
     * 调用方需自行删除临时文件。
     */
    override fun putToCache(url: String, file: File, eTag: String?, lastModified: String?) {
        if (!file.exists() || file.length() <= 0L) return
        val target = cacheFile(url)
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()
        file.copyTo(target, overwrite = true)
        target.setLastModified(System.currentTimeMillis())
        writeMeta(url, eTag, lastModified)
        trimToLimit()
    }

    /**
     * 获取缓存文件大小上限（字节）。
     */
    fun maxCacheBytes(): Long = MAX_CACHE_BYTES

    /**
     * 当前缓存已用字节数。
     */
    fun currentCacheSize(): Long {
        return cacheDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".meta") }
            ?.sumOf { it.length() }
            ?: 0L
    }

    /**
     * 清空全部缓存。
     */
    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    // ── LRU 淘汰 ──────────────────────────────────────────

    private fun trimToLimit() {
        val files = cacheDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".meta") && !it.name.endsWith(".tmp") && !it.name.endsWith(".partial") }
            ?: return

        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_CACHE_BYTES) return

        // 按 lastAccess（meta 文件中记录的时间）升序排列，最久未访问的先删
        val sorted = files.sortedBy { file ->
            val metaFile = metaFile(urlFromCacheFile(file))
            runCatching { metaFile.readLines().getOrNull(2)?.toLongOrNull() ?: 0L }
                .getOrDefault(0L)
        }

        for (file in sorted) {
            if (totalSize <= MAX_CACHE_BYTES) break
            val fileSize = file.length()
            if (file.delete()) {
                totalSize -= fileSize
            }
            // 同步删除 meta 文件
            metaFile(urlFromCacheFile(file)).delete()
        }
    }

    // ── 文件命名 ──────────────────────────────────────────

    private fun cacheFile(url: String): File {
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            ?: "audio"
        return File(cacheDir, "${sha256(url)}.$extension")
    }

    private fun metaFile(url: String): File {
        return File(cacheDir, "${sha256(url)}.meta")
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 从缓存文件反向推导出 URL 的 sha256（用于 LRU 排序时关联 meta 文件）。
     * 实际无法反推，但 meta 文件名与 cache 文件名一致，通过列表交集匹配。
     */
    private fun urlFromCacheFile(file: File): String {
        // meta 文件名与 cache 文件名共享同一 sha256 前缀
        // 但这里我们不需要反推 URL——直接用 meta 文件名
        return file.nameWithoutExtension // sha256 prefix without extension
    }

    // ── Meta 文件读写 ──────────────────────────────────────────

    private fun touchAccessTime(url: String) {
        val metaFile = metaFile(url)
        if (!metaFile.exists()) {
            writeMeta(url, null, null)
            return
        }
        runCatching {
            val lines = metaFile.readLines().toMutableList()
            // 确保至少 3 行
            while (lines.size < 3) lines.add("")
            lines[2] = System.currentTimeMillis().toString()
            metaFile.writeText(lines.joinToString("\n"))
        }
    }

    private fun writeMeta(url: String, eTag: String?, lastModified: String?) {
        val metaFile = metaFile(url)
        metaFile.writeText(
            listOf(
                eTag ?: "",
                lastModified ?: "",
                System.currentTimeMillis().toString(),
            ).joinToString("\n"),
        )
    }

    data class CacheMeta(
        val eTag: String?,
        val lastModified: String?,
    )

    companion object {
        private const val CACHE_DIR = "webdav-cache"
        private const val MAX_CACHE_BYTES = 500L * 1024L * 1024L // 500 MB
    }
}

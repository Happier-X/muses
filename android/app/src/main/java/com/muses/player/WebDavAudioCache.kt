package com.muses.player

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * WebDAV 音频缓存。
 *
 * 支持：
 * - 整文件下载（元数据扫描等）
 * - 渐进下载：边写边可读，达到起播阈值即可用 file:// 交给播放器
 * - 与元数据扫描共用同一 cache key，避免同 URL 双份文件
 */
class WebDavAudioCache(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val cacheDirectory: File
        get() = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }

    /** 同一 URL 的渐进下载会话，避免并发重复拉取。 */
    private val progressiveSessions = ConcurrentHashMap<String, ProgressiveDownloadSession>()

    fun getCachedFile(url: String): File? {
        val file = cacheFile(url)
        if (!file.exists() || file.length() <= 0L) {
            return null
        }
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    fun getOrDownload(url: String, username: String, password: String): File {
        return getCachedFile(url) ?: download(url, username, password)
    }

    fun downloadInBackground(url: String, username: String, password: String) {
        Thread {
            runCatching { download(url, username, password) }
        }.apply {
            name = "webdav-audio-cache"
            isDaemon = true
            start()
        }
    }

    /**
     * 启动渐进下载并在达到可播阈值后返回可读的缓存文件。
     * 已完整缓存时立即返回；否则边下边写，达到 [readyBytesThreshold] 或下载完成即返回。
     */
    fun startProgressiveDownload(
        url: String,
        username: String,
        password: String,
        readyBytesThreshold: Long = DEFAULT_READY_BYTES,
        onProgress: (ProgressSnapshot) -> Unit,
    ): ProgressiveHandle {
        val target = cacheFile(url)
        val existing = getCachedFile(url)
        if (existing != null) {
            val length = existing.length()
            val snapshot = ProgressSnapshot(
                writtenBytes = length,
                contentLength = length,
                fullyBuffered = true,
                ready = true,
                fileUri = Uri.fromFile(existing).toString(),
            )
            onProgress(snapshot)
            return ProgressiveHandle(
                file = existing,
                fileUri = snapshot.fileUri!!,
                cancel = {},
            )
        }

        val session = progressiveSessions.compute(url) { _, current ->
            if (current != null && !current.cancelled.get() && !current.failed.get()) {
                current
            } else {
                ProgressiveDownloadSession(
                    url = url,
                    target = target,
                    username = username,
                    password = password,
                )
            }
        }!!

        session.addListener(onProgress)

        if (!session.started.compareAndSet(false, true)) {
            // 已有会话：若已 ready 直接回调最新快照
            session.latestSnapshot()?.let(onProgress)
        } else {
            Thread {
                runCatching {
                    session.run(httpClient, readyBytesThreshold)
                }.onFailure { error ->
                    session.markFailed(error)
                }.also {
                    progressiveSessions.remove(url, session)
                }
            }.apply {
                name = "webdav-progressive-download"
                isDaemon = true
                start()
            }
        }

        // 超时用 READY_WAIT_TIMEOUT_MS，勿把 readyBytesThreshold（字节）误当毫秒
        val readyFile = session.awaitReady()
            ?: throw IllegalStateException(session.failureMessage ?: "webdavProgressiveReadyTimeout")

        return ProgressiveHandle(
            file = readyFile,
            fileUri = Uri.fromFile(readyFile).toString(),
            cancel = { session.cancel() },
        )
    }

    fun cancelProgressiveDownload(url: String) {
        progressiveSessions[url]?.cancel()
    }

    private fun download(url: String, username: String, password: String): File {
        val target = cacheFile(url)
        val temp = File(cacheDirectory, "${target.name}.tmp")
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", Credentials.basic(username, password))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("webdavCacheDownloadFailed:${response.code}")
            }
            response.body?.byteStream()?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("webdavCacheEmptyBody")
        }

        if (temp.length() <= 0L) {
            temp.delete()
            throw IllegalStateException("webdavCacheEmptyFile")
        }

        if (target.exists()) {
            target.delete()
        }
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        target.setLastModified(System.currentTimeMillis())
        trimToLimit()
        return target
    }

    /**
     * 将 progressive 的 .partial 固化为正式缓存文件。
     * 若目标被占用或 rename 失败，保留 partial 作为可读路径（播放中安全）。
     */
    private fun finalizePartialToTarget(partial: File, target: File): File {
        if (!partial.exists()) {
            return if (target.exists()) target else partial
        }
        if (target.exists() && target.absolutePath != partial.absolutePath) {
            // 不强制删除正在使用的 target；优先 rename partial 覆盖
            runCatching { target.delete() }
        }
        if (partial.renameTo(target)) {
            return target
        }
        // rename 失败（常见：播放器仍打开 partial）：复制到 target 供下次命中缓存，
        // 但不要删除 partial，以免打断当前播放。
        return runCatching {
            partial.copyTo(target, overwrite = true)
            target
        }.getOrDefault(partial)
    }

    private fun cacheFile(url: String): File {
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            ?: "audio"
        return File(cacheDirectory, "${sha256(url)}.$extension")
    }

    private fun trimToLimit() {
        val files = cacheDirectory.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") && !it.name.endsWith(".partial") }
            ?.sortedBy { it.lastModified() }
            ?: return
        var totalSize = files.sumOf { it.length() }
        for (file in files) {
            if (totalSize <= MAX_CACHE_BYTES) {
                break
            }
            val fileSize = file.length()
            if (file.delete()) {
                totalSize -= fileSize
            }
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    data class ProgressSnapshot(
        val writtenBytes: Long,
        val contentLength: Long?,
        val fullyBuffered: Boolean,
        val ready: Boolean,
        val fileUri: String?,
    )

    data class ProgressiveHandle(
        val file: File,
        val fileUri: String,
        val cancel: () -> Unit,
    )

    private inner class ProgressiveDownloadSession(
        private val url: String,
        private val target: File,
        private val username: String,
        private val password: String,
    ) {
        val started = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        @Volatile
        var failureMessage: String? = null
            private set

        private val readyLatch = Object()
        @Volatile
        private var readyFile: File? = null
        @Volatile
        private var latest: ProgressSnapshot? = null
        private val listeners = mutableListOf<(ProgressSnapshot) -> Unit>()

        fun addListener(listener: (ProgressSnapshot) -> Unit) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        fun latestSnapshot(): ProgressSnapshot? = latest

        fun cancel() {
            cancelled.set(true)
            synchronized(readyLatch) {
                readyLatch.notifyAll()
            }
        }

        fun markFailed(error: Throwable) {
            failed.set(true)
            failureMessage = error.message ?: "webdavProgressiveFailed"
            synchronized(readyLatch) {
                readyLatch.notifyAll()
            }
        }

        fun awaitReady(timeoutMs: Long = READY_WAIT_TIMEOUT_MS): File? {
            val deadline = System.currentTimeMillis() + timeoutMs
            synchronized(readyLatch) {
                while (readyFile == null && !cancelled.get() && !failed.get()) {
                    val remaining = deadline - System.currentTimeMillis()
                    if (remaining <= 0L) {
                        break
                    }
                    readyLatch.wait(remaining.coerceAtMost(500L))
                }
                return readyFile
            }
        }

        fun run(httpClient: OkHttpClient, readyBytesThreshold: Long) {
            val partial = File(target.parentFile, "${target.name}.partial")
            if (partial.exists()) {
                partial.delete()
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", Credentials.basic(username, password))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("webdavCacheDownloadFailed:${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("webdavCacheEmptyBody")
                val contentLength = body.contentLength().takeIf { it > 0L }
                var written = 0L
                var signaledReady = false

                body.byteStream().use { input ->
                    RandomAccessFile(partial, "rw").use { raf ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        while (!cancelled.get()) {
                            val read = input.read(buffer)
                            if (read < 0) {
                                break
                            }
                            raf.write(buffer, 0, read)
                            written += read

                            val fully = contentLength != null && written >= contentLength
                            val ready = fully || written >= readyBytesThreshold
                            val snapshot = ProgressSnapshot(
                                writtenBytes = written,
                                contentLength = contentLength,
                                fullyBuffered = false,
                                ready = ready,
                                fileUri = if (ready) Uri.fromFile(partial).toString() else null,
                            )
                            publish(snapshot)

                            if (ready && !signaledReady) {
                                signaledReady = true
                                markReady(partial)
                            }
                        }
                    }
                }

                if (cancelled.get()) {
                    partial.delete()
                    return
                }

                if (written <= 0L) {
                    partial.delete()
                    throw IllegalStateException("webdavCacheEmptyFile")
                }

                // 下载完成：尽量把 partial 固化为正式缓存。
                // 播放器可能仍打开 partial（边下边播），rename/delete 失败时保留 partial，
                // 避免删掉正在播放的文件导致解码中断。
                val finalFile = finalizePartialToTarget(partial, target)
                finalFile.setLastModified(System.currentTimeMillis())
                trimToLimit()

                val finalSnapshot = ProgressSnapshot(
                    writtenBytes = finalFile.length(),
                    contentLength = finalFile.length(),
                    fullyBuffered = true,
                    ready = true,
                    fileUri = Uri.fromFile(finalFile).toString(),
                )
                publish(finalSnapshot)
                markReady(finalFile)
            }
        }

        private fun markReady(file: File) {
            synchronized(readyLatch) {
                if (readyFile == null) {
                    readyFile = file
                }
                readyLatch.notifyAll()
            }
        }

        private fun publish(snapshot: ProgressSnapshot) {
            latest = snapshot
            val copy = synchronized(listeners) { listeners.toList() }
            copy.forEach { listener ->
                runCatching { listener(snapshot) }
            }
        }
    }

    companion object {
        const val CACHE_DIRECTORY = "webdav-audio"
        const val MAX_CACHE_BYTES = 1_073_741_824L
        /** 起播阈值：约 256KB，兼顾首开速度与解码器对前缀的需求。 */
        const val DEFAULT_READY_BYTES = 256L * 1024L
        private const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        private const val READY_WAIT_TIMEOUT_MS = 60_000L
    }
}

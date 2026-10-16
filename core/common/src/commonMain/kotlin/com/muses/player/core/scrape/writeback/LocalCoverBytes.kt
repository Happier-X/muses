package com.muses.player.core.scrape.writeback

import com.muses.player.core.scrape.http.ScrapeHttp
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.min

/** 本地路径与文件地址共用限额；读取中途文件增长时也不能越过上限。 */
internal suspend fun readLocalCoverBytes(
    location: String,
    maxBytes: Int = ScrapeHttp.MAX_COVER_BYTES,
): ByteArray? {
    require(maxBytes > 0) { "封面大小上限必须大于零" }
    return withContext(Dispatchers.IO) {
        try {
            currentCoroutineContext().ensureActive()
            // File(URI) 负责解码及 Windows 盘符处理，不能直接把 URI.path 当成本地路径。
            val file = if (location.startsWith("file:")) File(URI(location)) else File(location)
            if (!file.isFile || file.length() > maxBytes.toLong()) return@withContext null
            file.inputStream().use { input ->
                val output = ByteArrayOutputStream(min(maxBytes, 8192))
                val buffer = ByteArray(min(maxBytes, 8192))
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val requested = min(buffer.size.toLong(), maxBytes.toLong() - output.size() + 1L).toInt()
                    val count = input.read(buffer, 0, requested)
                    if (count < 0) break
                    if (count > maxBytes - output.size()) return@withContext null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

package com.muses.player.core.appupdate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 更新包下载（jvmShared：安卓/desktop 双端共用；安装动作各平台自理）。
 *
 * 发版约定（见 .github/workflows/release.yml）：每个 tag 附带 `Muses-v<version>.msi`
 * 与两个 APK（主包 / `-mi` MIUI 定制包）。流程：[fetchRelease] 选资产 →
 * [downloadReleaseFile] 落盘（进度回调）→ 平台安装（桌面 msiexec / 安卓系统安装器）。
 */

/** 更新包本地缓存目录：%LOCALAPPDATA%/Muses/updates（异常回落系统临时目录）。 */
fun windowsUpdateDir(): File {
    val base = runCatching {
        val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotEmpty() }
        if (localAppData != null) File(localAppData, "Muses/updates") else null
    }.getOrNull()
    return base ?: File(System.getProperty("java.io.tmpdir"), "Muses-updates")
}

/**
 * 下载更新包到 [dest]（边下边写同名 .part，成功后原子改名；已存在且长度与 [expectedSize]
 * 一致则跳过下载）。[onProgress] 在 IO 线程回调 (已下载字节, 总字节<=0表未知)，
 * 调用方切主线程更新 UI；协程取消时删除 .part。
 */
suspend fun downloadReleaseFile(
    url: String,
    dest: File,
    expectedSize: Long = -1L,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
): Result<File> = withContext(Dispatchers.IO) {
    try {
        if (dest.isFile && expectedSize > 0 && dest.length() == expectedSize) {
            onProgress(expectedSize, expectedSize)
            return@withContext Result.success(dest)
        }
        dest.parentFile?.mkdirs()
        val part = File(dest.parentFile, "${dest.name}.part")
        var connection: java.net.HttpURLConnection? = null
        try {
            // release 下载链 github.com → objects.githubusercontent.com 跨域 302，
            // HttpURLConnection 对 GET 会自动跟随；显式打开以防平台默认差异。
            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Muses")
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("下载失败(${connection.responseCode})"))
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize
            var downloaded = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            if (expectedSize > 0 && part.length() != expectedSize) {
                part.delete()
                return@withContext Result.failure(IllegalStateException("安装包不完整，请重试"))
            }
            if (dest.exists() && !dest.delete()) {
                part.delete()
                return@withContext Result.failure(IllegalStateException("无法写入更新目录"))
            }
            if (!part.renameTo(dest)) {
                part.delete()
                return@withContext Result.failure(IllegalStateException("无法保存安装包"))
            }
            onProgress(dest.length(), total.takeIf { it > 0 } ?: dest.length())
            Result.success(dest)
        } finally {
            connection?.disconnect()
        }
    } catch (e: CancellationException) {
        // 协程取消必须透出（否则上层 Job 无法进入 cancelled，取消按钮失效）
        runCatching { File(dest.parentFile, "${dest.name}.part").takeIf { it.exists() }?.delete() }
        throw e
    } catch (e: Exception) {
        // 失败清掉半截 .part，避免下次误判
        runCatching { File(dest.parentFile, "${dest.name}.part").takeIf { it.exists() }?.delete() }
        Result.failure(e)
    }
}

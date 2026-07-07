package ionic.muses

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

class WebDavAudioCache(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val cacheDirectory: File
        get() = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }

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
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
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

    companion object {
        const val CACHE_DIRECTORY = "webdav-audio"
        const val MAX_CACHE_BYTES = 1_073_741_824L
    }
}

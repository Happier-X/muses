package com.muses.player.core.data.tag

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音频标签读取器
 *
 * 支持从本地文件和 WebDAV 读取音频标签（标题、歌手、专辑、封面、歌词等）。
 * 对于 WebDAV 文件，使用 Range 请求只下载头部数据，减少网络开销。
 */
@Singleton
class AudioTagReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "audio_tags").also { it.mkdirs() }
    }

    private val tagCache = mutableMapOf<String, AudioTags>()

    /**
     * 读取音频标签
     *
     * @param source 音频文件路径或 URL
     * @return 解析出的标签信息，失败返回 null
     */
    fun readTags(source: String): AudioTags? {
        // 命中内存缓存直接返回
        tagCache[source]?.let { return it }

        return try {
            val file = resolveFile(source)
            val tags = parseTags(file)
            tagCache[source] = tags
            tags
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取音频标签（协程版本）
     */
    suspend fun readTagsSuspend(source: String): AudioTags? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            readTags(source)
        }
    }

    /**
     * 解析文件路径，WebDAV 自动下载到缓存
     */
    private fun resolveFile(source: String): File {
        return if (source.startsWith("http://") || source.startsWith("https://")) {
            downloadFile(source)
        } else {
            File(source)
        }
    }

    /**
     * 下载 WebDAV 文件（支持 Range 请求，带认证）
     *
     * 若服务器支持 Range，先取头部 HEAD_SIZE 字节尝试解析；解析失败时扩大至 MAX_HEAD。
     * 不支持 Range 或认证失败时回退全量下载。
     */
    private fun downloadFile(url: String): File {
        val cacheFile = getCacheFile(url)

        // 缓存文件存在且有效，直接返回
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return cacheFile
        }

        // 尝试 Range 请求下载头部（带认证）
        val rangeOk = try {
            downloadRange(url, cacheFile, 0, HEAD_SIZE)
            true
        } catch (_: Exception) {
            false
        }
        if (!rangeOk) {
            // Range 失败：全量下载
            cacheFile.delete()
            downloadFullFile(url, cacheFile)
            return cacheFile
        }
        // Range 成功：尝试解析，若标签为空且文件被截断，扩大范围重试
        return try {
            val probe = runCatching { AudioFileIO.read(cacheFile) }.getOrNull()
            val hasMeaningfulTag = probe?.tag?.let {
                !it.getFirst(FieldKey.TITLE).isNullOrBlank() ||
                    !it.getFirst(FieldKey.ARTIST).isNullOrBlank() ||
                    !it.getFirst(FieldKey.ALBUM).isNullOrBlank()
            } ?: false
            if (!hasMeaningfulTag && cacheFile.length() >= HEAD_SIZE) {
                // 可能标签超出首段或音轨较长，尝试扩大至 MAX_HEAD
                cacheFile.delete()
                downloadRange(url, cacheFile, 0, MAX_HEAD)
            }
            cacheFile
        } catch (_: Exception) {
            cacheFile
        }
    }

    /**
     * Range 请求下载部分文件（认证由 OkHttpClient 拦截器自动注入）
     */
    private fun downloadRange(url: String, target: File, start: Long, end: Long) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use { resp ->
            if (resp.isSuccessful) {
                resp.body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                throw Exception("Range 请求失败: ${resp.code}")
            }
        }
    }

    /**
     * 下载完整文件（认证由 OkHttpClient 拦截器自动注入）
     */
    private fun downloadFullFile(url: String, target: File) {
        val request = Request.Builder().url(url).build()

        val response = okHttpClient.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw Exception("下载失败: ${resp.code}")
            }

            resp.body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * 用 jaudiotagger 解析标签
     */
    private fun parseTags(file: File): AudioTags {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tag

        val durationMs = audioFile.audioHeader?.trackLength?.times(1000L) ?: 0L

        return AudioTags(
            title = tag?.getFirst(FieldKey.TITLE),
            artist = tag?.getFirst(FieldKey.ARTIST),
            album = tag?.getFirst(FieldKey.ALBUM),
            lyrics = tag?.getFirst(FieldKey.LYRICS),
            cover = tag?.firstArtwork?.binaryData,
            durationMs = durationMs
        )
    }

    /**
     * 提取封面并保存到本地文件
     *
     * @param source 音频文件路径或 URL
     * @param songId 歌曲 ID，用于生成唯一文件名
     * @return 封面文件路径，无封面返回 null
     */
    fun extractCover(source: String, songId: String): String? {
        return try {
            val file = resolveFile(source)
            val audioFile = AudioFileIO.read(file)
            val coverData = audioFile.tag?.firstArtwork?.binaryData ?: return null

            val coverFile = File(cacheDir, "cover_${songId}.jpg")
            FileOutputStream(coverFile).use { it.write(coverData) }
            coverFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取缓存文件
     */
    private fun getCacheFile(url: String): File {
        val hash = url.hashCode().toString(16)
        val safeName = url.substringAfterLast("/").take(50)
        return File(cacheDir, "audio_${hash}_${safeName}")
    }

    /**
     * 清理过期缓存
     */
    fun clearOldCache(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        cacheDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAgeMs) {
                file.delete()
            }
        }
    }

    /**
     * 清理所有缓存
     */
    fun clearAllCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        tagCache.clear()
    }

    /**
     * 读取标签并返回可用于更新 SongEntity 的数据
     *
     * @param path 音频文件路径或 URL
     * @param songId 歌曲 ID，用于生成封面文件名
     * @return TagUpdateData，包含可更新的字段
     */
    fun readTagForUpdate(path: String, songId: String): TagUpdateData? {
        val tags = readTags(path) ?: return null
        
        // 提取封面到本地文件
        val coverPath = tags.cover?.let { coverBytes ->
            try {
                val coverFile = File(cacheDir, "cover_${songId}.jpg")
                FileOutputStream(coverFile).use { it.write(coverBytes) }
                coverFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
        
        return TagUpdateData(
            title = tags.title,
            artist = tags.artist,
            album = tags.album,
            lyrics = tags.lyrics,
            coverUri = coverPath,
            durationMs = tags.durationMs
        )
    }

    companion object {
        // 头部大小，足够包含 ID3 标签（64KB），扩大重试上限 256KB
        private const val HEAD_SIZE = 64 * 1024L
        private const val MAX_HEAD = 256 * 1024L
    }
}

/**
 * 标签更新数据，用于更新 SongEntity
 */
data class TagUpdateData(
    val title: String?,
    val artist: String?,
    val album: String?,
    val lyrics: String?,
    val coverUri: String?,
    val durationMs: Long = 0L
)

/**
 * 音频标签数据类
 */
data class AudioTags(
    val title: String?,
    val artist: String?,
    val album: String?,
    val lyrics: String?,
    val cover: ByteArray?,
    val durationMs: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioTags) return false
        return title == other.title &&
                artist == other.artist &&
                album == other.album &&
                lyrics == other.lyrics &&
                cover.contentEquals(other.cover) &&
                durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (cover?.contentHashCode() ?: 0)
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

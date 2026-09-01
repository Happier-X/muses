package com.muses.player.core.data.tag

import android.content.Context
import android.net.Uri
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
     * 解析文件路径，支持 WebDAV / content:// / file:// / 绝对路径四态。
     * - http(s) → Range 下载到缓存（WebDAV）
     * - content:// → ContentResolver 拷贝到缓存（本地懒补充，Song.path 为 content URI）
     * - file:// → Uri 解析取 path
     * - 其它 → 视为文件绝对路径直接 File
     */
    private fun resolveFile(source: String): File {
        return when {
            source.startsWith("http://") || source.startsWith("https://") -> downloadFile(source)
            source.startsWith("content://") -> copyContentUriToCache(source)
            source.startsWith("file://") -> {
                val path = Uri.parse(source).path ?: source.removePrefix("file://")
                File(path)
            }
            else -> File(source)
        }
    }

    /**
     * 将 content:// URI 经 ContentResolver 拷贝到本地缓存文件，供 jaudiotagger 解析。
     * 缓存命中复用（播放切歌不再重复拷贝）；失败抛异常由外层转 null（懒扫描静默重试）。
     */
    private fun copyContentUriToCache(uriString: String): File {
        val hash = uriString.hashCode().toString(16)
        val rawSuffix = uriString.substringAfterLast('/').substringAfterLast(':').take(30)
        val safeSuffix = rawSuffix.replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "audio" }
        val target = File(cacheDir, "content_${hash}_${safeSuffix}.tmp")
        if (target.exists() && target.length() > 0) return target
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } ?: throw java.io.FileNotFoundException("无法打开 content URI: $uriString")
        return target
    }

    /**
     * 下载 WebDAV 文件（支持 Range 请求，带认证）
     *
     * 策略：先 Range 头部 HEAD_SIZE 字节；
     * 若文件是 ID3v2，则按标签头声明的标签大小补齐（内嵌大封面可让标签达到数百 KB，
     * 固定上限会截断标签导致 jaudiotagger 整段解析失败——0321 - space x 封面 531KB 案例）；
     * 补齐仍失败或非 ID3 文件保持头部，交给解析器容错。
     * 不支持 Range 或认证失败时回退全量下载。
     */
    private fun downloadFile(url: String): File {
        val cacheFile = getCacheFile(url)

        // 缓存文件存在且有效，直接返回；ID3v2 缓存若短于标签声明大小（旧版 256KB 截断）删除重下
        if (cacheFile.exists() && cacheFile.length() > 0) {
            val declared = readId3v2TagSize(cacheFile)
            if (declared <= 0 || cacheFile.length() >= ID3V2_HEADER_SIZE + declared) {
                return cacheFile
            }
            cacheFile.delete()
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

        // 按 ID3v2 头声明的标签大小补齐（synchsafe），上限 MAX_HEAD 防恶意声明
        val declared = readId3v2TagSize(cacheFile)
        if (declared > 0) {
            val needed = ID3V2_HEADER_SIZE + declared
            if (cacheFile.length() < needed && needed <= MAX_HEAD) {
                cacheFile.delete()
                downloadRange(url, cacheFile, 0, needed - 1)
            }
        }
        return cacheFile
    }

    /** 读取 ID3v2 标签头声明的标签总大小（synchsafe 32-bit）；非 ID3 文件返回 0 */
    private fun readId3v2TagSize(file: File): Long {
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val head = ByteArray(ID3V2_HEADER_SIZE.toInt())
                if (raf.read(head) < ID3V2_HEADER_SIZE) return 0L
                if (head[0] != 'I'.code.toByte() || head[1] != 'D'.code.toByte() || head[2] != '3'.code.toByte()) {
                    return 0L
                }
                ((head[6].toLong() and 0x7F) shl 21) or
                    ((head[7].toLong() and 0x7F) shl 14) or
                    ((head[8].toLong() and 0x7F) shl 7) or
                    (head[9].toLong() and 0x7F)
            }
        } catch (_: Exception) {
            0L
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
     *
     * 优先通用 [AudioFileIO.read]（FLAC/M4A/完整 MP3，可同时取时长）；
     * 失败时回退 [ID3v24Tag(ByteBuffer)] / [ID3v23Tag(ByteBuffer)] 直接解析 ID3v2 数据——
     * WebDAV 头部探测文件只含标签 + 少量音频帧（内嵌大封面标签可达数百 KB，音频被 Range 截断），
     * `AudioFileIO.read` / `MP3File` 均因找不到音频帧整体抛异常，ByteBuffer 方式不碰音频即可拿到文本帧。
     */
    private fun parseTags(file: File): AudioTags {
        val general = runCatching {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            AudioTags(
                title = tag?.getFirst(FieldKey.TITLE),
                artist = tag?.getFirst(FieldKey.ARTIST),
                album = tag?.getFirst(FieldKey.ALBUM),
                lyrics = tag?.getFirst(FieldKey.LYRICS),
                cover = tag?.firstArtwork?.binaryData,
                durationMs = audioFile.audioHeader?.trackLength?.times(1000L) ?: 0L,
            )
        }.getOrNull()
        if (general != null) {
            return general
        }

        // 回退：ID3v2 ByteBuffer 解析（仅头部文件场景；文件上限 MAX_HEAD=4MB，读内存可控）
        val id3 = parseId3v2FromBuffer(file) ?: throw org.jaudiotagger.tag.TagException("Not a parseable ID3v2 buffer")
        return AudioTags(
            title = id3.getFirst(FieldKey.TITLE),
            artist = id3.getFirst(FieldKey.ARTIST),
            album = id3.getFirst(FieldKey.ALBUM),
            lyrics = id3.getFirst(FieldKey.LYRICS),
            cover = id3.firstArtwork?.binaryData,
            durationMs = 0L,
        )
    }

    /** 从头部缓存文件读取 ID3v2 标签（v2.4 → ID3v24Tag，v2.3 → ID3v23Tag）；非 ID3 返回 null */
    private fun parseId3v2FromBuffer(file: File): org.jaudiotagger.tag.id3.AbstractID3v2Tag? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 10 || bytes[0] != 'I'.code.toByte() ||
                bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()
            ) {
                return null
            }
            when (bytes[3]) {
                4.toByte() -> org.jaudiotagger.tag.id3.ID3v24Tag(java.nio.ByteBuffer.wrap(bytes))
                3.toByte() -> org.jaudiotagger.tag.id3.ID3v23Tag(java.nio.ByteBuffer.wrap(bytes))
                else -> null
            }
        } catch (_: Exception) {
            null
        }
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
        // ID3v2 头固定 10 字节（含 synchsafe 大小声明）
        private const val ID3V2_HEADER_SIZE = 10L
        // 首次探测头部大小（64KB，ID3v2 文本帧基本都在此范围内）
        private const val HEAD_SIZE = 64 * 1024L
        // 按标签声明补齐的上限（4MB）：内嵌大封面可达数百 KB~数 MB；超限防恶意声明拖垮
        private const val MAX_HEAD = 4 * 1024 * 1024L
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

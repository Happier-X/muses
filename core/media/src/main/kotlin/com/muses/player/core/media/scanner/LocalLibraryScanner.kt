package com.muses.player.core.media.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.muses.player.core.media.metadata.TagReader
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 本地库扫描器（MediaStore → jaudiotagger，去 Capacitor 化）。
 * - MediaStore 负责枚举本机音频（IS_MUSIC），可选按音源目录前缀过滤；
 * - jaudiotagger 读 ID3/Vorbis 标签与内嵌封面，失败回退 MediaStore 列值；
 * - 封面落盘 cacheDir/covers/<sha256(songKey)>.jpg，返回安全 file:// URI。
 */
@Singleton
class LocalLibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val progressInternal = MutableStateFlow(ScanProgress())

    /** 扫描进度流；扫描开始时重置 */
    val scanProgress: StateFlow<ScanProgress> = progressInternal.asStateFlow()

    /**
     * 扫描本地音频。传 [source]（LOCAL）时仅保留其目录前缀下的文件；
     * 返回 domain Song 列表（不写库——持久化由调用方/Worker 完成）。
     *
     * [readTags] = false 时跳过 TagReader（jaudiotagger）逐文件读取，标签全空，
     * 直接回退 MediaStore 列值/文件名（对照 Web「读取音乐标签」开关关闭态）；
     * 默认 true 保持既有调用方兼容。
     */
    suspend fun scan(source: Source? = null, readTags: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        val sourceId = source?.id ?: DEFAULT_LOCAL_SOURCE_ID
        val pathPrefix = source?.path?.trim()?.trimEnd('/')
        val prefixNormalized = pathPrefix?.let { "$it/" }

        progressInternal.value = ScanProgress()
        val items = queryMediaStore(prefixNormalized)
        var index = 0
        val songs = ArrayList<Song>(items.size)

        for (item in items) {
            index++
            progressInternal.value =
                ScanProgress(current = index, total = items.size, currentFile = item.displayName)

            val tags = if (readTags) readTagsSafely(item.data) else TagReaderResult.empty
            val song = Song(
                id = stableSongId(sourceId, item.data),
                sourceId = sourceId,
                path = item.uri.toString(),
                title = tags.title ?: item.titleFromStore ?: item.displayName.substringBeforeLast('.'),
                artist = tags.artist ?: item.artist,
                album = tags.album ?: item.album,
                durationMs = item.durationMs ?: 0L,
                durationSec = (item.durationMs ?: 0L) / 1000L,
                coverUri = tags.coverBytes?.let { CoverCacheWriter.write(context, stableSongId(sourceId, item.data), it) },
                lyrics = tags.lyrics,
                replayGainTrackDb = tags.replayGainTrackDb,
                sourceType = SourceType.LOCAL,
                tagsVersion = TAGS_VERSION,
            )
            songs.add(song)
        }

        progressInternal.value = ScanProgress(
            current = items.size,
            total = items.size,
            finished = true,
        )
        songs
    }

    private fun queryMediaStore(pathPrefix: String?): List<MediaItem> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val result = ArrayList<MediaItem>()

        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val data = cursor.getString(dataCol) ?: continue
                    if (!isSupportedAudio(data)) continue
                    if (pathPrefix != null && !data.replace('\\', '/').startsWith(pathPrefix)) continue
                    val contentUri = ContentUris.withAppendedId(collection, cursor.getLong(idCol))
                    result.add(
                        MediaItem(
                            uri = contentUri,
                            data = data.replace('\\', '/'),
                            displayName = cursor.getString(nameCol).orEmpty(),
                            titleFromStore = cursor.getString(titleCol),
                            artist = cursor.getString(artistCol)?.takeIf { it != MediaStore.UNKNOWN_STRING },
                            album = cursor.getString(albumCol)?.takeIf { it != MediaStore.UNKNOWN_STRING },
                            durationMs = cursor.getLong(durationCol).takeIf { it > 0L },
                        ),
                    )
                }
            }
        } catch (_: SecurityException) {
            // READ_MEDIA_AUDIO 未授权：返回空结果，权限请求在设置/音源页处理
        } catch (_: IllegalArgumentException) {
            // 游标列异常：返回已收集部分
        }
        return result
    }

    private fun readTagsSafely(filePath: String): TagReaderResult {
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) return TagReaderResult.empty
        return try {
            val tags = TagReader.read(file)
            TagReaderResult(tags.title, tags.artist, tags.album, tags.lyrics, tags.replayGainTrackDb, tags.coverBytes)
        } catch (_: Exception) {
            TagReaderResult.empty
        }
    }

    private data class TagReaderResult(
        val title: String?,
        val artist: String?,
        val album: String?,
        val lyrics: String?,
        val replayGainTrackDb: Double?,
        val coverBytes: ByteArray?,
    ) {
        companion object {
            val empty = TagReaderResult(null, null, null, null, null, null)
        }
    }

    private data class MediaItem(
        val uri: Uri,
        val data: String,
        val displayName: String,
        val titleFromStore: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long?,
    )

    companion object {
        const val TAGS_VERSION = 1

        /** 未指定音源时的默认本地扫描标识 */
        const val DEFAULT_LOCAL_SOURCE_ID = "local"

        /** 稳定歌曲 ID：sourceId + 文件路径哈希 */
        fun stableSongId(sourceId: String, filePath: String): String =
            CoverCacheWriter.sha256("$sourceId|$filePath")

        fun isSupportedAudio(pathOrName: String): Boolean {
            val extension = pathOrName.substringAfterLast('.', "").lowercase()
            return extension in SUPPORTED_AUDIO_EXTENSIONS
        }

        private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
            "aac", "aiff", "alac", "ape", "flac", "m4a", "m4b", "mp3", "ogg", "opus", "wav", "wma",
        )
    }
}

package com.muses.player.core.data.tag

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.muses.player.core.data.dao.SongDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 后台读取音频标签的 Worker
 *
 * 处理数据库中所有未读取标签的歌曲（tagsVersion < 1），
 * 使用 jaudiotagger 读取元数据并更新数据库。
 */
@HiltWorker
class TagReaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val songDao: SongDao,
    private val tagReader: AudioTagReader,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val untaggedIds = songDao.getUntaggedSongIds()
            if (untaggedIds.isEmpty()) {
                return@withContext Result.success()
            }

            var processed = 0
            var failed = 0

            for (songId in untaggedIds) {
                if (isStopped) {
                    return@withContext Result.success()
                }

                try {
                    val song = songDao.getById(songId) ?: continue
                    val tagData = tagReader.readTagForUpdate(song.path, songId)

                    if (tagData != null) {
                        val hasUpdate = !tagData.title.isNullOrBlank() ||
                            !tagData.artist.isNullOrBlank() ||
                            !tagData.album.isNullOrBlank() ||
                            tagData.coverUri != null ||
                            tagData.durationMs > 0
                        val updatedSong = song.copy(
                            title = tagData.title?.takeIf { it.isNotBlank() } ?: song.title,
                            artist = tagData.artist ?: song.artist,
                            albumTitle = tagData.album ?: song.albumTitle,
                            lyrics = tagData.lyrics ?: song.lyrics,
                            coverUri = tagData.coverUri ?: song.coverUri,
                            durationMs = tagData.durationMs.coerceAtLeast(song.durationMs),
                            durationSec = (tagData.durationMs / 1000).coerceAtLeast(song.durationSec),
                            // TAGS_VERSION = 1（对齐 LocalLibraryScanner.TAGS_VERSION）
                            tagsVersion = 1
                        )
                        songDao.upsert(updatedSong)
                        if (hasUpdate) processed++ else failed++
                    } else {
                        // 标签读取失败，保持原 tagsVersion=0 下次重试，不标记为已处理
                        failed++
                    }

                    // 每处理 10 首报告进度
                    if ((processed + failed) % 10 == 0) {
                        setProgressAsync(
                            androidx.work.workDataOf(
                                PROGRESS_TOTAL to untaggedIds.size,
                                PROGRESS_CURRENT to processed + failed,
                            )
                        )
                    }
                } catch (e: Exception) {
                    failed++
                }
            }

            Result.success(
                androidx.work.workDataOf(
                    RESULT_PROCESSED to processed,
                    RESULT_FAILED to failed,
                )
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "tag_reader_worker"
        const val PROGRESS_TOTAL = "total"
        const val PROGRESS_CURRENT = "current"
        const val RESULT_PROCESSED = "processed"
        const val RESULT_FAILED = "failed"
    }
}

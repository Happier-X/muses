package com.muses.player.core.scrape.writeback

import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.RollbackEntry
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.RollbackSongSnapshot
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.ScrapeHistoryEntry
import com.muses.player.core.model.scrape.WritebackResult
import com.muses.player.core.model.scrape.WritebackStatus
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 刮削写回编排 + 回滚 journal（规格书 = src/features/scrape/writeback.ts，五步流程逐一对齐）：
 *
 * 1. 写前快照旧值到回滚 journal（上限 200 条）
 * 2. 写文件（本地并行 / WebDAV 串行）
 * 3. 文件保存核验成功后写库，失败不覆盖已保存标签
 * 4. 逐行返回成功/失败状态（success / file-failed / failed）
 * 5. 恢复同样先写文件，保存成功后同步曲库
 *
 * W3 上收 commonMain（任务 09-05-scrape-kmp R4）：三仓库依赖直连 commonMain
 * SongRepository；文件写入经 [AudioTagFileWriter]（TagPort 收口）；日志经 safeLogW。
 */
class WritebackOrchestrator(
    private val songRepository: SongRepository,
    private val journalStore: RollbackJournalStore,
    private val fileWriter: AudioTagFileWriter,
    /** 远程封面字节获取；null = 不内嵌远程封面 */
    private val coverBytesFetcher: CoverBytesFetcher? = null,
    /**
     * 历史落库旁路（S4 ScrapeHistoryStore 接线）；失败不影响写回主流程。
     * 默认空实现 = Web recordHistory 的 try/catch 吞错语义。
     */
    private val historySink: suspend (List<ScrapeHistoryEntry>) -> Unit = { },
    /** 时钟注入（测试用） */
    private val nowMs: () -> Long = System::currentTimeMillis,
    /** 音频标签缓存失效器（写文件成功后清除 AudioTagReader 缓存，避免下次播放读旧缓存） */
    private val audioTagCacheInvalidator: ((String) -> Unit)? = null,
) {

    /** applyScrapeChanges 返回值（Web：{ journalId, results }） */
    data class ApplyResult(
        val journalId: String,
        val results: List<WritebackResult>,
    )

    // ── 步骤 1：写前快照 ─────────────────────────────────

    private fun snapshotSong(song: Song): RollbackSongSnapshot = RollbackSongSnapshot(
        title = song.title,
        artist = song.artist,
        album = song.album,
        coverUri = song.coverUri,
        lyrics = song.lyrics,
        lyricsFormat = song.lyricsFormat,
        lyricsSource = song.lyricsSource,
        metaSources = song.metaSources,
    )

    // ── 步骤 3：写库 ─────────────────────────────────────

    /** 已保存文件的标签同步到曲库；返回库是否更新。 */
    private suspend fun updateSongInLibrary(
        songId: String,
        changes: ScrapeChanges,
        fileOk: Boolean,
        coverWritten: Boolean,
    ): Boolean {
        // 曲库只缓存已保存的文件标签；候选与待上传数据由各自队列保存。
        if (!fileOk) return false

        val song = songRepository.getSong(songId) ?: return false
        val metaSources = song.metaSources ?: com.muses.player.core.model.scrape.MetaSources()
        val fieldSource = MetaFieldSource.EMBEDDED
        val effectiveCoverUri = changes.coverUri ?: changes.coverRemoteUrl
        val newMetaSources = com.muses.player.core.model.scrape.MetaSources(
            title = if (changes.title != null) fieldSource else metaSources.title,
            artist = if (changes.artist != null) fieldSource else metaSources.artist,
            album = if (changes.album != null) fieldSource else metaSources.album,
            // 封面取不到字节时只保留展示地址并标记待内嵌，后续可重新刮削；显式清空按已写入处理。
            cover = if (effectiveCoverUri != null) {
                if (coverWritten) fieldSource else com.muses.player.core.model.scrape.MetaFieldSource.SCRAPE
            } else metaSources.cover,
        )

        songRepository.upsert(
            song.copy(
                title = changes.title ?: song.title,
                artist = changes.artist ?: song.artist,
                album = changes.album ?: song.album,
                // 空串语义 = 清空（Web changes.coverUri || undefined）；coverRemoteUrl 同步落库为 coverUri 供 coil 直链展示
                coverUri = when {
                    effectiveCoverUri == null -> song.coverUri
                    effectiveCoverUri.isEmpty() -> null
                    else -> effectiveCoverUri
                },
                lyrics = changes.lyrics ?: song.lyrics,
                lyricsFormat = changes.lyricsFormat ?: song.lyricsFormat,
                lyricsSource = if (changes.lyrics != null) {
                    com.muses.player.core.model.scrape.LyricsSource.EMBEDDED
                } else {
                    song.lyricsSource
                },
                metaSources = newMetaSources.takeIf {
                    it.title != null || it.artist != null || it.album != null || it.cover != null
                },
            ),
        )
        return true
    }

    // ── 远程封面内嵌（ensureLocalCover 语义）─────────────

    private suspend fun fetchCoverBytes(changes: ScrapeChanges): ByteArray? {
        val remoteUrl = changes.coverRemoteUrl
            ?: changes.coverUri?.takeIf { it.startsWith("file://") || java.io.File(it).isFile }
            ?: return null
        if (!remoteUrl.startsWith("http://") && !remoteUrl.startsWith("https://")) {
            val path = if (remoteUrl.startsWith("file://")) {
                runCatching { java.net.URI(remoteUrl).path }.getOrNull()
            } else {
                remoteUrl
            }
            return path?.let { file -> runCatching { java.io.File(file).readBytes() }.getOrNull() }
        }
        return try {

            coverBytesFetcher?.fetch(remoteUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    // ── 旁路：历史落库 ───────────────────────────────────

    /** 对齐 recordHistory：changedFields 归并 coverRemoteUrl→cover、lyricsFormat→lyrics */
    private fun buildHistoryEntries(
        candidates: List<ScrapeCandidate>,
        results: List<WritebackResult>,
        changesMap: Map<String, ScrapeChanges>,
        journalId: String,
    ): List<ScrapeHistoryEntry> {
        val candidateMap = candidates.associateBy { it.songId }
        return results.map { result ->
            val song = candidateMap[result.songId]?.song
            val changes = changesMap[result.songId] ?: ScrapeChanges()
            // ScrapeChanges key → 可读字段名归并：coverRemoteUrl 与 coverUri 同属 cover
            val changedFields = buildSet {
                if (changes.coverUri != null || changes.coverRemoteUrl != null) add("cover")
                if (changes.lyrics != null || changes.lyricsFormat != null) add("lyrics")
                changes.title?.let { add("title") }
                changes.artist?.let { add("artist") }
                changes.album?.let { add("album") }
            }
            ScrapeHistoryEntry(
                id = "history-${nowMs()}-${result.songId}",
                journalId = journalId,
                songId = result.songId,
                songTitle = song?.title ?: result.songId,
                songArtist = song?.artist,
                at = nowMs().isoUtc(),
                status = result.status,
                failureReason = if (result.status != WritebackStatus.SUCCESS) {
                    describeWritebackFailure(
                        WritebackFailureInput(
                            fileResultCode = result.fileResult.code,
                            fileResultMessage = result.fileResult.message,
                            error = result.error,
                        ),
                    )
                } else {
                    null
                },
                changedFields = changedFields.toList(),
            )
        }
    }

    // ── 公开 API ─────────────────────────────────────────

    /**
     * 批量写回：逐曲独立结果。写前自动快照到回滚 journal；返回 journalId 用于撤销。
     */
    suspend fun applyScrapeChanges(
        candidates: List<ScrapeCandidate>,
        checkedIds: Set<String>,
        changesMap: Map<String, ScrapeChanges>,
    ): ApplyResult {
        // 1. 写前快照（截断到上限 200）
        val journalId = "journal-${nowMs()}"
        val entries = candidates.asSequence()
            .filter { it.songId in checkedIds }
            .mapNotNull { candidate ->
                snapshotSong(candidate.song).let {
                    RollbackEntry(
                        songId = candidate.songId,
                        songBefore = it,
                        createdAt = nowMs().isoUtc(),
                    )
                }
            }
            .toList()
            .takeLast(MAX_ROLLBACK_ENTRIES)
        journalStore.write(RollbackJournal(version = 1, journalId = journalId, entries = entries))

        // 2. 分流：本地可并行，WebDAV 串行（Web localQueue/webdavQueue 语义）
        val checked = candidates.filter { it.songId in checkedIds }
        val webdavQueue = checked.filter { it.song.sourceType == com.muses.player.core.model.SourceType.WEBDAV }
        val localQueue = checked.filter { it.song.sourceType != com.muses.player.core.model.SourceType.WEBDAV }

        suspend fun writeOne(candidate: ScrapeCandidate): WritebackResult {
            val changes = changesMap[candidate.songId] ?: ScrapeChanges()
            return try {
                val outcome = writeSingleFile(candidate.song, changes)
                val fileResult = outcome.result

                safeLogW(
                    "Writeback",
                    "writeOne ${candidate.songId} fileOk=${fileResult.ok} code=${fileResult.code} msg=${fileResult.message} changes=$changes",
                )
                // 写文件成功后失效标签缓存，保证后续懒扫描读到新文件内容
                if (fileResult.ok) {
                    try {
                        audioTagCacheInvalidator?.invoke(candidate.song.path)
                    } catch (_: Exception) {
                        // 缓存失效失败不影响主流程
                    }
                }
                val libraryUpdated = updateSongInLibrary(candidate.songId, changes, fileResult.ok, outcome.coverWritten)


                WritebackResult(
                    songId = candidate.songId,
                    status = if (fileResult.ok) WritebackStatus.SUCCESS else WritebackStatus.FILE_FAILED,
                    fileResult = fileResult,
                    libraryUpdated = libraryUpdated,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WritebackResult(
                    songId = candidate.songId,
                    status = WritebackStatus.FAILED,
                    fileResult = FileWriteResult(ok = false, code = "unknown", message = e.message ?: "写回失败"),
                    libraryUpdated = false,
                    error = e.message ?: "写回失败",
                )
            }
        }

        val results = coroutineScope {
            val localResults = localQueue.map { candidate -> async { writeOne(candidate) } }.awaitAll()
            val webdavResults = webdavQueue.map { candidate -> writeOne(candidate) }
            localResults + webdavResults
        }

        // 仅已保存的歌曲可以恢复；未保存任务保持在候选或补传队列。
        val savedIds = results.filter { it.status == WritebackStatus.SUCCESS }.map { it.songId }.toSet()
        journalStore.write(RollbackJournal(version = 1, journalId = journalId,
            entries = entries.filter { it.songId in savedIds }))
        // 旁路落历史（确认写回与重试都经过此处，不漏记；失败不阻断主流程）
        try {
            historySink(buildHistoryEntries(candidates, results, changesMap, journalId))
        } catch (_: Exception) {
            // 历史记录为旁路能力，失败静默
        }

        return ApplyResult(journalId = journalId, results = results)
    }

    /** coverWritten 表示文件中封面状态已按本次请求落地（含显式清空）。 */
    private data class FileWriteOutcome(val result: FileWriteResult, val coverWritten: Boolean)

    /**
     * 单曲文件写入（含封面字节获取）。
     * 内嵌封面失败只跳过封面：文本标签照常保存并上传，封面保留展示地址待后续重刮
     * （对齐 ensureLocalCover「失败返回 null 跳过内嵌，不阻断写回」语义）；
     * 显式清空封面（coverUri = ""）不依赖封面字节，照常写入。
     */
    private suspend fun writeSingleFile(song: Song, changes: ScrapeChanges): FileWriteOutcome {
        val coverCleared = changes.coverUri == ""
        val coverRequested = !coverCleared && (changes.coverUri != null || changes.coverRemoteUrl != null)
        val coverBytes = if (coverCleared) null else fetchCoverBytes(changes)
        val coverEmbedded = coverRequested && coverBytes?.isNotEmpty() == true
        val coverWritten = coverCleared || !coverRequested || coverEmbedded
        val writable = if (coverRequested && !coverEmbedded) {
            changes.copy(coverUri = null, coverRemoteUrl = null)
        } else {
            changes
        }
        return FileWriteOutcome(
            result = fileWriter.write(song, writable, if (coverEmbedded) coverBytes else null),
            coverWritten = coverWritten,
        )
    }

    // ── 撤销 ─────────────────────────────────────────────


    /**
     * 恢复：先写回旧标签并核验，再同步数据库；失败条目保留以便重试。
     * journalId 不匹配当前 journal → 不动作（对齐 revertScrapeJournal 防御）。
     */
    suspend fun revertScrapeJournal(journalId: String): RevertResult {
        val journal = journalStore.read()
        if (journal == null || journal.journalId != journalId) {
            return RevertResult(reverted = 0)
        }

        var reverted = 0
        val remaining = mutableListOf<com.muses.player.core.model.scrape.RollbackEntry>()
        for (entry in journal.entries) {
            val song = songRepository.getSong(entry.songId) ?: continue
            val before = entry.songBefore
            val changes = ScrapeChanges(title = before.title, artist = before.artist.orEmpty(),
                album = before.album.orEmpty(), lyrics = before.lyrics.orEmpty(),
                lyricsFormat = before.lyricsFormat, coverUri = before.coverUri.orEmpty(),
                coverRemoteUrl = before.coverUri?.takeIf { it.startsWith("http://") || it.startsWith("https://") })
            val bytes = if (changes.coverRemoteUrl != null) fetchCoverBytes(changes) else
                before.coverUri?.takeIf { it.isNotBlank() }?.let { path ->
                    runCatching { java.io.File(if (path.startsWith("file://")) java.net.URI(path).path else path).readBytes() }.getOrNull()
                }
            val result = fileWriter.write(song, changes, bytes)
            if (!result.ok) { remaining.add(entry); continue }
            audioTagCacheInvalidator?.invoke(song.path)
            reverted += 1
            songRepository.upsert(
                song.copy(
                    title = before.title,
                    artist = before.artist,
                    album = before.album,
                    coverUri = before.coverUri,
                    lyrics = before.lyrics,
                    lyricsFormat = before.lyricsFormat,
                    lyricsSource = before.lyricsSource,
                    metaSources = before.metaSources,
                ),
            )
        }

        if (remaining.isEmpty()) journalStore.clear()
        else journalStore.write(journal.copy(entries = remaining))
        return RevertResult(reverted = reverted, failed = remaining.size)
    }

    /** 读取当前回滚 journal（UI 显示用） */
    suspend fun getCurrentRollbackJournal(): RollbackJournal? = journalStore.read()

    /** revertScrapeJournal 返回值（Web：{ reverted }） */
    data class RevertResult(val reverted: Int, val failed: Int = 0)

    companion object {
        /** Web MAX_ROLLBACK_ENTRIES = 200 */
        const val MAX_ROLLBACK_ENTRIES: Int = 200
    }
}

/** epoch ms → ISO UTC 字符串（Web new Date().toISOString() 对齐） */
internal fun Long.isoUtc(): String {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = this@isoUtc
    }
    return String.format(
        Locale.ROOT,
        "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ",
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        cal.get(java.util.Calendar.SECOND),
        cal.get(java.util.Calendar.MILLISECOND),
    )
}

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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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
    private val readReviewFileRevision: suspend (Song) -> String? = ::readLocalAudioRevision,
    private val coverSnapshotStore: CoverSnapshotStore? = null,
) {

    // 各页面共用同一编排器；整次操作互斥，避免文件、曲库与单条撤销记录交错。
    private val operationMutex = Mutex()

    private suspend inline fun <T> withExclusiveOperation(block: () -> T): T {
        if (!operationMutex.tryLock()) throw WritebackInProgressException()
        try {
            return block()
        } finally {
            operationMutex.unlock()
        }
    }

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

    private suspend fun captureEntry(song: Song, fields: Set<String>?): RollbackEntry {
        val revision = if (coverSnapshotStore != null) readReviewFileRevision(song) else null
        val captured = if (coverSnapshotStore != null) fileWriter.captureSnapshot(song) else null
        if (revision != null && readReviewFileRevision(song) != revision) error("建立历史快照期间文件已变化，请重新审核")
        val tags = captured?.tags
        val before = if (tags == null) snapshotSong(song) else snapshotSong(song).copy(
            title = tags.title.orEmpty(), artist = tags.artist, album = tags.album, lyrics = tags.lyrics)
        val cover = if (fields == null || "cover" in fields) {
            if (captured?.coverKnown == true) {
                if (tags?.cover == null) "" else try { coverSnapshotStore?.save(tags.cover) }
                catch (e: CancellationException) { throw e } catch (_: Exception) { null }
            } else null
        } else null
        return RollbackEntry(song.id, before, nowMs().isoUtc(), fields, fileSaved = false,
            coverSnapshot = cover, fileSnapshotKnown = tags != null,
            coverSnapshotRequired = coverSnapshotStore != null && (fields == null || "cover" in fields))
    }

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
            ?: changes.coverUri?.takeIf {
                it.startsWith("http://") || it.startsWith("https://") ||
                    it.startsWith("file:") || java.io.File(it).isFile
            }
            ?: return null
        if (!remoteUrl.startsWith("http://") && !remoteUrl.startsWith("https://")) {
            return readLocalCoverBytes(remoteUrl)
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
                            fileResultCode = result.fileResult.code.takeUnless { result.fileResult.ok },
                            fileResultMessage = result.fileResult.message.takeUnless { result.fileResult.ok },
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
    ): ApplyResult = withExclusiveOperation {
        applyChanges(candidates, checkedIds, changesMap)
    }

    private suspend fun applyChanges(
        candidates: List<ScrapeCandidate>,
        checkedIds: Set<String>,
        changesMap: Map<String, ScrapeChanges>,
    ): ApplyResult {
        // 审核页可能停留很久：当前曲库对象决定快照、来源及文件路径。
        val checked = mutableListOf<ScrapeCandidate>()
        val preparationFailures = mutableListOf<WritebackResult>()
        fun preparationFailed(songId: String, code: String, message: String) {
            preparationFailures.add(WritebackResult(
                songId = songId, status = WritebackStatus.FAILED,
                fileResult = FileWriteResult(false, code, message),
                libraryUpdated = false, error = message,
            ))
        }
        for (candidate in candidates.filter { it.songId in checkedIds }) {
            try {
                val current = songRepository.getSong(candidate.songId)
                currentCoroutineContext().ensureActive()
                if (current == null) {
                    preparationFailed(candidate.songId, "song_missing", "歌曲已不在曲库中，请返回队列刷新")
                } else if (candidate.expectedFileRevision != null && (
                    current.path != candidate.song.path || current.sourceId != candidate.song.sourceId ||
                    current.sourceType != candidate.song.sourceType || snapshotSong(current) != snapshotSong(candidate.song) ||
                    readReviewFileRevision(current) != candidate.expectedFileRevision)) {
                    preparationFailed(candidate.songId, "review_changed", "审核期间歌曲信息或音频文件已变化，已停止保存，请重新读取后再审核。")
                } else {
                    checked.add(candidate.copy(song = current))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                preparationFailed(candidate.songId, "song_read_failed", "读取歌曲信息失败，请稍后重试")
            }
        }
        // 1. 写前快照（截断到上限 200）；全部无法开始时保留上次撤销记录。
        // 编号不依赖时钟，避免同毫秒写回或时钟回拨时旧入口误操作新记录。
        val journalId = if (checked.isEmpty()) "" else "journal-${UUID.randomUUID()}"
        val entries = checked.takeLast(MAX_ROLLBACK_ENTRIES).map { candidate ->
            captureEntry(candidate.song, (changesMap[candidate.songId] ?: ScrapeChanges()).changedFields())
        }
        val previousJournal = if (checked.isNotEmpty()) journalStore.read() else null
        val pendingJournal = RollbackJournal(version = 1, journalId = journalId, entries = entries)
        if (checked.isNotEmpty()) {
            journalStore.write(pendingJournal)
        }

        // 2. 本地完整保存流程有界并行，避免封面、临时音频和磁盘写入同时堆积。
        val webdavQueue = checked.filter { it.song.sourceType == com.muses.player.core.model.SourceType.WEBDAV }
        val localQueue = checked.filter { it.song.sourceType != com.muses.player.core.model.SourceType.WEBDAV }
        val localPermits = Semaphore(MAX_CONCURRENT_LOCAL_WRITES)
        val coverWrites = ConcurrentHashMap<String, Boolean>()

        suspend fun writeOne(candidate: ScrapeCandidate): WritebackResult {
            val changes = changesMap[candidate.songId] ?: ScrapeChanges()
            return try {
                val outcome = writeSingleFile(candidate.song, changes, candidate.expectedFileRevision)
                val fileResult = outcome.result
                if (fileResult.ok) coverWrites[candidate.songId] = outcome.coverWritten

                safeLogW(
                    "Writeback",
                    "writeOne ${candidate.songId} fileOk=${fileResult.ok} code=${fileResult.code} msg=${fileResult.message} changes=$changes",
                )
                // 写文件成功后失效标签缓存，保证后续懒扫描读到新文件内容
                if (fileResult.ok) {
                    try {
                        audioTagCacheInvalidator?.invoke(candidate.song.path)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // 缓存失效失败不影响主流程
                    }
                }
                val libraryUpdated = try {
                    updateSongInLibrary(candidate.songId, changes, fileResult.ok, outcome.coverWritten)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }


                WritebackResult(
                    songId = candidate.songId,
                    status = when {
                        !fileResult.ok -> WritebackStatus.FILE_FAILED
                        !libraryUpdated -> WritebackStatus.FAILED
                        else -> WritebackStatus.SUCCESS
                    },
                    fileResult = fileResult,
                    libraryUpdated = libraryUpdated,
                    error = if (fileResult.ok && !libraryUpdated) "文件标签已保存，但曲库更新失败，请重试" else null,
                    coverSkipped = fileResult.ok && !outcome.coverWritten,
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

        fun savedEntries(ids: Set<String>) = entries.filter { it.songId in ids }.map {
            it.copy(coverWritten = coverWrites[it.songId] ?: true, fileSaved = true)
        }
        val results = try {
            coroutineScope {
                val localResults = localQueue.map { candidate ->
                    async {
                        localPermits.withPermit {
                            currentCoroutineContext().ensureActive()
                            writeOne(candidate)
                        }
                    }
                }.awaitAll()
                val webdavResults = webdavQueue.map { candidate -> writeOne(candidate) }
                localResults + webdavResults + preparationFailures
            }
        } catch (e: CancellationException) {
            // 取消仍传播，但不能把未写入的歌曲或未内嵌的封面作为已保存内容撤销。
            try {
                withContext(NonCancellable) {
                    if (checked.isNotEmpty()) {
                        val confirmed = savedEntries(coverWrites.keys).associateBy { it.songId }
                        journalStore.write(RollbackJournal(1, journalId, entries.map { confirmed[it.songId] ?: it }))
                    }
                }
            } catch (_: Exception) {
                // 写前快照保持未确认状态，后续撤销不能自动覆盖这些文件。
            }
            throw e
        }

        // 仅已保存的歌曲可以恢复；未保存任务保持在候选或补传队列。
        val savedIds = results.filter { it.fileResult.ok }.map { it.songId }.toSet()
        if (checked.isNotEmpty()) {
            if (results.all { !it.fileResult.ok && it.fileResult.code in setOf("review_changed", "file_changed", "review_check_unsupported", "song_missing", "song_read_failed") }) {
                // 确认尚未写入时保留上一条撤销；只替换本次完整快照，不覆盖其他实例的新记录。
                journalStore.replaceIfUnchanged(pendingJournal, previousJournal)
            } else {
                journalStore.write(RollbackJournal(version = 1, journalId = journalId,
                    entries = savedEntries(savedIds)))
            }
        }
        // 旁路落历史（确认写回与重试都经过此处，不漏记；失败不阻断主流程）
        try {
            val currentById = checked.associateBy { it.songId }
            val historyCandidates = candidates.map { currentById[it.songId] ?: it }
            historySink(buildHistoryEntries(historyCandidates, results, changesMap, journalId))
        } catch (e: CancellationException) {
            throw e
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
    private suspend fun writeSingleFile(song: Song, changes: ScrapeChanges, expectedRevision: String? = null): FileWriteOutcome {
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
            result = if (expectedRevision == null) fileWriter.write(song, writable, if (coverEmbedded) coverBytes else null)
                else fileWriter.writeReviewed(song, writable, if (coverEmbedded) coverBytes else null, expectedRevision),
            coverWritten = coverWritten,
        )
    }

    // ── 撤销 ─────────────────────────────────────────────


    /**
     * 恢复：先写回旧标签并核验，再同步数据库；失败条目保留以便重试。
     * journalId 不匹配当前 journal → 不动作（对齐 revertScrapeJournal 防御）。
     */
    suspend fun revertScrapeJournal(journalId: String): RevertResult = withExclusiveOperation {
        revertJournal(journalId)
    }

    private suspend fun revertJournal(journalId: String, historical: RollbackJournal? = null,
        expectedRevision: String? = null, onFileSaved: suspend () -> Unit = {}): RevertResult {
        val journal = historical ?: journalStore.read()
        if (journal == null || journal.journalId != journalId) {
            return RevertResult(reverted = 0)
        }

        var reverted = 0
        val remaining = mutableListOf<com.muses.player.core.model.scrape.RollbackEntry>()
        for (entry in journal.entries) {
            if (!entry.fileSaved) {
                remaining.add(entry)
                continue
            }
            try {
                val song = songRepository.getSong(entry.songId)
                if (song == null) {
                    remaining.add(entry)
                    continue
                }
                val before = entry.songBefore
                val fields = entry.changedFields
                fun restores(field: String) = fields == null || field in fields
                val restoreCoverFile = restores("cover") && entry.coverWritten
                if (restoreCoverFile && entry.coverSnapshotRequired && entry.coverSnapshot == null) {
                    remaining.add(entry)
                    continue
                }
                val changes = ScrapeChanges(
                    title = before.title.takeIf { restores("title") },
                    artist = before.artist.orEmpty().takeIf { restores("artist") },
                    album = before.album.orEmpty().takeIf { restores("album") },
                    lyrics = before.lyrics.orEmpty().takeIf { restores("lyrics") },
                    lyricsFormat = before.lyricsFormat.takeIf { restores("lyrics") },
                    coverUri = if (!restoreCoverFile) null else when (entry.coverSnapshot) {
                        null -> before.coverUri.orEmpty()
                        "" -> ""
                        else -> "snapshot:${entry.coverSnapshot}"
                    },
                    coverRemoteUrl = before.coverUri?.takeIf { restoreCoverFile && entry.coverSnapshot == null && (it.startsWith("http://") || it.startsWith("https://")) },
                )
                val bytes = if (restoreCoverFile && !entry.coverSnapshot.isNullOrEmpty())
                    requireNotNull(coverSnapshotStore) { "封面快照存储不可用" }.read(entry.coverSnapshot)
                    else fetchCoverBytes(changes)
                val result = if (changes == ScrapeChanges()) FileWriteResult(true)
                    else if (expectedRevision != null) fileWriter.writeReviewed(song, changes, bytes, expectedRevision)
                    else fileWriter.write(song, changes, bytes)
                if (!result.ok) { remaining.add(entry); continue }
                onFileSaved()
                try {
                    audioTagCacheInvalidator?.invoke(song.path)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // 缓存失效为旁路，失败不阻断文件与曲库恢复。
                }
                songRepository.upsert(
                    song.copy(
                        title = if (restores("title")) before.title else song.title,
                        artist = if (restores("artist")) before.artist else song.artist,
                        album = if (restores("album")) before.album else song.album,
                        coverUri = if (restores("cover")) {
                            if (restoreCoverFile && !entry.coverSnapshot.isNullOrEmpty()) coverSnapshotStore?.location(entry.coverSnapshot) else before.coverUri
                        } else song.coverUri,
                        lyrics = if (restores("lyrics")) before.lyrics else song.lyrics,
                        lyricsFormat = if (restores("lyrics")) before.lyricsFormat else song.lyricsFormat,
                        lyricsSource = if (restores("lyrics")) before.lyricsSource else song.lyricsSource,
                        metaSources = if (fields == null) before.metaSources else com.muses.player.core.model.scrape.MetaSources(
                            title = if (restores("title")) before.metaSources?.title else song.metaSources?.title,
                            artist = if (restores("artist")) before.metaSources?.artist else song.metaSources?.artist,
                            album = if (restores("album")) before.metaSources?.album else song.metaSources?.album,
                            cover = if (restores("cover")) before.metaSources?.cover else song.metaSources?.cover,
                        ).takeIf { it.title != null || it.artist != null || it.album != null || it.cover != null },
                    ),
                )
                reverted += 1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 单曲恢复失败只保留本条重试，其余歌曲继续处理。
                remaining.add(entry)
            }
        }

        // 恢复期间可能产生新的写回记录；收尾只更新本次读取的原快照。
        if (historical == null) journalStore.replaceIfUnchanged(journal, journal.copy(entries = remaining).takeIf { remaining.isNotEmpty() })
        return RevertResult(reverted = reverted, failed = remaining.size, unconfirmed = remaining.count { !it.fileSaved })
    }

    /** 历史预览携带当前版本，防止弹窗停留期间文件或曲库变化。 */
    data class HistoryPreview(val journalId: String, val entry: RollbackEntry, val current: Song, val revision: String?,
        val currentValues: RollbackSongSnapshot, val coverPreviewUri: String?)

    suspend fun getRollbackHistory(): List<RollbackJournal> = journalStore.readHistory()

    suspend fun previewHistoricalEntry(journalId: String, songId: String): HistoryPreview {
        val entry = journalStore.readHistory().firstOrNull { it.journalId == journalId }?.entries?.singleOrNull { it.songId == songId }
            ?: error("历史记录已不存在，请刷新")
        val song = songRepository.getSong(songId) ?: error("歌曲已不在曲库中")
        val revision = readReviewFileRevision(song)
        val actual = fileWriter.captureSnapshot(song)?.tags
        val currentValues = if (actual == null) snapshotSong(song) else snapshotSong(song).copy(
            title = actual.title.orEmpty(), artist = actual.artist, album = actual.album, lyrics = actual.lyrics)
        check(readReviewFileRevision(song) == revision) { "预览期间文件已变化，请重试" }
        return HistoryPreview(journalId, entry, song, revision, currentValues,
            entry.coverSnapshot?.takeIf { it.isNotEmpty() }?.let { coverSnapshotStore?.location(it) })
    }

    /** 恢复单曲历史保留原历史，并将恢复前的实际标签保存为新的可撤销版本。 */
    suspend fun restoreHistoricalEntry(preview: HistoryPreview): RevertResult = withExclusiveOperation {
        val selected = journalStore.readHistory().firstOrNull { it.journalId == preview.journalId }?.entries
            ?.singleOrNull { it.songId == preview.entry.songId } ?: error("历史记录已不存在，请刷新")
        check(selected == preview.entry && selected.fileSaved) { "历史记录已变化，请重新预览" }
        val current = songRepository.getSong(selected.songId) ?: error("歌曲已不在曲库中")
        check(current == preview.current && readReviewFileRevision(current) == preview.revision && preview.revision != null) {
            "歌曲或文件已变化，或无法核对版本，请重新预览"
        }
        val fields = selected.changedFields
        val restoresCover = (fields == null || "cover" in fields) && selected.coverWritten
        check(!restoresCover || selected.coverSnapshot != null) { "该版本没有可靠的主封面快照，无法完整恢复本次字段" }
        if (restoresCover && !selected.coverSnapshot.isNullOrEmpty()) requireNotNull(coverSnapshotStore).read(selected.coverSnapshot)
        val auditEntry = captureEntry(current, fields).copy(coverWritten = selected.coverWritten)
        check(auditEntry.fileSnapshotKnown && (!restoresCover || auditEntry.coverSnapshot != null)) { "无法保存恢复前的标签或主封面快照，已停止恢复" }
        check(readReviewFileRevision(current) == preview.revision) { "建立快照期间文件已变化，请重新预览" }
        val previous = journalStore.read()
        val audit = RollbackJournal(1, "journal-${UUID.randomUUID()}", listOf(auditEntry))
        journalStore.write(audit)
        var saved = false
        try {
            currentCoroutineContext().ensureActive()
            // 单曲历史恢复的写入、结果接收与新记录确认一起完成，避免取消丢失已保存状态。
            val result = withContext(NonCancellable) {
                revertJournal(preview.journalId, RollbackJournal(1, preview.journalId, listOf(selected)), preview.revision) {
                    saved = true
                    check(journalStore.replaceIfUnchanged(audit, audit.copy(entries = listOf(auditEntry.copy(fileSaved = true))))) {
                        "恢复已保存，但撤销记录发生变化，请核对历史"
                    }
                }
            }
            if (!saved) journalStore.replaceIfUnchanged(audit, previous)
            result
        } catch (e: CancellationException) {
            if (!saved) withContext(NonCancellable) { journalStore.replaceIfUnchanged(audit, previous) }
            throw e
        }
        catch (e: Exception) {
            if (!saved) journalStore.replaceIfUnchanged(audit, previous)
            throw e
        }
    }

    /**
     * 放弃撤销记录：记录已无法恢复（例如歌曲已从曲库删除）时收尾。
     * journalId 不匹配当前记录 → 不动作，避免清掉之后新产生的记录。
     */
    suspend fun discardRollbackJournal(journalId: String): Boolean = withExclusiveOperation {
        journalStore.clearIfMatches(journalId)
    }
    /** 读取当前回滚 journal（UI 显示用） */
    suspend fun getCurrentRollbackJournal(): RollbackJournal? = journalStore.read()

    /** revertScrapeJournal 返回值（Web：{ reverted }） */
    data class RevertResult(val reverted: Int, val failed: Int = 0, val unconfirmed: Int = 0)

    companion object {
        /** 许可覆盖封面获取、文件保存和曲库更新整个过程。 */
        internal const val MAX_CONCURRENT_LOCAL_WRITES = 2

        /** Web MAX_ROLLBACK_ENTRIES = 200 */
        const val MAX_ROLLBACK_ENTRIES: Int = 200
    }
}

/** 其他页面有写回或恢复在进行，本次未启动且可稍后重试。 */
class WritebackInProgressException : IllegalStateException("正在写回或恢复标签，请等待完成后重试")

private fun ScrapeChanges.changedFields(): Set<String> = buildSet {
    if (title != null) add("title")
    if (artist != null) add("artist")
    if (album != null) add("album")
    if (lyrics != null || lyricsFormat != null) add("lyrics")
    if (coverUri != null || coverRemoteUrl != null) add("cover")
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

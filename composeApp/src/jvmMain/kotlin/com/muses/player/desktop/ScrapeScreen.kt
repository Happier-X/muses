package com.muses.player.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.WritebackStatus
import com.muses.player.core.model.scrape.WritebackResult
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.EditDimKey
import com.muses.player.core.scrape.editmeta.EditDimStatus
import com.muses.player.core.scrape.editmeta.SearchOptions
import com.muses.player.core.scrape.writeback.WritebackFailureInput
import com.muses.player.core.scrape.writeback.describeWritebackFailure
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.components.ScrapeProgressBar
import com.muses.player.core.ui.components.ScrapeResultRow
import com.muses.player.core.ui.components.ScrapeReviewCard
import com.muses.player.core.ui.components.ScrapeStatusKind
import com.muses.player.core.ui.components.ScrapeCandidateRow
import com.muses.player.core.ui.components.SharedReviewField
import com.muses.player.core.ui.components.SharedScrapeCandidate
import com.muses.player.core.ui.components.SharedWritebackResult
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.desktop.di.DesktopContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * 桌面刮削页（W4 桌面装配收尾，任务 09-05-scrape-kmp R5）。
 *
 * 引擎已 KMP 化（W1-W3），本页去回调占位接真实链路（装配见 [DesktopScrapeGraph]）：
 * - 扫队列 → 匹配（[EditCloudMetaSearch] 全链：文本五源 + 封面六源 + 歌词维度）→ 预览（字段勾选，
 *   默认勾选有值且有差异的字段，写回安全红线）→ 写回（[WritebackOrchestrator]，
 *   按 sourceType 分流本地/WebDAV，标题/专辑/封面/歌词落库 + 文件落盘）→ 结果（逐行状态 + 撤销）；
 * - 歌词维度已接通（09-05-lyrics-kmp X4）：AMLL TTML + 平台五源 + LRCLIB，
 *   最优候选进预览可勾选，写回落文件（ID3 LYRICS）+ 落库。
 */

/** 队列行展示数据（Room SongEntity 反查后的本地快照）。 */
private data class DesktopScrapeQueueRow(
    val songId: String,
    val title: String,
    val subtitle: String?,
    val coverUri: String?,
)

/** 预览行（匹配结果 + 字段勾选状态）。 */
private data class DesktopPreviewItem(
    val songId: String,
    val songTitle: String,
    val artist: String?,
    val album: String?,
    val coverUri: String?,
    /** 当前已存歌词（song.lyrics 快照，供预览行 original 展示与差异比较） */
    val currentLyrics: String?,
    val matchedTitle: String?,
    val matchedArtist: String?,
    val matchedAlbum: String?,
    val matchedCoverUrl: String?,
    /** 最优歌词候选全文 + 格式 wire 值（lrc/ttml/yrc/qrc；写回经 LyricsFormat 还原） */
    val matchedLyricsText: String?,
    val matchedLyricsFormat: String?,
    val sourceWire: String?,
    val network: Boolean,
    val checkedFields: Set<String>,
) {
    val hasCandidate: Boolean
        get() = matchedTitle != null || matchedArtist != null || matchedAlbum != null ||
            matchedCoverUrl != null || matchedLyricsText != null
}

/** 页面四态机（queue → matching → preview → writing → result；语义对齐安卓 ScrapePageState）。 */
private sealed interface DesktopScrapeState {
    data object Queue : DesktopScrapeState
    data class Matching(val total: Int, val done: Int, val currentTitle: String?) : DesktopScrapeState
    data class Preview(val items: List<DesktopPreviewItem>) : DesktopScrapeState
    data class Writing(val total: Int) : DesktopScrapeState

    /** @param titles songId → 歌名快照（出队后仍可展示，对齐安卓 queueTitles 透传） */
    data class Result(
        val results: List<WritebackResult>,
        val journalId: String,
        val titles: Map<String, String>,
    ) : DesktopScrapeState
}

@Composable
fun ScrapeScreen(modifier: Modifier = Modifier) {
    val salt = LocalSaltColors.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<DesktopScrapeState>(DesktopScrapeState.Queue) }
    var queueRows by remember { mutableStateOf<List<DesktopScrapeQueueRow>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }

    fun reloadQueue() {
        scope.launch {
            runCatching {
                val ids = DesktopScrapeGraph.queueStore.load().map { it.songId }
                val db = DesktopContainer.database()
                queueRows = ids.mapNotNull { id ->
                    db.songDao().getById(id)?.let { e ->
                        DesktopScrapeQueueRow(
                            songId = e.id,
                            title = e.title,
                            subtitle = listOfNotNull(e.artist, e.albumTitle)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .takeIf { it.isNotBlank() },
                            coverUri = e.coverUri,
                        )
                    }
                }
            }.onFailure { e ->
                message = "读取刮削队列失败：${e.message}"
            }
        }
    }

    fun toggleField(songId: String, key: String) {
        val preview = state as? DesktopScrapeState.Preview ?: return
        state = preview.copy(
            items = preview.items.map { item ->
                if (item.songId != songId) {
                    item
                } else {
                    item.copy(
                        checkedFields = if (key in item.checkedFields) {
                            item.checkedFields - key
                        } else {
                            item.checkedFields + key
                        },
                    )
                }
            },
        )
    }

    /** 全部开始：队列逐曲全链云搜（文本 + 封面 + 歌词三维度） */
    fun startMatch() {
        val rows = queueRows
        if (rows.isEmpty()) return
        message = null
        matchJob?.cancel()
        matchJob = scope.launch {
            state = DesktopScrapeState.Matching(total = rows.size, done = 0, currentTitle = null)
            val db = DesktopContainer.database()
            val preview = mutableListOf<DesktopPreviewItem>()
            for ((index, row) in rows.withIndex()) {
                // EditCloudMetaSearch 内部会把普通 CancellationException 归为网络错误，
                // 取消检查点放在曲与曲之间（引擎既有语义，安卓同款处理）
                currentCoroutineContext().ensureActive()
                val song = db.songDao().getById(row.songId)?.toDomain()
                if (song == null) continue
                state = DesktopScrapeState.Matching(total = rows.size, done = index, currentTitle = song.title)
                val result = try {
                    DesktopScrapeGraph.editSearch.search(
                        query = EditCloudMetaQuery(
                            songId = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            durationSec = song.durationSec.takeIf { it > 0 }?.toDouble(),
                        ),
                        options = SearchOptions(
                            dimensions = setOf(EditDimKey.TEXT, EditDimKey.COVER, EditDimKey.LYRICS),
                        ),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                val text = result?.text?.items?.firstOrNull()
                val cover = result?.cover?.items?.firstOrNull()
                // 最优歌词候选：仅 status == OK 时取首条（对齐安卓 EditMetaViewModel 语义）
                val lyrics = result?.lyrics
                    ?.takeIf { it.status == EditDimStatus.OK }
                    ?.items?.firstOrNull()
                val network = (result == null) ||
                    (text == null && result.text.status == EditDimStatus.NETWORK) ||
                    (cover == null && result.cover.status == EditDimStatus.NETWORK) ||
                    (lyrics == null && result.lyrics.status == EditDimStatus.NETWORK)
                // 写回安全红线：默认勾选有值且有差异的字段
                val checked = buildSet {
                    if (text?.title?.takeIf { it.isNotBlank() } != null && text.title != song.title) add("title")
                    if (text?.artist?.takeIf { it.isNotBlank() } != null && text.artist != song.artist) add("artist")
                    if (text?.album?.takeIf { it.isNotBlank() } != null && text.album != song.album) add("album")
                    if (cover != null && cover.remoteUrl != song.coverUri) add("cover")
                    if (lyrics != null && lyrics.text.isNotBlank() && lyrics.text != song.lyrics) add("lyrics")
                }
                preview += DesktopPreviewItem(
                    songId = song.id,
                    songTitle = song.title,
                    artist = song.artist,
                    album = song.album,
                    coverUri = song.coverUri,
                    currentLyrics = song.lyrics,
                    matchedTitle = text?.title?.takeIf { it.isNotBlank() },
                    matchedArtist = text?.artist?.takeIf { it.isNotBlank() },
                    matchedAlbum = text?.album?.takeIf { it.isNotBlank() },
                    matchedCoverUrl = cover?.remoteUrl,
                    matchedLyricsText = lyrics?.text,
                    matchedLyricsFormat = lyrics?.format,
                    sourceWire = text?.source?.wire,
                    network = network && text == null && cover == null && lyrics == null,
                    checkedFields = checked,
                )
            }
            state = DesktopScrapeState.Preview(preview)
        }
    }

    fun cancelMatch() {
        matchJob?.cancel()
        matchJob = null
        state = DesktopScrapeState.Queue
        reloadQueue()
    }

    /** 确认写回：仅写回各首勾选的字段；逐曲结果进 result 态（对齐安卓 confirmWriteback） */
    fun confirmWriteback(preview: DesktopScrapeState.Preview) {
        val checkedItems = preview.items.filter { it.checkedFields.isNotEmpty() }
        if (checkedItems.isEmpty()) return
        message = null
        scope.launch {
            state = DesktopScrapeState.Writing(checkedItems.size)
            try {
                val db = DesktopContainer.database()
                val candidates = mutableListOf<ScrapeCandidate>()
                val changesMap = mutableMapOf<String, ScrapeChanges>()
                for (item in checkedItems) {
                    val song = db.songDao().getById(item.songId)?.toDomain() ?: continue
                    candidates += ScrapeCandidate(songId = song.id, song = song)
                    val writeLyrics = item.matchedLyricsText.takeIf { "lyrics" in item.checkedFields }
                    changesMap[song.id] = ScrapeChanges(
                        title = item.matchedTitle.takeIf { "title" in item.checkedFields },
                        artist = item.matchedArtist.takeIf { "artist" in item.checkedFields },
                        album = item.matchedAlbum.takeIf { "album" in item.checkedFields },
                        coverRemoteUrl = item.matchedCoverUrl.takeIf { "cover" in item.checkedFields },
                        lyrics = writeLyrics,
                        // 候选格式 wire（lrc/ttml/yrc/qrc）→ LyricsFormat；未识别格式落 null 走默认
                        lyricsFormat = writeLyrics?.let {
                            item.matchedLyricsFormat?.let { wire ->
                                LyricsFormat.entries.firstOrNull { it.wire == wire }
                            }
                        },
                    )
                }
                val applyResult = DesktopScrapeGraph.orchestrator.applyScrapeChanges(
                    candidates = candidates,
                    checkedIds = changesMap.keys,
                    changesMap = changesMap,
                )
                // 写回完成后出队已处理歌曲并刷新（对齐安卓：全部已处理者出队）
                DesktopScrapeGraph.queueStore.remove(changesMap.keys.toList())
                reloadQueue()
                state = DesktopScrapeState.Result(
                    results = applyResult.results,
                    journalId = applyResult.journalId,
                    titles = candidates.associate { it.songId to it.song.title },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "写回失败：${e.message}"
                state = preview
            }
        }
    }

    /** 撤销本次写回：journal 回放恢复库旧值（文件不可逆，UI 明示） */
    fun undoWriteback(journalId: String) {
        scope.launch {
            runCatching { DesktopScrapeGraph.orchestrator.revertScrapeJournal(journalId) }
                .onFailure { e -> message = "撤销失败：${e.message}" }
            reloadQueue()
            state = DesktopScrapeState.Queue
        }
    }

    LaunchedEffect(Unit) { reloadQueue() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "刮削",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "待刮削歌曲统一补全标题/歌手/专辑/封面/歌词（匹配 → 预览 → 写回）。",
            color = salt.text2,
            fontSize = 12.sp,
        )
        message?.let {
            Text(text = it, color = salt.danger, fontSize = 13.sp)
        }

        when (val s = state) {
            DesktopScrapeState.Queue -> QueueContent(
                rows = queueRows,
                modifier = Modifier.weight(1f),
                onRemove = { songId ->
                    scope.launch {
                        runCatching { DesktopScrapeGraph.queueStore.remove(listOf(songId)) }
                            .onFailure { e -> message = "移除失败：${e.message}" }
                        reloadQueue()
                    }
                },
                onClear = {
                    scope.launch {
                        runCatching { DesktopScrapeGraph.queueStore.clear() }
                            .onFailure { e -> message = "清空失败：${e.message}" }
                        reloadQueue()
                    }
                },
                onStartMatch = ::startMatch,
            )
            is DesktopScrapeState.Matching -> ScrapeProgressBar(
                current = s.done,
                total = s.total,
                currentItem = s.currentTitle,
                title = "正在匹配 ${s.done} / ${s.total}",
                onCancel = ::cancelMatch,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            is DesktopScrapeState.Preview -> PreviewContent(
                state = s,
                modifier = Modifier.weight(1f),
                onToggleField = ::toggleField,
                onWriteback = { confirmWriteback(s) },
                onBack = {
                    matchJob = null
                    state = DesktopScrapeState.Queue
                    reloadQueue()
                },
            )
            is DesktopScrapeState.Writing -> ScrapeProgressBar(
                current = 0,
                total = s.total,
                title = "正在写回 ${s.total} 首（WebDAV 上传/本地落盘需数秒）",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            is DesktopScrapeState.Result -> ResultContent(
                state = s,
                modifier = Modifier.weight(1f),
                onUndo = { undoWriteback(s.journalId) },
                onBack = {
                    state = DesktopScrapeState.Queue
                    reloadQueue()
                },
            )
        }
    }
}

// ── Queue 态 ─────────────────────────────────────────────

@Composable
private fun QueueContent(
    rows: List<DesktopScrapeQueueRow>,
    modifier: Modifier = Modifier,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onStartMatch: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (rows.isEmpty()) {
            SaltEmpty(
                title = "待刮削队列为空",
                description = "请先在歌曲页标记需要刮削的歌曲。",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(rows, key = { it.songId }) { row ->
                    ScrapeCandidateRow(
                        candidate = SharedScrapeCandidate(
                            songId = row.songId,
                            title = row.title,
                            subtitle = row.subtitle,
                            coverUri = row.coverUri,
                        ),
                        trailing = {
                            SaltTextButton(text = "移除", onClick = { onRemove(row.songId) })
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onClear, modifier = Modifier.weight(1f)) { Text("清空") }
            Button(onClick = onStartMatch, modifier = Modifier.weight(2f), enabled = rows.isNotEmpty()) {
                Text("全部开始")
            }
        }
    }
}

// ── Preview 态 ───────────────────────────────────────────

@Composable
private fun PreviewContent(
    state: DesktopScrapeState.Preview,
    modifier: Modifier = Modifier,
    onToggleField: (String, String) -> Unit,
    onWriteback: () -> Unit,
    onBack: () -> Unit,
) {
    val salt = LocalSaltColors.current
    val checkedCount = state.items.count { it.checkedFields.isNotEmpty() }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.songId }) { item ->
                PreviewCard(item = item, onToggleField = onToggleField)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SaltTextButton(text = "返回队列", onClick = onBack)
            Spacer(Modifier.weight(1f))
            Button(onClick = onWriteback, enabled = checkedCount > 0) {
                Text("写回选中（$checkedCount）")
            }
        }
        Text(
            text = "写回含文件落盘（本地直写 / WebDAV 下载-写-上传），标题/专辑/封面/歌词同时落库。",
            color = salt.text2,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun PreviewCard(
    item: DesktopPreviewItem,
    onToggleField: (String, String) -> Unit,
) {
    val salt = LocalSaltColors.current
    val originalCover = item.coverUri ?: "（无）"
    val fields = listOf(
        SharedReviewField(
            key = "title",
            label = "标题",
            original = item.songTitle,
            updated = item.matchedTitle,
            checked = "title" in item.checkedFields,
        ),
        SharedReviewField(
            key = "artist",
            label = "歌手",
            original = item.artist ?: "（无）",
            updated = item.matchedArtist,
            checked = "artist" in item.checkedFields,
        ),
        SharedReviewField(
            key = "album",
            label = "专辑",
            original = item.album ?: "（无）",
            updated = item.matchedAlbum,
            checked = "album" in item.checkedFields,
        ),
        SharedReviewField(
            key = "cover",
            label = "封面",
            original = originalCover,
            updated = item.matchedCoverUrl,
            checked = "cover" in item.checkedFields,
        ),
        // 歌词维度（09-05-lyrics-kmp X4）：最优候选全文可勾选写回（单行截断展示，写回带全文）
        SharedReviewField(
            key = "lyrics",
            label = "歌词",
            original = item.currentLyrics ?: "（无）",
            updated = item.matchedLyricsText,
            checked = "lyrics" in item.checkedFields,
        ),
    )
    Column {
        ScrapeReviewCard(
            candidate = SharedScrapeCandidate(
                songId = item.songId,
                title = item.songTitle,
                subtitle = listOfNotNull(item.artist, item.album)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .takeIf { it.isNotBlank() },
                coverUri = item.matchedCoverUrl ?: item.coverUri,
                sourceLabel = item.sourceWire,
            ),
            fields = fields,
            onToggleField = { key -> onToggleField(item.songId, key) },
        )
        if (!item.hasCandidate) {
            Text(
                text = if (item.network) {
                    "未命中：网络/限流异常，可稍后重试"
                } else {
                    "未命中：各源均无候选"
                },
                color = if (item.network) salt.primary else salt.text2,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
        }
    }
}

// ── Result 态 ────────────────────────────────────────────

@Composable
private fun ResultContent(
    state: DesktopScrapeState.Result,
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.results, key = { it.songId }) { result ->
                ScrapeResultRow(
                    result = result.toShared(title = state.titles[result.songId] ?: result.songId),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SaltTextButton(text = "撤销本次写回（恢复库旧值）", onClick = onUndo)
            Spacer(Modifier.weight(1f))
            Button(onClick = onBack) { Text("返回队列") }
        }
    }
}

/** WritebackResult → 共用结果行（状态配色/失败文案对齐安卓 describeWritebackFailure）。 */
private fun WritebackResult.toShared(title: String): SharedWritebackResult = SharedWritebackResult(
    songId = songId,
    title = title,
    statusKind = when (status) {
        WritebackStatus.SUCCESS -> ScrapeStatusKind.SUCCESS
        WritebackStatus.FILE_FAILED -> ScrapeStatusKind.WARNING
        WritebackStatus.FAILED -> ScrapeStatusKind.ERROR
    },
    statusWire = status.wire,
    detail = if (status == WritebackStatus.SUCCESS) {
        null
    } else {
        describeWritebackFailure(
            WritebackFailureInput(
                fileResultCode = fileResult.code,
                fileResultMessage = fileResult.message,
                error = error,
            ),
        )
    },
)

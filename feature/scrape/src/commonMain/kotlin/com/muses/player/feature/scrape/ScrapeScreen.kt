package com.muses.player.feature.scrape

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muses.player.core.model.scrape.WritebackStatus
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.ScrapeReviewFieldRow
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesBottomSheet
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesCheckbox
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesRefreshableContent
import com.muses.player.core.ui.components.MusesRefreshablePlaceholder
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.ScrapeProgressBar
import com.muses.player.core.ui.components.ScrapeResultRow
import com.muses.player.core.ui.components.ScrapeStatusKind
import com.muses.player.core.ui.components.SharedReviewField
import com.muses.player.core.ui.components.SharedScrapeCandidate
import com.muses.player.core.ui.components.SharedWritebackResult
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import com.muses.player.core.ui.components.MarqueeText as Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 批量刮削：待处理、核对候选、应用结果；所有分组在同一个列表中滚动。 */
@Composable
fun ScrapeScreen(
    modifier: Modifier = Modifier,
    viewModel: ScrapeViewModel = koinViewModel(),
    onBack: (() -> Unit)? = null,
    onOpenReview: (String) -> Unit = {},
    onStartReviewQueue: (firstSongId: String, queue: List<String>) -> Unit = { _, _ -> },
) {
    val state by viewModel.pageState.collectAsState()
    val queueIds by viewModel.queueSongIds.collectAsState()
    val titles by viewModel.queueTitles.collectAsState()
    val songs by viewModel.queueSongs.collectAsState()
    val networkFailed by viewModel.throttledIds.collectAsState()
    val message by viewModel.throttleMessage.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val undoing by viewModel.undoing.collectAsState()
    val pendingUndo by viewModel.pendingUndo.collectAsState()
    val rollbackHistory by viewModel.rollbackHistory.collectAsState()
    val historyPreview by viewModel.historyPreview.collectAsState()
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var confirmDiscardUndo by rememberSaveable { mutableStateOf(false) }
    val scheme = MiuixTheme.colorScheme
    Scaffold(
        modifier = modifier.fillMaxSize().background(scheme.surface),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "刮削", onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (error != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error.orEmpty(), style = MiuixTheme.textStyles.footnote1, color = scheme.error, modifier = Modifier.weight(1f))
                    ScrapeTextAction(text = "知道了", onClick = viewModel::dismissError)
                }
            }
            if (state is ScrapePageState.Queue && rollbackHistory.isNotEmpty()) {
                ScrapeTextAction(text = "标签历史 · ${rollbackHistory.size} 个批次", enabled = !undoing,
                    onClick = { showHistory = true }, modifier = Modifier.padding(horizontal = 16.dp))
            }
            val undoInfo = pendingUndo
            if (state is ScrapePageState.Queue && undoInfo != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pendingUndoSummaryText(undoInfo),
                        style = MiuixTheme.textStyles.footnote1,
                        color = scheme.onBackgroundVariant,
                        modifier = Modifier.weight(1f),
                    )
                    ScrapeTextAction(
                        text = if (undoing) "正在恢复…" else "恢复标签",
                        onClick = viewModel::undoPendingJournal,
                    )
                    ScrapeTextAction(
                        text = "忽略",
                        enabled = !undoing,
                        onClick = { confirmDiscardUndo = true },
                    )
                }
            }
            when (val page = state) {
                ScrapePageState.Queue -> MusesRefreshableContent(onRefresh = { viewModel.reloadQueue().join() }) {
                    QueueContent(queueIds, titles, songs, viewModel)
                }
                is ScrapePageState.Matching -> ScrapeProgressBar(
                    current = page.current, total = page.total, currentItem = page.currentItem,
                    title = "已完成 ${page.current} / ${page.total} 首",
                    message = message ?: "停止后保留已完成的匹配，未处理歌曲仍在队列中",
                    onCancel = viewModel::stopMatching, cancelText = "停止匹配",
                    modifier = Modifier.weight(1f),
                )
                is ScrapePageState.Preview -> PreviewContent(
                    page, networkFailed, titles, viewModel, onOpenReview,
                    onStartReview = {
                        viewModel.startReviewQueue()?.let { onStartReviewQueue(it, viewModel.pendingReviewQueue.value) }
                    },
                )
                is ScrapePageState.Writing -> Column(
                    Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(20.dp))
                    Text("正在应用 ${page.count} 首歌曲", style = MiuixTheme.textStyles.main)
                    Spacer(Modifier.height(8.dp))
                    Text("正在更新曲库与音频文件，请稍候", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                    Spacer(Modifier.height(20.dp))
                    ScrapeTextAction(text = "停止写回", onClick = viewModel::cancelWriteback)
                }
                is ScrapePageState.Result -> ResultContent(page, queueIds.size, undoing, viewModel)
            }
        }
    }
    if (showHistory) {
        MusesBottomSheet(title = if (historyPreview == null) "标签历史" else "预览恢复", onDismiss = {
            if (!undoing) { showHistory = false; viewModel.closeHistoryPreview() }
        }) {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val preview = historyPreview
                if (preview == null) {
                    Text("保留最近 20 个批次；选择歌曲预览原来修改的字段后再恢复。", style = MiuixTheme.textStyles.footnote1)
                    rollbackHistory.forEach { journal ->
                        Text(journal.entries.firstOrNull()?.createdAt.orEmpty(), style = MiuixTheme.textStyles.footnote2)
                        journal.entries.forEach { entry ->
                            ScrapeTextAction(text = "${entry.songBefore.title} · ${entry.songBefore.artist.orEmpty()}",
                                onClick = { viewModel.previewHistory(journal.journalId, entry.songId) })
                        }
                    }
                } else {
                    val target = preview.entry.songBefore
                    val current = preview.currentValues
                    val fields = preview.entry.changedFields
                    Text("${preview.entry.createdAt} · ${if (preview.entry.fileSnapshotKnown) "文件标签快照" else "曲库快照（未核对文件标签）"}",
                        style = MiuixTheme.textStyles.footnote2)
                    fun shows(field: String) = fields == null || field in fields
                    if (shows("title")) Text("标题：${current.title} → ${target.title}")
                    if (shows("artist")) Text("歌手：${current.artist.orEmpty()} → ${target.artist.orEmpty()}")
                    if (shows("album")) Text("专辑：${current.album.orEmpty()} → ${target.album.orEmpty()}")
                    if (shows("lyrics")) {
                        Text("歌词：当前 ${current.lyrics.orEmpty().length} 字 → 历史 ${target.lyrics.orEmpty().length} 字")
                        Text(target.lyrics.orEmpty().take(300), style = MiuixTheme.textStyles.footnote2)
                    }
                    if (shows("cover")) {
                        Text(when {
                            !preview.entry.coverWritten -> "封面：仅恢复曲库展示地址"
                            preview.entry.coverSnapshot == null -> "封面：没有可靠的主封面快照，无法恢复本次全部字段"
                            preview.entry.coverSnapshot == "" -> "封面：恢复为无内嵌封面"
                            else -> "封面：恢复保存的主封面字节（不含其他图片帧）"
                        }, style = MiuixTheme.textStyles.footnote1)
                        preview.coverPreviewUri?.let { MusesCover(uri = it, size = 160.dp) }
                    }
                    if (preview.revision == null) Text("当前文件无法核对版本，暂不支持从历史恢复。", style = MiuixTheme.textStyles.footnote2)
                    Text("恢复会写入文件；恢复前的实际标签和主封面将保存为新版本。", style = MiuixTheme.textStyles.footnote2)
                    val canRestore = preview.revision != null && (!shows("cover") || !preview.entry.coverWritten || preview.entry.coverSnapshot != null)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ScrapeTextAction(text = "返回历史", enabled = !undoing, onClick = viewModel::closeHistoryPreview)
                        ScrapeTextAction(text = if (undoing) "正在恢复…" else "确认恢复", enabled = !undoing && canRestore,
                            onClick = viewModel::restoreHistory)
                    }
                }
            }
        }
    }
    if (confirmDiscardUndo) {
        MusesDialog(
            onDismiss = { confirmDiscardUndo = false },
            title = "忽略撤销记录",
            message = "忽略后不再显示当前快捷恢复入口，已经归档的历史版本不受影响。确定忽略吗？",
            confirmText = "忽略",
            onConfirm = {
                confirmDiscardUndo = false
                viewModel.discardPendingUndo()
            },
            dismissText = "取消",
        )
    }
}

@Composable
private fun QueueContent(ids: List<String>, titles: Map<String, String>, songs: Map<String, Song>, viewModel: ScrapeViewModel) {
    if (ids.isEmpty()) {
        // 空队列用全应用统一的空状态占位，不再单独写引导文案
        MusesRefreshablePlaceholder {
        MusesEmpty(
            modifier = Modifier.fillMaxSize(), bottomInset = LocalBottomChromePadding.current,
        )
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                ScrapeSummary(
                    "待匹配 · ${ids.size} 首",
                    "匹配后核对，应用时才保存",
                )
            }
            items(ids, key = { it }) { id ->
                val song = songs[id]
                MusesListRow(
                    title = titles[id] ?: "待刮削歌曲",
                    subtitle = listOfNotNull(song?.artist, song?.album).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
                    songLayout = true,
                    leading = { MusesCover(uri = song?.coverUri, size = 44.dp, modifier = Modifier.padding(end = 12.dp)) },
                    after = {
                        MusesIconButton(onClick = { viewModel.removeFromQueue(listOf(id)) }, imageVector = TablerIcons.Close, contentDescription = "移出队列")
                    },
                )
            }
        }
        ScrapeActions {
            ScrapeTextAction(text = "清空队列", onClick = viewModel::clearQueue, modifier = Modifier.weight(1f))
            MusesButton(onClick = viewModel::startMatching, modifier = Modifier.weight(2f)) { Text("开始匹配（${ids.size} 首）") }
        }
    }
}

@Composable
private fun PreviewContent(
    state: ScrapePageState.Preview,
    networkFailed: List<String>,
    titles: Map<String, String>,
    viewModel: ScrapeViewModel,
    onOpenReview: (String) -> Unit,
    onStartReview: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    var showSelection by rememberSaveable { mutableStateOf(false) }
    val selectedSongs = state.items.count { it.checkedFields.isNotEmpty() }
    val selectedFields = state.items.sumOf { it.checkedFields.size }
    val pendingCount = (state.items.map { it.songId } + state.noMatchIds + networkFailed).distinct().size
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScrapeSummary(
                    "匹配结果",
                    "候选 ${state.items.size} · 未匹配 ${state.noMatchIds.size} · 失败 ${networkFailed.size}",
                )
            }
            if (pendingCount > 0) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScrapeTextAction(
                            text = "批量选择", enabled = state.items.any { it.availableFields().isNotEmpty() },
                            onClick = { showSelection = true }, modifier = Modifier.weight(1f),
                        )
                        ScrapeTextAction(text = "逐首核对（$pendingCount）", onClick = onStartReview, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (pendingCount == 0) {
                item { ScrapeSummary("暂无可核对结果", "返回队列可继续匹配尚未处理的歌曲。") }
            }
            items(state.items, key = { "candidate-${it.songId}" }) { candidate ->
                CompactCandidate(
                    candidate = SharedScrapeCandidate(
                        songId = candidate.songId, title = candidate.songTitle,
                        subtitle = listOfNotNull(candidate.currentArtist, candidate.currentAlbum).filter { it.isNotBlank() }.joinToString(" · "),
                        coverUri = candidate.coverUrl ?: candidate.currentCoverUri,
                        confidenceLabel = when (candidate.confidence) {
                            "HIGH" -> "匹配度高"
                            "MEDIUM" -> "建议核对"
                            "LOW" -> "请仔细核对"
                            else -> null
                        },
                    ),
                    fields = candidate.reviewFields(),
                    requestError = candidate.failedRequests.takeIf { it.isNotEmpty() }?.joinToString("、") { if (it == "cover") "封面" else "歌曲信息" }?.let { "${it}请求失败，可进入核对候选重新搜索" },
                    onToggleField = { viewModel.toggleField(candidate.songId, it) },
                    confirmText = "核对候选", onConfirm = { onOpenReview(candidate.songId) },
                    skipText = if (candidate.checkedFields.isEmpty()) "选择此首" else "取消此首",
                    onSkip = { viewModel.toggleChecked(candidate.songId) },
                )
            }
            if (state.noMatchIds.isNotEmpty()) {
                item { ScrapeSummary("未找到匹配 · ${state.noMatchIds.size} 首", "修改搜索词或重新匹配") }
                items(state.noMatchIds, key = { "unmatched-$it" }) { id ->
                    RetrySongRow(id, titles, onOpenReview, viewModel::retrySingle)
                }
            }
            if (networkFailed.isNotEmpty()) {
                item {
                    ScrapeSummary("请求失败（${networkFailed.size} 首）", "网络异常或服务繁忙，请稍后重试。")
                    ScrapeTextAction(text = "重试这些歌曲", onClick = viewModel::retryThrottled, modifier = Modifier.fillMaxWidth())
                }
                items(networkFailed, key = { "network-$it" }) { id ->
                    RetrySongRow(id, titles, onOpenReview, viewModel::retrySingle)
                }
            }
        }
        Text(
            if (selectedSongs == 0) "尚未选择变更" else "已选 $selectedSongs 首 · $selectedFields 项变更",
            style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        ScrapeActions {
            ScrapeTextAction(text = "返回队列", onClick = viewModel::backToQueue, modifier = Modifier.weight(1f))
            MusesButton(
                onClick = viewModel::confirmWriteback, enabled = selectedSongs > 0, modifier = Modifier.weight(2f),
            ) { Text(if (selectedSongs > 0) "应用到 $selectedSongs 首" else "应用变更") }
        }
    }
    if (showSelection) {
        MusesBottomSheet(onDismiss = { showSelection = false }, title = "批量选择变更") {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("只选择有新候选的字段；未勾选的内容保持原样。", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScrapeTextAction(text = "全部变更", onClick = { viewModel.setAllChecked(true) }, modifier = Modifier.weight(1f))
                    ScrapeTextAction(text = "仅选封面", onClick = { viewModel.selectFields(setOf("cover")) }, modifier = Modifier.weight(1f))
                    ScrapeTextAction(text = "全部取消", onClick = { viewModel.setAllChecked(false) }, modifier = Modifier.weight(1f))
                }
                scrapeFieldLabels.forEach { (field, label) ->
                    val available = state.items.filter { field in it.availableFields() }
                    val allSelected = available.isNotEmpty() && available.all { field in it.checkedFields }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$label（${available.size} 首可更新）", modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.body1)
                        MusesCheckbox(
                            checked = allSelected, enabled = available.isNotEmpty(),
                            onToggle = { viewModel.setAllFields(field, !allSelected) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                MusesButton(onClick = { showSelection = false }, modifier = Modifier.fillMaxWidth()) { Text("完成选择") }
            }
        }
    }
}

private val scrapeFieldLabels = listOf("title" to "标题", "artist" to "歌手", "album" to "专辑", "cover" to "封面", "lyrics" to "歌词")

private fun PreviewCandidate.reviewFields(): List<SharedReviewField> {
    val available = availableFields()
    return listOf(
        SharedReviewField("title", "标题", currentTitle, resolvedTitle().takeIf { "title" in available }, "title" in checkedFields),
        SharedReviewField("artist", "歌手", currentArtist ?: "无", resolvedArtist().takeIf { "artist" in available }, "artist" in checkedFields),
        SharedReviewField("album", "专辑", currentAlbum ?: "无", resolvedAlbum().takeIf { "album" in available }, "album" in checkedFields),
        SharedReviewField("cover", "封面", if (currentCoverUri.isNullOrBlank()) "无" else "已有封面", "使用匹配封面".takeIf { "cover" in available }, "cover" in checkedFields),
        SharedReviewField("lyrics", "歌词", if (currentLyrics.isNullOrBlank()) "无" else "已有歌词", "使用匹配歌词".takeIf { "lyrics" in available }, "lyrics" in checkedFields),
    )
}

@Composable
private fun RetrySongRow(id: String, titles: Map<String, String>, onReview: (String) -> Unit, onRetry: (String) -> Unit) {
    MusesListRow(
        title = titles[id] ?: "待刮削歌曲", songLayout = true,
        onClick = { onReview(id) },
        after = {
            MusesIconButton(onClick = { onReview(id) }, imageVector = TablerIcons.Search, contentDescription = "修改搜索词")
            MusesIconButton(onClick = { onRetry(id) }, imageVector = TablerIcons.Refresh, contentDescription = "重新匹配")
        },
    )
}

@Composable
private fun ScrapeSummary(title: String, description: String) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(title, style = MiuixTheme.textStyles.body2, fontWeight = FontWeight.Medium)
        Text(
            description, style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ScrapeActions(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp + LocalBottomChromePadding.current),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun ResultContent(state: ScrapePageState.Result, remaining: Int, undoing: Boolean, viewModel: ScrapeViewModel) {
    val summaryLines = buildScrapeResultSummary(state.results)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScrapeSummary(
                    "本次更新完成",
                    summaryLines.joinToString("\n"),
                )
            }
            items(state.results, key = { it.songId }) { result ->
                Column(Modifier.fillMaxWidth()) {
                    ScrapeResultRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        result = SharedWritebackResult(
                            songId = result.songId, title = state.titles[result.songId] ?: "已处理歌曲",
                            statusKind = when (result.status) {
                                WritebackStatus.SUCCESS -> ScrapeStatusKind.SUCCESS
                                WritebackStatus.FILE_FAILED -> ScrapeStatusKind.WARNING
                                WritebackStatus.FAILED -> ScrapeStatusKind.ERROR
                            },
                            statusWire = when (result.status) {
                                WritebackStatus.SUCCESS -> "已更新"
                                WritebackStatus.FILE_FAILED -> if (result.fileResult.code == "pending_upload") "本地已保存，待上传" else "文件未更新"
                                WritebackStatus.FAILED -> "更新失败"
                            },
                            detail = writebackRowDetail(result),
                            retryText = if (result.status == WritebackStatus.SUCCESS || undoing) null else "核对重试",
                        ),
                        onRetry = viewModel::reviewWritebackFailure,
                    )
                }
            }
            item {
                Text(
                    "恢复会重新写入音频标签，保存成功后同步更新曲库；WebDAV 文件需要重新上传。",
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
        ScrapeActions {
            ScrapeTextAction(
                text = if (undoing) "正在恢复…" else "恢复标签",
                enabled = !undoing && state.journalId.isNotBlank(),
                onClick = viewModel::undoLastWriteback, modifier = Modifier.weight(1f),
            )
            MusesButton(onClick = viewModel::backToQueue, enabled = !undoing, modifier = Modifier.weight(2f)) {
                Text(if (remaining > 0) "继续处理（$remaining 首）" else "返回队列")
            }
        }
    }
}

/** 首屏展示歌曲和变更数量，展开后才显示逐字段对比。 */
@Composable
private fun CompactCandidate(
    candidate: SharedScrapeCandidate,
    fields: List<SharedReviewField>,
    requestError: String?,
    onToggleField: (String) -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    skipText: String,
    onSkip: () -> Unit,
) {
    var expanded by rememberSaveable(candidate.songId) { mutableStateOf(false) }
    Column {
        MusesListRow(
            title = candidate.title,
            subtitle = listOfNotNull(candidate.subtitle?.takeIf { it.isNotBlank() }, candidate.confidenceLabel).joinToString(" · ").ifBlank { null },
            songLayout = true,
            leading = { MusesCover(uri = candidate.coverUri, size = 44.dp, modifier = Modifier.padding(end = 12.dp)) },
            onClick = { expanded = !expanded },
            after = {
                MusesCheckbox(checked = fields.any { it.checked }, onToggle = onSkip, enabled = fields.any { it.updated != null && it.updated != it.original })
                MusesIconButton(onClick = { expanded = !expanded }, imageVector = if (expanded) TablerIcons.ChevronDown else TablerIcons.ChevronRight, contentDescription = if (expanded) "收起变更" else "查看变更")
            },
        )
        val availableCount = fields.count { it.updated != null && it.updated != it.original }
        Text(
            if (availableCount == 0) "信息一致，无需更新" else "${availableCount} 项可更新 · 已选 ${fields.count { it.checked }} 项",
            modifier = Modifier.padding(start = 68.dp, end = 12.dp, bottom = 4.dp),
            style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        requestError?.let {
            Text(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.error)
        }
        if (expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                fields.forEach { field ->
                    ScrapeReviewFieldRow(field = field, enabled = !field.updated.isNullOrBlank() && field.updated != field.original, onCheckedChange = { onToggleField(field.key) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ScrapeTextAction(text = skipText, onClick = onSkip)
                    ScrapeTextAction(text = confirmText, onClick = onConfirm)
                }
            }
        }
    }
}

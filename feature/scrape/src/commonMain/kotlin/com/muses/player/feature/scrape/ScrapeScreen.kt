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
import com.muses.player.core.ui.components.MusesBottomSheet
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesCheckbox
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.ScrapeProgressBar
import com.muses.player.core.ui.components.ScrapeReviewCard
import com.muses.player.core.ui.components.ScrapeResultRow
import com.muses.player.core.ui.components.ScrapeStatusKind
import com.muses.player.core.ui.components.SharedReviewField
import com.muses.player.core.ui.components.SharedScrapeCandidate
import com.muses.player.core.ui.components.SharedWritebackResult
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
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
    val networkFailed by viewModel.throttledIds.collectAsState()
    val message by viewModel.throttleMessage.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val undoing by viewModel.undoing.collectAsState()
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
                    MusesTextButton(text = "知道了", onClick = viewModel::dismissError)
                }
            }
            when (val page = state) {
                ScrapePageState.Queue -> QueueContent(queueIds, titles, viewModel)
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
                }
                is ScrapePageState.Result -> ResultContent(page, queueIds.size, undoing, viewModel)
            }
        }
    }
}

@Composable
private fun QueueContent(ids: List<String>, titles: Map<String, String>, viewModel: ScrapeViewModel) {
    if (ids.isEmpty()) {
        // 空队列用全应用统一的空状态占位，不再单独写引导文案
        MusesEmpty(
            modifier = Modifier.fillMaxSize(), bottomInset = LocalBottomChromePadding.current,
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ScrapeSummary(
                    "待处理 ${ids.size} 首",
                    "先匹配歌曲信息与封面，再核对要应用的变更。匹配完成后不会自动写入。",
                )
            }
            items(ids, key = { it }) { id ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            titles[id] ?: "待刮削歌曲", style = MiuixTheme.textStyles.body1,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        MusesTextButton(text = "移出队列", onClick = { viewModel.removeFromQueue(listOf(id)) })
                    }
                }
            }
        }
        ScrapeActions {
            MusesTextButton(text = "清空队列", onClick = viewModel::clearQueue, modifier = Modifier.weight(1f))
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
            Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScrapeSummary(
                    "核对匹配结果",
                    "找到 ${state.items.size} 首候选 · 未匹配 ${state.noMatchIds.size} 首 · 请求失败 ${networkFailed.size} 首\n只会应用勾选的变更，可逐首核对或批量选择。",
                )
            }
            if (pendingCount > 0) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MusesTextButton(
                            text = "批量选择", enabled = state.items.any { it.availableFields().isNotEmpty() },
                            onClick = { showSelection = true }, modifier = Modifier.weight(1f),
                        )
                        MusesTextButton(text = "逐首核对（$pendingCount）", onClick = onStartReview, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (pendingCount == 0) {
                item { ScrapeSummary("暂无可核对结果", "返回队列可继续匹配尚未处理的歌曲。") }
            }
            items(state.items, key = { "candidate-${it.songId}" }) { candidate ->
                ScrapeReviewCard(
                    candidate = SharedScrapeCandidate(
                        songId = candidate.songId, title = candidate.songTitle,
                        subtitle = if (candidate.failedRequests.isNotEmpty()) {
                            candidate.failedRequests.joinToString("、") { if (it == "cover") "封面" else "歌曲信息" } + "请求失败，可在下方重试"
                        } else listOfNotNull(candidate.currentArtist, candidate.currentAlbum).filter { it.isNotBlank() }.joinToString(" · "),
                        coverUri = candidate.coverUrl ?: candidate.currentCoverUri,
                        confidenceLabel = when (candidate.confidence) {
                            "HIGH" -> "匹配度高"
                            "MEDIUM" -> "建议核对"
                            "LOW" -> "请仔细核对"
                            else -> null
                        },
                    ),
                    fields = candidate.reviewFields(),
                    onToggleField = { viewModel.toggleField(candidate.songId, it) },
                    confirmText = "核对候选", onConfirm = { onOpenReview(candidate.songId) },
                    skipText = if (candidate.checkedFields.isEmpty()) "选择此首" else "取消此首",
                    onSkip = { viewModel.toggleChecked(candidate.songId) },
                )
            }
            if (state.noMatchIds.isNotEmpty()) {
                item { ScrapeSummary("未找到匹配（${state.noMatchIds.size} 首）", "可以修改搜索词再查找，或重新匹配。") }
                items(state.noMatchIds, key = { "unmatched-$it" }) { id ->
                    RetrySongRow(id, titles, onOpenReview, viewModel::retrySingle)
                }
            }
            if (networkFailed.isNotEmpty()) {
                item {
                    ScrapeSummary("请求失败（${networkFailed.size} 首）", "网络异常或服务繁忙，请稍后重试。")
                    MusesTextButton(text = "重试这些歌曲", onClick = viewModel::retryThrottled, modifier = Modifier.fillMaxWidth())
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
            MusesTextButton(text = "返回队列", onClick = viewModel::backToQueue, modifier = Modifier.weight(1f))
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
                    MusesTextButton(text = "全部变更", onClick = { viewModel.setAllChecked(true) }, modifier = Modifier.weight(1f))
                    MusesTextButton(text = "仅选封面", onClick = { viewModel.selectFields(setOf("cover")) }, modifier = Modifier.weight(1f))
                    MusesTextButton(text = "全部取消", onClick = { viewModel.setAllChecked(false) }, modifier = Modifier.weight(1f))
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(titles[id] ?: "待刮削歌曲", style = MiuixTheme.textStyles.body1, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MusesTextButton(text = "修改搜索词", onClick = { onReview(id) })
                MusesTextButton(text = "重新匹配", onClick = { onRetry(id) })
            }
        }
    }
}

@Composable
private fun ScrapeSummary(title: String, description: String) {
    Column {
        Text(title, style = MiuixTheme.textStyles.main, fontWeight = FontWeight.SemiBold)
        Text(
            description, style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant, modifier = Modifier.padding(top = 6.dp),
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
    val success = state.results.count { it.status == WritebackStatus.SUCCESS }
    val partial = state.results.count { it.status == WritebackStatus.FILE_FAILED }
    val failed = state.results.count { it.status == WritebackStatus.FAILED }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScrapeSummary(
                    "本次更新完成",
                    "成功 $success 首 · 仅曲库更新 $partial 首 · 失败 $failed 首\n失败歌曲仍保留在队列中，可核对本次变更后重试。",
                )
            }
            items(state.results, key = { it.songId }) { result ->
                Card(Modifier.fillMaxWidth()) {
                    ScrapeResultRow(
                        modifier = Modifier.padding(16.dp),
                        result = SharedWritebackResult(
                            songId = result.songId, title = state.titles[result.songId] ?: "已处理歌曲",
                            statusKind = when (result.status) {
                                WritebackStatus.SUCCESS -> ScrapeStatusKind.SUCCESS
                                WritebackStatus.FILE_FAILED -> ScrapeStatusKind.WARNING
                                WritebackStatus.FAILED -> ScrapeStatusKind.ERROR
                            },
                            statusWire = when (result.status) {
                                WritebackStatus.SUCCESS -> "已更新"
                                WritebackStatus.FILE_FAILED -> "文件未更新"
                                WritebackStatus.FAILED -> "更新失败"
                            },
                            detail = if (result.status == WritebackStatus.SUCCESS) null else
                                result.fileResult.message ?: result.error ?: "请检查文件写入权限或网络连接",
                            retryText = if (result.status == WritebackStatus.SUCCESS || undoing) null else "核对重试",
                        ),
                        onRetry = viewModel::reviewWritebackFailure,
                    )
                }
            }
            item {
                Text(
                    "恢复仅还原本次修改前的曲库信息，不会还原已写入的音频文件。",
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
        ScrapeActions {
            MusesTextButton(
                text = if (undoing) "正在恢复…" else "恢复曲库",
                enabled = !undoing, onClick = viewModel::undoLastWriteback, modifier = Modifier.weight(1f),
            )
            MusesButton(onClick = viewModel::backToQueue, enabled = !undoing, modifier = Modifier.weight(2f)) {
                Text(if (remaining > 0) "继续处理（$remaining 首）" else "返回队列")
            }
        }
    }
}

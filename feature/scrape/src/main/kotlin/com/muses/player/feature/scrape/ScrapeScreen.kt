package com.muses.player.feature.scrape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import coil3.compose.AsyncImage
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors
import kotlinx.coroutines.launch

/**
 * 刮削页 —— 对照 src/views/ScrapePage.vue 手机形态。
 * pageState 四态机：queue（待刮削队列）→ matching（匹配中）→ preview（候选确认）→ result（结果+撤销）。
 */
@Composable
fun ScrapeScreen(
    modifier: Modifier = Modifier,
    viewModel: ScrapeViewModel = koinViewModel(),
    /** S2：打开审核页（单曲改词重搜；由 app 宿主接导航到 ScrapeReview 路由） */
    onOpenReview: (String) -> Unit = {},
    /**
     * S3：开始逐首审核（首 songId + 队列上下文；由 app 宿主打开带 queue 参数的审核页）。
     * 队列本身在 ScrapeViewModel.pendingReviewQueue，宿主通过它构造路由。
     */
    onStartReviewQueue: (firstSongId: String, queue: List<String>) -> Unit = { _, _ -> },
) {
    val salt = LocalSaltColors.current
    val pageState by viewModel.pageState.collectAsState()
    val queueSongIds by viewModel.queueSongIds.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        SaltNavbar(
            title = "刮削",
        )

        // 限流可观察（任务 08-27-scrape-throttle-429）
        val throttleMessage by viewModel.throttleMessage.collectAsState()
        val throttledIds by viewModel.throttledIds.collectAsState()

        when (val state = pageState) {
            is ScrapePageState.Queue -> QueueStateContent(
                queueSongIds = queueSongIds,
                queueTitles = viewModel.queueTitles.collectAsState().value,
                onRemove = { viewModel.removeFromQueue(listOf(it)) },
                onClear = { viewModel.clearQueue() },
                onStartAll = { viewModel.startMatching() },
            )

            is ScrapePageState.Matching -> MatchingStateContent(state, throttleMessage)

            is ScrapePageState.Preview -> PreviewStateContent(
                state = state,
                throttleMessage = throttleMessage,
                throttledIds = throttledIds,
                queueTitles = viewModel.queueTitles.collectAsState().value,
                onToggle = viewModel::toggleChecked,
                onSetAll = viewModel::setAllChecked,
                onToggleField = viewModel::toggleField,
                onSetAllFields = viewModel::setAllFields,
                onConfirm = viewModel::confirmWriteback,
                onCancel = viewModel::backToQueue,
                onRetrySingle = viewModel::retrySingle,
                onRetryThrottled = viewModel::retryThrottled,
                onEdit = viewModel::updatePreviewItem,
                onOpenReview = onOpenReview,
                // 队列构造统一走 VM（startReviewQueue 置待审队列并返回首 songId），路由 queue 参数取 VM 待审队列
                onStartReviewQueue = {
                    val first = viewModel.startReviewQueue()
                    if (first != null) onStartReviewQueue(first, viewModel.pendingReviewQueue.value)
                },
            )

            is ScrapePageState.Writing -> WritingStateContent(state)

            is ScrapePageState.Result -> ResultStateContent(
                state = state,
                throttleMessage = throttleMessage,
                throttledIds = throttledIds,
                queueTitles = viewModel.queueTitles.collectAsState().value,
                onUndo = viewModel::undoLastWriteback,
                onBack = viewModel::backToQueue,
                onRetrySingle = viewModel::retrySingle,
            )
        }
    }
}

// ── queue 态 ──────────────────────────────────────────

@Composable
private fun QueueStateContent(
    queueSongIds: List<String>,
    queueTitles: Map<String, String>,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onStartAll: () -> Unit,
) {
    val salt = LocalSaltColors.current
    if (queueSongIds.isEmpty()) {
        SaltEmpty(
            title = "待刮削队列为空",
            description = "请先在歌曲页标记需要刮削的歌曲。",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(queueSongIds, key = { it }) { songId ->
                // 队列只持久化 songId，歌名展示时反查库（对齐 Web 版队列行 title）
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = queueTitles[songId] ?: "待刮削歌曲",
                        fontSize = 16.sp,
                        color = salt.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    SaltTextButton(text = "移除", onClick = { onRemove(songId) })
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onClear, modifier = Modifier.weight(1f)) { Text("清空") }
            Button(onClick = onStartAll, modifier = Modifier.weight(2f)) { Text("全部开始") }
        }
    }
}

// ── writing 态（写回中）──────────────────────────────────

@Composable
private fun WritingStateContent(state: ScrapePageState.Writing) {
    val salt = LocalSaltColors.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text("正在写回 ${state.count} 首…", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = salt.text)
        Spacer(Modifier.height(8.dp))
        Text("正在写入文件与数据库，请稍候", fontSize = 13.sp, color = salt.text2)
    }
}

// ── matching 态 ──────────────────────────────────────

@Composable
private fun MatchingStateContent(state: ScrapePageState.Matching, throttleMessage: String? = null) {
    val salt = LocalSaltColors.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text("正在匹配 ${state.current} / ${state.total}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = salt.text)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.currentItem,
            fontSize = 13.sp,
            color = salt.text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (throttleMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                throttleMessage,
                fontSize = 13.sp,
                color = salt.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── preview 态 ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewStateContent(
    state: ScrapePageState.Preview,
    throttleMessage: String? = null,
    throttledIds: List<String> = emptyList(),
    queueTitles: Map<String, String> = emptyMap(),
    onToggle: (String) -> Unit,
    onSetAll: (Boolean) -> Unit,
    onToggleField: (String, String) -> Unit,
    onSetAllFields: (String, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetrySingle: (String) -> Unit = {},
    onRetryThrottled: () -> Unit = {},
    onEdit: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    /** S2：打开审核页（未命中改词重搜） */
    onOpenReview: (String) -> Unit = {},
    /** S3：开始逐首审核（按钮只发信号；首 songId + 队列由调用方从 VM 取） */
    onStartReviewQueue: () -> Unit = {},
) {
    val salt = LocalSaltColors.current
    Column(Modifier.fillMaxSize()) {
        // 命中分维度统计
        val textHits = remember(state.items) { state.items.count { it.matchedTitle != null || it.matchedArtist != null || it.matchedAlbum != null } }
        val coverHits = remember(state.items) { state.items.count { it.coverUrl != null } }
        val totalCheckedFields = remember(state.items) { state.items.sumOf { it.checkedFields.size } }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildString {
                    append("文本命中 $textHits · 封面命中 $coverHits · 共 ${state.items.size} 首")
                },
                fontSize = 13.sp,
                color = salt.text2,
                modifier = Modifier.weight(1f),
            )
        }
        // 逐字段批量全选/全不选 + 逐首审核入口（S3）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("批量字段：", fontSize = 11.sp, color = salt.text2)
            listOf("title" to "标题", "artist" to "歌手", "album" to "专辑", "cover" to "封面", "lyrics" to "歌词").forEach { (field, label) ->
                SaltTextButton(text = label, onClick = {
                    val allHave = state.items.all { field in it.checkedFields }
                    onSetAllFields(field, !allHave)
                })
            }
        }
        // S3 逐首审核入口（Tagger「连续审核」）：有命中才展示
        if (state.items.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("不想一次性全勾？", fontSize = 11.sp, color = salt.text2)
                Spacer(Modifier.weight(1f))
                SaltTextButton(text = "逐首审核（${state.items.size}）", onClick = onStartReviewQueue)
            }
        }
        if (throttleMessage != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    throttleMessage,
                    fontSize = 12.sp,
                    color = salt.primary,
                    modifier = Modifier.weight(1f),
                )
                if (throttledIds.isNotEmpty()) {
                    SaltTextButton(text = "重试限流", onClick = onRetryThrottled)
                }
            }
        }
        if (throttledIds.isNotEmpty() && state.items.isEmpty()) {
            // 空命中但有被限流的歌曲：给出单首重试入口
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("${throttledIds.size} 首触发限流，稍后重试", fontSize = 13.sp, color = salt.text2)
                Spacer(Modifier.height(8.dp))
                throttledIds.take(5).forEach { sid ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(queueTitles[sid] ?: sid.take(8), fontSize = 12.sp, color = salt.text2, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SaltTextButton(text = "重试", onClick = { onRetrySingle(sid) })
                    }
                }
            }
        }
        // S2：未命中分组（普通 NO_MATCH，不再静默消失；可单独重试或去审核改词重搜）
        if (state.noMatchIds.isNotEmpty()) {
            NoMatchGroup(
                noMatchIds = state.noMatchIds,
                queueTitles = queueTitles,
                onRetry = onRetrySingle,
                onOpenReview = onOpenReview,
            )
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.songId }) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(salt.surface1, RoundedCornerShape(10.dp))
                        .border(0.5.dp, if (item.checkedFields.isNotEmpty()) salt.primary.copy(alpha = 0.5f) else salt.surface2, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    // 歌曲标题 + 置信度
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.songTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = salt.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (item.confidence != null) {
                            Box(Modifier.padding(start = 4.dp).background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(item.confidence!!, fontSize = 10.sp, color = salt.primary)
                            }
                        }
                        if (item.coverUrl != null) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = "封面",
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(salt.surface2).padding(start = 6.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // 逐字段 Checkbox 行
                    PreviewFieldRow(label = "标题", checked = "title" in item.checkedFields, original = item.currentTitle, updated = item.resolvedTitle(), onCheckedChange = { onToggleField(item.songId, "title") })
                    PreviewFieldRow(label = "歌手", checked = "artist" in item.checkedFields, original = item.currentArtist ?: "—", updated = item.resolvedArtist(), onCheckedChange = { onToggleField(item.songId, "artist") })
                    PreviewFieldRow(label = "专辑", checked = "album" in item.checkedFields, original = item.currentAlbum ?: "—", updated = item.resolvedAlbum(), onCheckedChange = { onToggleField(item.songId, "album") })
                    PreviewFieldRow(label = "封面", checked = "cover" in item.checkedFields, original = "—", updated = if (item.coverUrl != null) "有新封面" else null, onCheckedChange = { onToggleField(item.songId, "cover") })
                    PreviewFieldRow(label = "歌词", checked = "lyrics" in item.checkedFields, original = if (!item.currentLyrics.isNullOrBlank()) "有（${item.currentLyrics!!.length}字）" else "无", updated = if (!item.resolvedLyrics().isNullOrBlank()) "有（${item.resolvedLyrics()!!.length}字）" else null, onCheckedChange = { onToggleField(item.songId, "lyrics") })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                enabled = totalCheckedFields > 0,
            ) { Text("应用" + if (totalCheckedFields > 0) "（$totalCheckedFields）" else "") }
        }
    }
}

/** S2：未命中折叠分组（普通 NO_MATCH 行内重试 + 去审核改词重搜） */
@Composable
private fun NoMatchGroup(
    noMatchIds: List<String>,
    queueTitles: Map<String, String>,
    onRetry: (String) -> Unit,
    onOpenReview: (String) -> Unit,
) {
    val salt = LocalSaltColors.current
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SaltTextButton(
                text = if (expanded) "未命中（${noMatchIds.size} 首）收起" else "未命中（${noMatchIds.size} 首）展开",
                onClick = { expanded = !expanded },
            )
            Spacer(Modifier.weight(1f))
            Text("暂无匹配，可重试或改词重搜", fontSize = 11.sp, color = salt.text2)
        }
        if (expanded) {
            noMatchIds.forEach { sid ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        queueTitles[sid] ?: sid.take(8),
                        fontSize = 12.sp,
                        color = salt.text2,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SaltTextButton(text = "重试", onClick = { onRetry(sid) })
                    SaltTextButton(text = "去审核", onClick = { onOpenReview(sid) })
                }
            }
        }
    }
}

@Composable
private fun PreviewFieldRow(
    label: String,
    checked: Boolean,
    original: String,
    updated: String?,
    onCheckedChange: (Boolean) -> Unit,
) {
    val salt = LocalSaltColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = salt.primary),
        )
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            val display = if (updated != null && updated != original) "$original → $updated" else original
            Text(
                "$label：$display",
                fontSize = 12.sp,
                color = if (checked) salt.text else salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewEditSheet(
    candidate: PreviewCandidate,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, String?, String?) -> Unit,
) {
    val salt = LocalSaltColors.current
    var title by remember(candidate.songId) { mutableStateOf(candidate.resolvedTitle() ?: candidate.currentTitle) }
    var artist by remember(candidate.songId) { mutableStateOf(candidate.resolvedArtist() ?: candidate.currentArtist.orEmpty()) }
    var album by remember(candidate.songId) { mutableStateOf(candidate.resolvedAlbum() ?: candidate.currentAlbum.orEmpty()) }
    var lyrics by remember(candidate.songId) { mutableStateOf(candidate.resolvedLyrics() ?: candidate.currentLyrics.orEmpty()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = salt.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("编辑刮削结果", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = salt.text)
            Spacer(Modifier.height(4.dp))
            Text("仅影响本次写回，未勾选行不落库", fontSize = 12.sp, color = salt.text2)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("歌手") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("专辑") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = lyrics,
                onValueChange = { lyrics = it },
                label = { Text("歌词（可选，粘贴 LRC/TTML 原文）") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 5,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                Button(
                    onClick = {
                        // 输入与匹配值相同视为未编辑（传 null 回退），空视为不改该字段
                        val outTitle = title.trim().takeIf { it.isNotEmpty() && it != candidate.matchedTitle }
                        val outArtist = artist.trim().takeIf { it.isNotEmpty() && it != candidate.matchedArtist }
                        val outAlbum = album.trim().takeIf { it.isNotEmpty() && it != candidate.matchedAlbum }
                        val outLyrics = lyrics.trim().takeIf { it.isNotEmpty() && it != candidate.matchedLyrics } ?: lyrics.trim().takeIf { it.isNotEmpty() && it != candidate.currentLyrics }
                        scope.launch { sheetState.hide() }
                        onConfirm(outTitle, outArtist, outAlbum, outLyrics)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("确认") }
            }
        }
    }
}

// ── result 态 ──────────────────────────────────────

/** 判断写回结果是否疑似限流（429）。 */
private fun isWritebackThrottled(r: com.muses.player.core.model.scrape.WritebackResult): Boolean {
    val msg = (r.fileResult.message ?: "") + (r.error ?: "")
    return msg.contains("429") || r.fileResult.code?.contains("429") == true
}

@Composable
private fun ResultStateContent(
    state: ScrapePageState.Result,
    throttleMessage: String? = null,
    throttledIds: List<String> = emptyList(),
    queueTitles: Map<String, String> = emptyMap(),
    onUndo: () -> Unit,
    onBack: () -> Unit,
    onRetrySingle: (String) -> Unit = {},
) {
    val salt = LocalSaltColors.current
    val success = state.results.count { it.status == com.muses.player.core.model.scrape.WritebackStatus.SUCCESS }
    val fileFailed = state.results.count { it.status == com.muses.player.core.model.scrape.WritebackStatus.FILE_FAILED }
    val failed = state.results.count { it.status == com.muses.player.core.model.scrape.WritebackStatus.FAILED }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "成功 $success · 文件失败 $fileFailed · 失败 $failed",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = salt.text,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "撤销仅恢复曲库，音频文件已写入不可逆",
            fontSize = 11.sp,
            color = salt.text2,
        )
        Spacer(Modifier.height(12.dp))
        if (throttleMessage != null) {
            Text(throttleMessage, fontSize = 12.sp, color = salt.primary)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.results, key = { it.songId }) { r ->
                val throttled = isWritebackThrottled(r)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(statusColor(r.status), RoundedCornerShape(4.dp)))
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(queueTitles[r.songId] ?: r.songId.take(8), fontSize = 13.sp, color = salt.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (throttled) {
                            Text("限流，稍后重试", fontSize = 11.sp, color = salt.primary)
                        } else if (r.status != com.muses.player.core.model.scrape.WritebackStatus.SUCCESS) {
                            val detail = listOfNotNull(r.fileResult.code, r.fileResult.message ?: r.error).joinToString(": ").takeIf { it.isNotBlank() }
                            if (detail != null) Text(detail, fontSize = 11.sp, color = salt.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(r.status.wire, fontSize = 13.sp, color = statusColor(r.status))
                    if (r.status != com.muses.player.core.model.scrape.WritebackStatus.SUCCESS) {
                        Spacer(Modifier.size(8.dp))
                        SaltTextButton(text = if (throttled) "限流重试" else "重试", onClick = { onRetrySingle(r.songId) })
                    }
                }
            }
            if (throttledIds.isNotEmpty()) {
                throttledIds.forEach { sid ->
                    item(key = "throttled-$sid") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(salt.primary, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.size(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(queueTitles[sid] ?: sid.take(8), fontSize = 13.sp, color = salt.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("限流，稍后重试", fontSize = 11.sp, color = salt.primary)
                            }
                            SaltTextButton(text = "重试", onClick = { onRetrySingle(sid) })
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 12.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onUndo, modifier = Modifier.weight(1f)) { Text("撤销上次") }
            Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("返回队列") }
        }
    }
}

private fun statusColor(status: com.muses.player.core.model.scrape.WritebackStatus) =
    when (status) {
        com.muses.player.core.model.scrape.WritebackStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF34C759)
        com.muses.player.core.model.scrape.WritebackStatus.FILE_FAILED -> androidx.compose.ui.graphics.Color(0xFFFF9500)
        com.muses.player.core.model.scrape.WritebackStatus.FAILED -> androidx.compose.ui.graphics.Color(0xFFFF3B30)
    }

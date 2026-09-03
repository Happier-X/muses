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
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: ScrapeViewModel = hiltViewModel(),
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
                onConfirm = viewModel::confirmWriteback,
                onCancel = viewModel::backToQueue,
                onRetrySingle = viewModel::retrySingle,
                onRetryThrottled = viewModel::retryThrottled,
                onEdit = viewModel::updatePreviewItem,
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
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetrySingle: (String) -> Unit = {},
    onRetryThrottled: () -> Unit = {},
    onEdit: (String, String?, String?, String?) -> Unit = { _, _, _, _ -> },
) {
    val salt = LocalSaltColors.current
    var editTarget by remember { mutableStateOf<PreviewCandidate?>(null) }
    // 编辑弹层：复用 resolved 值填充，可空回退原值
    editTarget?.let { target ->
        PreviewEditSheet(
            candidate = target,
            onDismiss = { editTarget = null },
            onConfirm = { t, a, al ->
                onEdit(target.songId, t, a, al)
                editTarget = null
            },
        )
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "共 ${state.items.size} 首命中，默认全不选，勾选后才写回",
                fontSize = 13.sp,
                color = salt.text2,
                modifier = Modifier.weight(1f),
            )
            SaltTextButton(text = "全选", onClick = { onSetAll(true) })
            SaltTextButton(text = "清空", onClick = { onSetAll(false) })
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
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.items, key = { it.songId }) { item ->
                val isEdited = item.editTitle != null || item.editArtist != null || item.editAlbum != null
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(salt.surface1, RoundedCornerShape(10.dp))
                        .border(0.5.dp, if (item.checked) salt.primary.copy(alpha = 0.5f) else salt.surface2, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { onToggle(item.songId) },
                            colors = CheckboxDefaults.colors(checkedColor = salt.primary),
                        )
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            // 原值
                            Text(
                                "原：${item.currentTitle} · ${item.currentArtist ?: "未知艺术家"}${item.currentAlbum?.let { " · $it" } ?: ""}",
                                fontSize = 12.sp,
                                color = salt.text2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            // 新值（编辑覆写后高亮）
                            val newTitle = item.resolvedTitle() ?: "—"
                            val newArtist = item.resolvedArtist() ?: "—"
                            val newAlbum = item.resolvedAlbum()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    buildString {
                                        append("新：")
                                        append(newTitle)
                                        append(" · ")
                                        append(newArtist)
                                        if (newAlbum != null) { append(" · "); append(newAlbum) }
                                    },
                                    fontSize = 13.sp,
                                    color = if (isEdited) salt.primary else salt.text,
                                    fontWeight = if (isEdited) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.confidence != null) {
                                    Box(
                                        Modifier.background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(item.confidence!!, fontSize = 10.sp, color = salt.primary)
                                    }
                                }
                                if (isEdited) {
                                    Box(
                                        Modifier.padding(start = 4.dp).background(salt.primary, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
                                    ) {
                                        Text("已改", fontSize = 10.sp, color = salt.onPrimary)
                                    }
                                }
                            }
                        }
                        // 封面缩略
                        if (item.coverUrl != null) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = "封面",
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(salt.surface2),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(salt.surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("无封面", fontSize = 9.sp, color = salt.text2)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        SaltTextButton(text = "编辑", onClick = { editTarget = item })
                    }
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
                enabled = state.items.any { it.checked },
            ) { Text("写回选中") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewEditSheet(
    candidate: PreviewCandidate,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, String?) -> Unit,
) {
    val salt = LocalSaltColors.current
    var title by remember(candidate.songId) { mutableStateOf(candidate.resolvedTitle() ?: candidate.currentTitle) }
    var artist by remember(candidate.songId) { mutableStateOf(candidate.resolvedArtist() ?: candidate.currentArtist.orEmpty()) }
    var album by remember(candidate.songId) { mutableStateOf(candidate.resolvedAlbum() ?: candidate.currentAlbum.orEmpty()) }
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
                        scope.launch { sheetState.hide() }
                        onConfirm(outTitle, outArtist, outAlbum)
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

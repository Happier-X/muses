package com.muses.player.feature.scrape

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesBottomSheet
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.components.MusesCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.components.MarqueeText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesImagePreview
import com.muses.player.core.ui.components.MusesTopBar
import top.yukonga.miuix.kmp.basic.Scaffold
import com.muses.player.core.ui.components.ScrapeBadgeBox
import com.muses.player.core.ui.components.ScrapeCandidateRow
import com.muses.player.core.ui.components.ScrapeCoverThumb
import com.muses.player.core.ui.components.SharedScrapeCandidate
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.scrape.text.evaluateTextMatch
import com.muses.player.core.scrape.text.groupTextCandidates
import top.yukonga.miuix.kmp.basic.BasicComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 单曲刮削审核页（Tagger 式「就地审核」全屏页，design §2.3）：
 * 歌曲头 → 搜索词行（改词重搜）→ 文本字段审核（本地值→候选值逐字段勾选 + 候选切换条）→
 * 封面多候选缩略图（点选 + 大图预览）→ 歌词候选（来源+格式角标 + 预览）→ 底部「应用（N）」。
 * 由 SingleScrapeSheet 升级改造而来，旧浮层已退役。
 */
@Composable
fun ScrapeReviewScreen(
    onBack: () -> Unit,
    viewModel: ScrapeReviewViewModel = koinViewModel(),
    /**
     * S3「应用并下一首」：批量模式审核页写回成功后，宿主先推进 ScrapeViewModel 待审队列
     * （[advanceReview 回调由宿主实现]），再打开下一首审核页。
     * @param writtenSongId 刚写回的歌曲；@param nextSongId 队列中下一首（null = 队列结束）
     */
    onAppliedAndNext: (writtenSongId: String, nextSongId: String?) -> Unit = { _, _ -> },
    /** S3 用户手动返回（非应用路径）：宿主清 ScrapeViewModel 待审队列，不强推下一首 */
    onManualBack: () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val state by viewModel.state.collectAsState()
    val keyword by viewModel.keyword.collectAsState()
    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelAiMatching() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "刮削审核",
                onBack = {
                    viewModel.cancelAiMatching()
                    // 手动返回即清待审队列（S3：不强推下一首）
                    onManualBack()
                    onBack()
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
        when (val s = state) {
            is ScrapeReviewState.Searching -> SearchingContent(viewModel.currentSong?.title)

            is ScrapeReviewState.Review -> ReviewContent(
                state = s,
                keyword = keyword,
                viewModel = viewModel,
            )

            is ScrapeReviewState.Empty -> EmptyContent(
                reason = s.reason,
                keyword = keyword,
                viewModel = viewModel,
            )

            ScrapeReviewState.Writing -> WritingContent()

            is ScrapeReviewState.Success -> SuccessContent(
                message = s.message,
                nextSongId = s.nextSongId,
                // 成功页返回同样视为结束连续审核（清待审队列；队列由「应用并下一首」推进）
                onBack = { onManualBack(); onBack() },
                onNext = { next -> onAppliedAndNext(viewModel.lastWrittenSongId.orEmpty(), next) },
            )
        }
        }
    }
}

// (顶栏已并入 Scaffold topBar 槽：SaltReviewNavbar 退役)

// ── Searching / Writing / Success / Empty 态 ──────────────

@Composable
private fun SearchingContent(songTitle: String?) {
    val scheme = MiuixTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在搜索候选…", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
            if (songTitle != null) {
                Text(songTitle, style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WritingContent() {
    val scheme = MiuixTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在写回…", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
            Text("写入文件与数据库，WebDAV 曲目需数秒", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
        }
    }
}

@Composable
private fun SuccessContent(
    message: String,
    nextSongId: String?,
    onBack: () -> Unit,
    onNext: (String) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MiuixTheme.textStyles.body1, color = scheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (nextSongId != null) {
                // S3 批量模式：应用并下一首
                MusesButton(onClick = { onNext(nextSongId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("核对下一首")
                }
                Spacer(Modifier.height(8.dp))
            }
            ScrapeTextAction(text = "返回", onClick = onBack)
        }
    }
}

/** 空态：reason 区分暂无匹配与限流；保留搜索词行（改词引导）+ 重试 */
@Composable
private fun EmptyContent(
    reason: String,
    keyword: ReviewKeyword,
    viewModel: ScrapeReviewViewModel,
) {
    val scheme = MiuixTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (reason == "暂无匹配") "未找到候选" else reason,
                style = MiuixTheme.textStyles.body2,
                color = scheme.onBackgroundVariant,
            )
            if (reason == "暂无匹配") {
                Spacer(Modifier.height(4.dp))
                Text("可修改下方搜索词后重新搜索", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
            }
            Spacer(Modifier.height(12.dp))
            SearchKeywordRow(keyword = keyword, viewModel = viewModel)
        }
        MusesButton(
            onClick = { viewModel.search() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    // 底部按钮贴内容底：叠加悬浮件避让（悬浮件高度见 BottomChrome）
                    bottom = 16.dp + com.muses.player.core.ui.theme.LocalBottomChromePadding.current,
                ),
        ) {
            Text("重试")
        }
    }
}

// ── Review 态 ─────────────────────────────────────────────

@Composable
private fun ReviewContent(
    state: ScrapeReviewState.Review,
    keyword: ReviewKeyword,
    viewModel: ScrapeReviewViewModel,
) {
    val scheme = MiuixTheme.colorScheme
    var previewCoverUrl by remember { mutableStateOf<String?>(null) }
    var showSearchFields by remember(state.song.id) { mutableStateOf(false) }
    var choosingField by remember(state.song.id) { mutableStateOf<ReviewTextField?>(null) }
    val aiState by viewModel.aiState.collectAsState()
    val sourceDraft by viewModel.searchSources.collectAsState()
    val aiDecision = (aiState as? ScrapeAiState.Ready)?.decision

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 歌曲头：本地标题/歌手/专辑 + 封面小图
            item(key = "song-head") { SongHead(state) }
            if (state.restoredDraft || state.draftBaselineChanged) {
                item(key = "restored-draft") {
                    Text(if (state.draftBaselineChanged) "歌曲或文件已变化，或无法核对审核基准；本地文件请重新搜索并审核，其他来源请重新核对勾选"
                        else "已恢复上次审核草稿，请核对后手动应用",
                        style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                }
            }
            val failedSources = (state.text.failedSources + state.cover.failedSources + state.lyrics.failedSources).distinct()
            if (failedSources.isNotEmpty()) {
                item(key = "search-failures") {
                    SearchFailureNotice(state, onRetry = viewModel::retryFailedSources)
                }
            }
            item(key = "ai-match") {
                AiMatchCard(
                    state = aiState,
                    hasCandidates = !state.retryingFailures && (state.text.items.isNotEmpty() || state.lyrics.items.isNotEmpty()),
                    onMatch = viewModel::matchWithAi, onCancel = viewModel::cancelAiMatching,
                )
            }
            item(key = "keyword") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索词", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant, modifier = Modifier.weight(1f))
                        ScrapeTextAction(
                            text = if (showSearchFields) "收起搜索词" else "修改搜索词",
                            onClick = { showSearchFields = !showSearchFields },
                        )
                    }
                    Text("本次来源：歌曲信息与封面 · ${metadataSourceLabel(state.sources.metadata)}；歌词 · ${lyricsSourceLabel(state.sources.lyrics)} + AMLL",
                        style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                    if (sourceDraft != state.sources) {
                        Text("来源选择已修改，重新搜索后生效", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                    }
                    if (showSearchFields) SearchKeywordRow(keyword = keyword, viewModel = viewModel)
                }
            }

            // 文本字段审核区（逐字段 Checkbox + 本地值 → 候选值 + 来源/推荐角标）
            item(key = "text-fields") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("歌曲信息", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                        Spacer(Modifier.weight(1f))
                        ScrapeTextAction(text = "全选", onClick = {
                            selectableFields(state).forEach { field ->
                                if (field !in state.checkedFields) viewModel.toggleField(field)
                            }
                        })
                        ScrapeTextAction(text = "全不选", onClick = {
                            state.checkedFields.toList().forEach { viewModel.toggleField(it) }
                        })
                    }
                    Spacer(Modifier.height(4.dp))
                    val hit = state.selectedHit?.copy(title = state.resolvedTitle(), artist = state.resolvedArtist(), album = state.resolvedAlbum())
                    if (hit != null) {
                        val evidence = evaluateTextMatch(hit, state.matchQuery())
                        if (evidence.reasons.isNotEmpty()) {
                            Text("匹配依据：${evidence.reasons.joinToString(" · ")}", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                        }
                        val sourceWarnings = ReviewTextField.entries.mapNotNull { field ->
                            if (state.editedValue(field) != null) return@mapNotNull null
                            state.fieldHit(field)?.let { source ->
                                evaluateTextMatch(source, state.matchQuery()).warnings
                                    .filter { "版本" in it || field in state.textFieldIndices }.takeIf { it.isNotEmpty() }
                                    ?.let { "${field.label}来源：${it.joinToString("；")}" }
                            }
                        }
                        val warnings = (evidence.warnings + sourceWarnings).distinct()
                        if (warnings.isNotEmpty()) {
                            Text("请核对：${warnings.joinToString("；")}", style = MiuixTheme.textStyles.footnote2, color = scheme.error)
                        }
                        if (evidence.requiresReview) {
                            Text("请核对后手动勾选要应用的歌曲信息", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                        }
                    }
                    ReviewTextField.entries.forEach { field ->
                        TextFieldReviewRow(state, field, onToggle = { viewModel.toggleField(field.key) }, onChoose = { choosingField = field })
                    }
                    TextFieldEditOverrides(state = state, viewModel = viewModel)
                }
            }

            // 文本候选切换条：横向 chip（源 + 标题），当前选中高亮
            if (state.text.items.isNotEmpty()) {
                item(key = "text-candidates") {
                    TextCandidateStrip(state = state, viewModel = viewModel, aiRecommendedIndex = aiDecision?.text?.index)
                }
            }

            // 封面区：Checkbox + 横向候选缩略图（点选，再点已选项弹大图预览）
            item(key = "cover") {
                CoverSection(state = state, viewModel = viewModel, onPreview = { previewCoverUrl = it })
            }

            // 歌词区：Checkbox + 候选列表（来源+format 角标）+ 预览
            item(key = "lyrics") {
                LyricsSection(state = state, viewModel = viewModel, aiRecommendedIndex = aiDecision?.lyrics?.index)
            }
        }

        // 底部「应用（N）」：仅写回勾选字段；无勾选 disabled（写回安全语义）
        MusesButton(
            onClick = { viewModel.apply() },
            enabled = state.checkedFields.isNotEmpty() && aiState != ScrapeAiState.Matching && !state.retryingFailures,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    // 底部按钮贴内容底：叠加悬浮件避让（悬浮件高度见 BottomChrome）
                    bottom = 16.dp + com.muses.player.core.ui.theme.LocalBottomChromePadding.current,
                ),
        ) {
            Text("应用" + if (state.checkedFields.isNotEmpty()) "（${state.checkedFields.size}）" else "")
        }
    }

    // 封面大图预览：统一走 MusesImagePreview（miuix OverlayDialog，遮罩与底部弹窗/对话框同源）
    previewCoverUrl?.let { url ->
        MusesImagePreview(
            imageUrl = url,
            onDismiss = { previewCoverUrl = null },
        )
    }
    choosingField?.let { field ->
        TextFieldSourceSheet(state, field, onDismiss = { choosingField = null }) { index ->
            viewModel.selectTextFieldCandidate(field, index)
            choosingField = null
        }
    }
}

@Composable
private fun TextFieldReviewRow(state: ScrapeReviewState.Review, field: ReviewTextField, onToggle: () -> Unit, onChoose: () -> Unit) {
    val original = when (field) {
        ReviewTextField.TITLE -> state.song.title
        ReviewTextField.ARTIST -> state.song.artist
        ReviewTextField.ALBUM -> state.song.album
    }
    val updated = when (field) {
        ReviewTextField.TITLE -> state.resolvedTitle()
        ReviewTextField.ARTIST -> state.resolvedArtist()
        ReviewTextField.ALBUM -> state.resolvedAlbum()
    }
    val source = if (state.editedValue(field) != null) "手动编辑" else
        state.fieldHit(field)?.takeIf { !field.value(it).isNullOrBlank() }?.source?.wire?.let(::scrapeSourceLabel)
    val scheme = MiuixTheme.colorScheme
    val hasCandidate = !updated.isNullOrBlank()
    val changed = hasCandidate && updated != original
    val checked = field.key in state.checkedFields
    val status = when {
        !hasCandidate -> "无候选，保持当前值"
        !changed -> "与当前相同"
        original.isNullOrBlank() -> "可补全"
        else -> "有变化"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        MusesCheckbox(
            checked = checked, enabled = hasCandidate, onToggle = onToggle,
            checkedColor = scheme.primary,
        )
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text("${field.label} · $status", style = MiuixTheme.textStyles.footnote1,
                color = scheme.onBackgroundVariant)
            // 分行展示完整值，避免旧值过长把真正要保存的内容挤出屏幕。
            SelectionContainer {
                Column {
                    Text("当前：${original?.takeIf { it.isNotBlank() } ?: "未填写"}",
                        style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                    if (changed) {
                        Text("候选：$updated", style = MiuixTheme.textStyles.body2,
                            color = if (checked) scheme.primary else scheme.onBackground)
                    }
                }
            }
            if (hasCandidate && source != null) {
                Text("来源：$source", style = MiuixTheme.textStyles.footnote2,
                    color = scheme.onBackgroundVariant)
            }
        }
        if (state.text.items.any { !field.value(it).isNullOrBlank() }) {
            MusesIconButton(onClick = onChoose, imageVector = TablerIcons.ChevronDown, contentDescription = "选择${field.label}来源")
        }
    }
}

@Composable
private fun TextFieldSourceSheet(state: ScrapeReviewState.Review, field: ReviewTextField, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    MusesBottomSheet(onDismiss = onDismiss, title = "选择${field.label}来源") {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            itemsIndexed(state.text.items) { index, hit ->
                val value = field.value(hit)
                if (!value.isNullOrBlank()) {
                    val selected = state.editedValue(field) == null && index == (state.textFieldIndices[field] ?: state.selectedTextIndex)
                    val evidence = evaluateTextMatch(hit, state.matchQuery())
                    BasicComponent(
                        title = value,
                        summary = (listOf(scrapeSourceLabel(hit.source.wire), listOfNotNull(hit.title, hit.artist, hit.album).filter(String::isNotBlank).joinToString(" · ")) +
                            listOfNotNull(hit.versionInfo?.takeIf(String::isNotBlank)?.let { "版本：$it" }, candidateDurationLabel(hit.durationSec)) +
                            evidence.warnings.map { "请核对：$it" }).joinToString("\n"),
                        endActions = { if (selected) Text("已选", color = MiuixTheme.colorScheme.primary) },
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFailureNotice(state: ScrapeReviewState.Review, onRetry: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val result = com.muses.player.core.scrape.editmeta.EditCloudMetaResult(state.text, state.cover, state.lyrics)
    val failed = (state.text.failedSources + state.cover.failedSources + state.lyrics.failedSources).distinct()
    val hasCandidates = state.text.items.isNotEmpty() || state.cover.items.isNotEmpty() || state.lyrics.items.isNotEmpty()
    var expanded by remember(state.song.id, failed, hasCandidates) { mutableStateOf(!hasCandidates) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${failed.size} 个来源失败" + if (hasCandidates) "，已有候选可用" else "",
                style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
                modifier = Modifier.weight(1f))
            if (state.retryingFailures) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                ScrapeTextAction(text = "重试", onClick = onRetry)
            }
            MusesIconButton(onClick = { expanded = !expanded },
                imageVector = if (expanded) TablerIcons.ChevronDown else TablerIcons.ChevronRight,
                contentDescription = if (expanded) "收起失败原因" else "查看失败原因")
        }
        if (expanded) {
            Text(describeScrapeFailures(result).joinToString("\n"), style = MiuixTheme.textStyles.footnote2,
                color = scheme.onBackgroundVariant)
        }
    }
}

@Composable
private fun AiMatchCard(state: ScrapeAiState, hasCandidates: Boolean, onMatch: () -> Unit, onCancel: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 帮我选", style = MiuixTheme.textStyles.body2, modifier = Modifier.weight(1f))
            if (state == ScrapeAiState.Matching) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                ScrapeTextAction(text = "取消", onClick = onCancel)
            } else {
                ScrapeTextAction(
                    text = if (state is ScrapeAiState.Ready || state is ScrapeAiState.Failed) "重新匹配" else "开始匹配",
                    enabled = hasCandidates, onClick = onMatch,
                )
            }
        }
        when (state) {
            ScrapeAiState.Idle -> Text(
                "比较歌曲信息与歌词候选",
                style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
            )
            ScrapeAiState.Matching -> Text(
                "正在比较候选…",
                style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
            )
            is ScrapeAiState.Failed -> Text(state.message, style = MiuixTheme.textStyles.footnote1, color = scheme.error)
            is ScrapeAiState.Ready -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("已选择候选，请核对后应用", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant, modifier = Modifier.weight(1f))
                    MusesIconButton(onClick = { expanded = !expanded }, imageVector = if (expanded) TablerIcons.ChevronDown else TablerIcons.ChevronRight, contentDescription = if (expanded) "收起匹配依据" else "查看匹配依据")
                }
                if (expanded) {
                    Text("歌曲信息：${state.decision.text.reason}", style = MiuixTheme.textStyles.footnote1)
                    Text("歌词：${state.decision.lyrics.reason}", style = MiuixTheme.textStyles.footnote1)
                }
            }
        }
    }
}

/** 歌曲头：本地标题 · 歌手 · 专辑 + 封面小图（V3 共用化：ScrapeCandidateRow，「本地值」列的数据来源快照） */
@Composable
private fun SongHead(state: ScrapeReviewState.Review) {
    ScrapeCandidateRow(
        candidate = SharedScrapeCandidate(
            songId = state.song.id,
            title = state.song.title,
            subtitle = listOfNotNull(
                state.song.artist ?: "未知艺术家",
                state.song.album ?: "未知专辑",
            ).joinToString(" · "),
            coverUri = state.song.coverUri,
        ),
    )
}

/** 搜索词行：title/artist/album 三输入（默认预填本地值）+ 「重新搜索」 */
@Composable
private fun SearchKeywordRow(keyword: ReviewKeyword, viewModel: ScrapeReviewViewModel) {
    Column {
        MusesTextField(
            value = keyword.title,
            onValueChange = viewModel::updateKeywordTitle,
            label = "标题",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MusesTextField(
                value = keyword.artist,
                onValueChange = viewModel::updateKeywordArtist,
                label = "歌手",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            MusesTextField(
                value = keyword.album,
                onValueChange = viewModel::updateKeywordAlbum,
                label = "专辑",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        SearchSourcesFields(viewModel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ScrapeTextAction(
                text = "重新搜索",
                onClick = { viewModel.search() },
                enabled = !keyword.titleBlank,
            )
        }
    }
}

/** 逐字段手改覆写（迁移自 SingleScrapeSheet 的编辑区；歌词覆写由歌词候选选择区承担） */
@Composable
private fun TextFieldEditOverrides(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ScrapeTextAction(text = if (expanded) "收起编辑" else "编辑", onClick = {
        viewModel.cancelAiMatching()
        expanded = !expanded
    })
    if (expanded) {
        var title by remember(state.resolvedTitle()) { mutableStateOf(state.resolvedTitle() ?: state.song.title) }
        var artist by remember(state.resolvedArtist()) { mutableStateOf(state.resolvedArtist() ?: state.song.artist.orEmpty()) }
        var album by remember(state.resolvedAlbum()) { mutableStateOf(state.resolvedAlbum() ?: state.song.album.orEmpty()) }
        Spacer(Modifier.height(6.dp))
        MusesTextField(
            value = title,
            onValueChange = { viewModel.cancelAiMatching(); title = it },
            label = "标题覆写",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        MusesTextField(
            value = artist,
            onValueChange = { viewModel.cancelAiMatching(); artist = it },
            label = "歌手覆写",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        MusesTextField(
            value = album,
            onValueChange = { viewModel.cancelAiMatching(); album = it },
            label = "专辑覆写",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ScrapeTextAction(text = "应用编辑", onClick = {
                viewModel.updateEditTitle(title)
                viewModel.updateEditArtist(artist)
                viewModel.updateEditAlbum(album)
                expanded = false
            })
        }
    }
}

/** 使用已完成搜索的关键词快照，避免输入草稿改变现有候选的匹配依据。 */
private fun ScrapeReviewState.Review.matchQuery() = OnlineTextQuery(
    songId = song.id,
    title = keyword.title,
    artist = keyword.artist,
    album = keyword.album,
    durationSec = song.reviewDurationSec(),
)

/** 文本候选切换条：展示来源、标题、歌手和专辑，便于辨别不同版本。 */
@Composable
private fun TextCandidateStrip(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel, aiRecommendedIndex: Int?) {
    val scheme = MiuixTheme.colorScheme
    val groups = remember(state.text.items) { groupTextCandidates(state.text.items) }
    if (groups.isEmpty()) return
    var anchor by remember(state.song.id, state.selectedTextIndex) { mutableStateOf(state.selectedTextIndex) }
    val active = groups.firstOrNull { anchor in it.candidateIndices } ?: groups.first()
    Column {
        Text("歌曲候选 ${state.text.items.size} · 点击替换标题、歌手和专辑",
            style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
        if (groups.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(groups) { _, group ->
                    val representative = state.text.items[group.candidateIndices.first()]
                    ScrapeTextAction(text = listOfNotNull(representative.title, representative.artist, representative.versionInfo)
                        .filter(String::isNotBlank).joinToString(" · ") + if (group == active) " · 已展开" else "",
                        onClick = { anchor = group.candidateIndices.first() })
                }
            }
        }
        active.releases.forEach { release ->
            val album = release.candidateIndices.firstNotNullOfOrNull { state.text.items[it].album?.takeIf(String::isNotBlank) }
            Text(album ?: "专辑未提供", style = MiuixTheme.textStyles.footnote1,
                color = scheme.onBackgroundVariant, modifier = Modifier.padding(top = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(release.candidateIndices) { _, index ->
                    TextCandidateCard(state, index, aiRecommendedIndex, onSelect = { viewModel.selectTextCandidate(index) })
                }
            }
        }
    }
}

@Composable
private fun TextCandidateCard(state: ScrapeReviewState.Review, index: Int, aiRecommendedIndex: Int?, onSelect: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val hit = state.text.items[index]
    val selected = state.textFieldIndices.isEmpty() && state.editTitle == null && state.editArtist == null &&
        state.editAlbum == null && index == state.selectedTextIndex
    val evidence = evaluateTextMatch(hit, state.matchQuery())
    Column(Modifier.width(220.dp).clickable(onClick = onSelect).padding(vertical = 8.dp, horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(scrapeSourceLabel(hit.source.wire), style = MiuixTheme.textStyles.footnote2,
                color = scheme.primary, fontWeight = FontWeight.SemiBold)
            if (index == aiRecommendedIndex || (aiRecommendedIndex == null && index == state.text.defaultIndex)) {
                Spacer(Modifier.width(4.dp))
                Text(if (index == aiRecommendedIndex) "AI 推荐" else "推荐", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
            }
            if (selected) {
                Spacer(Modifier.weight(1f))
                Text("已选", style = MiuixTheme.textStyles.footnote2, color = scheme.primary)
            }
        }
        Text(hit.title ?: "（无标题）", style = MiuixTheme.textStyles.footnote1,
            color = if (selected) scheme.onBackground else scheme.onBackgroundVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        Text(listOfNotNull(hit.artist, hit.album).joinToString(" · "), style = MiuixTheme.textStyles.footnote2,
            color = scheme.onBackgroundVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        hit.versionInfo?.takeIf(String::isNotBlank)?.let {
            Text("版本：$it", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        candidateDurationLabel(hit.durationSec)?.let {
            Text(it, style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
        }
        if (evidence.warnings.isNotEmpty()) {
            Text(evidence.warnings.joinToString("；"), style = MiuixTheme.textStyles.footnote2,
                color = scheme.error)
        }
    }
}

/** 封面区：Checkbox + 横向候选缩略图（首列本地封面供对比；点选，再点已选项弹大图预览） */
@Composable
private fun CoverSection(
    state: ScrapeReviewState.Review,
    viewModel: ScrapeReviewViewModel,
    onPreview: (String) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MusesCheckbox(
                checked = "cover" in state.checkedFields,
                onToggle = { viewModel.toggleField("cover") },
                enabled = state.cover.items.isNotEmpty(),
                checkedColor = scheme.primary,
            )
            Text("封面", style = MiuixTheme.textStyles.footnote1, color = if ("cover" in state.checkedFields) scheme.onBackground else scheme.onBackgroundVariant)
            Spacer(Modifier.weight(1f))
            Text("候选 ${state.cover.items.size}", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 本地封面（对比用，不可选中）
            item(key = "local") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MusesCover(uri = state.song.coverUri, size = 72.dp, radius = MusesCoverRadius.SM)
                    Spacer(Modifier.height(2.dp))
                    Text("本地", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                }
            }
            itemsIndexed(state.cover.items) { index, candidate ->
                val selected = index == state.selectedCoverIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // V3 共用化：候选缩略图 = ScrapeCoverThumb（选中态 primary 边框）；
                    // 「再点已选项→大图预览，否则切换选中」逻辑留在调用方
                    ScrapeCoverThumb(
                        url = candidate.remoteUrl,
                        selected = selected,
                        contentDescription = "封面候选 ${index + 1}",
                        onClick = {
                            if (selected) {
                                onPreview(candidate.remoteUrl)
                            } else {
                                viewModel.selectCover(index)
                            }
                        },
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(scrapeSourceLabel(candidate.source.wire), style = MiuixTheme.textStyles.footnote2, color = if (selected) scheme.primary else scheme.onBackgroundVariant)
                }
            }
        }
    }
}

/** 歌词区：Checkbox + 候选列表（来源 + format 角标）+ 预览前几行 */
@Composable
private fun LyricsSection(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel, aiRecommendedIndex: Int?) {
    val scheme = MiuixTheme.colorScheme
    var previewLyrics by remember(state.song.id) { mutableStateOf<com.muses.player.core.scrape.editmeta.EditLyricsCandidate?>(null) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MusesCheckbox(
                checked = "lyrics" in state.checkedFields,
                onToggle = { viewModel.toggleField("lyrics") },
                enabled = state.lyrics.items.isNotEmpty(),
                checkedColor = scheme.primary,
            )
            Text("歌词", style = MiuixTheme.textStyles.footnote1, color = if ("lyrics" in state.checkedFields) scheme.onBackground else scheme.onBackgroundVariant)
            Spacer(Modifier.weight(1f))
            Text("候选 ${state.lyrics.items.size}", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
        }
        if (state.lyrics.items.isEmpty()) {
            Text("未命中歌词候选", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant, modifier = Modifier.padding(start = 44.dp))
        } else {
            state.lyrics.items.forEachIndexed { index, candidate ->
                LyricsCandidateRow(
                    index = index,
                    candidate = candidate,
                    selected = index == state.selectedLyricsIndex,
                    onClick = { viewModel.selectLyrics(index) },
                    onPreview = { previewLyrics = candidate },
                    aiRecommended = index == aiRecommendedIndex,
                )
            }
        }
    }
    previewLyrics?.let { candidate ->
        val preview = rememberLyricsCandidatePreview(candidate)
        MusesBottomSheet(onDismiss = { previewLyrics = null }, title = "歌词预览") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("${scrapeSourceLabel(candidate.source)} · ${preview?.kind ?: "正在解析"}",
                    style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        preview?.text ?: preview?.snippet ?: "正在生成歌词预览…",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberLyricsCandidatePreview(candidate: com.muses.player.core.scrape.editmeta.EditLyricsCandidate): LyricsCandidatePreview? {
    return androidx.compose.runtime.key(candidate.text, candidate.format) {
        val preview by produceState<LyricsCandidatePreview?>(null) {
            value = withContext(Dispatchers.Default) { lyricsCandidatePreview(candidate) }
        }
        preview
    }
}

@Composable
private fun LyricsCandidateRow(
    index: Int,
    candidate: com.muses.player.core.scrape.editmeta.EditLyricsCandidate,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    aiRecommended: Boolean,
) {
    val scheme = MiuixTheme.colorScheme
    val preview = rememberLyricsCandidatePreview(candidate)
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", style = MiuixTheme.textStyles.footnote2, color = if (selected) scheme.primary else scheme.onBackgroundVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            ScrapeBadgeBox(text = scrapeSourceLabel(candidate.source))
            Spacer(Modifier.size(4.dp))
            ScrapeBadgeBox(text = preview?.kind ?: "正在解析")
            if (aiRecommended) {
                Spacer(Modifier.size(4.dp))
                Text("AI 推荐", style = MiuixTheme.textStyles.footnote2, color = scheme.primary)
            }
            if (selected) {
                Spacer(Modifier.weight(1f))
                Text("已选", style = MiuixTheme.textStyles.footnote2, color = scheme.primary)
            }
        }
        Spacer(Modifier.height(4.dp))
        // 保留歌词的换行；完整内容通过预览展开。
        Text(
            preview?.snippet ?: "正在生成歌词预览…",
            style = MiuixTheme.textStyles.footnote2,
            color = if (selected) scheme.onBackground else scheme.onBackgroundVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        ScrapeTextAction(text = "查看完整歌词", onClick = onPreview)
    }
}

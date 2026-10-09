package com.muses.player.feature.scrape

import top.yukonga.miuix.kmp.theme.MiuixTheme
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
import top.yukonga.miuix.kmp.basic.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.ScrapeBadgeBox
import com.muses.player.core.ui.components.ScrapeCandidateRow
import com.muses.player.core.ui.components.ScrapeCoverThumb
import com.muses.player.core.ui.components.ScrapeReviewFieldRow
import com.muses.player.core.ui.components.SharedReviewField
import com.muses.player.core.ui.components.SharedScrapeCandidate

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
    nextSongId: String?,
    onBack: () -> Unit,
    onNext: (String) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("已更新", style = MiuixTheme.textStyles.body1, color = scheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (nextSongId != null) {
                // S3 批量模式：应用并下一首
                MusesButton(onClick = { onNext(nextSongId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("核对下一首")
                }
                Spacer(Modifier.height(8.dp))
            }
            MusesTextButton(text = "返回", onClick = onBack)
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
    val aiState by viewModel.aiState.collectAsState()
    val aiDecision = (aiState as? ScrapeAiState.Ready)?.decision

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 歌曲头：本地标题/歌手/专辑 + 封面小图
            item(key = "song-head") { SongHead(state) }
            item(key = "ai-match") {
                AiMatchCard(
                    state = aiState,
                    hasCandidates = state.text.items.isNotEmpty() || state.lyrics.items.isNotEmpty(),
                    onMatch = viewModel::matchWithAi, onCancel = viewModel::cancelAiMatching,
                )
            }
            item(key = "keyword") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("核对候选后勾选要更新的字段", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant, modifier = Modifier.weight(1f))
                        MusesTextButton(
                            text = if (showSearchFields) "收起搜索词" else "修改搜索词",
                            onClick = { showSearchFields = !showSearchFields },
                        )
                    }
                    if (showSearchFields) SearchKeywordRow(keyword = keyword, viewModel = viewModel)
                }
            }

            // 文本字段审核区（逐字段 Checkbox + 本地值 → 候选值 + 来源/推荐角标）
            item(key = "text-fields") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("应用字段：", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                        Spacer(Modifier.weight(1f))
                        MusesTextButton(text = "全选", onClick = {
                            selectableFields(state).forEach { field ->
                                if (field !in state.checkedFields) viewModel.toggleField(field)
                            }
                        })
                        MusesTextButton(text = "全不选", onClick = {
                            state.checkedFields.toList().forEach { viewModel.toggleField(it) }
                        })
                    }
                    Spacer(Modifier.height(4.dp))
                    val hit = state.selectedHit
                    val recommended = state.selectedTextIndex == (aiDecision?.text?.index ?: state.text.defaultIndex)
                    // V3 共用化：字段审核行 = ScrapeReviewFieldRow（Checkbox+对比+来源/推荐角标）；
                    // enabled=无解析值禁勾（写回安全红线），语义与原 FieldCheckRow 一致
                    ScrapeReviewFieldRow(
                        field = SharedReviewField(
                            key = "title",
                            label = "标题",
                            original = state.song.title,
                            updated = state.resolvedTitle() ?: "—",
                            checked = "title" in state.checkedFields,
                        ),
                        enabled = state.resolvedTitle() != null,
                        onCheckedChange = { viewModel.toggleField("title") },
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
                    ScrapeReviewFieldRow(
                        field = SharedReviewField(
                            key = "artist",
                            label = "歌手",
                            original = state.song.artist ?: "—",
                            updated = state.resolvedArtist() ?: "—",
                            checked = "artist" in state.checkedFields,
                        ),
                        enabled = state.resolvedArtist() != null,
                        onCheckedChange = { viewModel.toggleField("artist") },
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
                    ScrapeReviewFieldRow(
                        field = SharedReviewField(
                            key = "album",
                            label = "专辑",
                            original = state.song.album ?: "—",
                            updated = state.resolvedAlbum() ?: "—",
                            checked = "album" in state.checkedFields,
                        ),
                        enabled = state.resolvedAlbum() != null,
                        onCheckedChange = { viewModel.toggleField("album") },
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
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
            enabled = state.checkedFields.isNotEmpty() && aiState != ScrapeAiState.Matching,
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
}

@Composable
private fun AiMatchCard(state: ScrapeAiState, hasCandidates: Boolean, onMatch: () -> Unit, onCancel: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 帮我选", style = MiuixTheme.textStyles.main, modifier = Modifier.weight(1f))
            if (state == ScrapeAiState.Matching) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                MusesTextButton(text = "取消", onClick = onCancel)
            } else {
                MusesTextButton(
                    text = if (state is ScrapeAiState.Ready || state is ScrapeAiState.Failed) "重新匹配" else "开始匹配",
                    enabled = hasCandidates, onClick = onMatch,
                )
            }
        }
        when (state) {
            ScrapeAiState.Idle -> Text(
                "将歌曲信息和歌词片段交给已配置的 AI 比较，自动选中有把握的候选，保留已有的手动编辑。封面仍需手动核对。",
                style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
            )
            ScrapeAiState.Matching -> Text(
                "正在比较候选…手动修改选择会取消本次 AI 匹配。",
                style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant,
            )
            is ScrapeAiState.Failed -> Text(state.message, style = MiuixTheme.textStyles.footnote1, color = scheme.error)
            is ScrapeAiState.Ready -> {
                Text("歌曲信息：${state.decision.text.reason}", style = MiuixTheme.textStyles.footnote1)
                Spacer(Modifier.height(6.dp))
                Text("歌词：${state.decision.lyrics.reason}", style = MiuixTheme.textStyles.footnote1)
                Spacer(Modifier.height(8.dp))
                Text("已有手动编辑会保留，核对后点击底部「应用」保存；封面仍需手动核对。", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
            }
        }
    }
}

/** 当前有解析值、可勾选的字段（全选用；写回安全语义：无值字段不可勾） */
private fun selectableFields(state: ScrapeReviewState.Review): List<String> = listOfNotNull(
    "title".takeIf { state.resolvedTitle() != null },
    "artist".takeIf { state.resolvedArtist() != null },
    "album".takeIf { state.resolvedAlbum() != null },
    "cover".takeIf { state.selectedCover != null },
    "lyrics".takeIf { state.selectedLyrics != null },
)

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MusesTextButton(
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
    MusesTextButton(text = if (expanded) "收起编辑" else "编辑", onClick = {
        viewModel.cancelAiMatching()
        expanded = !expanded
    })
    if (expanded) {
        var title by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedTitle() ?: state.song.title) }
        var artist by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedArtist() ?: state.song.artist.orEmpty()) }
        var album by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedAlbum() ?: state.song.album.orEmpty()) }
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
            MusesTextButton(text = "应用编辑", onClick = {
                viewModel.updateEditTitle(title)
                viewModel.updateEditArtist(artist)
                viewModel.updateEditAlbum(album)
                expanded = false
            })
        }
    }
}

/** 文本候选切换条：展示来源、标题、歌手和专辑，便于辨别不同版本。 */
@Composable
private fun TextCandidateStrip(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel, aiRecommendedIndex: Int?) {
    val scheme = MiuixTheme.colorScheme
    Column {
        Text("文本候选（${state.text.items.size}）", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.text.items) { index, hit ->
                val selected = index == state.selectedTextIndex
                Card(
                    modifier = Modifier.width(220.dp),
                    insideMargin = PaddingValues(12.dp),
                    onClick = { viewModel.selectTextCandidate(index) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(hit.source.wire, style = MiuixTheme.textStyles.footnote2, color = scheme.primary, fontWeight = FontWeight.SemiBold)
                        if (index == aiRecommendedIndex || (aiRecommendedIndex == null && index == state.text.defaultIndex)) {
                            Spacer(Modifier.width(4.dp))
                            Text(if (index == aiRecommendedIndex) "AI 推荐" else "推荐", style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant)
                        }
                        if (selected) {
                            Spacer(Modifier.weight(1f))
                            Text("已选", style = MiuixTheme.textStyles.footnote2, color = scheme.primary)
                        }
                    }
                    Text(
                        hit.title ?: "（无标题）",
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (selected) scheme.onBackground else scheme.onBackgroundVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        listOfNotNull(hit.artist, hit.album).joinToString(" · "),
                        style = MiuixTheme.textStyles.footnote2, color = scheme.onBackgroundVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
                    Text(candidate.source.wire, style = MiuixTheme.textStyles.footnote2, color = if (selected) scheme.primary else scheme.onBackgroundVariant)
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
        MusesBottomSheet(onDismiss = { previewLyrics = null }, title = "歌词预览") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("${candidate.source} · ${candidate.format}", style = MiuixTheme.textStyles.footnote1, color = scheme.onBackgroundVariant)
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        candidate.text,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        insideMargin = PaddingValues(12.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", style = MiuixTheme.textStyles.footnote2, color = if (selected) scheme.primary else scheme.onBackgroundVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            ScrapeBadgeBox(text = candidate.source)
            Spacer(Modifier.size(4.dp))
            ScrapeBadgeBox(text = candidate.format)
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
            candidate.text.take(240),
            style = MiuixTheme.textStyles.footnote2,
            color = if (selected) scheme.onBackground else scheme.onBackgroundVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        MusesTextButton(text = "查看完整歌词", onClick = onPreview)
    }
}

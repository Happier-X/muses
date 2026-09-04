package com.muses.player.feature.scrape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.muses.player.core.ui.components.SaltCover
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors

/**
 * 单曲刮削审核页（Tagger 式「就地审核」全屏页，design §2.3）：
 * 歌曲头 → 搜索词行（改词重搜）→ 文本字段审核（本地值→候选值逐字段勾选 + 候选切换条）→
 * 封面多候选缩略图（点选 + 大图预览）→ 歌词候选（来源+格式角标 + 预览）→ 底部「应用（N）」。
 * 由 SingleScrapeSheet 升级改造而来，旧浮层已退役。
 */
@Composable
fun ScrapeReviewScreen(
    onBack: () -> Unit,
    viewModel: ScrapeReviewViewModel = hiltViewModel(),
    /**
     * S3「应用并下一首」：批量模式审核页写回成功后，宿主先推进 ScrapeViewModel 待审队列
     * （[advanceReview 回调由宿主实现]），再打开下一首审核页。
     * @param writtenSongId 刚写回的歌曲；@param nextSongId 队列中下一首（null = 队列结束）
     */
    onAppliedAndNext: (writtenSongId: String, nextSongId: String?) -> Unit = { _, _ -> },
    /** S3 用户手动返回（非应用路径）：宿主清 ScrapeViewModel 待审队列，不强推下一首 */
    onManualBack: () -> Unit = {},
) {
    val salt = LocalSaltColors.current
    val state by viewModel.state.collectAsState()
    val keyword by viewModel.keyword.collectAsState()

    Column(Modifier.fillMaxSize().background(salt.surface)) {
        SaltReviewNavbar(
            onBack = {
                // 手动返回即清待审队列（S3：不强推下一首）
                onManualBack()
                onBack()
            },
        )

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

// ── 顶栏 ──────────────────────────────────────────────────

@Composable
private fun SaltReviewNavbar(onBack: () -> Unit) {
    val salt = LocalSaltColors.current
    SaltNavbar(
        title = "刮削审核",
        left = {
            SaltIconButton(
                onClick = onBack,
                imageVector = TablerIcons.ArrowBack,
                contentDescription = "返回",
                tint = salt.text,
            )
        },
    )
}

// ── Searching / Writing / Success / Empty 态 ──────────────

@Composable
private fun SearchingContent(songTitle: String?) {
    val salt = LocalSaltColors.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在搜索候选…", fontSize = 13.sp, color = salt.text2)
            if (songTitle != null) {
                Text(songTitle, fontSize = 12.sp, color = salt.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WritingContent() {
    val salt = LocalSaltColors.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在写回…", fontSize = 13.sp, color = salt.text2)
            Text("写入文件与数据库，WebDAV 曲目需数秒", fontSize = 12.sp, color = salt.text2)
        }
    }
}

@Composable
private fun SuccessContent(
    nextSongId: String?,
    onBack: () -> Unit,
    onNext: (String) -> Unit,
) {
    val salt = LocalSaltColors.current
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("已更新", fontSize = 15.sp, color = salt.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (nextSongId != null) {
                // S3 批量模式：应用并下一首
                Button(onClick = { onNext(nextSongId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("应用并下一首")
                }
                Spacer(Modifier.height(8.dp))
            }
            SaltTextButton(text = "返回", onClick = onBack)
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
    val salt = LocalSaltColors.current
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(reason, fontSize = 14.sp, color = salt.text2)
            if (reason == "暂无匹配") {
                Spacer(Modifier.height(4.dp))
                Text("可修改下方搜索词后重新搜索", fontSize = 12.sp, color = salt.text2)
            }
            Spacer(Modifier.height(12.dp))
            SearchKeywordRow(keyword = keyword, viewModel = viewModel)
        }
        Button(
            onClick = { viewModel.search() },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
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
    val salt = LocalSaltColors.current
    var previewCoverUrl by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 歌曲头：本地标题/歌手/专辑 + 封面小图
            item(key = "song-head") { SongHead(state) }
            // 搜索词行：三输入 + 重新搜索
            item(key = "keyword") { SearchKeywordRow(keyword = keyword, viewModel = viewModel) }

            // 文本字段审核区（逐字段 Checkbox + 本地值 → 候选值 + 来源/推荐角标）
            item(key = "text-fields") {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("应用字段：", fontSize = 12.sp, color = salt.text2)
                        Spacer(Modifier.weight(1f))
                        SaltTextButton(text = "全选", onClick = {
                            selectableFields(state).forEach { field ->
                                if (field !in state.checkedFields) viewModel.toggleField(field)
                            }
                        })
                        SaltTextButton(text = "全不选", onClick = {
                            state.checkedFields.toList().forEach { viewModel.toggleField(it) }
                        })
                    }
                    Spacer(Modifier.height(4.dp))
                    val hit = state.selectedHit
                    val recommended = state.selectedTextIndex == state.text.defaultIndex
                    FieldCheckRow(
                        label = "标题",
                        checked = "title" in state.checkedFields,
                        enabled = state.resolvedTitle() != null,
                        onCheckedChange = { viewModel.toggleField("title") },
                        original = state.song.title,
                        updated = state.resolvedTitle() ?: "—",
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
                    FieldCheckRow(
                        label = "歌手",
                        checked = "artist" in state.checkedFields,
                        enabled = state.resolvedArtist() != null,
                        onCheckedChange = { viewModel.toggleField("artist") },
                        original = state.song.artist ?: "—",
                        updated = state.resolvedArtist() ?: "—",
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
                    FieldCheckRow(
                        label = "专辑",
                        checked = "album" in state.checkedFields,
                        enabled = state.resolvedAlbum() != null,
                        onCheckedChange = { viewModel.toggleField("album") },
                        original = state.song.album ?: "—",
                        updated = state.resolvedAlbum() ?: "—",
                        sourceBadge = hit?.source?.wire,
                        recommended = recommended,
                    )
                    TextFieldEditOverrides(state = state, viewModel = viewModel)
                }
            }

            // 文本候选切换条：横向 chip（源 + 标题），当前选中高亮
            if (state.text.items.isNotEmpty()) {
                item(key = "text-candidates") { TextCandidateStrip(state = state, viewModel = viewModel) }
            }

            // 封面区：Checkbox + 横向候选缩略图（点选，再点已选项弹大图预览）
            item(key = "cover") {
                CoverSection(state = state, viewModel = viewModel, onPreview = { previewCoverUrl = it })
            }

            // 歌词区：Checkbox + 候选列表（来源+format 角标）+ 预览
            item(key = "lyrics") { LyricsSection(state = state, viewModel = viewModel) }
        }

        // 底部「应用（N）」：仅写回勾选字段；无勾选 disabled（写回安全语义）
        Button(
            onClick = { viewModel.apply() },
            enabled = state.checkedFields.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        ) {
            Text("应用" + if (state.checkedFields.isNotEmpty()) "（${state.checkedFields.size}）" else "")
        }
    }

    // 封面大图预览（全屏 Dialog，点击关闭）
    previewCoverUrl?.let { url ->
        Dialog(
            onDismissRequest = { previewCoverUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { previewCoverUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = "封面大图预览",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
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

/** 歌曲头：本地标题 · 歌手 · 专辑 + 封面小图（「本地值」列的数据来源快照） */
@Composable
private fun SongHead(state: ScrapeReviewState.Review) {
    val salt = LocalSaltColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SaltCover(uri = state.song.coverUri, size = 48.dp, radius = SaltCoverRadius.SM)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = salt.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(state.song.artist ?: "未知艺术家", state.song.album ?: "未知专辑").joinToString(" · "),
                fontSize = 12.sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 搜索词行：title/artist/album 三输入（默认预填本地值）+ 「重新搜索」 */
@Composable
private fun SearchKeywordRow(keyword: ReviewKeyword, viewModel: ScrapeReviewViewModel) {
    Column {
        OutlinedTextField(
            value = keyword.title,
            onValueChange = viewModel::updateKeywordTitle,
            label = { Text("标题") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = keyword.artist,
                onValueChange = viewModel::updateKeywordArtist,
                label = { Text("歌手") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = keyword.album,
                onValueChange = viewModel::updateKeywordAlbum,
                label = { Text("专辑") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SaltTextButton(
                text = "重新搜索",
                onClick = { viewModel.search() },
                enabled = !keyword.titleBlank,
            )
        }
    }
}

/**
 * 字段审核行（FieldCheckRow 自 SingleScrapeSheet 迁移改造）：
 * Checkbox + 「本地值 → 候选值」+ 来源角标 + 推荐角标。
 * 注：EditCloudMetaSearch 不提供 per-hit 置信度（引擎零改动约束，D2），推荐候选（defaultIndex）
 * 以「推荐」角标置于原置信度角标位置。
 */
@Composable
private fun FieldCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    original: String,
    updated: String,
    sourceBadge: String? = null,
    recommended: Boolean = false,
) {
    val salt = LocalSaltColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = salt.primary),
        )
        Text(
            buildString {
                append(label).append("：")
                if (updated != original) {
                    append(original).append(" → ").append(updated)
                } else {
                    append(original)
                }
            },
            fontSize = 12.sp,
            color = if (checked) salt.text else salt.text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        if (sourceBadge != null) {
            BadgeBox(text = sourceBadge)
            Spacer(Modifier.size(4.dp))
        }
        if (recommended) {
            Box(
                Modifier
                    .background(salt.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text("推荐", fontSize = 9.sp, color = salt.primary)
            }
        }
    }
}

/** 逐字段手改覆写（迁移自 SingleScrapeSheet 的编辑区；歌词覆写由歌词候选选择区承担） */
@Composable
private fun TextFieldEditOverrides(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel) {
    var expanded by remember { mutableStateOf(false) }
    SaltTextButton(text = if (expanded) "收起编辑" else "编辑", onClick = { expanded = !expanded })
    if (expanded) {
        var title by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedTitle() ?: state.song.title) }
        var artist by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedArtist() ?: state.song.artist.orEmpty()) }
        var album by remember(state.selectedTextIndex) { mutableStateOf(state.resolvedAlbum() ?: state.song.album.orEmpty()) }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("标题覆写") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("歌手覆写") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text("专辑覆写") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SaltTextButton(text = "应用编辑", onClick = {
                viewModel.updateEditTitle(title)
                viewModel.updateEditArtist(artist)
                viewModel.updateEditAlbum(album)
                expanded = false
            })
        }
    }
}

/** 文本候选切换条：横向 chip 列出 text.items（源 wire 值 + 标题），当前选中高亮 */
@Composable
private fun TextCandidateStrip(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel) {
    val salt = LocalSaltColors.current
    Column {
        Text("文本候选（${state.text.items.size}）", fontSize = 12.sp, color = salt.text2)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.text.items) { index, hit ->
                val selected = index == state.selectedTextIndex
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) salt.primary.copy(alpha = 0.12f) else salt.surface1)
                        .border(1.dp, if (selected) salt.primary else salt.surface2, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectTextCandidate(index) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(hit.source.wire, fontSize = 10.sp, color = salt.primary, fontWeight = FontWeight.SemiBold)
                        if (index == state.text.defaultIndex) {
                            Spacer(Modifier.width(4.dp))
                            Text("推荐", fontSize = 9.sp, color = salt.text2)
                        }
                    }
                    Text(
                        hit.title ?: "（无标题）",
                        fontSize = 12.sp,
                        color = if (selected) salt.text else salt.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(96.dp),
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
    val salt = LocalSaltColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = "cover" in state.checkedFields,
                onCheckedChange = { viewModel.toggleField("cover") },
                enabled = state.cover.items.isNotEmpty(),
                colors = CheckboxDefaults.colors(checkedColor = salt.primary),
            )
            Text("封面", fontSize = 12.sp, color = if ("cover" in state.checkedFields) salt.text else salt.text2)
            Spacer(Modifier.weight(1f))
            Text("候选 ${state.cover.items.size}", fontSize = 11.sp, color = salt.text2)
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 本地封面（对比用，不可选中）
            item(key = "local") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SaltCover(uri = state.song.coverUri, size = 72.dp, radius = SaltCoverRadius.SM)
                    Spacer(Modifier.height(2.dp))
                    Text("本地", fontSize = 9.sp, color = salt.text2)
                }
            }
            itemsIndexed(state.cover.items) { index, candidate ->
                val selected = index == state.selectedCoverIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(salt.surface2)
                            .border(
                                width = if (selected) 2.dp else 0.5.dp,
                                color = if (selected) salt.primary else salt.surface2,
                                shape = RoundedCornerShape(6.dp),
                            )
                            .clickable {
                                if (selected) {
                                    // 再点已选项 → 大图预览
                                    onPreview(candidate.remoteUrl)
                                } else {
                                    viewModel.selectCover(index)
                                }
                            },
                    ) {
                        AsyncImage(
                            model = candidate.remoteUrl,
                            contentDescription = "封面候选 ${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(candidate.source.wire, fontSize = 9.sp, color = if (selected) salt.primary else salt.text2)
                }
            }
        }
    }
}

/** 歌词区：Checkbox + 候选列表（来源 + format 角标）+ 预览前几行 */
@Composable
private fun LyricsSection(state: ScrapeReviewState.Review, viewModel: ScrapeReviewViewModel) {
    val salt = LocalSaltColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = "lyrics" in state.checkedFields,
                onCheckedChange = { viewModel.toggleField("lyrics") },
                enabled = state.lyrics.items.isNotEmpty(),
                colors = CheckboxDefaults.colors(checkedColor = salt.primary),
            )
            Text("歌词", fontSize = 12.sp, color = if ("lyrics" in state.checkedFields) salt.text else salt.text2)
            Spacer(Modifier.weight(1f))
            Text("候选 ${state.lyrics.items.size}", fontSize = 11.sp, color = salt.text2)
        }
        if (state.lyrics.items.isEmpty()) {
            Text("未命中歌词候选", fontSize = 12.sp, color = salt.text2, modifier = Modifier.padding(start = 44.dp))
        } else {
            state.lyrics.items.forEachIndexed { index, candidate ->
                LyricsCandidateRow(
                    index = index,
                    candidate = candidate,
                    selected = index == state.selectedLyricsIndex,
                    onClick = { viewModel.selectLyrics(index) },
                )
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
) {
    val salt = LocalSaltColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 44.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) salt.primary.copy(alpha = 0.12f) else salt.surface1)
            .border(1.dp, if (selected) salt.primary else salt.surface2, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", fontSize = 11.sp, color = if (selected) salt.primary else salt.text2, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            BadgeBox(text = candidate.source)
            Spacer(Modifier.size(4.dp))
            BadgeBox(text = candidate.format)
        }
        Spacer(Modifier.height(4.dp))
        // 预览前几行（时间轴行可能很长，只取前 90 字符）
        Text(
            candidate.text.take(90).replace('\n', ' '),
            fontSize = 11.sp,
            color = if (selected) salt.text else salt.text2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BadgeBox(text: String) {
    val salt = LocalSaltColors.current
    Box(
        Modifier
            .background(salt.surface2, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(text, fontSize = 9.sp, color = salt.text2)
    }
}

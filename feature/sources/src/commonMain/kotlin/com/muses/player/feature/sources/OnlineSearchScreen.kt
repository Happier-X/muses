package com.muses.player.feature.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesIconButtonSize
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.icons.TablerIcons
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 在线搜索页（独立页面）。
 *
 * 交互：输入关键词 → 5 平台并行搜索 → 按平台分组展示 → 点结果直接播放。
 * 部分平台失败不影响其它平台（失败原因就地展示在该平台分组下）。
 */
@Composable
fun OnlineSearchScreen(
    onBack: () -> Unit,
    viewModel: OnlineSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val availableQualities by viewModel.availableQualities.collectAsState()
    val preferredQuality by viewModel.preferredQuality.collectAsState()
    val scheme = MiuixTheme.colorScheme

    // 首次进入即汇总脚本声明的音质档位（决定音质行展示哪些选项）
    LaunchedEffect(Unit) { viewModel.refreshAvailableQualities() }

    Scaffold(
        topBar = {
            MusesTopBar(
                title = "在线搜索",
                largeTitle = "在线搜索",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── 搜索输入行 ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MusesTextField(
                    value = state.keyword,
                    onValueChange = viewModel::updateKeyword,
                    modifier = Modifier.weight(1f),
                    label = "搜索歌曲、歌手",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                )
                Spacer(Modifier.width(8.dp))
                MusesButton(
                    onClick = { viewModel.search() },
                    enabled = !state.searching,
                ) {
                    Text(if (state.searching) "搜索中" else "搜索")
                }
            }

            // ── 平台筛选胶囊 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlatformPill(
                    label = "全部",
                    selected = state.filterPlatform == null,
                    onClick = { viewModel.setFilter(null) },
                )
                state.platforms.forEach { p ->
                    PlatformPill(
                        label = p.displayName,
                        selected = state.filterPlatform == p.platform,
                        // 该平台有结果时显示条数，便于一眼看出哪个平台可用
                        badge = p.results.size.takeIf { it > 0 }?.toString(),
                        onClick = { viewModel.setFilter(p.platform) },
                    )
                }
            }

            // ── 音质选择（仅列出已加载脚本实际声明的档位）──
            if (availableQualities.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "音质",
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary,
                    )
                    availableQualities.forEach { q ->
                        PlatformPill(
                            label = q.label,
                            selected = preferredQuality == q.key,
                            onClick = { viewModel.setPreferredQuality(q.key) },
                        )
                    }
                }
                // 高音质档提示：可能返回加密容器（如酷我 .mflac/.mgg）或播放器不支持的编码
                if (LxQuality.fromKey(preferredQuality)?.isHighTier == true) {
                    Text(
                        text = "高音质档取决于音源脚本能力，可能因加密容器/编码不受支持而失败。",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
            }

            // 提示横幅（如「该平台无可用脚本」）：点按可关闭
            state.message?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.errorContainer)
                        .clickable { viewModel.clearMessage() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TablerIcons.Info,
                        contentDescription = null,
                        tint = scheme.onErrorContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = scheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = TablerIcons.Close,
                        contentDescription = "关闭提示",
                        tint = scheme.onErrorContainer,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                state.searching && state.totalResults == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.searched && state.totalResults == 0 -> {
                    MusesEmpty(
                        title = "没有找到相关歌曲",
                        description = "换个关键词试试；部分平台可能需要网络可达。",
                        icon = TablerIcons.Search,
                    )
                }
                !state.searched -> {
                    MusesEmpty(
                        title = "在线搜索",
                        description = "输入关键词，从酷我/QQ/网易云/酷狗/咪咕同时搜索。\n播放需要先导入可用的音源脚本。",
                        icon = TablerIcons.Search,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp + com.muses.player.core.ui.theme.LocalBottomChromePadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.visiblePlatforms.forEach { platform ->
                            item(key = "hdr-${platform.platform}") {
                                PlatformSectionHeader(platform)
                            }
                            items(
                                items = platform.results,
                                key = { "${platform.platform}-${it.songId}" },
                            ) { result ->
                                val index = platform.results.indexOf(result)
                                SearchResultRow(
                                    result = result,
                                    onClick = { viewModel.playResult(platform.platform, index) },
                                )
                            }
                            if (platform.hasMore) {
                                item(key = "more-${platform.platform}") {
                                    LoadMoreRow(
                                        loading = platform.loadingMore,
                                        onClick = { viewModel.loadMore(platform.platform) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 平台筛选胶囊 */
@Composable
private fun PlatformPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = if (badge != null) "$label $badge" else label,
            fontSize = 13.sp,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 平台分组标题（含加载/错误态） */
@Composable
private fun PlatformSectionHeader(platform: PlatformSearchState) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = platform.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
        )
        when {
            platform.loading -> {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(13.dp))
            }
            platform.error != null -> {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = platform.error,
                    fontSize = 12.sp,
                    color = scheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            platform.results.isNotEmpty() -> {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${platform.results.size} 首",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 单条搜索结果 */
@Composable
private fun SearchResultRow(
    result: OnlineSearchResult,
    onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(result.artist, result.album)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            result.durationMs?.takeIf { it > 0 }?.let { ms ->
                Text(
                    text = formatDuration(ms),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = TablerIcons.Play,
                contentDescription = "播放",
                modifier = Modifier.size(17.dp),
                tint = scheme.primary,
            )
        }
    }
}

/** 加载更多 */
@Composable
private fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
        } else {
            Text(
                text = "加载更多",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 毫秒 → m:ss */
private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

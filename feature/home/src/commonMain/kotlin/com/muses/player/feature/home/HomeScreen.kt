package com.muses.player.feature.home

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
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
import com.muses.player.core.ai.AiRecommendedTrack
import com.muses.player.core.search.OnlineChart
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesTextField
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.components.SongListItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页：顶部搜索框 + 排行榜 + 猜你喜欢。
 *
 * 交互约定：
 * - 搜索框只负责**收集关键词并跳转**在线搜索页（搜索 UI/状态机在 :feature:sources，不重复造）；
 * - 排行榜：平台胶囊 → 榜单胶囊 → 点歌即播（榜单歌曲与搜索结果同构，走同一条播放链路）；
 * - 猜你喜欢：AI 读曲库画像出「歌名+歌手」，再回平台精确匹配；未启用/未配置时给明确入口。
 */
@Composable
fun HomeScreen(
    /** 携带关键词跳到在线搜索页（空串 = 只进页面不搜） */
    onOpenOnlineSearch: (String) -> Unit,
    /** 跳设置页（AI 推荐配置入口） */
    onOpenAiSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val topBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val chart = state.chart
    val recommend = state.recommend
    val platformNames = chart.platformNames

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        topBar = {
            MusesTopBar(
                title = "首页",
                largeTitle = "首页",
                scrollBehavior = topBarScrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp + LocalBottomChromePadding.current,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ── 搜索框 ──
            item(key = "search") {
                SearchRow(
                    keyword = state.keyword,
                    onKeywordChange = viewModel::updateKeyword,
                    onSubmit = { onOpenOnlineSearch(state.keyword.trim()) },
                )
            }

            // 一次性提示（如「该平台没有可用音源脚本」）
            state.message?.let { message ->
                item(key = "message") {
                    HomeBanner(text = message, onClose = viewModel::clearMessage)
                }
            }

            // ── 排行榜 ──
            item(key = "chart-title") { SmallTitle(text = "排行榜") }

            if (chart.platforms.isNotEmpty()) {
                item(key = "chart-platforms") {
                    PillRow {
                        chart.platforms.forEach { platform ->
                            HomePill(
                                label = platformNames[platform] ?: platform,
                                selected = chart.selectedPlatform == platform,
                                onClick = { viewModel.selectPlatform(platform) },
                            )
                        }
                    }
                }
            }
            if (chart.charts.isNotEmpty()) {
                item(key = "chart-tabs") {
                    PillRow {
                        chart.charts.forEach { item: OnlineChart ->
                            HomePill(
                                label = item.name,
                                selected = chart.selectedChartId == item.chartId,
                                onClick = { viewModel.selectChart(item.chartId) },
                            )
                        }
                    }
                }
            }

            when {
                chart.loadingCharts || chart.loadingSongs -> item(key = "chart-loading") {
                    LoadingRow()
                }
                chart.error != null -> item(key = "chart-error") {
                    HomeBanner(text = chart.error, onClose = null)
                }
                chart.songs.isEmpty() -> item(key = "chart-empty") {
                    MusesEmpty(
                        title = "暂无榜单数据",
                        description = "换个平台或稍后重试；榜单接口偶发波动。",
                        icon = TablerIcons.QueueMusic,
                    )
                }
                else -> itemsIndexed(
                    items = chart.songs,
                    key = { _, song: OnlineSearchResult -> "chart-${song.platform}-${song.songId}" },
                ) { index, song ->
                    SongListItem(
                        song = SongItem(
                            id = "${song.platform}-${song.songId}",
                            title = song.name,
                            artist = song.artist,
                            albumTitle = song.album,
                        ),
                        isCurrent = false,
                        onClick = { viewModel.playChartSong(index) },
                    )
                }
            }

            // ── 猜你喜欢 ──
            item(key = "recommend-title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { SmallTitle(text = "猜你喜欢") }
                    if (recommend.enabled && recommend.configured) {
                        MusesIconButton(onClick = { viewModel.refreshRecommend() }) {
                            Icon(
                                imageVector = TablerIcons.Refresh,
                                contentDescription = "换一批推荐",
                                tint = scheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            when {
                !recommend.enabled -> item(key = "rec-off") {
                    AiHintCard(
                        title = "用 AI 根据你的曲库推荐",
                        description = "读取本地/WebDAV 曲库的歌手与专辑偏好（统计 + 抽样），" +
                            "由你配置的 AI 服务推荐歌曲，再回各平台匹配可播放的曲目。",
                        actionLabel = "去开启",
                        onAction = onOpenAiSettings,
                    )
                }
                !recommend.configured -> item(key = "rec-unconfigured") {
                    AiHintCard(
                        title = "还差一步：配置 AI 服务",
                        description = "选择服务商（DeepSeek / Kimi / 智谱 / 通义 / 自定义）并填入 API Key，" +
                            "Key 会加密保存在本机。",
                        actionLabel = "去配置",
                        onAction = onOpenAiSettings,
                    )
                }
                recommend.loading -> item(key = "rec-loading") {
                    LoadingRow(text = "AI 正在读你的曲库…")
                }
                recommend.error != null && recommend.result == null -> item(key = "rec-error") {
                    Column {
                        HomeBanner(text = recommend.error, onClose = null)
                        Spacer(Modifier.height(6.dp))
                        MusesButton(onClick = { viewModel.refreshRecommend() }) { Text("重试") }
                    }
                }
                recommend.result != null -> {
                    val result = recommend.result
                    item(key = "rec-summary") {
                        Text(
                            text = buildString {
                                append("AI 推荐 ${result.suggested} 首，匹配到 ${result.matched} 首")
                                if (result.unmatched.isNotEmpty()) {
                                    append("（${result.unmatched.size} 首在各平台未找到，已丢弃）")
                                }
                            },
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariantSummary,
                        )
                    }
                    if (result.tracks.isEmpty()) {
                        item(key = "rec-empty") {
                            MusesEmpty(
                                title = "这次没匹配到可播放的推荐",
                                description = "AI 有时会给出平台搜不到的曲目；点右上角刷新换一批。",
                                icon = TablerIcons.Refresh,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = result.tracks,
                            key = { _, track: AiRecommendedTrack -> "rec-${track.result.platform}-${track.result.songId}" },
                        ) { index, track ->
                            RecommendRow(
                                index = index,
                                track = track,
                                platformLabel = track.result.platform.let { platformNames[it] ?: it },
                                onClick = { viewModel.playRecommend(index) },
                            )
                        }
                    }
                }
            }

            item(key = "bottom-space") { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/** 搜索行：输入 + 回车/按钮 → 跳在线搜索页（带关键词） */
@Composable
private fun SearchRow(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusesTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.weight(1f),
            label = "搜索歌曲、歌手",
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        Spacer(Modifier.width(8.dp))
        MusesButton(onClick = onSubmit) { Text("搜索") }
    }
}

/** 胶囊行（横向滚动）：平台筛选与榜单切换共用 */
@Composable
private fun PillRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/**
 * 胶囊按钮（视觉与在线搜索页的 PlatformPill 同口径）。
 *
 * 未提到 ui-shared 共用：两处形态一致但归属不同 feature，先就地实现；
 * 若第三处出现再上收（避免过早抽象）。
 */
@Composable
private fun HomePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
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
            text = label,
            fontSize = 13.sp,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** AI 推荐行：序号 + 歌名 +（歌手 · 推荐理由）+ 平台标签 */
@Composable
private fun RecommendRow(
    index: Int,
    track: AiRecommendedTrack,
    platformLabel: String,
    onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            fontSize = 13.sp,
            color = scheme.onSurfaceVariantSummary,
            modifier = Modifier.width(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = track.result.name,
                fontSize = 14.sp,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(
                track.result.artist?.takeIf { it.isNotBlank() },
                track.suggestion.reason?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = platformLabel,
            fontSize = 11.sp,
            color = scheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** AI 未启用/未配置时的引导卡片 */
@Composable
private fun AiHintCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 14.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(text = description, fontSize = 12.sp, color = scheme.onSurfaceVariantSummary)
            Spacer(Modifier.height(10.dp))
            MusesButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** 提示横幅；[onClose] 非空时可点关闭 */
@Composable
private fun HomeBanner(text: String, onClose: (() -> Unit)?) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.errorContainer)
            .then(if (onClose != null) Modifier.clickable(onClick = onClose) else Modifier)
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
            text = text,
            fontSize = 12.sp,
            color = scheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            Icon(
                imageVector = TablerIcons.Close,
                contentDescription = "关闭提示",
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/** 加载态行 */
@Composable
private fun LoadingRow(text: String = "") {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        if (text.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Text(text = text, fontSize = 12.sp, color = scheme.onSurfaceVariantSummary)
        }
    }
}

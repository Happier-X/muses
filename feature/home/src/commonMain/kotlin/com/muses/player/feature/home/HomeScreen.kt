package com.muses.player.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ai.AiRecommendedTrack
import com.muses.player.core.search.OnlineChart
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.components.SongListItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowListPopup
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 探索（原「首页」）：排行榜 + 猜你喜欢。
 *
 * 交互约定：
 * - 排行榜：右上角选择有可用 LX 脚本的平台，点击榜单卡片进入歌曲子页；
 * - 猜你喜欢：AI 读曲库画像出「歌名+歌手」，再回平台精确匹配；未启用/未配置时给明确入口。
 */
@Composable
fun HomeScreen(
    /** 跳设置页（AI 推荐总开关入口） */
    onOpenAiSettings: () -> Unit,
    /** 跳 AI 服务二级页（地址/模型/Key 配置入口） */
    onOpenAiConfig: () -> Unit,
    /** 打开指定排行榜歌曲页 */
    onOpenChart: (platform: String, chartId: String, chartName: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val chart = state.chart
    val recommend = state.recommend
    val platformNames = chart.platformNames
    var showPlatformPopup by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "探索",
                actions = {
                    MusesIconButton(
                        onClick = { viewModel.loadCharts(forceRefresh = true) },
                        imageVector = TablerIcons.Refresh,
                        contentDescription = "刷新排行榜",
                        enabled = !chart.loadingCharts && !chart.refreshingCharts,
                    )
                    if (chart.platforms.isNotEmpty()) {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            Button(
                                onClick = { showPlatformPopup = true },
                                minWidth = 0.dp,
                                minHeight = 40.dp,
                                insideMargin = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(),
                            ) {
                                Text(platformNames[chart.selectedPlatform] ?: chart.selectedPlatform.orEmpty())
                                Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            WindowListPopup(
                                show = showPlatformPopup,
                                alignment = PopupPositionProvider.Align.End,
                                enableWindowDim = true,
                                onDismissRequest = { showPlatformPopup = false },
                            ) {
                                ListPopupColumn {
                                    chart.platforms.forEachIndexed { index, platform ->
                                        DropdownImpl(
                                            text = platformNames[platform] ?: platform,
                                            optionSize = chart.platforms.size,
                                            isSelected = platform == chart.selectedPlatform,
                                            index = index,
                                            onSelectedIndexChange = {
                                                viewModel.selectPlatform(platform)
                                                showPlatformPopup = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
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
            // 一次性提示（如「该平台没有可用音源脚本」）
            state.message?.let { message ->
                item(key = "message") {
                    HomeBanner(text = message, onClose = viewModel::clearMessage)
                }
            }

            // ── 排行榜 ──
            item(key = "chart-title") { SmallTitle(text = "排行榜", insideMargin = SectionTitleMargin) }

            when {
                chart.loadingCharts && chart.charts.isEmpty() -> item(key = "chart-loading") {
                    LoadingRow()
                }
                chart.error != null -> item(key = "chart-error") {
                    HomeBanner(text = chart.error, onClose = null)
                }
                chart.charts.isEmpty() -> item(key = "chart-empty") {
                    MusesEmpty(title = "空空如也~")
                }
                else -> item(key = "chart-cards") {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val cardWidth = ((maxWidth - 24.dp) / 3.15f).coerceAtMost(164.dp)
                        val orderedCharts = chart.charts.chunked(6).flatMap { group ->
                            (0..2).flatMap { column ->
                                listOfNotNull(group.getOrNull(column), group.getOrNull(column + 3))
                            }
                        }
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth().height((cardWidth + 42.dp) * 2 + 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(orderedCharts, key = { it.chartId }) { chartItem ->
                                ChartCard(
                                    chart = chartItem,
                                    width = cardWidth,
                                    onClick = {
                                        onOpenChart(chartItem.platform, chartItem.chartId, chartItem.name)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ── 猜你喜欢 ──
            item(key = "recommend-title") {
                SmallTitle(text = "猜你喜欢", insideMargin = SectionTitleMargin)
            }

            when {
                !recommend.enabled -> item(key = "rec-off") {
                    AiHintCard(
                        title = "开启 AI 推荐",
                        description = "根据你的曲库偏好发现新歌。",
                        actionLabel = "去开启",
                        onAction = onOpenAiSettings,
                    )
                }
                !recommend.configured -> item(key = "rec-unconfigured") {
                    AiHintCard(
                        title = "配置 AI 服务",
                        description = "填写服务地址、模型和 API Key。",
                        actionLabel = "去配置",
                        onAction = onOpenAiConfig,
                    )
                }
                recommend.loading -> item(key = "rec-loading") {
                    LoadingRow(text = "AI 正在读你的曲库…")
                }
                recommend.error != null && recommend.result == null -> item(key = "rec-error") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HomeBanner(text = recommend.error, onClose = null)
                        Spacer(Modifier.height(6.dp))
                        MusesButton(onClick = { viewModel.refreshRecommend() }) { Text("重试") }
                    }
                }
                recommend.result != null -> {
                    val result = recommend.result
                    if (result.tracks.isEmpty()) {
                        item(key = "rec-empty") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                MusesEmpty(
                                    title = "空空如也~",
                                )
                                MusesButton(onClick = { viewModel.refreshRecommend() }) { Text("重试") }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = result.tracks,
                            key = { _, track: AiRecommendedTrack -> "rec-${track.result.platform}-${track.result.songId}" },
                        ) { index, track ->
                            RecommendRow(
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

/**
 * 区块标题缩进：与列表行内容左对齐（列表行 12dp 内缩 + 页面 16dp 外边距）。
 *
 * 不用 SmallTitle 默认的 28dp：那是配合 Card 内缩的取值，本页列表没有 Card 承载，
 * 沿用默认会让标题比列表内容多缩进 16dp，看起来「没对齐」。
 */
private val SectionTitleMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

private val ChartColors = listOf(
    listOf(Color(0xFFE83967), Color(0xFFFF918B)),
    listOf(Color(0xFF0ACB83), Color(0xFF73E8AA)),
    listOf(Color(0xFF5379A3), Color(0xFF89A6C3)),
    listOf(Color(0xFF5249CD), Color(0xFFAA80F7)),
    listOf(Color(0xFF21B6DD), Color(0xFF7BD9F5)),
    listOf(Color(0xFF48BD4B), Color(0xFFBCE5D6)),
)

@Composable
private fun ChartCard(chart: OnlineChart, width: Dp, onClick: () -> Unit) {
    val scheme = MiuixTheme.colorScheme
    val colors = ChartColors[(chart.chartId.hashCode() and Int.MAX_VALUE) % ChartColors.size]
    Column(modifier = Modifier.width(width).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val shape = Path().apply {
                    moveTo(0f, size.height * 0.62f)
                    lineTo(size.width * 0.35f, size.height * 0.26f)
                    quadraticTo(size.width * 0.5f, size.height * 0.13f, size.width * 0.65f, size.height * 0.3f)
                    lineTo(size.width, size.height * 0.68f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(shape, Color.White.copy(alpha = 0.13f))
            }
            Text(
                text = chart.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(chart.name, fontSize = 13.sp, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = chart.updateInfo.orEmpty(),
            fontSize = 11.sp,
            color = scheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** AI 推荐行：封面 + 歌名 +（歌手 · 推荐理由）+ 平台来源 */
@Composable
private fun RecommendRow(
    track: AiRecommendedTrack,
    platformLabel: String,
    onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .squircleClip(8.dp)
            // 与 SongListItem 同一套官方按压反馈（下沉 + 叠底），避免默认 ripple 的方块灰框
            .pressable(interactionSource = interactionSource, indication = SinkFeedback())
            .background(if (pressed) scheme.surface.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 封面：与榜单/搜索列表同一视觉口径（远程 URL 经 Coil 双端加载，缺失/失败落稳定占位）。
        // 不展示序号：封面已承担行首的视觉锚点，再加序号会拥挤。
        MusesCover(
            uri = track.result.coverUrl,
            size = 44.dp,
            radius = MusesCoverRadius.SM,
            contentDescription = null,
        )
        Spacer(Modifier.width(12.dp))
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
        // 平台名只作来源说明：用次级文字而非彩色胶囊，避免页面上堆满彩色小块
        Text(
            text = platformLabel,
            fontSize = 11.sp,
            color = scheme.onSurfaceVariantSummary,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(text = title, fontSize = 15.sp, color = scheme.onSurface, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                Text(text = description, fontSize = 12.sp, color = scheme.onSurfaceVariantSummary)
            }
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

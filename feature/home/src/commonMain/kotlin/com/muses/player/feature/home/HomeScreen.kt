package com.muses.player.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ai.AiRecommendedTrack
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.ui.components.MusesButton
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.components.SongListItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 探索（原「首页」）：排行榜 + 猜你喜欢。
 *
 * 交互约定：
 * - 排行榜：平台与榜单各一行 miuix TabRow（平台为主、榜单为次，用尺寸拉开层级）→ 点歌即播
 *   （榜单歌曲与搜索结果同构，走同一条播放链路）；
 * - 猜你喜欢：AI 读曲库画像出「歌名+歌手」，再回平台精确匹配；未启用/未配置时给明确入口。
 */
@Composable
fun HomeScreen(
    /** 跳设置页（AI 推荐总开关入口） */
    onOpenAiSettings: () -> Unit,
    /** 跳 AI 服务二级页（地址/模型/Key 配置入口） */
    onOpenAiConfig: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scheme = MiuixTheme.colorScheme
    val chart = state.chart
    val recommend = state.recommend
    val platformNames = chart.platformNames

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "探索",
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

            if (chart.platforms.isNotEmpty()) {
                item(key = "chart-platforms") {
                    TabRow(
                        tabs = chart.platforms.map { platform -> platformNames[platform] ?: platform },
                        selectedTabIndex = chart.platforms.indexOf(chart.selectedPlatform).coerceAtLeast(0),
                        onTabSelected = { index -> viewModel.selectPlatform(chart.platforms[index]) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            if (chart.charts.isNotEmpty()) {
                item(key = "chart-tabs") {
                    TabRow(
                        tabs = chart.charts.map { chartItem -> chartItem.name },
                        selectedTabIndex = chart.charts
                            .indexOfFirst { chartItem -> chartItem.chartId == chart.selectedChartId }
                            .coerceAtLeast(0),
                        onTabSelected = { index -> viewModel.selectChart(chart.charts[index].chartId) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        // 二级选择器：比平台行更矮更窄，形成「平台 > 榜单」的层级差
                        height = 34.dp,
                        cornerRadius = 10.dp,
                        minWidth = 64.dp,
                        maxWidth = 96.dp,
                        itemSpacing = 8.dp,
                    )
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
                            // 榜单歌曲带远程封面（各平台 provider 已补齐；缺失时落占位音符）
                            coverUri = song.coverUrl,
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
                    Box(Modifier.weight(1f)) { SmallTitle(text = "猜你喜欢", insideMargin = SectionTitleMargin) }
                    recommendStatusText(recommend)?.let { status ->
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 12.dp),
                        )
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
                        description = "填写服务地址、模型并填入 API Key，" +
                            "Key 会加密保存在本机。",
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
                                    title = "这次没匹配到可播放的推荐",
                                    description = "AI 有时会给出平台搜不到的曲目，可重试。",
                                    icon = TablerIcons.Refresh,
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

/** 标题行右侧的次级状态：有结果时报数量与丢弃数，未出结果时只提示更新频率 */
private fun recommendStatusText(recommend: RecommendSectionState): String? = when {
    recommend.result != null -> buildString {
        append("今日推荐 ${recommend.result.matched} 首")
        if (recommend.result.unmatched.isNotEmpty()) {
            append("（${recommend.result.unmatched.size} 首未找到）")
        }
    }
    recommend.enabled && recommend.configured -> "每日更新"
    else -> null
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

package com.muses.player.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.muses.player.core.search.OnlinePlaylist
import com.muses.player.core.ui.components.MusesCover
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import com.muses.player.feature.home.resources.Res
import com.muses.player.feature.home.resources.home_classical
import com.muses.player.feature.home.resources.home_pop
import com.muses.player.feature.home.resources.home_soft
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/** 首页只展示横幅与六个入口，歌曲和榜单在各自页面中打开。 */
@Composable
fun HomeScreen(
    onOpenRecommendations: () -> Unit,
    onOpenCharts: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenPlaylist: (platform: String, playlistId: String, title: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadFeaturedPlaylists() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "探索") },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val compact = maxHeight - LocalBottomChromePadding.current < 540.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = if (compact) 8.dp else 12.dp,
                bottom = 24.dp + LocalBottomChromePadding.current,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
        ) {
            item(key = "featured") {
                FeaturedPlaylistCarousel(
                    state.featured, compact,
                    onOpenPlaylist = { onOpenPlaylist(it.platform, it.id, it.title) },
                    onRetry = { viewModel.loadFeaturedPlaylists(forceRefresh = true) },
                )
            }
            item(key = "discovery") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiscoveryCard("猜你喜欢", "每日精选", TablerIcons.Sparkles,
                        listOf(Color(0xFFEEEAF8), Color(0xFFF7F5FC)), Color(0xFF7A68A0),
                        Modifier.weight(1f), compact, onOpenRecommendations)
                    DiscoveryCard("排行榜", "此刻热门", TablerIcons.Chart,
                        listOf(Color(0xFFE7EEF7), Color(0xFFF3F7FC)), Color(0xFF537BA3),
                        Modifier.weight(1f), compact, onOpenCharts)
                    DiscoveryCard("随心听", "随机漫游", TablerIcons.PlayFill,
                        listOf(Color(0xFFF7EBDD), Color(0xFFFCF7EF)), Color(0xFFA8794C),
                        Modifier.weight(1f), compact, viewModel::playRandom)
                }
            }
            item(key = "genres") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GenreCard("古典", "聆听经典\n让时光慢下来", Res.drawable.home_classical,
                        listOf(Color(0xFFF8D9ED), Color(0xFFFFF1F9)), Modifier.weight(1f), compact) { onSearch("古典音乐") }
                    GenreCard("轻音乐", "温柔旋律\n陪你放松片刻", Res.drawable.home_soft,
                        listOf(Color(0xFFFFE4BF), Color(0xFFFFF4E5)), Modifier.weight(1f), compact) { onSearch("轻音乐") }
                    GenreCard("流行", "好心情\n从一首歌开始", Res.drawable.home_pop,
                        listOf(Color(0xFFFFDCC8), Color(0xFFFFF1E9)), Modifier.weight(1f), compact) { onSearch("流行音乐") }
                }
            }
        }
        }
    }
}

/** 与官方列表相同的下沉反馈；截图中的渐变和圆角只用于首页内容卡片。 */
@Composable
private fun Modifier.homeCardClick(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return pressable(interactionSource = interaction, indication = SinkFeedback())
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable
private fun homeCardColors(colors: List<Color>): List<Color> {
    val scheme = MiuixTheme.colorScheme
    return if (scheme.surface.luminance() < 0.5f) {
        colors.map { color -> androidx.compose.ui.graphics.lerp(scheme.surfaceContainerHigh, color, 0.16f) }
    } else colors
}

@Composable
private fun FeaturedPlaylistCarousel(
    state: FeaturedPlaylistState,
    compact: Boolean,
    onOpenPlaylist: (OnlinePlaylist) -> Unit,
    onRetry: () -> Unit,
) {
    val height = if (compact) 164.dp else 224.dp
    if (state.items.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().height(height).squircleClip(24.dp)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .then(if (!state.loading) Modifier.homeCardClick(onRetry) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (state.loading) "正在加载精选歌单…" else "歌单加载失败，点击重试",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        return
    }
    val pager = rememberPagerState(pageCount = { state.items.size })
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val interactionActive by rememberUpdatedState(dragged || pressed)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(pager, lifecycle, state.items.size) {
        if (state.items.size <= 1 || lifecycle != Lifecycle.State.RESUMED) return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            if (!interactionActive && !pager.isScrollInProgress) {
                // 翻页过程中 currentPage 会提前变化，不能用它重启并取消动画。
                // 手动拖动只取消当前翻页子任务，下一轮自动轮播仍继续。
                coroutineScope {
                    launch {
                        pager.animateScrollToPage((pager.settledPage + 1) % pager.pageCount)
                    }.join()
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(height)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { "${state.items[it].platform}:${state.items[it].id}" },
        ) { index ->
            FeaturedPlaylistCard(state.items[index], compact, interaction) { onOpenPlaylist(state.items[index]) }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (compact) 9.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(state.items.size) { index ->
                Box(Modifier.size(if (index == pager.currentPage) 16.dp else 4.dp, 4.dp)
                    .clip(CircleShape).background(MiuixTheme.colorScheme.onSurface.copy(
                        alpha = if (index == pager.currentPage) 0.45f else 0.15f)))
            }
        }
    }
}

@Composable
private fun FeaturedPlaylistCard(
    playlist: OnlinePlaylist, compact: Boolean, interaction: MutableInteractionSource, onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val imageContext = LocalPlatformContext.current
    val darkBackground = scheme.surface.luminance() < 0.5f
    val backgroundRequest = remember(playlist.coverUrl, imageContext) {
        ImageRequest.Builder(imageContext)
            .data(playlist.coverUrl).size(24, 24).precision(Precision.EXACT).build()
    }
    val cardPadding = if (compact) 10.dp else 20.dp
    BoxWithConstraints(
        Modifier.fillMaxSize().squircleClip(24.dp)
            .pressable(interactionSource = interaction, indication = SinkFeedback())
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(scheme.surfaceContainerHigh),
    ) {
        // 低分辨率封面放大后柔化成色彩背景；无需另一次全尺寸解码。
        AsyncImage(backgroundRequest, null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(32.dp, BlurredEdgeTreatment.Unbounded))
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            scheme.surface.copy(alpha = if (darkBackground) 0.86f else 0.72f),
            scheme.surface.copy(alpha = if (darkBackground) 0.68f else 0.50f),
        ))))
        val coverSize = if (compact) 94.dp else ((maxWidth - cardPadding * 2) * 0.38f).coerceIn(96.dp, 176.dp)
        Column(Modifier.padding(cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(TablerIcons.MusicNote, null, Modifier.size(12.dp), tint = scheme.onSurface.copy(alpha = 0.55f))
                Spacer(Modifier.width(5.dp))
                Text("精选歌单 · 网易云音乐", fontSize = 10.sp, color = scheme.onSurface.copy(alpha = 0.55f))
            }
            Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.onSurface.copy(alpha = 0.08f)))
            Spacer(Modifier.height(if (compact) 6.dp else 18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(playlist.title, fontSize = if (compact) 17.sp else 21.sp,
                        lineHeight = if (compact) 23.sp else 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(playlist.creator ?: playlist.trackCount?.let { "$it 首歌曲" } ?: "精选音乐",
                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface.copy(alpha = 0.55f))
                    Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("查看歌单", fontSize = 11.sp, color = scheme.onSurface.copy(alpha = 0.65f))
                        Icon(TablerIcons.ChevronRight, null, Modifier.size(13.dp), tint = scheme.onSurface.copy(alpha = 0.65f))
                    }
                }
                MusesCover(uri = playlist.coverUrl, size = coverSize, contentDescription = "${playlist.title}封面")
            }
            Spacer(Modifier.height(if (compact) 6.dp else 18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.onSurface.copy(alpha = 0.08f)))
        }
    }
}

@Composable
private fun DiscoveryCard(
    title: String, subtitle: String, icon: ImageVector, colors: List<Color>, accent: Color,
    modifier: Modifier, compact: Boolean, onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    val iconColor = if (scheme.surface.luminance() < 0.5f) {
        androidx.compose.ui.graphics.lerp(accent, Color.White, 0.35f)
    } else accent
    Column(
        modifier.heightIn(min = if (compact) 100.dp else 120.dp)
            .squircleClip(20.dp).homeCardClick(onClick)
            .background(Brush.linearGradient(homeCardColors(colors)))
            .padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 15.dp),
    ) {
        Text(title, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 11.sp, color = scheme.onSurfaceVariantSummary,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        // 图标单独占一行，文字放大时卡片随内容增高，避免互相覆盖。
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Icon(icon, null, Modifier.size(24.dp), tint = iconColor)
        }
    }
}

@Composable
private fun GenreCard(
    title: String, subtitle: String, artwork: DrawableResource, colors: List<Color>,
    modifier: Modifier, compact: Boolean, onClick: () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Column(
        modifier.squircleClip(20.dp).homeCardClick(onClick)
            .background(Brush.linearGradient(homeCardColors(colors)))
            .padding(horizontal = if (compact) 14.dp else 12.dp, vertical = if (compact) 6.dp else 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(artwork), null,
            Modifier.fillMaxWidth().aspectRatio(1f).squircleClip(14.dp), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        Text(title, fontSize = if (compact) 14.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Text(subtitle, fontSize = if (compact) 10.sp else 11.sp, lineHeight = if (compact) 14.sp else 18.sp,
            color = scheme.onSurface.copy(alpha = 0.55f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

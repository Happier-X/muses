package com.muses.player.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import com.muses.player.core.ui.icons.LucideIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.feature.player.backdrop.FlowingLightBackdrop
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.feature.player.lyric.LyricsPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 沉浸式播放页 —— 一比一复刻 Capacitor PlayerPage.vue
 *
 * 复刻契约（BEM → Compose 映射，保留原类名以便对照）：
 * - .player-page__drag-layer：translateY(dragOffsetY) + is-dragging，无过渡跟手，松手 0.22s easeOut 回弹
 * - .player-page__bg / BackgroundRender：album/flowSpeed/hasLyric + fallback-background，opacity 0.75，粘性封面
 * - .player-page__song-head--fixed：标题/艺术家常驻（手机面板外），平板由 --in-panel 承担
 * - .panels：width 200% → translateX(-activePanel*50%)，0.22s easeOut，info-panel / lyric-panel 各 50%（平板收缩为 100% + 左右双栏）
 * - .player-page__cover-hero：aspect-ratio 1 正方形，max-height min(50vh,420px)，圆角 12
 * - .player-page__meta-window：displayedWindow 五行，当前行 scale 1.05 / 0.92，79px 视口，translateY -29.5 居中
 * - .progress-range + .player-page__time-row：m-range + 时间行 + bufferHint（缓冲中）
 * - .controls：三键 lg（48/28），.mode-bar 四键 md（40/20）max-w 320，无 is-active
 * - LyricPlayer：lyric-lines/current-time/align center 0.5 enableBlur/enableScale/wordFadeWidth 0.5，Fab 组 3s idle 隐藏
 * - .player-page__bottom-bar：平板全宽控制条（进度全宽 + 三段式按钮 spaceBetween）
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onOpenQueue: () -> Unit = {},
    onOpenEditMeta: () -> Unit = {},
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val parsedLines by viewModel.parsedLines.collectAsStateWithLifecycle()
    val lyricsDocument by viewModel.lyricsDocument.collectAsStateWithLifecycle()
    val hasTranslation by viewModel.hasTranslation.collectAsStateWithLifecycle()
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val lyricPosition by viewModel.lyricPosition.collectAsStateWithLifecycle()
    // 卡拉OK 逐词渐变需要逐帧位置：VM 的 ~100ms 轮询值作为锚点，UI 每帧线性外推
    val lyricPositionProvider = rememberLyricPositionProvider(
        positionFlow = viewModel.lyricPosition,
        isPlaying = isPlaying,
    )
    val stickyCover by viewModel.stickyCover.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isTabletLayout = remember(configuration) {
        configuration.screenWidthDp >= 768 && configuration.screenHeightDp < configuration.screenWidthDp
    }
    val isNarrowHeight = remember(configuration) { configuration.screenHeightDp <= 520 }

    val title = currentMediaItem?.mediaMetadata?.title?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: ""
    val artist = currentMediaItem?.mediaMetadata?.artist?.toString()?.trim() ?: ""
    val hasSong = currentMediaItem != null && title.isNotEmpty()

    // 拖动层状态（对齐 PlayerPage.vue drag-layer）
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingVertically by remember { mutableStateOf(false) }
    // 歌词面板是否激活：垂直下滑仅 info-panel 生效（对齐 canStartVerticalDismiss → isLyricPanelTarget）
    var isLyricPanelActive by remember { mutableStateOf(false) }
    var isLyricAtTop by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // 回弹动画：0.22s easeOut（motion-v easeOut ≈ CubicBezier(0,0,0.58,1)）
    val reboundEasing = remember { CubicBezierEasing(0f, 0f, 0.58f, 1f) }

    fun startRebound(from: Float) {
        if (from <= 0f) return
        scope.launch {
            val anim = androidx.compose.animation.core.Animatable(from)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = reboundEasing),
            ) { dragOffsetY = value }
            dragOffsetY = 0f
            isDraggingVertically = false
        }
    }

    fun clearDragImmediate() {
        dragOffsetY = 0f
        isDraggingVertically = false
    }

    // 下滑阈值：96~160，取 0.18*height 的 clamp（对齐 getDismissThreshold）
    val dismissThresholdPx = with(density) {
        val h = configuration.screenHeightDp.dp.toPx()
        (h * 0.18f).coerceIn(96.dp.toPx(), 160.dp.toPx())
    }

    // 外层：m-popup 背景透明（对齐 .player-page__popup background: transparent !important）——
    // 无 scrim 黑化，drag-layer 下滑时直接漏出底下列表（原版 1:1）
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 用 offset 而非 graphicsLayer.translationY 做下滑跟手位移：
                // offset 无独立 RenderNode，绘制指令并入窗口根 display list，位移每帧
                // 触发全窗口重绘 → 下滑暴露的新区域必然重绘底层主页面列表；
                // graphicsLayer 纯位移只做合成、暴露区不重绘（露窗口底色，绿屏实验证实）
                .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                .background(Color(0xFF05070D))
                .then(
                    if (isLyricPanelActive && !isLyricAtTop) Modifier
                    else Modifier.pointerInput(isTabletLayout, dismissThresholdPx) {
                        var accumulatedY = 0f
                        var ignoreDrag = false
                        val bottomExclusionPx = with(density) { 180.dp.toPx() }
                        detectVerticalDragGestures(
                            onDragStart = { offset: Offset ->
                                // 底部模式/控制区（约 180dp）不参与下滑关闭，避免与 WebView 底部按钮点击冲突
                                if (offset.y > size.height - bottomExclusionPx) {
                                    ignoreDrag = true
                                    isDraggingVertically = false
                                } else {
                                    ignoreDrag = false
                                    accumulatedY = 0f
                                    isDraggingVertically = true
                                }
                            },
                            onVerticalDrag = { _: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                if (ignoreDrag) return@detectVerticalDragGestures
                                if (dragAmount > 0f || accumulatedY > 0f) {
                                    accumulatedY = (accumulatedY + dragAmount).coerceAtLeast(0f)
                                    dragOffsetY = accumulatedY
                                }
                            },
                            onDragEnd = {
                                if (ignoreDrag) { ignoreDrag = false; return@detectVerticalDragGestures }
                                isDraggingVertically = false
                                if (accumulatedY >= dismissThresholdPx) { clearDragImmediate(); onClose() }
                                else if (accumulatedY > 0f) { val from = accumulatedY; scope.launch { val anim = androidx.compose.animation.core.Animatable(from); anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }; dragOffsetY = 0f } }
                                accumulatedY = 0f
                            },
                            onDragCancel = {
                                if (ignoreDrag) { ignoreDrag = false; return@detectVerticalDragGestures }
                                isDraggingVertically = false
                                if (accumulatedY > 0f) { val from = accumulatedY; scope.launch { val anim = androidx.compose.animation.core.Animatable(from); anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }; dragOffsetY = 0f } }
                                accumulatedY = 0f
                            },
                        )
                    }
                )
        ) {
            // 原生重构：FlowingLightBackdrop + 手机/平板双形态（无 WebView）
            FlowingLightBackdrop(
                coverUri = stickyCover,
                hasLyric = parsedLines.isNotEmpty(),
                modifier = Modifier.fillMaxSize(),
                flowSpeed = 2f,
            )
            var activePanel by remember { mutableStateOf(0) }
            LaunchedEffect(activePanel) { isLyricPanelActive = activePanel == 1 }
            if (isTabletLayout) {
                TabletImmersiveLayout(
                    title = title,
                    artist = artist,
                    coverUri = stickyCover,
                    lines = parsedLines,
                    lyricsDocument = lyricsDocument,
                    lyricPosition = lyricPosition,
                    lyricPositionProvider = lyricPositionProvider,
                    hasTranslation = hasTranslation,
                    translationEnabled = translationEnabled,
                    onToggleTranslation = { viewModel.toggleTranslation() },
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleModeEnabled,
                    onSeek = { viewModel.seekTo(it); viewModel.onSeekEnd(it) },
                    onSeekStart = { viewModel.onSeekStart() },
                    onSeekEnd = { viewModel.onSeekEnd(it) },
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.skipToPrevious() },
                    onNext = { viewModel.skipToNext() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    isNarrowHeight = isNarrowHeight,
                    maxWidth = screenWidth,
                    maxHeight = screenHeight,
                    onLyricAtTopChange = { isLyricAtTop = it },
                )
            } else {
                PhoneImmersiveLayout(
                    title = title,
                    artist = artist,
                    coverUri = stickyCover,
                    lines = parsedLines,
                    lyricsDocument = lyricsDocument,
                    lyricPosition = lyricPosition,
                    lyricPositionProvider = lyricPositionProvider,
                    hasTranslation = hasTranslation,
                    translationEnabled = translationEnabled,
                    onToggleTranslation = { viewModel.toggleTranslation() },
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleModeEnabled,
                    onSeek = { viewModel.seekTo(it); viewModel.onSeekEnd(it) },
                    onSeekStart = { viewModel.onSeekStart() },
                    onSeekEnd = { viewModel.onSeekEnd(it) },
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.skipToPrevious() },
                    onNext = { viewModel.skipToNext() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    isNarrowHeight = isNarrowHeight,
                    maxWidth = screenWidth,
                    maxHeight = screenHeight,
                    onRequestClose = onClose,
                    dragOffsetY = dragOffsetY,
                    isDragging = isDraggingVertically,
                    onActivePanelChange = { activePanel = it },
                    onLyricAtTopChange = { isLyricAtTop = it },
                )
            }
        }

        // 限流/播放错误条（Snackbar）
        if (playbackError != null) {
            androidx.compose.material3.Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.retryPlayback() }) {
                        Text("重试", color = Color.White)
                    }
                },
                containerColor = Color(0xCC1A1A1A),
                contentColor = Color.White,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(playbackError!!, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        LucideIcons.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp).clickable { viewModel.clearPlaybackError() },
                    )
                }
            }
        }
    }
}

// ---------- 背景：BackgroundRender 复刻 + fallback-background ----------

/**
 * 背景层一比一复刻 Capacitor BackgroundRender
 * - album：封面 URI（对应 BackgroundRender :album）
 * - flowSpeed：2（固定，MeshGradientRenderer 流速）
 * - hasLyric：是否有歌词（影响背景渲染参数，Compose 侧暂仅作语义保留，背景始终渲染）
 * - fallback-background：无封面时深色占位（#05070D 纵向渐变），hidden 时 opacity 0
 */
@Composable
private fun PlayerBackground(
    coverUri: String?,
    hasLyric: Boolean,
    modifier: Modifier = Modifier,
) {
    // hasLyric 语义保留：供未来 MeshGradient 强度调节，当前 Compose 侧背景渲染一致
    @Suppress("UNUSED_PARAMETER") val _hasLyric = hasLyric
    Box(modifier = modifier.background(Color(0xFF05070D))) {
        // fallback-background：始终存在，showAlbumBackground 时 --hidden opacity 0
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1E2E), Color(0xFF0D0F1A), Color(0xFF05070D)),
                    ),
                )
                .alpha(if (!coverUri.isNullOrBlank()) 0f else 1f),
        )
        if (!coverUri.isNullOrBlank()) {
            // BackgroundRender：album 模糊铺满，opacity 0.75，flowSpeed 2 模拟为静态模糊 + 渐变
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.08f, scaleY = 1.08f)
                    .blur(28.dp)
                    .alpha(0.75f),
            )
            // 渐变遮罩保证前景可读（顶部浅、底部深）
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.22f),
                                Color(0xFF05070D).copy(alpha = 0.55f),
                                Color(0xFF05070D).copy(alpha = 0.92f),
                            ),
                        ),
                    )
                    .alpha(0.75f),
            )
            // 顶部光晕（径向高光，非纯黑证明）
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                            center = Offset(0.5f * 1080f, 0.28f * 1920f),
                            radius = 900f,
                        ),
                    )
                    .alpha(0.75f),
            )
        }
    }
}

// ---------- 手机布局：固定头部 + 双面板 0.22s easeOut ----------

@Composable
private fun PhoneImmersiveLayout(
    title: String,
    artist: String,
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricsDocument: LyricsDocument?,
    lyricPosition: Long,
    lyricPositionProvider: () -> Int,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    isNarrowHeight: Boolean,
    maxWidth: Dp,
    maxHeight: Dp,
    onRequestClose: () -> Unit,
    dragOffsetY: Float,
    isDragging: Boolean,
    onActivePanelChange: (Int) -> Unit = {},
    onLyricAtTopChange: (Boolean) -> Unit = {},
) {
    var activePanel by remember { mutableStateOf(0) }
    // HorizontalPager 替代 Row 200% 以避免 TabsLayout 半屏约束导致的半宽偏移
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 }, initialPage = activePanel)
    LaunchedEffect(activePanel) {
        if (pagerState.currentPage != activePanel) {
            try { pagerState.animateScrollToPage(activePanel, animationSpec = tween(durationMillis = 220, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))) } catch (_: Exception) {}
        }
        onActivePanelChange(activePanel)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != activePanel) {
            activePanel = pagerState.currentPage
            onActivePanelChange(activePanel)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 固定头部：player-page__song-head--fixed（手机常驻，平板隐藏，此处仅手机分支）
        // 顶部避让 calc(16px + safe-area) 左右 24px，平板隐藏
        FixedSongHead(
            title = title,
            artist = artist,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 0.dp),
        )

        // 已移除手机端额外小圆点指示器：对齐 Capacitor 原版无指示器（PRD R7 1:1）

        // 面板容器：改用 HorizontalPager 以确保单屏全宽
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            beyondViewportPageCount = 0,
        ) { page ->
            when (page) {
                0 -> InfoPanel(
                    coverUri = coverUri,
                    lines = lines,
                    lyricPosition = lyricPosition,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    onSeek = onSeek,
                    onSeekStart = onSeekStart,
                    onSeekEnd = onSeekEnd,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleRepeat = onToggleRepeat,
                    onToggleShuffle = onToggleShuffle,
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    isNarrowHeight = isNarrowHeight,
                    isTablet = false,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                )
                1 -> LyricsPanel(
                    document = lyricsDocument,
                    positionMs = lyricPosition,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                )
            }
        }
    }
}

@Composable
private fun TabletImmersiveLayout(
    title: String,
    artist: String,
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricsDocument: LyricsDocument?,
    lyricPosition: Long,
    lyricPositionProvider: () -> Int,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    isNarrowHeight: Boolean,
    maxWidth: Dp,
    maxHeight: Dp,
    onLyricAtTopChange: (Boolean) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        // 平板不渲染固定头部，由面板内头部承担
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 左栏：info-panel（50%）含面板内头部 + 封面 hero（居中），不含三行小窗与手机控件区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 面板内头部：player-page__song-head--in-panel（仅平板显示）
                FixedSongHead(
                    title = title,
                    artist = artist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                )
                // 封面居中（平板 info-inner justify-content center）：CoverHero 响应式 min(50vh,420) contain
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverHero(
                        coverUri = coverUri,
                        screenHeight = maxHeight,
                        screenWidth = maxWidth,
                        isNarrowHeight = isNarrowHeight,
                    )
                }
                // 平板左侧不展示三行歌词与手机控件区（display:none），由底部条承担
                Spacer(Modifier.height(8.dp))
            }
            // 右栏：lyric-panel（50%）右侧歌词 — Immersive  面板，header 在平板隐藏
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                LyricsPanel(
                    document = lyricsDocument,
                    positionMs = lyricPosition,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                )
            }
        }
        // 底部全宽控制条：player-page__bottom-bar（仅平板，flex none，z 10，渐变背景）
        TabletBottomBar(
            position = position,
            duration = duration,
            isBuffering = isBuffering,
            isPlaying = isPlaying,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            onSeekStart = onSeekStart,
            onSeekEnd = onSeekEnd,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleRepeat = onToggleRepeat,
            onToggleShuffle = onToggleShuffle,
            onOpenQueue = onOpenQueue,
            onOpenEditMeta = onOpenEditMeta,
        )
    }
}

// ---------- 固定头部：player-page__song-head--fixed / --in-panel ----------

@Composable
private fun FixedSongHead(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp, // 0.01em × 20px
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 26.sp,
        )
        if (artist.isNotEmpty()) {
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ---------- 信息面板：info-panel（手机：含三行小窗+控制，平板：仅封面） ----------

@Composable
private fun InfoPanel(
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    isNarrowHeight: Boolean,
    isTablet: Boolean,
    maxWidth: Dp = 360.dp,
    maxHeight: Dp = 800.dp,
) {
    // info-panel：panel padding calc(16+safe) 24 16（对齐 .player-overlay .panel）；
    // info-inner gap 14、padding-top 16、song-meta margin-bottom 18（对齐 .info-panel-inner）
    // 断点收紧（对齐全局 media query）：≤720 gap 4、≤520 gap 2
    val shortHeight = maxHeight <= 720.dp
    val innerGap = when {
        isNarrowHeight -> 2.dp
        shortHeight -> 4.dp
        else -> 14.dp
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        // info-inner overflow: hidden（对齐 .player-page__info-inner）——页面不可滚动，封面弹性吃满剩余空间
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isTablet) Arrangement.Center else Arrangement.Bottom,
    ) {
        // 平板已在外层渲染头部，此处不再重复；手机版头部由 fixed 承担，此处不渲染 in-panel
        Spacer(Modifier.height(16.dp)) // info-inner padding-top 16
        // 封面 hero：弹性占满剩余空间（对齐 cover-hero flex:1 1 auto；min-height 0 收缩），
        // 上限 min(50vh,420px)，窄屏 34vw/150 限制通过 CoverHero 内部计算
        CoverHero(
            coverUri = coverUri,
            screenHeight = maxHeight,
            screenWidth = maxWidth,
            isNarrowHeight = isNarrowHeight,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(innerGap))
        // 手机控件区：player-page__info-controls（平板 display:none，由底部条承担）
        if (!isTablet) {
            ProgressSection(
                position = position,
                duration = duration,
                isBuffering = isBuffering,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
            )
            Spacer(Modifier.height(innerGap))
            ControlsRow(isPlaying = isPlaying, onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext)
            Spacer(Modifier.height(innerGap))
            ModeBarRow(
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onToggleRepeat = onToggleRepeat,
                onToggleShuffle = onToggleShuffle,
                onOpenQueue = onOpenQueue,
                onOpenEditMeta = onOpenEditMeta,
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------- 封面 hero：player-page__cover-hero ----------

@Composable
private fun CoverHero(
    coverUri: String?,
    modifier: Modifier = Modifier,
    screenHeight: Dp = 800.dp,
    screenWidth: Dp = 360.dp,
    isNarrowHeight: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    // 对齐 Capacitor player-page__cover-hero：容器 max-height min(50vh,420px) + cover-hero-img aspect 1 contain
    val maxHeroHeight = minOf(screenHeight * 0.5f, 420.dp)
    val narrowMaxWidth = if (isNarrowHeight) minOf(screenWidth * 0.34f, 150.dp) else null
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeroHeight),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val targetSize = when {
            narrowMaxWidth != null -> narrowMaxWidth
            else -> minOf(availableWidth, availableHeight)
        }
        Box(
            modifier = Modifier
                .size(targetSize)
                .aspectRatio(1f)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!coverUri.isNullOrBlank()) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                )
            } else {
                Icon(LucideIcons.MusicNoteOutlined, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
            }
        }
    }
}

// ---------- 进度区：m-range + time-row + bufferHint ----------

@Composable
private fun ProgressSection(
    position: Long,
    duration: Long,
    isBuffering: Boolean,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
) {
    var previewMs by remember { mutableStateOf<Long?>(null) }
    val displayPos = previewMs ?: position
    val max = duration.coerceAtLeast(1L)
    val canSeek = duration > 0L
    // 兼容 Capacitor：k-range 细轨（4px 白） + thumbWrap 隐藏（无圆球），
    // 完全自绘——不依赖 Material3 Slider 的 disabled 灰轨/默认 thumb
    Column(Modifier.fillMaxWidth()) {
        val barHeight = if (LocalConfiguration.current.screenHeightDp <= 520) 18.dp else 20.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(canSeek, max) {
                    if (!canSeek) return@pointerInput
                    fun fractionAt(offset: androidx.compose.ui.geometry.Offset): Float =
                        (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    detectTapGestures { offset ->
                        onSeekStart()
                        onSeekEnd((fractionAt(offset) * max).toLong().coerceIn(0L, duration))
                    }
                    detectDragGestures(
                        onDragStart = { offset ->
                            previewMs = (fractionAt(offset) * max).toLong()
                            onSeekStart()
                        },
                        onDrag = { change, _ ->
                            previewMs = (fractionAt(change.position) * max).toLong()
                        },
                        onDragEnd = {
                            onSeekEnd((previewMs ?: position).coerceIn(0L, duration))
                            previewMs = null
                        },
                        onDragCancel = { previewMs = null },
                    )
                }
                .drawBehind {
                    // 底轨 rgba(255,255,255,0.25) + 填充 #fff，4dp 圆角细轨（对齐 .progress-range 全局样式）
                    val trackH = 4.dp.toPx()
                    val top = (size.height - trackH) / 2f
                    val fraction = (displayPos.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.25f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(size.width, trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(size.width * fraction, trackH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
                    )
                },
        )
        // time-row：对齐 Capacitor 12px tabular-nums rgba0.68 + 缓冲提示 11px rgba0.55，margin-top 2px
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(displayPos),
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            )
            if (isBuffering) {
                Text(
                    text = "缓冲中",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(
                text = if (duration > 0) formatTime(duration) else "--:--",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

// ---------- 控制区：controls 三键 lg + mode-bar 四键 ----------

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    compact: Boolean = false,
) {
    // controls：三键 lg（48/28），gap clamp(24,10vw,44)；矮屏断点收紧（≤720 clamp(12,4vw,20)、≤520 clamp(10,3.5vw,16)）
    val configuration = LocalConfiguration.current
    val gap = if (compact) 12.dp else {
        val vw = configuration.screenWidthDp.dp
        val vw10 = vw * 0.10f
        when {
            configuration.screenHeightDp <= 520 -> (vw * 0.035f).coerceIn(10.dp, 16.dp)
            configuration.screenHeightDp <= 720 -> (vw * 0.04f).coerceIn(12.dp, 20.dp)
            else -> vw10.coerceIn(24.dp, 44.dp)
        }
    }
    val btnSize = SaltIconButtonSize.LG
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaltIconButton(
            onClick = onPrevious,
            imageVector = LucideIcons.SkipPreviousFill,
            contentDescription = "上一曲",
            size = btnSize,
            tint = Color.White.copy(alpha = 0.9f),
        )
        SaltIconButton(
            onClick = onPlayPause,
            imageVector = if (isPlaying) LucideIcons.PauseFill else LucideIcons.PlayFill,
            contentDescription = if (isPlaying) "暂停" else "播放",
            size = btnSize,
            tint = Color.White.copy(alpha = 0.92f),
        )
        SaltIconButton(
            onClick = onNext,
            imageVector = LucideIcons.SkipNextFill,
            contentDescription = "下一曲",
            size = btnSize,
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun ModeBarRow(
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
) {
    // mode-bar：max-width 320（≤720 收 280、≤520 收 260），space-between，无 is-active（仅图标对 + aria-label）
    // 图标：repeatOutline(repeat) / repeat(one)，shuffle / listOutline（顺序播放）
    val modeMaxWidth = when {
        LocalConfiguration.current.screenHeightDp <= 520 -> 260.dp
        LocalConfiguration.current.screenHeightDp <= 720 -> 280.dp
        else -> 320.dp
    }
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = modeMaxWidth),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaltIconButton(
            onClick = onToggleRepeat,
            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) LucideIcons.RepeatOne else LucideIcons.Repeat,
            contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onToggleShuffle,
            imageVector = if (shuffleEnabled) LucideIcons.Shuffle else LucideIcons.FormatListBulleted,
            contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onOpenQueue,
            imageVector = LucideIcons.QueueMusic,
            contentDescription = "播放队列",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onOpenEditMeta,
            imageVector = LucideIcons.MoreVert,
            contentDescription = "更多",
            tint = Color.White.copy(alpha = 0.8f),
        )
    }
}

// ---------- 平板底部控制条：player-page__bottom-bar ----------

@Composable
private fun TabletBottomBar(
    position: Long,
    duration: Long,
    isBuffering: Boolean,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x0005070D), Color(0x8C05070D)),
                ),
            )
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 8.dp)
            .navigationBarsPadding(),
    ) {
        // 进度全宽：bottom-progress
        ProgressSection(
            position = position,
            duration = duration,
            isBuffering = isBuffering,
            onSeekStart = onSeekStart,
            onSeekEnd = onSeekEnd,
        )
        Spacer(Modifier.height(2.dp))
        // 三段式：left mode + center controls + right mode，space-between，中组居中于屏幕中心
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SaltIconButton(
                    onClick = onToggleRepeat,
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) LucideIcons.RepeatOne else LucideIcons.Repeat,
                    contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
                    tint = Color.White.copy(alpha = 0.8f),
                )
                SaltIconButton(
                    onClick = onToggleShuffle,
                    imageVector = if (shuffleEnabled) LucideIcons.Shuffle else LucideIcons.FormatListBulleted,
                    contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
                    tint = Color.White.copy(alpha = 0.8f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    (LocalConfiguration.current.screenWidthDp.dp * 0.05f).coerceIn(20.dp, 44.dp)
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaltIconButton(onClick = onPrevious, imageVector = LucideIcons.SkipPreviousFill, contentDescription = "上一曲", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.9f))
                SaltIconButton(onClick = onPlayPause, imageVector = if (isPlaying) LucideIcons.PauseFill else LucideIcons.PlayFill, contentDescription = if (isPlaying) "暂停" else "播放", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.92f))
                SaltIconButton(onClick = onNext, imageVector = LucideIcons.SkipNextFill, contentDescription = "下一曲", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.9f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SaltIconButton(onClick = onOpenQueue, imageVector = LucideIcons.QueueMusic, contentDescription = "播放队列", tint = Color.White.copy(alpha = 0.8f))
                SaltIconButton(onClick = onOpenEditMeta, imageVector = LucideIcons.MoreVert, contentDescription = "更多", tint = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ---------- 工具 ----------

/**
 * 歌词逐帧进度源：为卡拉OK 渲染器提供 () -> Int 的播放位置（ms）。
 *
 * 为什么不能直接把 VM 的 lyricPosition 传进去：
 * - VM 轮询粒度 ~100ms，仅够驱动「整词二段高亮」；逐词渐变需要 60fps 连续进度，否则填充边缘会跳变
 * - 若把 lyricPosition 作为 State 在 Compose 层读取，每 100ms 会重组整棵歌词 LazyColumn（旧实现即如此）
 *
 * 做法（对齐 lyrics-ui 官方 sample 的 awaitFrame 模式）：
 * - 锚点：VM 轮询值，在 LaunchedEffect 内 collect（协程内收集不触发重组），记入普通字段
 * - 外推：每帧 withFrameMillis 以「锚点 + 帧时间差」线性推算，写 MutableLongState
 * - 只有渲染器的 Canvas DrawScope 会读取该 State → 仅触发绘制失效，不触发重组
 */
@Composable
private fun rememberLyricPositionProvider(
    positionFlow: StateFlow<Long>,
    isPlaying: Boolean,
): () -> Int {
    val animatedPosition = remember { mutableLongStateOf(0L) }
    // 锚点用普通字段：避免被 Compose 快照记录从而引发重组
    val clock = remember { LyricClock() }
    val playingState = rememberUpdatedState(isPlaying)

    LaunchedEffect(positionFlow) {
        positionFlow.collect { clock.anchorPositionMs = it }
    }

    LaunchedEffect(Unit) {
        var lastAnchor = -1L
        while (true) {
            withFrameMillis { frameTimeMs ->
                val base = clock.anchorPositionMs
                if (base != lastAnchor) {
                    // VM 给出新锚点（~100ms 一次 / seek 后立即），重置外推起点
                    lastAnchor = base
                    clock.anchorFrameMs = frameTimeMs
                }
                animatedPosition.longValue = if (playingState.value) {
                    base + (frameTimeMs - clock.anchorFrameMs).coerceAtLeast(0L)
                } else {
                    base
                }
            }
        }
    }

    return remember { { animatedPosition.longValue.toInt() } }
}

/** 逐帧外推锚点：[anchorPositionMs] 为 VM 轮询位置，[anchorFrameMs] 为收到该锚点时的帧时刻 */
private class LyricClock {
    @Volatile var anchorPositionMs: Long = 0L
    @Volatile var anchorFrameMs: Long = 0L
}

private fun computeCurrentIndex(lines: List<AmllLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) idx = i else break
    }
    if (idx == -1) return 0
    val last = lines.last()
    if (positionMs > last.endTime) return lines.lastIndex
    return idx.coerceIn(0, lines.lastIndex)
}

private fun formatTime(ms: Long): String {
    // 对齐 Capacitor formatTime：分钟补零（"03:45"）
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

// ---------- 队列页（保持原有 Salt 风格，轻微对齐） ----------

@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val currentIndex = queue.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }

    Column(
        modifier = modifier.fillMaxSize().background(LocalSaltColors.current.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SaltSpacing.spacing, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LocalSaltColors.current.text)
            Row {
                if (queue.isNotEmpty()) {
                    Icon(LucideIcons.Delete, contentDescription = "清空队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable { viewModel.clearQueue() })
                    Spacer(Modifier.width(16.dp))
                }
                Icon(LucideIcons.Close, contentDescription = "关闭队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable(onClick = onClose))
            }
        }

        val salt = LocalSaltColors.current
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = salt.text.copy(alpha = 0.6f))
            }
        } else {
            val surfaceVariant = salt.surfaceVariant
            val hairline = salt.hairline
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier.background(if (isCurrent) surfaceVariant else Color.Transparent).drawBehind {
                            drawRect(color = hairline, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.playAtIndex(index) }.padding(horizontal = SaltSpacing.spacing, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.mediaMetadata.title?.toString() ?: "未知歌曲", color = salt.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.mediaMetadata.artist?.toString() ?: "未知歌手", color = salt.text2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text((index + 1).toString(), color = salt.text2, fontSize = 13.sp)
                            Spacer(Modifier.width(12.dp))
                            Icon(LucideIcons.Close, contentDescription = "从队列删除", tint = salt.text2, modifier = Modifier.size(18.dp).clickable { viewModel.removeQueueItemAt(index) })
                        }
                    }
                }
            }
        }
    }
}

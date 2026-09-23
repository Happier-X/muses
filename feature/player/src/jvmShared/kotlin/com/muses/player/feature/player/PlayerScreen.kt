package com.muses.player.feature.player

import com.muses.player.core.ui.components.MusesBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.muses.player.core.playback.PlaybackStates
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.ui.components.PlayerControls
import com.muses.player.core.ui.components.PlayerCoverHero
import com.muses.player.core.ui.components.PlayerModeBar
import com.muses.player.core.ui.components.PlayerProgress
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.feature.player.backdrop.FlowingLightBackdrop
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.feature.player.lyric.LyricsPanel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.abs
import top.yukonga.miuix.kmp.squircle.squircleBackground

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
 * - .progress-range + .player-page__time-row：m-range + 时间行
 * - .controls：三键 lg（48/28），.mode-bar 四键 md（40/20）max-w 320，无 is-active
 * - LyricPlayer：lyric-lines/current-time/align center 0.5 enableBlur/enableScale/wordFadeWidth 0.5，Fab 组 3s idle 隐藏
 * - .player-page__bottom-bar：平板全宽控制条（进度全宽 + 三段式按钮 spaceBetween）
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
    onOpenQueue: () -> Unit = {},
    onOpenEditMeta: () -> Unit = {},
    /**
     * 下滑跟手中：上报收起进度（0 = 完全展开，1 = 已收起）。
     *
     * 非 null 时由**宿主**的 `SeekableTransitionState.seekTo(fraction)` 驱动
     * 全屏播放页纵向位移，本页不再叠加自己的拖动位移。
     */
    onSeekCollapse: ((Float) -> Unit)? = null,
    /** 下滑松手：上报是否应当收起（true = 收起回迷你条） */
    onSettleCollapse: ((Boolean) -> Unit)? = null,
    /**
     * 是否正处于主屏与全屏播放页之间的转场中。
     *
     * 转场中 [FlowingLightBackdrop] 的全屏模糊层会持续重绘，代价较高。所以转场期间换成纯色底，
     * 转场结束再恢复流光背景。
     */
    isTransitioning: Boolean = false,
    /**
     * 内容显现进度：0 = 标题/歌词/控制区隐藏，1 = 完全显示。
     *
     * 封面不跟随此透明度，由共享元素独立飞行；宿主在打开时延迟推进，收起时保持为 1。
     *
     * 传 lambda（内部才读 State），避免转场中每帧重组整棵沉浸页。
     */
    transitionProgress: () -> Float = { 1f },
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
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

    // U21：屏幕尺寸改用视口约束（见下方 BoxWithConstraints），原 LocalConfiguration 仅安卓可用

    // U12：标题/艺术家改由当前曲展示流（曲库实体与在线会话条目归一）；原 Media3 动态 ID3 标签与扫描标签同源，
    // 差异仅在未回写窗口期。注意：metaTitle/metaArtist 是来源标记（embedded/scrape…，见 Mappers 写入 wire 值），
    // 不是展示值！展示永远取 title/artist 本体（刮削写回已把刮削值写入本体，标记只做
    // 「是否被刮削过」的非空判断，见 mergeNowPlaying）。此前误把标记当名字展示，
    // 刮削完艺术家会显示成 "embedded"。
    val nowPlayingMeta by viewModel.nowPlayingMeta.collectAsStateWithLifecycle()
    val title = nowPlayingMeta?.title?.trim()?.takeIf { it.isNotEmpty() } ?: ""
    val artist = nowPlayingMeta?.artist ?: ""
    val hasSong = nowPlayingMeta != null && title.isNotEmpty()

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

    // 外层：m-popup 背景透明（对齐 .player-page__popup background: transparent !important）——
    // 无 scrim 黑化，drag-layer 下滑时直接漏出底下列表（原版 1:1）
    BoxWithConstraints(
        // clipToBounds：宿主在转场中平移整页，这里裁掉视口外内容，避免屏外绘制。
        modifier = modifier.fillMaxSize().clipToBounds(),
    ) {
        // U21：屏幕尺寸取自视口约束（原 LocalConfiguration 仅安卓可用）
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isTabletLayout = screenWidth >= 768.dp && screenHeight < screenWidth
        val isNarrowHeight = screenHeight <= 520.dp

        // 下滑阈值：96~160，取 0.18*height 的 clamp（对齐 getDismissThreshold）
        val dismissThresholdPx = with(density) {
            val h = screenHeight.toPx()
            (h * 0.18f).coerceIn(96.dp.toPx(), 160.dp.toPx())
        }
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
                    // 歌词未滚到顶时保留歌词列表自己的纵向滚动；其他页面区域由外层在 Initial
                    // pass 观察手势，避免子级 Pager/进度条先消费事件导致下拉识别被取消。
                    if (isLyricPanelActive && !isLyricAtTop) Modifier
                    else Modifier.pointerInput(isTabletLayout, dismissThresholdPx) {
                        val bottomExclusionPx = with(density) { 180.dp.toPx() }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            // 底部模式/控制区（约 180dp）不参与下拉关闭，避免与控制按钮冲突。
                            if (down.position.y <= size.height - bottomExclusionPx) {
                                var totalX = 0f
                                var totalY = 0f
                                var accumulatedY = 0f
                                var dragging = false
                                var finished = false
                                var cancelled = false
                                val touchSlop = viewConfiguration.touchSlop

                                while (!finished && !cancelled) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        cancelled = true
                                        continue
                                    }
                                    val delta = change.positionChange()
                                    if (!dragging) {
                                        totalX += delta.x
                                        totalY += delta.y
                                        // 横向意图交给面板 Pager；只有明确向下越过 touch slop 才接管。
                                        if (abs(totalX) > touchSlop && abs(totalX) > abs(totalY)) {
                                            cancelled = true
                                            continue
                                        }
                                        if (totalY > touchSlop && totalY > abs(totalX)) {
                                            dragging = true
                                            isDraggingVertically = true
                                            accumulatedY = (totalY - touchSlop).coerceAtLeast(0f)
                                            change.consume()
                                        }
                                    } else {
                                        accumulatedY = (accumulatedY + delta.y).coerceAtLeast(0f)
                                        change.consume()
                                        if (onSeekCollapse != null) {
                                            onSeekCollapse((accumulatedY / size.height.coerceAtLeast(1)).coerceIn(0f, 0.999f))
                                        } else {
                                            dragOffsetY = accumulatedY
                                        }
                                    }
                                    finished = !change.pressed
                                }

                                if (dragging) {
                                    isDraggingVertically = false
                                    if (finished) {
                                        if (onSettleCollapse != null) {
                                            onSettleCollapse(accumulatedY >= dismissThresholdPx)
                                        } else if (accumulatedY >= dismissThresholdPx) {
                                            clearDragImmediate()
                                            onClose()
                                        } else if (accumulatedY > 0f) {
                                            val from = accumulatedY
                                            scope.launch {
                                                val anim = Animatable(from)
                                                anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }
                                                dragOffsetY = 0f
                                            }
                                        }
                                    } else {
                                        if (onSettleCollapse != null) {
                                            onSettleCollapse(false)
                                        } else if (accumulatedY > 0f) {
                                            val from = accumulatedY
                                            scope.launch {
                                                val anim = Animatable(from)
                                                anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }
                                                dragOffsetY = 0f
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
        ) {
            // 转场中跳过流光背景：它是全屏 AsyncImage + blur(28dp)，持续重绘代价较高；
            // 用纯色顶一下，转场结束后再恢复。
            // 不用 alpha 渐变过渡：那要求它整时段都参与绘制，恰好把掉帧源又请回来了。
            // 视觉上不跳的原因：转场中「非封面内容」也基本不可见（见 contentAlphaFor），
            // 背景色差异被内容渐隐盖住。
            if (isTransitioning) {
                Box(Modifier.fillMaxSize().background(Color(0xFF05070D)))
            } else {
                FlowingLightBackdrop(
                    coverUri = stickyCover,
                    hasLyric = parsedLines.isNotEmpty(),
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    flowSpeed = 2f,
                )
            }
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
                    transitionProgress = transitionProgress,
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
                    transitionProgress = transitionProgress,
                )
            }
        }

        // 限流/播放错误条（深色浮层，播放页沉浸风格：常驻至手动关闭，带重试/关闭；
        // 一过性短提示走 MusesSnackbar，此处故意不用 Snackbar——自动消退会吞掉错误）
        if (playbackError != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .squircleBackground(Color(0xCC1A1A1A), 8.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(playbackError!!, color = Color.White, modifier = Modifier.weight(1f))
                    Text(
                        "重试",
                        color = Color.White,
                        modifier = Modifier.clickable(onClick = { viewModel.retryPlayback() }).padding(8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        TablerIcons.Close,
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

/** 打开时封面先飞到正封，标题、歌词与控制区再按宿主进度淡入。 */
private fun contentAlphaFor(progress: Float): Float = progress.coerceIn(0f, 1f)

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
    /** 转场进度（见 [PlayerScreen]）：驱动「非封面内容」渐隐 */
    transitionProgress: () -> Float = { 1f },
) {
    var activePanel by remember { mutableStateOf(0) }
    // 进度条手势进行中时禁用 pager 横滑：杜绝 seek 拖动被当成切页（由 ProgressSection.onSeekDragActive 驱动）
    var isSeekDragging by remember { mutableStateOf(false) }
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

    // 「非封面内容」统一可见度：标题 / 歌词 / 控制都据此渐隐，
    // 而封面（InfoPanel 里的 CoverHero）不受影响——它走 share element，由宿主插值 bounds。
    // 注意用 graphicsLayer 且**延迟读进度**（不在此处取值，否则每帧重组整棵页面）。
    val contentAlpha = Modifier.graphicsLayer { alpha = contentAlphaFor(transitionProgress()) }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        // 固定头部：player-page__song-head--fixed（手机常驻，平板隐藏，此处仅手机分支）
        // 顶部避让 calc(16px + safe-area) 左右 24px，平板隐藏
        FixedSongHead(
            title = title,
            artist = artist,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 0.dp)
                .then(contentAlpha),
        )

        // 已移除手机端额外小圆点指示器：对齐 Capacitor 原版无指示器（PRD R7 1:1）

        // 面板容器：改用 HorizontalPager 以确保单屏全宽
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            beyondViewportPageCount = 0,
            userScrollEnabled = !isSeekDragging,
        ) { page ->
            when (page) {
                0 -> InfoPanel(
                    coverUri = coverUri,
                    lines = lines,
                    lyricPosition = lyricPosition,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
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
                    onSeekDragActive = { isSeekDragging = it },
                    contentAlpha = { contentAlphaFor(transitionProgress()) },
                )
                1 -> LyricsPanel(
                    document = lyricsDocument,
                    positionMs = lyricPosition,
                    isPlaying = isPlaying,
                    onSeek = onSeek,
                    // 歌词面板整体都是「非封面内容」，直接整块渐隐
                    modifier = Modifier.graphicsLayer { alpha = contentAlphaFor(transitionProgress()) },
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
    /** 转场进度（见 [PlayerScreen]）：驱动「非封面内容」渐隐 */
    transitionProgress: () -> Float = { 1f },
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
                // 歌词面板（V2 范围外，不动）：NativeLyricsPanel 手势分流保持原生
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
            screenWidth = maxWidth,
            screenHeight = maxHeight,
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
            style = MiuixTheme.textStyles.title3,
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
                style = MiuixTheme.textStyles.footnote1,
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
    onSeekDragActive: (Boolean) -> Unit = {},
    /**
     * 「非封面内容」的可见度 lambda（传 lambda 不传值：转场中每帧都要重算，
     * 而取值的调用点在 graphicsLayer 里，不会触发重组）。
     */
    contentAlpha: () -> Float = { 1f },
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
        // 整块随转场进度渐隐：椒盐转场里只有封面在动，进度条/按钮是浮出来的
        if (!isTablet) {
            // horizontalAlignment 必须显式居中：这层是转场渐隐包裹 Column，
            // 外层 InfoPanel 的 CenterHorizontally 不会传导给子级布局参数，
            // 缺省 Start 会让 wrap-content 的 PlayerControls 贴左、与
            // fillMaxWidth 的进度条错位（MuMu 实测三键组中心 331 vs 540）。
            Column(
                modifier = Modifier.graphicsLayer { alpha = contentAlpha() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProgressSection(
                    position = position,
                    duration = duration,
                    onSeekStart = onSeekStart,
                    onSeekEnd = onSeekEnd,
                    screenHeight = maxHeight,
                    onSeekDragActive = onSeekDragActive,
                )
                Spacer(Modifier.height(innerGap))
                ControlsRow(
                    isPlaying = isPlaying,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    screenWidth = maxWidth,
                    screenHeight = maxHeight,
                )
                Spacer(Modifier.height(innerGap))
                ModeBarRow(
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    onToggleRepeat = onToggleRepeat,
                    onToggleShuffle = onToggleShuffle,
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    screenHeight = maxHeight,
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------- 非歌词部分已上收 ui-shared（V2）： ----------
// CoverHero → PlayerCoverHero / ProgressSection → PlayerProgress /
// ControlsRow → PlayerControls / ModeBarRow → PlayerModeBar。
// 安卓保留同名私有适配壳（断点 gap/barHeight 计算 + 参数映射），
// 手势铁律与渲染实现只在 ui-shared commonMain 维护一份。

@Composable
private fun CoverHero(
    coverUri: String?,
    modifier: Modifier = Modifier,
    screenHeight: Dp = 800.dp,
    screenWidth: Dp = 360.dp,
    isNarrowHeight: Boolean = false,
) {
    PlayerCoverHero(
        coverUri = coverUri,
        modifier = modifier,
        screenHeight = screenHeight,
        screenWidth = screenWidth,
        isNarrowHeight = isNarrowHeight,
    )
}

@Composable
private fun ProgressSection(
    position: Long,
    duration: Long,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    screenHeight: Dp,
    // 进度条手势活跃状态：按下即 true（tap/拖动均算），抬手/取消即 false。
    // 手机布局据此禁用 HorizontalPager 横滑，杜绝 seek 拖动被当成切页。
    onSeekDragActive: (Boolean) -> Unit = {},
) {
    val barHeight = if (screenHeight <= 520.dp) 18.dp else 20.dp
    PlayerProgress(
        positionMs = position,
        durationMs = duration,
        onSeekStart = onSeekStart,
        onSeekEnd = onSeekEnd,
        barHeight = barHeight,
        onSeekDragActive = onSeekDragActive,
    )
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    screenWidth: Dp,
    screenHeight: Dp,
    compact: Boolean = false,
) {
    // controls：三键 lg（48/28），gap clamp(24,10vw,44)；矮屏断点收紧（≤720 clamp(12,4vw,20)、≤520 clamp(10,3.5vw,16)）
    val gap = if (compact) 12.dp else {
        when {
            screenHeight <= 520.dp -> (screenWidth * 0.035f).coerceIn(10.dp, 16.dp)
            screenHeight <= 720.dp -> (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
            else -> (screenWidth * 0.10f).coerceIn(24.dp, 44.dp)
        }
    }
    PlayerControls(
        isPlaying = isPlaying,
        onPrevious = onPrevious,
        onPlayPause = onPlayPause,
        onNext = onNext,
        compact = compact,
        gap = gap,
    )
}

@Composable
private fun ModeBarRow(
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    screenHeight: Dp,
) {
    // mode-bar：max-width 320（≤720 收 280、≤520 收 260），space-between，无 is-active（仅图标对 + aria-label）
    val modeMaxWidth = when {
        screenHeight <= 520.dp -> 260.dp
        screenHeight <= 720.dp -> 280.dp
        else -> 320.dp
    }
    PlayerModeBar(
        repeatMode = repeatMode,
        shuffleEnabled = shuffleEnabled,
        onToggleRepeat = onToggleRepeat,
        onToggleShuffle = onToggleShuffle,
        onOpenQueue = onOpenQueue,
        onOpenEditMeta = onOpenEditMeta,
        maxWidth = modeMaxWidth,
    )
}

// ---------- 平板底部控制条：player-page__bottom-bar ----------

@Composable
private fun TabletBottomBar(
    position: Long,
    duration: Long,
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
    screenWidth: Dp,
    screenHeight: Dp,
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
            onSeekStart = onSeekStart,
            onSeekEnd = onSeekEnd,
            screenHeight = screenHeight,
        )
        Spacer(Modifier.height(2.dp))
        // 三段式：left mode + center controls + right mode，space-between，中组居中于屏幕中心
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                MusesIconButton(
                    onClick = onToggleRepeat,
                    imageVector = if (repeatMode == PlaybackStates.REPEAT_MODE_ONE) TablerIcons.RepeatOne else TablerIcons.Repeat,
                    contentDescription = if (repeatMode == PlaybackStates.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
                    tint = Color.White.copy(alpha = 0.8f),
                )
                MusesIconButton(
                    onClick = onToggleShuffle,
                    imageVector = if (shuffleEnabled) TablerIcons.Shuffle else TablerIcons.FormatListBulleted,
                    contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
                    tint = Color.White.copy(alpha = 0.8f),
                )
            }
            PlayerControls(
                isPlaying = isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                compact = true,
                gap = (screenWidth * 0.05f).coerceIn(20.dp, 44.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                MusesIconButton(onClick = onOpenQueue, imageVector = TablerIcons.QueueMusic, contentDescription = "播放队列", tint = Color.White.copy(alpha = 0.8f))
                MusesIconButton(onClick = onOpenEditMeta, imageVector = TablerIcons.MoreVert, contentDescription = "更多", tint = Color.White.copy(alpha = 0.8f))
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

// 注：computeCurrentIndex/formatTime 已随 V2 上收到 ui-shared（formatPlayerTime），
// 本文件残留版本已删除；SimpleLyricsPanel 自带同名私有实现，不受影响。

// ---------- 队列弹窗（底部 popup：遮罩 + 上滑进入 + 顶部圆角，内容沿用原队列页） ----------

@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val queue by viewModel.queueRows.collectAsStateWithLifecycle()
    val currentId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val currentIndex = queue.indexOfFirst { it.songId == currentId }
    val scheme = MiuixTheme.colorScheme

    // 统一封装：miuix OverlayBottomSheet（遮罩/圆角/动画走官方）
    MusesBottomSheet(
        onDismiss = onClose,
        title = "播放队列",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f),
        ) {
            // 操作行（标题已由 sheet 提供，这里只留清空/关闭）
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (queue.isNotEmpty()) {
                    Icon(TablerIcons.Delete, contentDescription = "清空队列", tint = scheme.onBackground.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable { viewModel.clearQueue() })
                    Spacer(Modifier.width(16.dp))
                }
                Icon(TablerIcons.Close, contentDescription = "关闭队列", tint = scheme.onBackground.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable(onClick = onClose))
            }

            if (queue.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("队列为空", color = scheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                val surfaceVariant = scheme.surfaceVariant
                val hairline = scheme.dividerLine
                // 底部内边距 16dp：弹窗内无迷你条，原 96dp 预留不再需要
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(queue, key = { _, item -> item.songId }, contentType = { _, _ -> "queue" }) { index, item ->
                        val isCurrent = index == currentIndex
                        Box(
                            Modifier.background(if (isCurrent) surfaceVariant else Color.Transparent).drawBehind {
                                drawRect(color = hairline, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
                            },
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.playAtIndex(index) }.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, color = scheme.onBackground, style = MiuixTheme.textStyles.body1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.artist ?: "未知歌手", color = scheme.onBackgroundVariant, style = MiuixTheme.textStyles.footnote1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text((index + 1).toString(), color = scheme.onBackgroundVariant, style = MiuixTheme.textStyles.footnote1)
                                Spacer(Modifier.width(12.dp))
                                Icon(TablerIcons.Close, contentDescription = "从队列删除", tint = scheme.onBackgroundVariant, modifier = Modifier.size(18.dp).clickable { viewModel.removeQueueItemAt(index) })
                            }
                        }
                    }
                }
            }
        }
    }
}

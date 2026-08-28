package com.muses.player.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.muses.player.feature.player.backdrop.MeloXFlowingLightBackdrop
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.MeloXIOSLyricsPanel
import kotlinx.coroutines.delay
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
    val hasTranslation by viewModel.hasTranslation.collectAsStateWithLifecycle()
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val lyricPosition by viewModel.lyricPosition.collectAsStateWithLifecycle()
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
    // 歌词面板是否激活：垂直下滑仅 info-panel 生效（对齐 canStartVerticalDismiss / isLyricPanelTarget）
    var isLyricPanelActive by remember { mutableStateOf(false) }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070D))
    ) {
        // 背景层：MeloX 流体渐变与封面虚化（直接复刻 MeloXFlowingLightBackdrop，flowSpeed=2）
        MeloXFlowingLightBackdrop(
            coverUri = stickyCover,
            hasLyric = parsedLines.isNotEmpty(),
            modifier = Modifier.fillMaxSize(),
            flowSpeed = 2f,
        )

        if (!hasSong) {
            // 空态：保留沉浸底色与背景渐变，中央提示（避免黑屏误判）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("暂无播放歌曲", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("从歌曲列表选择一首音乐后，即可进入沉浸式播放。", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            // 拖动层：player-page__drag-layer（translateY + is-dragging）
            // 手势：垂直下滑关闭（仅 info-panel 生效，lyric-panel 内禁止），水平滑动切面板
            // 简化实现：垂直下滑用 detectVerticalDragGestures，回弹 0.22s easeOut；水平切面板由 Panels 内部处理
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = dragOffsetY
                    },
            ) {
                val maxWidth = maxWidth
                val maxHeight = maxHeight

                // 拖动层内容（flex column：固定头部 + panels + 底部条，对齐 Vue flex column）
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .pointerInput(isTabletLayout, isLyricPanelActive, dismissThresholdPx) {
                            // 垂直下滑：仅 info-panel 生效，歌词面板内禁止（对齐 Vue canStartVerticalDismiss / isLyricPanelTarget）
                            var accumulatedY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { _: Offset ->
                                    // 歌词面板激活时不启动垂直手势，避免滚动歌词误触发收起
                                    if (!isTabletLayout && isLyricPanelActive) return@detectVerticalDragGestures
                                    accumulatedY = 0f; isDraggingVertically = true
                                },
                                onVerticalDrag = { _: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                                    if (!isTabletLayout && isLyricPanelActive) return@detectVerticalDragGestures
                                    // 仅下拉（正向）生效，上滑忽略
                                    if (dragAmount > 0f || accumulatedY > 0f) {
                                        accumulatedY = (accumulatedY + dragAmount).coerceAtLeast(0f)
                                        dragOffsetY = accumulatedY
                                    }
                                },
                                onDragEnd = {
                                    isDraggingVertically = false
                                    if (accumulatedY >= dismissThresholdPx) {
                                        clearDragImmediate()
                                        onClose()
                                    } else if (accumulatedY > 0f) {
                                        val from = accumulatedY
                                        // pointerInputScope 本身是 CoroutineScope，可直接 launch
                                        // 使用外部 scope 回弹以避免 Restricted 限制
                                        scope.launch {
                                            val anim = androidx.compose.animation.core.Animatable(from)
                                            anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }
                                            dragOffsetY = 0f
                                        }
                                    }
                                    accumulatedY = 0f
                                },
                                onDragCancel = {
                                    isDraggingVertically = false
                                    if (accumulatedY > 0f) {
                                        val from = accumulatedY
                                        scope.launch {
                                            val anim = androidx.compose.animation.core.Animatable(from)
                                            anim.animateTo(0f, tween(220, easing = reboundEasing)) { dragOffsetY = value }
                                            dragOffsetY = 0f
                                        }
                                    }
                                    accumulatedY = 0f
                                },
                            )
                        }
                ) {
                    if (isTabletLayout) {
                        // 平板横屏：固定头部隐藏，由面板内头部承担（此处不渲染 fixed）
                        TabletImmersiveLayout(
                            title = title,
                            artist = artist,
                            coverUri = stickyCover,
                            lines = parsedLines,
                            lyricPosition = lyricPosition,
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
                            onToggleRepeat = {
                                val next = if (repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
                                viewModel.setRepeatMode(next)
                            },
                            onToggleShuffle = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                            onOpenQueue = onOpenQueue,
                            onOpenEditMeta = onOpenEditMeta,
                            isNarrowHeight = isNarrowHeight,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                        )
                    } else {
                        // 手机：固定头部 + 双面板滑动（panels 0.22s easeOut）
                        PhoneImmersiveLayout(
                            title = title,
                            artist = artist,
                            coverUri = stickyCover,
                            lines = parsedLines,
                            lyricPosition = lyricPosition,
                            hasTranslation = hasTranslation,
                            translationEnabled = translationEnabled,
                            onToggleTranslation = { viewModel.toggleTranslation() },
                            position = position,
                            duration = duration,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            repeatMode = repeatMode,
                            shuffleEnabled = shuffleModeEnabled,
                            onSeek = { viewModel.seekTo(it) },
                            onSeekStart = { viewModel.onSeekStart() },
                            onSeekEnd = { viewModel.onSeekEnd(it) },
                            onPlayPause = { viewModel.playPause() },
                            onPrevious = { viewModel.skipToPrevious() },
                            onNext = { viewModel.skipToNext() },
                            onToggleRepeat = {
                                val next = if (repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
                                viewModel.setRepeatMode(next)
                            },
                            onToggleShuffle = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                            onOpenQueue = onOpenQueue,
                            onOpenEditMeta = onOpenEditMeta,
                            isNarrowHeight = isNarrowHeight,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            onRequestClose = onClose,
                            dragOffsetY = dragOffsetY,
                            isDragging = isDraggingVertically,
                            onActivePanelChange = { isLyricPanelActive = it == 1 },
                        )
                    }
                }
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
                        Icons.Filled.Close,
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
    lyricPosition: Long,
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
) {
    var activePanel by remember { mutableStateOf(0) }
    LaunchedEffect(activePanel) { onActivePanelChange(activePanel) }
    // panels 动画：translateX(-activePanel*50%) 0.22s easeOut
    val easing = remember { CubicBezierEasing(0f, 0f, 0.58f, 1f) }
    val panelOffset by animateFloatAsState(
        targetValue = -activePanel * 0.5f,
        animationSpec = tween(durationMillis = 220, easing = easing),
        label = "panels-translateX",
    )

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

        // 指示器（原 Capacitor 无显式指示器，但为手机提供面板切换提示，保留 2 点，选中 20x6）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { idx ->
                val isSelected = activePanel == idx
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (isSelected) 20.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.35f))
                        .clickable { activePanel = idx },
                )
            }
        }

        // 面板容器：panels（width 200%，overflow hidden，height auto flex:1）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(activePanel) {
                    var totalDx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDx = 0f },
                        onDragEnd = {
                            if (totalDx < -40f && activePanel == 0) activePanel = 1
                            else if (totalDx > 40f && activePanel == 1) activePanel = 0
                        },
                        onHorizontalDrag = { _, dragAmount -> totalDx += dragAmount },
                    )
                },
        ) {
            // 内层轨道：200% 宽，translateX
            Row(
                modifier = Modifier
                    .requiredWidth(maxWidth * 2f)
                    .fillMaxHeight()
                    .offset { IntOffset((panelOffset * maxWidth.toPx() * 2f).roundToInt(), 0) },
            ) {
                // info-panel：50%（panel 宽 = maxWidth）
                Box(
                    modifier = Modifier
                        .width(maxWidth)
                        .fillMaxHeight(),
                ) {
                    InfoPanel(
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
                    )
                }
                // lyric-panel：50% — MeloX iOS 歌词面板（逐词高亮/翻译/和声/点击跳转）
                Box(
                    modifier = Modifier
                        .width(maxWidth)
                        .fillMaxHeight(),
                ) {
                    MeloXIOSLyricsPanel(
                        lines = lines,
                        lyricPosition = lyricPosition,
                        translationEnabled = translationEnabled,
                        hasTranslation = hasTranslation,
                        onToggleTranslation = onToggleTranslation,
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onSeek = onSeek,
                        showPlayFab = true,
                        isTablet = false,
                    )
                }
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
    lyricPosition: Long,
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
                // 封面居中（平板 info-inner justify-content center）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CoverHero(
                        coverUri = coverUri,
                        modifier = Modifier.size(360.dp),
                    )
                }
                // 平板左侧不展示三行歌词与手机控件区（display:none），由底部条承担
                Spacer(Modifier.height(8.dp))
            }
            // 右栏：lyric-panel（50%）右侧歌词 — MeloX iOS 面板，header 在平板隐藏
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                MeloXIOSLyricsPanel(
                    lines = lines,
                    lyricPosition = lyricPosition,
                    translationEnabled = translationEnabled,
                    hasTranslation = hasTranslation,
                    onToggleTranslation = onToggleTranslation,
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    showPlayFab = false,
                    isTablet = true,
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
            letterSpacing = 0.2.sp * 0.01f,
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
) {
    // info-panel inner：手机 justify-content flex-end（底部对齐），平板 center
    // gap 14px，width min(100%,420px) 居中，padding-top 16，overflow hidden
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isTablet) Arrangement.Center else Arrangement.Bottom,
    ) {
        // 平板已在外层渲染头部，此处不再重复；手机版头部由 fixed 承担，此处不渲染 in-panel
        Spacer(Modifier.height(8.dp))
        // 封面 hero：player-page__cover-hero（铺满与占位，aspect 1，max 50vh/420）
        // 窄屏 max-height 520 时封面 34vw/150 限制
        val coverSize = when {
            isNarrowHeight -> 150.dp
            isTablet -> 360.dp
            else -> 272.dp
        }
        CoverHero(coverUri = coverUri, modifier = Modifier.size(coverSize))
        Spacer(Modifier.height(18.dp))
        // 五行小窗：仅手机显示（平板 display:none）；窄屏单行模式
        if (!isTablet) {
            MetaWindow(
                lines = lines,
                lyricPosition = lyricPosition,
                isNarrowHeight = isNarrowHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (isNarrowHeight) 19.5.dp else 92.dp),
            )
            Spacer(Modifier.height(18.dp))
        }
        // 手机控件区：player-page__info-controls（平板 display:none，由底部条承担）
        if (!isTablet) {
            ProgressSection(
                position = position,
                duration = duration,
                isBuffering = isBuffering,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
            )
            Spacer(Modifier.height(12.dp))
            ControlsRow(isPlaying = isPlaying, onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext)
            Spacer(Modifier.height(12.dp))
            ModeBarRow(
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onToggleRepeat = onToggleRepeat,
                onToggleShuffle = onToggleShuffle,
                onOpenQueue = onOpenQueue,
                onOpenEditMeta = onOpenEditMeta,
            )
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------- 封面 hero：player-page__cover-hero ----------

@Composable
private fun CoverHero(coverUri: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
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
            Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
        }
    }
}

// ---------- 五行小窗：player-page__meta-window ----------

/**
 * 五行小窗一比一复刻：
 * - 高度 79px（窄屏 19.5px 单行）
 * - 窗口 translateY -29.5px 居中（当前行居中，三行完整可见）
 * - 行：13px/1.5，当前行 1.05 / 非当前 0.92，opacity 1 / 0.55，blur 0 / 0.6
 * - 切行：0.4s [0.32,0.72,0,1] 上移（Compose 用 animateFloatAsState 近似）
 */
@Composable
private fun MetaWindow(
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    isNarrowHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        Box(modifier = modifier.heightIn(min = 92.dp), contentAlignment = Alignment.Center) {
            Text("暂无歌词", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
        }
        return
    }
    val currentIdx = remember(lines, lyricPosition) { computeCurrentIndex(lines, lyricPosition) }

    // 显示窗口：始终 5 行（prev2/prev/current/next/next2），空行占位保持高度稳定
    val windowRows = remember(lines, currentIdx) {
        ( -2..2).map { offset ->
            val idx = currentIdx + offset
            val line = lines.getOrNull(idx)
            val raw = line?.words?.joinToString("") { it.word }?.trim() ?: ""
            Triple(offset, raw, offset == 0)
        }
    }

    // 窄屏单行模式：仅当前行可见
    if (isNarrowHeight) {
        val currentText = windowRows.find { it.third }?.second ?: ""
        Box(modifier = modifier.height(19.5.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = if (currentText.isEmpty()) " " else currentText,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    },
                lineHeight = 19.5.sp,
            )
        }
        return
    }

    // 79px 视口 + 窗口 -29.5 居中（对应 --meta-window-offset）
    Box(
        modifier = modifier
            .height(79.dp)
            .clip(RoundedCornerShape(0.dp)),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-29.5).dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            windowRows.forEach { (offset, raw, isCurrent) ->
                val text = if (raw.isEmpty()) " " else raw
                // 动画：scale 1.05/0.92，opacity 1/0.55，blur 0/0.6（Compose 用 alpha + scale 近似，blur 文本成本高仅保留 alpha 差异）
                val targetScale by animateFloatAsState(
                    targetValue = if (isCurrent) 1.05f else 0.92f,
                    animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)),
                    label = "meta-scale-$offset",
                )
                val targetAlpha by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.55f,
                    animationSpec = tween(260),
                    label = "meta-alpha-$offset",
                )
                Text(
                    text = text,
                    color = if (isCurrent) Color.White.copy(alpha = 0.92f * targetAlpha) else Color.White.copy(alpha = 0.6f * targetAlpha),
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = targetScale
                            scaleY = targetScale
                            alpha = if (raw.isEmpty()) 0f else targetAlpha
                        },
                    lineHeight = 19.5.sp,
                )
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
    val max = duration.coerceAtLeast(1L).toFloat()
    val canSeek = duration > 0L
    // 兼容 Capacitor：隐藏 thumbWrap（inset-inline-start），Compose 直接透明 thumb
    Column(Modifier.fillMaxWidth()) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        Slider(
            value = displayPos.coerceIn(0L, duration.coerceAtLeast(0L)).toFloat(),
            onValueChange = { v ->
                if (!canSeek) return@Slider
                if (previewMs == null) onSeekStart()
                previewMs = v.toLong()
            },
            onValueChangeFinished = {
                if (!canSeek) return@Slider
                val target = previewMs ?: position
                previewMs = null
                onSeekEnd(target.coerceIn(0L, duration.coerceAtLeast(0L)))
            },
            valueRange = 0f..max,
            enabled = canSeek,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                thumbColor = Color.White,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            thumb = {
                // 保留拖动热区但视觉隐藏 thumb（Capacitor 用 CSS 隐藏 thumbWrap）
                Box(
                    Modifier
                        .size(14.dp)
                        .background(Color.White, RoundedCornerShape(7.dp))
                        .alpha(0f),
                )
            },
        )
        // time-row：左右时间，中间缓冲提示
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatTime(displayPos), color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp)
            if (isBuffering) {
                Text(
                    "缓冲中",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(if (duration > 0) formatTime(duration) else "--:--", color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp)
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
    // controls：三键 lg（48/28），gap clamp(24,10vw,44)，touch-action manipulation
    val gap = if (compact) 12.dp else 28.dp
    val btnSize = SaltIconButtonSize.LG
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaltIconButton(
            onClick = onPrevious,
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "上一曲",
            size = btnSize,
            tint = Color.White.copy(alpha = 0.9f),
        )
        SaltIconButton(
            onClick = onPlayPause,
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            size = btnSize,
            tint = Color.White.copy(alpha = 0.92f),
        )
        SaltIconButton(
            onClick = onNext,
            imageVector = Icons.Filled.SkipNext,
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
    // mode-bar：max-width 320，space-between，无 is-active（仅图标对 + aria-label）
    // 图标：repeatOutline(repeat) / repeat(one)，shuffle / listOutline（顺序播放）
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaltIconButton(
            onClick = onToggleRepeat,
            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onToggleShuffle,
            imageVector = if (shuffleEnabled) Icons.Filled.Shuffle else Icons.Filled.FormatListBulleted,
            contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onOpenQueue,
            imageVector = Icons.Filled.QueueMusic,
            contentDescription = "播放队列",
            tint = Color.White.copy(alpha = 0.8f),
        )
        SaltIconButton(
            onClick = onOpenEditMeta,
            imageVector = Icons.Filled.MoreVert,
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
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 12.dp)
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
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
                    tint = Color.White.copy(alpha = 0.8f),
                )
                SaltIconButton(
                    onClick = onToggleShuffle,
                    imageVector = if (shuffleEnabled) Icons.Filled.Shuffle else Icons.Filled.FormatListBulleted,
                    contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
                    tint = Color.White.copy(alpha = 0.8f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaltIconButton(onClick = onPrevious, imageVector = Icons.Filled.SkipPrevious, contentDescription = "上一曲", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.9f))
                SaltIconButton(onClick = onPlayPause, imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.92f))
                SaltIconButton(onClick = onNext, imageVector = Icons.Filled.SkipNext, contentDescription = "下一曲", size = SaltIconButtonSize.LG, tint = Color.White.copy(alpha = 0.9f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SaltIconButton(onClick = onOpenQueue, imageVector = Icons.Filled.QueueMusic, contentDescription = "播放队列", tint = Color.White.copy(alpha = 0.8f))
                SaltIconButton(onClick = onOpenEditMeta, imageVector = Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ---------- 歌词面板：LyricPlayer 复刻 ----------

/**
 * 歌词面板一比一复刻 LyricPlayer
 * - lyric-lines / current-time（lyricPosition 钳制到末句 endTime，已在 VM 完成）
 * - alignAnchor center / alignPosition 0.5（当前行视口居中，LazyColumn animateScrollToItem -2 偏移居中）
 * - enableBlur / enableScale / wordFadeWidth 0.5（逐词 alpha 渐变）
 * - 空态：标题“暂无歌词” + 描述“未找到内嵌 …”
 * - Fab 组：is-visible 180ms fade，3s idle 隐藏，翻译仅 hasTranslation 时渲染，播放仅非平板
 */
@Composable
private fun LyricPanel(
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    translationEnabled: Boolean,
    hasTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    showPlayFab: Boolean,
    isTablet: Boolean,
) {
    // 浮动按钮显隐：默认隐藏，点击/滑动歌词后显示，3s 后隐藏，切回控制页立即隐藏
    var chromeVisible by remember { mutableStateOf(false) }
    var chromeIdleJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()

    fun revealChrome() {
        chromeVisible = true
        chromeIdleJob?.cancel()
        chromeIdleJob = scope.launch {
            delay(3000)
            chromeVisible = false
        }
    }
    fun hideImmediate() {
        chromeVisible = false
        chromeIdleJob?.cancel()
        chromeIdleJob = null
    }

    DisposableEffect(lines) {
        onDispose { chromeIdleJob?.cancel() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ) { if (lines.isNotEmpty()) revealChrome() },
    ) {
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(40.dp))
                    Text("暂无歌词", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("未找到内嵌歌词或同目录同名 .lrc 文件，可在刮削页获取。", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        } else {
            val listState = rememberLazyListState()
            val currentIdx = remember(lines, lyricPosition) { computeCurrentIndex(lines, lyricPosition) }

            // 随进度自动滚动 + 点击跳播（对齐LyricPlayer current-time + line-click → seek）
            // 当前行位于歌词可视区中心（align center 0.5）：滚动到 currentIdx-2 使当前行居中
            LaunchedEffect(currentIdx) {
                if (currentIdx >= 0) {
                    val target = (currentIdx - 2).coerceAtLeast(0)
                    runCatching { listState.animateScrollToItem(target) }
                }
            }

            // 用户滚动歌词时露出 chrome（wheel/touchmove）
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) revealChrome()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> revealChrome() }
                    },
                contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lines, key = { idx, line -> "${line.startTime}-$idx" }) { idx, line ->
                    val isCurrent = idx == currentIdx
                    val bg = if (isCurrent) Color.White.copy(alpha = 0.10f) else Color.Transparent
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable {
                                onSeek(line.startTime.toLong())
                                revealChrome()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 逐词高亮：wordFadeWidth 0.5 近似为已唱白色、未唱半透，enableBlur/enableScale 用 scale+alpha 表达
                        val annotated = remember(line, lyricPosition, isCurrent) {
                            buildAnnotatedString {
                                if (line.words.isEmpty()) {
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = if (isCurrent) 1f else 0.55f))) { append("") }
                                } else {
                                    line.words.forEach { w ->
                                        val alpha = when {
                                            !isCurrent -> 0.5f
                                            lyricPosition >= w.endTime -> 1f
                                            lyricPosition < w.startTime -> 0.42f
                                            else -> 1f // 正在唱的词
                                        }
                                        val weight = if (isCurrent && lyricPosition in w.startTime..w.endTime) FontWeight.ExtraBold else if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        // wordFadeWidth 0.5：当前词内插值，此处简化为二段
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White.copy(alpha = alpha),
                                                fontWeight = weight,
                                                fontSize = if (isCurrent) 18.sp else 15.sp,
                                            ),
                                        ) {
                                            append(w.word)
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = annotated,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    // enableScale：当前行 1.0，非当前 0.98（AMLL 内部 scale）
                                    scaleX = if (isCurrent) 1f else 0.98f
                                    scaleY = if (isCurrent) 1f else 0.98f
                                    // enableBlur：非当前行轻微透明
                                    alpha = if (isCurrent) 1f else 0.95f
                                },
                            lineHeight = 26.sp,
                        )
                        // 翻译/音译（translationEnabled 控制显隐，对齐 applyLyricTranslationVisibility）
                        if (translationEnabled && line.translatedLyric.isNotBlank()) {
                            Text(
                                text = line.translatedLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.45f),
                                fontSize = if (isCurrent) 13.sp else 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        }
                        if (translationEnabled && line.romanLyric.isNotBlank()) {
                            Text(
                                text = line.romanLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.38f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (line.isBG && line.words.isNotEmpty()) {
                            Text("· 和声", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 浮动操作：lyric-fabs（motion 180ms fade，is-visible 控制 opacity + pointerEvents）
        // 布局：hasTranslation 时 split（两端），否则 end（仅播放键居右）；平板无播放键
        val showFabContainer = hasTranslation || showPlayFab
        if (showFabContainer) {
            val fabAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "lyric-fab-alpha",
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .alpha(fabAlpha)
                    .graphicsLayer { alpha = fabAlpha },
                horizontalArrangement = if (hasTranslation && showPlayFab) Arrangement.SpaceBetween else if (hasTranslation) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasTranslation) {
                    androidx.compose.material3.FilledTonalIconButton(
                        onClick = {
                            onToggleTranslation()
                            revealChrome()
                        },
                        modifier = Modifier.size(40.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.14f),
                            contentColor = if (translationEnabled) Color.Black else Color.White,
                        ),
                        enabled = chromeVisible,
                    ) {
                        Icon(Icons.Filled.Translate, contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译", modifier = Modifier.size(18.dp))
                    }
                    if (showPlayFab) Spacer(Modifier.weight(1f))
                }
                if (showPlayFab) {
                    androidx.compose.material3.FilledIconButton(
                        onClick = {
                            onPlayPause()
                            revealChrome()
                        },
                        modifier = Modifier.size(48.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                        enabled = chromeVisible,
                    ) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", modifier = Modifier.size(24.dp))
                    }
                }
            }
            // 隐藏态禁止点击（pointerEvents none）：通过 enabled 控制已实现
            if (!chromeVisible) {
                // 占位：避免隐藏态仍可点击（已用 enabled=false，额外屏蔽）
            }
        }
    }
}

// ---------- 工具 ----------

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
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
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
                    Icon(Icons.Filled.Delete, contentDescription = "清空队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable { viewModel.clearQueue() })
                    Spacer(Modifier.width(16.dp))
                }
                Icon(Icons.Filled.Close, contentDescription = "关闭队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable(onClick = onClose))
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
                            Icon(Icons.Filled.Close, contentDescription = "从队列删除", tint = salt.text2, modifier = Modifier.size(18.dp).clickable { viewModel.removeQueueItemAt(index) })
                        }
                    }
                }
            }
        }
    }
}

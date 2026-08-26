package com.muses.player.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.feature.player.lyric.AmllLyricLine
import com.muses.player.feature.player.lyric.AmllWebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 播放页 —— PlayerPage.vue 手机形态一比一翻译（P4.3 精修）。
 *
 * 结构（Vue template 1~360 行逐段对照）：
 * ```
 * Box（沉浸底色 #05070d）
 * ├─ 背景层：封面模糊近似 MeshGradient（TODO(P4.4)：换 AMLL BackgroundRender 流体背景。
 * │   取舍说明：现有 AmllWebView 是「背景+歌词」一体 WebView，无法拆出纯背景层，
 * │   故背景用专辑封面放大模糊 + opacity 0.75 近似 __bg 层；歌词面板内嵌完整 AmllWebView）
 * └─ drag-layer Column（graphicsLayer.translationY 绑定下滑拖拽 + 手势监听同元素——Session 103 ③）
 *     ├─ 固定头部（椒盐式：歌名 h1 + 艺术家 p 常驻左上，无关闭按钮，关闭靠下滑手势）
 *     └─ panels 容器（双面板并排 width=200%，translateX(-activePanel*50%)，220ms easeOut）
 *         ├─ info 面板：大封面 hero → 五行歌词小窗 → 进度区（自绘无 thumb）→ 时间行（含缓冲中）→ 三键控制（无圆底）→ mode-bar 四键
 *         └─ 歌词面板：AmllWebView（嵌入面板区域而非全屏）+ FAB 组浮层（翻译+播放，3s 无操作淡出）
 * ```
 *
 * 手势系统（Vue script onTouchStart/Move/End 翻译）：
 * - 方向锁定互斥：|dy|>|dx| 锁竖直（下滑拖拽跟手），否则锁水平（松手 ≥40dp 切面板）；
 * - 下拉超阈值关闭播放页，否则 220ms easeOut 显式回弹归位（Animatable 单一状态源，
 *   snapTo 天然打断在途回弹——对应 journal Session 103 ①「回弹不能只依赖单次 watch 机会」）；
 * - ACTION_CANCEL / 子控件消费事件时兜底立即清零位移（Session 103 ②）；
 * - 进度条 / AMLL WebView 区域因子控件消费事件而天然隔离
 *   （对齐 canStartVerticalDismiss / onProgressGestureStart 的手势隔离语义）。
 *
 * 已知取舍（对照 Vue 源码注释保留）：
 * - 图标用 Material 近似 lucide 双轨描边风（工程既有映射约定）；
 * - repeat 键对齐 Vue 语义在 ALL↔ONE 间切换（v1 曾三态循环含 OFF）；
 * - 「更多」按钮仅入口占位，编辑歌曲信息等动作单属 M3 范围；
 * - 平板断点后置（PRD Out of Scope：先保证手机形态一比一）。
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onOpenQueue: () -> Unit = {},
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val lyricsJson by viewModel.lyricsJson.collectAsStateWithLifecycle()
    val lyricPosition by viewModel.lyricPosition.collectAsStateWithLifecycle()
    val hasTranslation by viewModel.hasTranslation.collectAsStateWithLifecycle()
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val parsedLines by viewModel.parsedLines.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val stickyCover by viewModel.stickyCover.collectAsStateWithLifecycle()

    val song = currentMediaItem?.let { item ->
        Song(
            id = item.mediaId,
            sourceId = "",
            path = item.localConfiguration?.uri?.toString() ?: "",
            title = item.mediaMetadata.title?.toString() ?: "未知歌曲",
            artist = item.mediaMetadata.artist?.toString(),
            album = item.mediaMetadata.albumTitle?.toString(),
            durationMs = if (duration > 0) duration else 0L,
            coverUri = item.mediaMetadata.artworkUri?.toString(),
        )
    }
    val lyricArtist = song?.artist?.trim().orEmpty()

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            // .player-page__drag-layer { background: #05070d }（沉浸底色，Vue 源码固定值）
            .background(PLAYER_BACKDROP),
    ) {
        val density = LocalDensity.current
        val panelWidth = maxWidth

        // ---- 背景层（__bg opacity 0.75 叠加 __fallback 底色；封面模糊近似流体渐变）----
        PlayerBackdrop(coverUri = if (song != null) stickyCover else null)

        if (song == null) {
            // ---- 空态（Vue empty-state：♪ 占位封面 + 文案）----
            // placeholder-cover 尺寸公式：width min(72vw, 100%, 340px, 52dvh)、
            // height min(72vw, 340px, 52dvh)；♪ font-size clamp(48px, 12vw, 72px)
            PlayerEmptyState(
                modifier = Modifier.align(Alignment.Center),
                iconSide = minOf(panelWidth * 0.72f, panelWidth, 340.dp, maxHeight * 0.52f),
                iconRadius = (panelWidth * 0.04f).coerceIn(18.dp, 28.dp),
                iconFontSize = with(density) { (panelWidth * 0.12f).toSp().value.coerceIn(48f, 72f).sp },
            )
        } else {
            var activePanel by remember { mutableIntStateOf(0) }

            // 面板切换位移：translateX(-activePanel*50%)，duration 0.22s easeOut
            val panelShiftPx = remember { Animatable(0f) }
            LaunchedEffect(activePanel, panelWidth) {
                panelShiftPx.animateTo(
                    targetValue = activePanel * with(density) { panelWidth.toPx() },
                    animationSpec = tween(durationMillis = 220, easing = EaseOut),
                )
            }

            // 下滑拖拽位移（px）：跟手 snapTo + 松手显式回弹，同一 Animatable 驱动 graphicsLayer
            val dragOffsetY = remember { Animatable(0f) }

            // 歌词页浮动 chrome：交互后显示，空闲 LYRIC_FAB_IDLE_MS 再藏（Vue lyricChromeVisible +
            // lyricChromeIdleTimer；tick 计数驱动 LaunchedEffect 重启计时，等价 clear/restart 定时器）
            var lyricChromeTick by remember { mutableIntStateOf(0) }
            LaunchedEffect(activePanel) {
                // 切离歌词面板立即隐藏 chrome（Vue watch activePanel → hideLyricChromeImmediate）
                if (activePanel != 1) lyricChromeTick = 0
            }
            LaunchedEffect(lyricChromeTick) {
                if (lyricChromeTick > 0) {
                    delay(LYRIC_FAB_IDLE_MS)
                    lyricChromeTick = 0
                }
            }

            // ---- 拖拽层：绑定元素 = 手势监听元素 = 回弹动画状态源（Session 103 三条踩坑的结构性解法）----
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragOffsetY.value },
            ) {
                // ---- 固定头部（椒盐式：--fixed 变体，左右滑切面板不移动；无关闭按钮）----
                PlayerSongHead(
                    title = song.title,
                    artist = lyricArtist,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ---- 双面板滑动容器（__panels：width 200% + overflow hidden + translateX）----
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    Row(
                        Modifier
                            .requiredWidth(panelWidth * 2)
                            .fillMaxHeight()
                            // 用 offset（影响 View 布局）替代 graphicsLayer（仅绘图 transform）。
                            // graphicsLayer 不移动 AndroidView 内部 SurfaceView——WebView 合成
                            // Surface 位置与 Compose 坐标系脱节，导致 WebView 在原始位置上屏。
                            .offset { IntOffset(-panelShiftPx.value.roundToInt(), 0) },
                    ) {
                        // ===== info 面板 =====
                        InfoPanel(
                            coverUri = stickyCover,
                            lines = parsedLines,
                            positionMs = position,
                            durationMs = duration,
                            isBuffering = isBuffering,
                            isPlaying = isPlaying,
                            repeatMode = repeatMode,
                            shuffleEnabled = shuffleModeEnabled,
                            onPrevious = viewModel::skipToPrevious,
                            onPlayPause = viewModel::playPause,
                            onNext = viewModel::skipToNext,
                            onSeekStart = viewModel::onSeekStart,
                            onSeekEnd = viewModel::onSeekEnd,
                            onToggleRepeat = {
                                // 对齐 Vue onToggleRepeat：one ↔ all 二态切换
                                viewModel.setRepeatMode(
                                    if (repeatMode == Player.REPEAT_MODE_ONE) {
                                        Player.REPEAT_MODE_ALL
                                    } else {
                                        Player.REPEAT_MODE_ONE
                                    },
                                )
                            },
                            onToggleShuffle = {
                                viewModel.setShuffleModeEnabled(!shuffleModeEnabled)
                            },
                            onOpenQueue = onOpenQueue,
                            modifier = Modifier.width(panelWidth).fillMaxHeight(),
                        )

                        // ===== 歌词面板 =====
                        LyricPanel(
                            payloadJson = lyricsJson,
                            lyricPositionFlow = viewModel.lyricPosition,
                            isPlayingFlow = viewModel.isPlaying,
                            hasLyrics = parsedLines.isNotEmpty(),
                            hasTranslation = hasTranslation,
                            translationEnabled = translationEnabled,
                            isPlaying = isPlaying,
                            chromeVisible = lyricChromeTick > 0,
                            onUserActivity = { lyricChromeTick++ },
                            onToggleTranslation = viewModel::toggleTranslation,
                            onPlayPause = viewModel::playPause,
                            modifier = Modifier.width(panelWidth).fillMaxHeight(),
                        )
                    }
                }
            }

            // ---- 手势系统（方向锁定互斥：水平切面板 / 竖直下拉关闭）----
            // AwaitPointerEventScope 是受限挂起作用域，不能直接调 Animatable 挂起函数；
            // 统一经 gestureScope.launch 调度（主队列 FIFO，顺序有保障），跟手延迟可忽略。
            val gestureScope = rememberCoroutineScope()
            val closeGesture by rememberUpdatedState(onClose)
            val switchPanel by rememberUpdatedState({ panel: Int -> activePanel = panel })
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        val swipeThresholdPx = 40.dp.toPx() // onTouchEnd |startX-endX| < 40 不切面板
                        val slopPx = 8.dp.toPx() // 方向判定阈值（Vue Math.max(|dx|,|dy|) > 8）
                        // getDismissThreshold(): min(160, max(96, innerHeight*0.18))
                        val dismissThresholdPx =
                            (size.height * 0.18f).coerceIn(96.dp.toPx(), 160.dp.toPx())

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var direction = GESTURE_NONE
                            // 收尾动作状态仅在本手势会话内有效，每次手势开始重置——
                            // 绝不能提升到 awaitEachGesture 外跨手势共享（否则残留上一把手的
                            // endAction/endDx，出现「点进度条却切面板」类误动作）
                            var endAction = END_NONE
                            var endDx = 0f
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull() ?: break

                                // 子控件已消费（进度条自绘拖动 / WebView 歌词滚动 / 控件点击）：
                                // 本会话作废并立即清零残留（对齐 onProgressGestureStart 隔离语义）
                                if (direction == GESTURE_NONE && event.changes.any { it.isConsumed }) {
                                    break
                                }

                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (direction == GESTURE_NONE &&
                                    maxOf(abs(dx), abs(dy)) > slopPx
                                ) {
                                    direction =
                                        if (abs(dy) > abs(dx)) GESTURE_VERTICAL else GESTURE_HORIZONTAL
                                    if (direction == GESTURE_HORIZONTAL) {
                                        gestureScope.launch { dragOffsetY.snapTo(0f) }
                                    }
                                }

                                if (direction == GESTURE_VERTICAL) {
                                    change.consume()
                                    val nextOffset = maxOf(0f, dy)
                                    // 回弹进行中再拖拽：snapTo 自动终止动画恢复纯跟手（Session 103 ①）
                                    gestureScope.launch { dragOffsetY.snapTo(nextOffset) }
                                }

                                if (!change.pressed) {
                                    endDx = dx
                                    // 触摸序列被打断（父级消费 = ACTION_CANCEL 语义）：兜底立即清零，
                                    // 不留半屏残留（journal Session 103 ②）。注意不能用
                                    // changes.none { changedToUp } 判定——取消时 change 同样满足 changedToUp。
                                    // Compose 无 PointerEventType.Cancel，取消 = change 已被父级消费
                                    val isCancelled = event.changes.any { it.isConsumed }
                                    endAction = when {
                                        isCancelled -> END_RESET
                                        direction == GESTURE_VERTICAL &&
                                            dragOffsetY.value >= dismissThresholdPx -> END_CLOSE
                                        direction == GESTURE_VERTICAL -> END_REBOUND
                                        direction == GESTURE_HORIZONTAL &&
                                            abs(dx) >= swipeThresholdPx -> END_SWITCH_PANEL
                                        else -> END_NONE
                                    }
                                    break
                                }
                            }
                            // 松手/取消收尾必须调度在 awaitEachGesture 循环体内：该循环永不返回，
                            // 收尾逻辑若写在其后即为不可达死代码。launch 是非挂起调用，
                            // 受限作用域允许；主队列 FIFO 保证与跟手 snapTo 的先后顺序。
                            when (endAction) {
                                END_RESET -> gestureScope.launch { dragOffsetY.snapTo(0f) }
                                // 松手超阈值：收起关闭播放页（露底由导航返回承担）
                                END_CLOSE -> gestureScope.launch {
                                    dragOffsetY.snapTo(0f)
                                    closeGesture()
                                }
                                // 未过阈值：显式 220ms easeOut 回弹归位（原 CSS transition）
                                END_REBOUND -> gestureScope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(220, easing = EaseOut),
                                    )
                                }
                                // 水平位移 ≥40dp 切换面板（endX < startX → 歌词面板）
                                END_SWITCH_PANEL -> switchPanel(if (endDx < 0) 1 else 0)
                            }
                        }
                    },
            )
        }
    }
}

// ---------- 常量与令牌 ----------

/** 沉浸底色：Vue `.player-page__drag-layer { background: #05070d }`（源码固定值，不随主题） */
private val PLAYER_BACKDROP = Color(0xFF05070D)

/** 歌词页浮动 chrome 空闲淡出延时（Vue LYRIC_FAB_IDLE_MS） */
private const val LYRIC_FAB_IDLE_MS = 3000L

/** 手势方向常量（Vue gestureDirection: 'horizontal' | 'vertical' | null） */
private const val GESTURE_NONE = 0
private const val GESTURE_HORIZONTAL = 1
private const val GESTURE_VERTICAL = 2

/** 手势结束动作常量（松手/取消后的收尾分支） */
private const val END_NONE = 0
private const val END_RESET = 1
private const val END_CLOSE = 2
private const val END_REBOUND = 3
private const val END_SWITCH_PANEL = 4

/** 五行歌词小窗：视口高 79px（__song-meta height）、行槽 29.5px（行高 19.5px + 行距 10px） */
private val MINI_WINDOW_HEIGHT = 79.dp
private val MINI_SLOT = 29.5.dp

/** 播放页白系文字令牌 —— Vue 源码 rgba 白值直译（沉浸深色层恒定，与主题无关，非主题硬编码） */
private object PlayerTints {
    val title = Color.White.copy(alpha = 0.95f)       // __song-title
    val artist = Color.White.copy(alpha = 0.6f)       // __song-artist
    val timeRow = Color.White.copy(alpha = 0.68f)     // __time-row
    val bufferHint = Color.White.copy(alpha = 0.55f)  // __buffer-hint
    val sideBtn = Color.White.copy(alpha = 0.9f)      // __side-btn
    val playBtn = Color.White.copy(alpha = 0.92f)     // __play-btn
    val modeBtn = Color.White.copy(alpha = 0.8f)      // __mode-btn
    val metaLine = Color.White.copy(alpha = 0.6f)     // __meta-line
    val metaCurrent = Color.White.copy(alpha = 0.92f) // __meta-current
}

// ---------- 背景层 ----------

/**
 * 背景层：`.player-page__bg { opacity: 0.75 }` 内的 BackgroundRender 流体渐变近似。
 *
 * TODO(P4.4)：接入 AMLL BackgroundRender MeshGradient 流体背景。当前取舍：
 * AmllWebView 是「背景+歌词」一体 WebView 无法拆出纯背景层，改用封面放大模糊 +
 * alpha 0.75 近似流体底（API <31 无 RenderEffect 时退化为静态放大封面，观感可接受）。
 */
@Composable
private fun PlayerBackdrop(coverUri: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // 放大避免模糊后边缘透出底色
                    .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }
                    .blur(radius = 64.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .alpha(0.75f),
            )
        }
    }
}

// ---------- 空态 ----------

/** Vue `.player-page__empty`：♪ 占位封面（placeholder-cover 渐变壳）+ 标题 + 引导文案 */
@Composable
private fun PlayerEmptyState(
    iconSide: Dp,
    iconRadius: Dp,
    iconFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(iconSide)
                // placeholder-cover：linear-gradient(135deg, white22%, white6%) + 圆角 clamp(18,4vw,28)
                .clip(RoundedCornerShape(iconRadius))
                .background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "♪", color = PlayerTints.modeBtn, fontSize = iconFontSize)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "暂无播放歌曲",
            color = PlayerTints.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "从歌曲列表选择一首音乐后，即可进入沉浸式播放。",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- 固定头部 ----------

/** `.player-page__song-head--fixed`：padding calc(16px+safe-top) 24px 0；歌名 h1 + 艺术家 p 左上 */
@Composable
private fun PlayerSongHead(title: String, artist: String, modifier: Modifier = Modifier) {
    Column(modifier.statusBarsPadding().padding(top = 16.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = PlayerTints.title,
            fontSize = 20.sp,
            lineHeight = 26.sp, // line-height 1.3
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp, // letter-spacing 0.01em
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotEmpty()) {
            // margin: 2px 0 0
            Text(
                text = artist,
                modifier = Modifier.padding(horizontal = 24.dp).offset(y = 2.dp),
                color = PlayerTints.artist,
                fontSize = 13.sp,
                lineHeight = 18.sp, // line-height 1.4
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- info 面板 ----------

/**
 * `.player-page__info-panel` / `__info-inner`：width min(100%,420px) 居中、内容底部对齐
 * （justify-content flex-end）、gap 14px、padding-top 16px；子块顺序 =
 * 封面 hero → 五行歌词小窗 → 进度区 → 时间行 → 三键控制 → mode-bar 四键。
 */
@Composable
private fun InfoPanel(
    coverUri: String?,
    lines: List<AmllLyricLine>,
    positionMs: Long,
    durationMs: Long,
    isBuffering: Boolean,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        // __info-inner width: min(100%, 420px)，居中
        val innerWidth = minOf(maxWidth, 420.dp)
        // __controls gap: clamp(24px, 10vw, 44px)（10vw 以内容宽为基准）
        val controlsGap = (maxWidth * 0.10f).coerceIn(24.dp, 44.dp)
        // __cover-hero max-height: min(50vh, 420px)
        val heroCap = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.5f, 420.dp)

        Column(
            Modifier
                .width(innerWidth)
                .fillMaxHeight()
                // Web 依赖 safe-area-inset-bottom 避让；Compose 以 navigationBarsPadding 等价实现
                .navigationBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom),
        ) {
            // ---- 大封面（__cover-hero：正方形 contain，radius-card 12dp，无封面 ♪ 占位）----
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                // 封面边长 = min(容器宽, 容器高, cap)，空间越大封面越大且恒为正方形
                val side = minOf(maxWidth, maxHeight, heroCap)
                if (!coverUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "歌曲封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(side)
                            .clip(RoundedCornerShape(SaltRadius.card)),
                    )
                } else {
                    Box(
                        Modifier
                            .size(side)
                            .clip(RoundedCornerShape(SaltRadius.card))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.06f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "♪", color = PlayerTints.modeBtn, fontSize = 56.sp)
                    }
                }
            }

            // ---- 五行歌词小窗（margin-bottom 18px 与进度条拉开间距）----
            if (lines.isNotEmpty()) {
                LyricMiniWindow(
                    lines = lines,
                    positionMs = positionMs,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }

            // ---- 进度区（__progress-area：进度条 + 时间行）----
            Column(Modifier.fillMaxWidth()) {
                ProgressSlider(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeekStart = onSeekStart,
                    onSeekEnd = onSeekEnd,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimeRow(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isBuffering = isBuffering,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- 三键控制（__controls：lg 图标无圆底，justify center + gap clamp）----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(controlsGap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaltIconButton(
                    onClick = onPrevious,
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一曲",
                    size = SaltIconButtonSize.LG,
                    tint = PlayerTints.sideBtn,
                )
                SaltIconButton(
                    onClick = onPlayPause,
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停播放" else "播放",
                    size = SaltIconButtonSize.LG,
                    tint = PlayerTints.playBtn,
                )
                SaltIconButton(
                    onClick = onNext,
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一曲",
                    size = SaltIconButtonSize.LG,
                    tint = PlayerTints.sideBtn,
                )
            }

            // ---- mode-bar 四键（循环/随机/队列/更多），max-width 320px space-between ----
            Row(
                Modifier.fillMaxWidth().widthIn(max = 320.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaltIconButton(
                    onClick = onToggleRepeat,
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环",
                    tint = PlayerTints.modeBtn,
                )
                SaltIconButton(
                    onClick = onToggleShuffle,
                    imageVector =
                        if (shuffleEnabled) {
                            Icons.Filled.Shuffle
                        } else {
                            Icons.Filled.FormatListBulleted
                        },
                    contentDescription = if (shuffleEnabled) "随机播放" else "顺序播放",
                    tint = PlayerTints.modeBtn,
                )
                SaltIconButton(
                    onClick = onOpenQueue,
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "播放队列",
                    tint = PlayerTints.modeBtn,
                )
                SaltIconButton(
                    onClick = { /* TODO(M3)：编辑歌曲信息动作单，本次仅入口占位 */ },
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = PlayerTints.modeBtn,
                )
            }
        }
    }
}

// ---------- 进度区 ----------

/**
 * `.progress-range`：m-range 轨道 6px（hairline 底 + primary 填充），thumbWrap 整体隐藏
 * （Vue `:deep([style*='inset-inline-start']) display:none`）→ Compose 直接自绘双轨无 thumb。
 * 行为基座自绘拖动：按下即锁 seek 手势（onProgressGestureStart 隔离语义），抬起 seek。
 */
@Composable
private fun ProgressSlider(
    positionMs: Long,
    durationMs: Long,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val salt = LocalSaltColors.current
    var previewFraction by remember { mutableStateOf<Float?>(null) }
    val canSeek = durationMs > 0
    val currentFraction =
        if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier.height(28.dp), // .m-range height: 28px 热区
        contentAlignment = Alignment.CenterStart,
    ) {
        // __track-bg：6px hairline 圆角轨道
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(salt.hairline))
        // __track-value：primary 填充（拖动中用本地 preview 跟手）
        val fraction = previewFraction ?: currentFraction
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(salt.primary),
            )
        }
        if (canSeek) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(durationMs) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // 消费整段手势：隔离 overlay 全局方向手势（onProgressGestureStart 语义）
                            down.consume()
                            onSeekStart()
                            fun fractionAt(x: Float): Float =
                                (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            var last = fractionAt(down.position.x)
                            previewFraction = last
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    // 系统取消（父级已消费 = ACTION_CANCEL 语义）不 seek，仅丢弃 preview
                                    val cancelled = event.changes.any { it.isConsumed }
                                    change.consume()
                                    if (!cancelled) {
                                        onSeekEnd((last * durationMs).toLong())
                                    }
                                    break
                                }
                                change.consume()
                                last = fractionAt(change.position.x)
                                previewFraction = last
                            }
                            previewFraction = null
                        }
                    },
            )
        }
    }
}

/** `.player-page__time-row`：两端时间 12px tabular-nums white68；中央「缓冲中」提示位 */
@Composable
private fun TimeRow(
    positionMs: Long,
    durationMs: Long,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(top = 2.dp), // margin-top 2px
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(positionMs),
            color = PlayerTints.timeRow,
            fontSize = 12.sp,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
        )
        // 缓冲提示位：Web 是 v-if 移除节点（非 opacity 占位），跟随源码条件渲染
        if (isBuffering) {
            Text(
                text = "缓冲中",
                color = PlayerTints.bufferHint,
                fontSize = 11.sp,
            )
        }
        Text(
            text = if (durationMs > 0) formatTime(durationMs) else "--:--",
            color = PlayerTints.timeRow,
            fontSize = 12.sp,
            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

// ---------- 五行歌词小窗 ----------

/**
 * 五行小窗行模型（key 对齐 Vue lyricWindow：prev2/prev/current/next/next2）。
 * lineIndex = 该行在完整歌词列表中的下标（越界为负），供切行动画 key 消歧——相邻行文本
 * 可能相同（空行常见），仅用 text 作 key 会碰撞导致 LaunchedEffect 不重启。
 */
private data class MiniRow(val key: String, val lineIndex: Int, val text: String, val isCurrent: Boolean)

/** 五行滚动窗口数据（AMLL 式：当前行居中；恒定五行空行占位防跳动） */
private fun buildMiniWindow(lines: List<AmllLyricLine>, positionMs: Long): List<MiniRow> {
    var currentIdx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) currentIdx = i else break
    }
    fun textOf(i: Int): String =
        lines.getOrNull(i)?.words?.joinToString(separator = "") { it.word } ?: ""
    return listOf(
        MiniRow("prev2", currentIdx - 2, textOf(currentIdx - 2), isCurrent = false),
        MiniRow("prev", currentIdx - 1, textOf(currentIdx - 1), isCurrent = false),
        MiniRow("current", currentIdx, textOf(currentIdx), isCurrent = true),
        MiniRow("next", currentIdx + 1, textOf(currentIdx + 1), isCurrent = false),
        MiniRow("next2", currentIdx + 2, textOf(currentIdx + 2), isCurrent = false),
    )
}

/**
 * `.player-page__song-meta` / `__meta-window`：五行连续滚动窗口。视口 79px 固定不跳动；
 * 基准 translateY(-29.5px) 当前行居中；切行整体上移一行（400ms cubic-bezier(.32,.72,0,1)）
 * 完成后换窗口并复位——视觉连续无跳。
 */
@Composable
private fun LyricMiniWindow(
    lines: List<AmllLyricLine>,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    val slotPx = with(LocalDensity.current) { MINI_SLOT.toPx() }
    val window = buildMiniWindow(lines, positionMs)
    var displayed by remember { mutableStateOf(window) }
    var scrolling by remember { mutableStateOf(false) }
    val shift = remember { Animatable(0f) }
    val latestWindow by rememberUpdatedState(window)
    val latestDisplayed by rememberUpdatedState(displayed)

    // 切行动画驱动：以「当前行下标:文本」为 key（对齐 Vue watch lyricContext.current；
    // 仅用文本会在相邻空行等同文本场景碰撞失效）
    LaunchedEffect(window.first { it.isCurrent }.let { "${it.lineIndex}:${it.text}" }) {
        if (scrolling) return@LaunchedEffect // 动画期间保持旧窗口渲染，完成后换新窗口
        val next = latestWindow
        val prevCurrent = latestDisplayed.first { it.isCurrent }.text
        when {
            // 非切行同步（初始/暂停停留）：直接更新
            next.first { it.isCurrent }.text == prevCurrent -> displayed = next
            // 窗口整体重置（切歌/seek 大跳）：新窗口 prev ≠ 旧 current，直接换窗口避免误触滚动
            next[1].text != prevCurrent || next[1].text.isEmpty() -> displayed = next
            // 相邻切行：单段连续上移（丝滑缓动），完成后窗口数据上移一位 + 复位（视觉无跳）
            else -> {
                scrolling = true
                try {
                    shift.snapTo(0f)
                    shift.animateTo(
                        targetValue = -slotPx,
                        animationSpec = tween(400, easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)),
                    )
                    displayed = next
                    shift.snapTo(0f)
                } finally {
                    scrolling = false
                }
            }
        }
    }

    Box(modifier.height(MINI_WINDOW_HEIGHT).clipToBounds()) {
        Column(
            // 基准偏移 -29.5px（当前行居中于视口）+ 切行动画位移
            Modifier
                .graphicsLayer { translationY = -slotPx + shift.value }
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp), // __meta-line + margin-top 10px
        ) {
            displayed.forEach { row -> MiniLineRow(row) }
        }
    }
}

/**
 * `.player-page__meta-line`：13px/1.5，当前行白92 放大 1.05，其余白60 缩小 0.92 + blur 0.6px。
 * 动效 spring stiffness 240 / damping 26（mass=1 → 阻尼比 ζ = 26/(2√240) ≈ 0.84）；
 * transform-origin left center（缩放从左缘开始，行左对齐不偏移）。
 */
@Composable
private fun MiniLineRow(row: MiniRow) {
    val motionSpec = spring<Float>(dampingRatio = 0.84f, stiffness = 240f)
    val opacity by animateFloatAsState(
        targetValue = if (row.isCurrent) 1f else 0.55f,
        animationSpec = motionSpec,
        label = "meta-opacity",
    )
    val scale by animateFloatAsState(
        targetValue = if (row.isCurrent) 1.05f else 0.92f,
        animationSpec = motionSpec,
        label = "meta-scale",
    )
    val blurRadius by animateDpAsState(
        targetValue = if (row.isCurrent) 0.dp else 0.6.dp,
        label = "meta-blur",
    )
    Text(
        text = row.text,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
                alpha = opacity
            }
            .blur(blurRadius, BlurredEdgeTreatment.Rectangle),
        color = if (row.isCurrent) PlayerTints.metaCurrent else PlayerTints.metaLine,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
    )
}

// ---------- 歌词面板 ----------

/**
 * `.player-page__lyric-panel`：AMLL WebView 嵌入本面板区域（背景+歌词一体，取舍见
 * PlayerBackdrop TODO 注释）；无词时空态文案。FAB 组浮层：用户滚动歌词后浮现，
 * [LYRIC_FAB_IDLE_MS] 无操作淡出（fade 0.2s）。
 */
@Composable
private fun LyricPanel(
    payloadJson: String?,
    lyricPositionFlow: StateFlow<Long>,
    isPlayingFlow: StateFlow<Boolean>,
    hasLyrics: Boolean,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    isPlaying: Boolean,
    chromeVisible: Boolean,
    onUserActivity: () -> Unit,
    onToggleTranslation: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reportActivity by rememberUpdatedState(onUserActivity)
    Box(modifier.clipToBounds()) {
        // 背景与歌词解耦（Web 层 #20）：无歌词也不卸载 WebView——AMLL MeshGradient
        // 流体背景照常渲染（封面取色），空态由前端 empty-state 承担
        AmllWebView(
            payloadJson = payloadJson,
            positionMsFlow = lyricPositionFlow,
            isPlaying = isPlayingFlow,
            modifier = Modifier.fillMaxSize(),
        )


        // 用户滚动歌词浮现 FAB：Initial pass 观测按压/移动（WebView 自行消费事件，观测不拦截）
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // 按压/拖动中任意事件都视为用户活动（WebView 自行消费事件，观测不拦截）
                            if (event.changes.any { it.pressed }) {
                                reportActivity()
                            }
                        }
                    }
                },
        )

        // __lyric-fabs：left/right 12px，bottom 8px+safe-area；split=space-between / end=flex-end
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement =
                    if (hasTranslation) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasTranslation) {
                    SaltIconButton(
                        onClick = {
                            reportActivity()
                            onToggleTranslation()
                        },
                        imageVector = Icons.Filled.Translate,
                        contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译",
                        tint = PlayerTints.modeBtn,
                    )
                }
                SaltIconButton(
                    onClick = {
                        reportActivity()
                        onPlayPause()
                    },
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                    size = SaltIconButtonSize.LG,
                    tint = Color.White,
                )
            }
        }
    }
}

/** 格式化时长为 m:ss */
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** 队列页 —— QueuePage.vue 一比一翻译 */
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
        modifier = modifier
            .fillMaxSize()
            .background(LocalSaltColors.current.surface),
    ) {
        // __header：标题 + 清空/关闭
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SaltSpacing.spacing, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row {
                // 对齐 Vue v-if="queueState.hasItems"：空队列不显示清空按钮
                if (queue.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清空队列",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.clearQueue() },
                    )
                    Spacer(Modifier.width(16.dp))
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭队列",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onClose),
                )
            }
        }

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier.background(
                            if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playAtIndex(index) }
                                .padding(horizontal = SaltSpacing.spacing, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "未知歌曲",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                (index + 1).toString(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "从队列删除",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.removeQueueItemAt(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

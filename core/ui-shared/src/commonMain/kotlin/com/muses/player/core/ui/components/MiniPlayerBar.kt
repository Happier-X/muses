package com.muses.player.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.HazeBlurStyleData
import com.muses.player.core.ui.theme.LocalHazeBlurState
import androidx.compose.foundation.isSystemInDarkTheme
import com.muses.player.core.ui.theme.musesBottomBarHazeStyle
import com.muses.player.core.uishared.platform.platformBlurModifier
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 滑动切歌：累计位移阈值（超过即切歌）。 */
private val SwipeToSkipThreshold = 48.dp

/** 滑动切歌：快速甩动手势的最小起始位移（防误触）。 */
private val SwipeToSkipFlingMinOffset = 16.dp

/** 滑动切歌：判定为甩动手势的最小速度（px/s）。 */
private const val SwipeToSkipMinFlingVelocity = 800f

/** 滑动切歌：切歌提示文字滑入的时长（ms）。 */
private const val SwipeToSkipHintMillis = 180

/**
 * 滑动切歌：提示初位在区外垫的额外距离（dp）。
 * 提示盒初位藏在「一区宽 + 该垫距」处，手指划过垫距后提示才从边缘冒头，
 * 避免一动就露、也避免藏太深要划很远。
 */
private val SwipeToSkipHintLead = 20.dp

/** 滑动切歌：未达阈值松手后的回弹动画规格。 */
private fun swipeSnapBackSpec() = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * `.mini-player` —— 底部迷你播放条（MiniPlayer.vue 一比一翻译）。
 * 数据经参数传入（P1 接线时由 app 层喂 ViewModel 状态），本组件不接 ViewModel。
 *
 * 视觉契约：
 * - 高 64px；左右 18px 悬浮（定位由页面控制，此处只画胶囊本体）；
 *   底部避让 safe-bottom + 8px 同样由页面布局负责；
 * - 液态玻璃（真磨砂）：`--m-glass-bg` + Haze `blur 20dp` + 白/黑 tint（暗 0.42 / 明 0.56），
 *   `border-radius: 40px` 胶囊 + `border: 1px solid rgba(255,255,255,.5)`（暗色 .12）；
 *   Haze 生效时由 [LocalMusesHazeState] 的 `hazeEffect` 提供实时背景模糊，
 *   无 Haze 时回退为 0.75 alpha 的纯色底（见 [SaltColors.glassBg]）；
 * - box-shadow：`inset 0 1px 0 rgba(255,255,255,.65)`（暗 .1）+
 *   `0 4px 16px rgba(0,0,0,.08)`（暗 .35）；
 * - 行内 gap `--m-spacing-sub`(12px)、水平 padding `--m-spacing`(16px)；
 * - 封面 48px；标题 15px/600/1.25 单行省略；副标题 13px/1.3/`--m-text-2`
 *   单行省略，两行间距 3px（`__info { gap: 3px }`）；
 * - 控制组 gap 2px，图标 18px 实心（Tabler Filled 系）；
 * - 无歌空态：显示「暂无播放歌曲 / 未知艺术家」占位文案（宽屏副标题带「- 未知专辑」，
 *   由调用方按断点决定），整条不可点、播放键禁用（`.mini-player--empty` + aria-disabled）。
 */
@Composable
fun MiniPlayerBar(
    title: String,
    subtitle: String,
    coverUri: String?,
    isPlaying: Boolean,
    onOpenPlayer: () -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    /** 是否有当前曲目（false = 空态：整条不可点、播放键禁用） */
    hasSong: Boolean = true,
    /** 左滑 → 下一曲（null = 不支持滑动切歌） */
    onNext: (() -> Unit)? = null,
    /** 右滑 → 上一曲（null = 不支持滑动切歌） */
    onPrevious: (() -> Unit)? = null,
) {
    val scheme = MiuixTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val capsuleShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)

    val borderColor = scheme.outline

    val clickInteraction = remember { MutableInteractionSource() }

    // 平台模糊风格数据
    // LocalHazeBlurState 由 app 层（TabsLayout）provide，值与 LocalMusesHazeState 相同
    val hazeState = LocalHazeBlurState.current
    val hazeStyle: HazeBlurStyleData = musesBottomBarHazeStyle(isDark)

    // ── 滑动切歌：左滑（offset < 0）→ 下一曲，右滑（offset > 0）→ 上一曲 ──
    // 空态或未提供回调时禁用滑动；阈值以外的小幅拖动松手后忽略（不切歌）。
    // 交互：仅中间文字区（__info）跟手平移，封面与右侧控制按钮保持不动；
    // 对侧的「上一曲/下一曲」纯文字提示同样跟手从区外被拖进来
    // （与当前文字同向同速平移，translate = offset -/+ 区宽）；
    // 达阈值松手触发切歌，未达阈值松手弹簧回弹。
    val scope = rememberCoroutineScope()
    // 拖动中直接同步写状态跟手（零延迟），松手后才跑协程结算动画
    var swipeOffsetPx by remember { mutableStateOf(0f) }
    var infoWidthPx by remember { mutableStateOf(0) }
    var isSwipeSettling by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SwipeToSkipThreshold.toPx() }
    val flingMinOffsetPx = with(density) { SwipeToSkipFlingMinOffset.toPx() }
    val swipeEnabled = hasSong && (onNext != null || onPrevious != null)
    // 拖动方向：负 = 左滑露「下一曲」，正 = 右滑露「上一曲」
    val hintDirection = when {
        swipeOffsetPx < 0 && onNext != null -> -1
        swipeOffsetPx > 0 && onPrevious != null -> 1
        else -> 0
    }
    fun settleSwipe(velocity: Float) {
        if (!swipeEnabled || isSwipeSettling) return
        val offset = swipeOffsetPx
        val toNext = offset <= -swipeThresholdPx ||
            (velocity < -SwipeToSkipMinFlingVelocity && offset <= -flingMinOffsetPx)
        val toPrevious = offset >= swipeThresholdPx ||
            (velocity > SwipeToSkipMinFlingVelocity && offset >= flingMinOffsetPx)
        scope.launch {
            isSwipeSettling = true
            try {
                if (toNext || toPrevious) {
                    if (toNext) onNext?.invoke() else onPrevious?.invoke()
                    // 切歌后提示快退、文字区回位
                    animate(
                        initialValue = swipeOffsetPx,
                        targetValue = 0f,
                        animationSpec = tween<Float>(SwipeToSkipHintMillis),
                    ) { v, _ -> swipeOffsetPx = v }
                    swipeOffsetPx = 0f
                } else {
                    // 未达阈值：弹簧回弹
                    animate(
                        initialValue = swipeOffsetPx,
                        targetValue = 0f,
                        animationSpec = swipeSnapBackSpec(),
                    ) { v, _ -> swipeOffsetPx = v }
                    swipeOffsetPx = 0f
                }
            } finally {
                isSwipeSettling = false
            }
        }
    }
    val infoDragState = rememberDraggableState { delta ->
        if (swipeEnabled && !isSwipeSettling) {
            swipeOffsetPx += delta
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            // TODO(U4): SaltShadows（android.graphics.BlurMaskFilter）暂不迁入 commonMain，
            // 完整阴影配方待 SaltShadows 完成跨平台抽象后恢复。
            .clip(capsuleShape)
            .then(
                platformBlurModifier(
                    isDark = isDark,
                    backgroundColor = scheme.surface.copy(alpha = 0.75f),
                    hazeState = hazeState,
                    hazeStyleData = hazeStyle,
                ),
            )
            .border(border = BorderStroke(1.dp, borderColor), shape = capsuleShape)
            .clickable(
                interactionSource = clickInteraction,
                indication = null,
                enabled = hasSong,
                onClick = onOpenPlayer,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MusesCover(uri = coverUri, size = 48.dp, radius = MusesCoverRadius.MD)

        // __info：gap 3px，flex:1 min-width:0
        // 滑动区：仅本列跟手平移 + 接收水平拖动，对侧拖入纯文字切歌提示
        // clip 保证文字滑出本区时被裁掉，不会压到封面/按钮上
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .draggable(
                    state = infoDragState,
                    orientation = Orientation.Horizontal,
                    enabled = swipeEnabled,
                    onDragStopped = { settleSwipe(it) },
                )
                .onSizeChanged { infoWidthPx = it.width },
        ) {
            // 底层：纯文字切歌提示（跟手从区外拖入，与当前文字同向同速）
            // 提示盒初位藏在「一区宽 + 前垫距」处；文字贴在盒子先进来的那一侧
            // （左滑露「下一曲」贴左端、右滑露「上一曲」贴右端），
            // 手指划过垫距后提示从边缘冒头。
            if (hintDirection != 0) {
                // 左滑（direction=-1）：提示初位在区右外（+宽+垫距），随手指左移进入；
                // 右滑（direction=+1）：提示初位在区左外（-宽-垫距），随手指右移进入。
                val widthPx = infoWidthPx.coerceAtLeast(1).toFloat()
                val leadPx = with(density) { SwipeToSkipHintLead.toPx() }
                val hintOffset = swipeOffsetPx - hintDirection * (widthPx + leadPx)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { translationX = hintOffset }
                        .padding(horizontal = 4.dp),
                    contentAlignment = if (hintDirection < 0) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Text(
                        text = if (hintDirection < 0) "下一曲" else "上一曲",
                        style = MiuixTheme.textStyles.footnote1,
                        color = scheme.onBackgroundVariant,
                    )
                }
            }
            // 顶层：当前歌曲文字（跟手平移，占满宽度；高度自适应以撑起滑动区）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = swipeOffsetPx },
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title, // 默认「暂无播放歌曲」由调用方按空态传
                    style = MiuixTheme.textStyles.body1,
                    lineHeight = (15f * 1.25f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle, // 「{artist} - {album}」由调用方拼装
                    style = MiuixTheme.textStyles.footnote1,
                    lineHeight = (13f * 1.3f).sp,
                    color = scheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // __controls：gap 2px，图标 18px（md 触控区 40px 不变）
        // 控制区需消费点击避免冒泡至外层 Row 的 onOpenPlayer（修复点击播放按钮同时打开播放页）
        // 外层 Box 以空 clickable 消费非按钮区域的 gap 点击；按钮自身可点击已天然拦截冒泡
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MusesIconButton(
                    onClick = onTogglePlayback,
                    imageVector = if (isPlaying) TablerIcons.PauseFill else TablerIcons.PlayFill, // fill 风格播放/暂停
                    contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                    enabled = hasSong, // :disabled="!currentSong || status==='loading'"
                    tint = scheme.onBackground, // __btn { color: var(--m-text) }
                    iconSizeOverride = 18.dp, // __icon { width: 18px }
                )
                MusesIconButton(
                    onClick = onOpenQueue,
                    imageVector = TablerIcons.QueueMusic, // tabler playlist
                    contentDescription = "打开播放队列",
                    tint = scheme.onBackground,
                    iconSizeOverride = 18.dp,
                )
            }
        }
    }
}

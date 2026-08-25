package com.muses.player.nativem1.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltSpacing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * `.tabs-layout` —— 主框架双形态导航（TabsPage.vue 一比一翻译）。
 *
 * 结构对照：
 * - 宽屏（≥768px，Web 断点口径）→ `.tabs-layout__aside`：固定左侧栏，宽 260px，
 *   surface-1 底、右侧 1px hairline；分组为非卡片形态（次组 border-top 分隔）；
 * - 窄屏 → `.tabs-layout__track` 推屏轨道：抽屉面板（50vw）+ 主内容（100vw）整体平移，
 *   开 = translateX(0)、关 = translateX(-50vw)，动画 240ms cubic-bezier(0.32,0.72,0,1)；
 *   抽屉内主/次菜单各为一张圆角卡（surface-1 + 1px hairline 描边、16px 圆角、**无阴影**，
 *   08-16 三版迭代定案）；透明关闭交互区覆盖被推开的主页面承接点击关闭；
 * - 导航项 `.tabs-layout__nav-link`：min-height 64px、水平 padding --m-spacing、
 *   图标壳固定 60px（图标 24px 居中）、文字 16px/--m-text；
 *   **激活态与普通项完全一致**（08-16 用户定案：无加粗、无背景色差），active 仅作语义。
 *
 * 手势对照（onPageTouchStart/Move/End）：任意位置右滑开抽屉、开态左滑关抽屉；
 * 水平位移锁 8px（Compose detectHorizontalDragGestures 的 touch slop 天然承担垂直让位）；
 * 结算阈值：位移 ≥ 抽屉宽 25%（SETTLE_RATIO）或快扫速度 ≥ 0.5 px/ms（FAST_SWIPE_PX_PER_MS）。
 */

/** Web 断点口径：viewportWidth >= 768 即平板形态 */
private val TabletBreakpoint = 768.dp

/** `.tabs-layout__aside { width: 260px }` */
private val AsideWidth = 260.dp

/** 抽屉宽度 = 视口 50vw（`__drawer { flex: 0 0 50vw }`） */
private const val DrawerWidthFraction = 0.5f

/** 关闭态轨道位移系数：Web 为 translateX(-50vw)，位移相对视口宽；
    抽屉宽本身即 50vw，故对抽屉宽度而言系数为 -1 */
private const val TrackClosedFraction = -1f

/** drawerTransition duration 0.24 / ease [0.32, 0.72, 0, 1] */
private val DrawerEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val DrawerAnimMs = 240

/** HORIZONTAL_LOCK_PX = 8（方向锁，由触摸滑动阈值近似承担） */
private const val SettleRatio = 0.25f          // SETTLE_RATIO
private const val FastSwipePxPerMs = 0.5f      // FAST_SWIPE_PX_PER_MS

/** 导航项（RouterLink 的 Compose 对应物入参） */
data class SaltNavItem(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

/**
 * 双形态主框架。
 *
 * @param navVisible false 时隐藏侧边栏/抽屉（播放页/队列页等覆盖路由全屏呈现，
 *   对照 Web 层 popup 盖住 tabs-layout 的观感）
 * @param bottomBar 叠加在内容区之上的底部悬浮层（MiniPlayer，z 序高于抽屉关闭区）
 */
@Composable
fun TabsLayout(
    primaryItems: List<SaltNavItem>,
    secondaryItems: List<SaltNavItem>,
    navVisible: Boolean,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (!navVisible) {
            // 覆盖路由形态：无导航 chrome，全屏内容
            Box(Modifier.fillMaxSize()) { content() }
            return@BoxWithConstraints
        }

        val isTablet = maxWidth >= TabletBreakpoint
        if (isTablet) {
            TabletLayout(primaryItems, secondaryItems, Modifier, content)
        } else {
            PhoneLayout(
                primaryItems = primaryItems,
                secondaryItems = secondaryItems,
                containerWidth = maxWidth,
                bottomBar = bottomBar,
                content = content,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 宽屏 aside 形态
// ---------------------------------------------------------------------------

/** `.tabs-layout__aside` + `.tabs-layout__panel`（非卡片分组形态） */
@Composable
private fun TabletLayout(
    primaryItems: List<SaltNavItem>,
    secondaryItems: List<SaltNavItem>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val salt = LocalSaltColors.current
    Row(modifier.background(salt.surface)) {
        // __aside：fixed top/left/bottom，width 260px，bg surface-1，border-right hairline
        Column(
            Modifier
                .width(AsideWidth)
                .fillMaxHeight()
                .background(salt.surface1),
        ) {
            // padding-top: var(--m-safe-area-top)（原生取状态栏 inset）
            Spacer(Modifier.statusBarsPadding())
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // __nav--primary（aside 非卡片形态）：仅 padding-top 8px
                NavGroup(
                    items = primaryItems,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // __nav--secondary（aside）：margin-top 9px + padding-top 9px + border-top hairline
                SecondaryGroupDivider()
                NavGroup(
                    items = secondaryItems,
                    modifier = Modifier.padding(top = 9.dp),
                )
                // 底部安全区留白（aside 可滚动）
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        // aside 的 border-right：1px hairline 竖线
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(salt.hairline),
        )
        // __main--tabbed：left 260px 起铺满
        Box(Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// 窄屏抽屉推屏形态
// ---------------------------------------------------------------------------

/** 推屏轨道手势会话（对照 onPageTouchStart/Move/End 的局部变量） */
private class DragSession {
    var mode: Mode? = null
    var totalDx = 0f
    var tracker: VelocityTracker? = null

    enum class Mode { OPENING, CLOSING }

    fun reset(mode: Mode?) {
        this.mode = mode
        totalDx = 0f
        tracker = null
    }
}

@Composable
private fun PhoneLayout(
    primaryItems: List<SaltNavItem>,
    secondaryItems: List<SaltNavItem>,
    containerWidth: Dp,
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val salt = LocalSaltColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val drawerWidth = containerWidth * DrawerWidthFraction
    val drawerWidthPx = with(density) { drawerWidth.roundToPx() }.toFloat()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    // NaN = 尚未初始化（offset lambda 按关态兜底），首个 effect snap 到真实位置
    val trackOffsetX = remember { Animatable(Float.NaN) }

    // 尺寸变化时同步轨道位置（updateViewportWidth 的对应处理）
    LaunchedEffect(drawerWidthPx) {
        val target = if (drawerOpen) 0f else drawerWidthPx * TrackClosedFraction
        if (trackOffsetX.value.isNaN() || trackOffsetX.value != target) {
            trackOffsetX.snapTo(target)
        }
    }
    // 覆盖路由切入时强制复位（Web 层 watch playerOverlayVisible → closeDrawer）
    LaunchedEffect(drawerOpen) {
        if (!drawerOpen) trackOffsetX.snapTo(drawerWidthPx * TrackClosedFraction)
    }

    suspend fun animateTrack(targetX: Float) =
        trackOffsetX.animateTo(targetX, tween(DrawerAnimMs, easing = DrawerEasing))

    fun closeDrawer() {
        scope.launch {
            animateTrack(drawerWidthPx * TrackClosedFraction)
            drawerOpen = false
        }
    }

    fun openDrawer() {
        drawerOpen = true
        scope.launch { animateTrack(0f) }
    }

    // Escape 关抽屉的等价物：系统返回键优先用于关抽屉
    BackHandler(enabled = drawerOpen) { closeDrawer() }

    val drag = remember { DragSession() }

    Box(
        Modifier
            .fillMaxSize()
            .background(salt.surface)
            // onPageTouchStart/Move/End：任意位置右滑开 / 开态左滑关；
            // 垂直滚动让位由 detectHorizontalDragGestures 的水平 touch slop 承担
            // （≈ HORIZONTAL_LOCK_PX 方向锁）。播放/队列覆盖路由打开时不挂手势
            // （isGestureBlocked 对 .m-popup/.m-sheet 的拦截语义）。
            .pointerInput(drawerOpen) {
                detectHorizontalDragGestures(
                    onDragStart = { _ ->
                        drag.reset(
                            if (drawerOpen) DragSession.Mode.CLOSING else DragSession.Mode.OPENING,
                        )
                        drag.tracker = VelocityTracker()
                    },
                    onDragEnd = {
                        val mode = drag.mode ?: return@detectHorizontalDragGestures
                        val velocity = drag.tracker?.calculateVelocity()?.x ?: 0f
                        val crossedDistance = abs(drag.totalDx) >= drawerWidthPx * SettleRatio
                        val crossedVelocity = abs(velocity) / 1000f >= FastSwipePxPerMs
                        val shouldOpen = if (mode == DragSession.Mode.OPENING) {
                            crossedDistance || crossedVelocity
                        } else {
                            !(crossedDistance || crossedVelocity)
                        }
                        drag.reset(null)
                        if (shouldOpen) openDrawer() else closeDrawer()
                    },
                    onDragCancel = {
                        // cancelTouchGesture：回弹到拖拽前状态
                        val wasOpening = drag.mode == DragSession.Mode.OPENING
                        drag.reset(null)
                        if (wasOpening != drawerOpen) {
                            if (wasOpening) closeDrawer() else openDrawer()
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    drag.tracker?.addPosition(change.uptimeMillis, change.position)
                    drag.totalDx += dragAmount
                    val raw = when (drag.mode) {
                        DragSession.Mode.OPENING ->
                            drawerWidthPx * TrackClosedFraction + drag.totalDx
                        DragSession.Mode.CLOSING -> drag.totalDx
                        null -> return@detectHorizontalDragGestures
                    }
                    val clamped = raw.coerceIn(drawerWidthPx * TrackClosedFraction, 0f)
                    scope.launch { trackOffsetX.snapTo(clamped) }
                }
            },
    ) {
        // __track：150vw 轨道 = 抽屉(50vw) + 主内容(100vw) 首尾相连，整体平移。
        // 关态 offset=-50vw → 抽屉全出屏、主内容恰落 x=0；开态 offset=0。
        // （初版两节点各自 offset 的写法会让关态抽屉右半截留在屏内——已废弃）
        Row(
            Modifier
                // requiredWidth：轨道 150vw 必须无视父级 100vw 约束，
                // 否则被压缩后主内容只剩 50vw 宽（实测踩坑）
                .requiredWidth(drawerWidth + containerWidth)
                .fillMaxHeight()
                .offsetX {
                    val v = trackOffsetX.value
                    // 初值 NaN 时按关态取位，避免首帧闪现打开态
                    (if (v.isNaN()) -drawerWidthPx else v).roundToInt()
                },
        ) {
            DrawerPanel(
                primaryItems = primaryItems,
                secondaryItems = secondaryItems,
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight(),
            )

            Box(
                Modifier
                    .width(containerWidth)
                    .fillMaxHeight()
            ) {
                content()
            }
        }

        // __drawer-dismiss：透明关闭交互区，覆盖被推开主页面可视区域（50vw..100vw）
        if (drawerOpen) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(start = drawerWidth)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { closeDrawer() },
            )
        }

        // MiniPlayer（z-index 1000）：层级高于 drawer-dismiss(30)
        Box(Modifier.align(Alignment.BottomCenter)) { bottomBar() }
    }
}

/** Modifier.offset 的 lambda 版快捷（避免拖拽期间逐帧重组） */
private fun Modifier.offsetX(x: () -> Int): Modifier =
    this.offset { IntOffset(x(), 0) }

// ---------------------------------------------------------------------------
// 导航分组与导航项（两种形态共用）
// ---------------------------------------------------------------------------

/**
 * `.tabs-layout__nav`（抽屉卡片形态）：主/次菜单各为一张圆角卡 ——
 * 左 18px + 右 12px 外边距、surface-1 底、1px hairline 描边、16px 圆角、
 * 上下 padding 8px、**无阴影**（椒盐实测空隙区纯色无投影，08-16 二版臆造已撤）。
 */
@Composable
private fun NavGroupCard(
    items: List<SaltNavItem>,
    modifier: Modifier = Modifier,
) {
    val salt = LocalSaltColors.current
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .padding(start = 18.dp, end = 12.dp)
            .background(salt.surface1, cardShape)
            .border(1.dp, salt.hairline, cardShape)
            .padding(vertical = 8.dp),
    ) {
        items.forEach { item -> SaltNavLink(item, inDrawer = true) }
    }
}

/** 卡片形态抽屉面板：`.tabs-layout__panel` + 两张 nav 卡（次卡与主卡间距 18px） */
@Composable
private fun DrawerPanel(
    primaryItems: List<SaltNavItem>,
    secondaryItems: List<SaltNavItem>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // padding: calc(var(--m-navbar-pt) + var(--m-spacing)) 0 calc(var(--m-spacing-sub) + safe-bottom) 0
    val navbarPt = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }.coerceAtLeast(SaltSpacing.navbarTopPaddingMin)
    val navBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(navbarPt + SaltSpacing.spacing))
        NavGroupCard(primaryItems)
        NavGroupCard(secondaryItems)
        Spacer(Modifier.height(SaltSpacing.spacingSub + navBottom))
    }
}

/** `.tabs-layout__nav`（aside 非卡片形态）：一组 nav-link 纵排 */
@Composable
private fun NavGroup(
    items: List<SaltNavItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        items.forEach { item -> SaltNavLink(item) }
    }
}

/** aside 次组的 border-top: 1px solid var(--m-hairline) */
@Composable
private fun SecondaryGroupDivider() {
    val salt = LocalSaltColors.current
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(9.dp)
            .drawBehind {
                drawRect(
                    color = salt.hairline,
                    topLeft = Offset.Zero,
                    size = Size(size.width, 1.dp.toPx()),
                )
            },
    )
}

/**
 * `.tabs-layout__nav-link`：min-height 64px、padding 0 --m-spacing、radius-sm 圆角、
 * 无涟漪（浏览器默认焦点环/高亮已隐藏的对应处理）；图标壳 flex 0 0 60px 居中、
 * 图标恒灰（--m-text-2，用户定案：激活项图标不变蓝）、文字 16px --m-text。
 * **激活态视觉与普通项完全一致**（08-16 定案），[SaltNavItem.active] 仅保留语义用途。
 *
 * [inDrawer] = `.tabs-layout__drawer-link` 变体：width 100% + **padding-left 0**
 * （`.tabs-layout__drawer &__nav-link { padding-left: 0 }` —— 18px 卡片空隙 +
 * 60px 图标列使文字自 ~78px 起，对齐椒盐文字 x204px 实测）。
 */
@Composable
private fun SaltNavLink(
    item: SaltNavItem,
    inDrawer: Boolean = false,
) {
    val salt = LocalSaltColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .then(if (inDrawer) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 64.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = item.onClick,
            )
            .clip(RoundedCornerShape(SaltRadius.sm))
            .padding(
                start = if (inDrawer) 0.dp else SaltSpacing.spacing,
                end = SaltSpacing.spacing,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // __nav-icon-shell：flex 0 0 60px，图标居中于 30px 处
        Box(
            Modifier.size(width = 60.dp, height = SaltSpacing.listIcon),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null, // aria-hidden
                tint = salt.text2,
                modifier = Modifier.size(SaltSpacing.listIcon),
            )
        }
        // __nav-label：font-size 16px / --m-text
        Text(
            text = item.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = salt.text,
        )
    }
}

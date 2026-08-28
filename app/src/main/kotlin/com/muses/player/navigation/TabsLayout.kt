package com.muses.player.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
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
 *   surface-1 底、右侧 1px hairline；
 * - 窄屏 → 推屏：抽屉面板（50vw）+ 主内容（100vw），开合进度驱动两者独立偏移，
 *   动画 240ms cubic-bezier(0.32,0.72,0,1)；透明关闭交互区覆盖被推开的主页面；
 * - 导航项 `.tabs-layout__nav-link`：min-height 64px、图标壳固定 60px、文字 16px；
 *   **激活态与普通项完全一致**（08-16 用户定案）。
 *
 * 手势：任意位置右滑开抽屉、开态左滑关抽屉；结算阈值 = 位移 ≥ 抽屉宽 25%
 * 或快扫 ≥ 0.5 px/ms。
 */

/** Web 断点口径：viewportWidth >= 768 即平板形态 */
private val TabletBreakpoint = 768.dp

/** `.tabs-layout__aside { width: 260px }` */
private val AsideWidth = 260.dp

/** 抽屉宽度 = 视口 50vw（`__drawer { flex: 0 0 50vw }`） */
private const val DrawerWidthFraction = 0.5f

/** drawerTransition duration 0.24 / ease [0.32, 0.72, 0, 1] */
private val DrawerEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val DrawerAnimMs = 240

/** SETTLE_RATIO 与 FAST_SWIPE_PX_PER_MS */
private const val SettleRatio = 0.25f
private const val FastSwipePxPerMs = 0.5f

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
 * @param navVisible false 时隐藏侧边栏/抽屉（播放页/队列页等覆盖路由全屏呈现）
 * @param bottomBar 叠加在内容区之上的底部悬浮层（MiniPlayer）
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
            TabletLayout(
                primaryItems = primaryItems,
                secondaryItems = secondaryItems,
                bottomBar = bottomBar,
                modifier = Modifier,
                content = content,
            )
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

/**
 * `.tabs-layout__aside` + `.tabs-layout__panel`（非卡片分组形态）。
 *
 * [bottomBar]（MiniPlayer）：Web 版 MiniPlayer 无平板覆盖段——平板下仍以
 * fixed left/right 18px 全宽胶囊悬浮，故 aside 形态照常渲染（z 序同手机）。
 */
@Composable
private fun TabletLayout(
    primaryItems: List<SaltNavItem>,
    secondaryItems: List<SaltNavItem>,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val salt = LocalSaltColors.current
    Row(modifier.background(salt.surface)) {
        // 平板 aside 改为卡片形态（对齐手机抽屉的 NavGroupCard），
        // 主/次菜单各为一张圆角卡：surface-1 底、1px hairline、16dp 圆角、
        // 左 18 右 12 空隙，两组间距 18dp（原版仅抽屉用卡片；现统一为卡片）
        Column(
            Modifier
                .width(AsideWidth)
                .fillMaxHeight()
                .background(salt.surface1),
        ) {
            Spacer(Modifier.statusBarsPadding())
            Column(Modifier.verticalScroll(rememberScrollState())) {
                NavGroupCard(items = primaryItems)
                NavGroupCard(
                    items = secondaryItems,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(salt.hairline),
        )
        Box(Modifier.weight(1f).fillMaxHeight()) {
            content()
            // MiniPlayer（z-index 1000）：悬浮于内容区之上，与手机形态同语义
            Box(Modifier.align(Alignment.BottomCenter)) { bottomBar() }
        }
    }
}

// ---------------------------------------------------------------------------
// 窄屏抽屉推屏形态
// ---------------------------------------------------------------------------

/** 推屏手势会话（对照 onPageTouchStart/Move/End 的局部变量） */
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val drawerWidth = containerWidth * DrawerWidthFraction
    val drawerWidthPx = with(density) { drawerWidth.roundToPx() }.toFloat()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }

    // 开合进度：0=关（抽屉全藏、主内容原位） 1=开（抽屉入屏、主内容右推）
    val openFraction = remember { Animatable(0f) }

    LaunchedEffect(drawerOpen) {
        openFraction.animateTo(if (drawerOpen) 1f else 0f, tween(DrawerAnimMs, easing = DrawerEasing))
    }

    fun closeDrawer() {
        drawerOpen = false
    }

    fun openDrawer() {
        drawerOpen = true
    }

    BackHandler(enabled = drawerOpen) { closeDrawer() }

    val drag = remember { DragSession() }

    Box(
        Modifier
            .fillMaxSize()
            .background(LocalSaltColors.current.surface)
            .pointerInput(drawerOpen) {
                detectHorizontalDragGestures(
                    onDragStart = { _ ->
                        // 打断进行中的开合动画，避免 animateTo 与跟手 snapTo 抢控制权
                        scope.launch { openFraction.stop() }
                        drag.reset(
                            if (drawerOpen) DragSession.Mode.CLOSING else DragSession.Mode.OPENING,
                        )
                        drag.tracker = VelocityTracker()
                    },
                    onDragEnd = {
                        val mode = drag.mode ?: return@detectHorizontalDragGestures
                        val velocity = drag.tracker?.calculateVelocity()?.x ?: 0f
                        val crossedDistance = abs(drag.totalDx) >= drawerWidthPx * SettleRatio
                        val crossedVelocity = abs(velocity) >= FastSwipePxPerMs * 1000f
                        val shouldOpen = if (mode == DragSession.Mode.OPENING) {
                            crossedDistance || crossedVelocity
                        } else {
                            !(crossedDistance || crossedVelocity)
                        }
                        drag.reset(null)
                        if (shouldOpen) openDrawer() else closeDrawer()
                    },
                    onDragCancel = {
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
                    // 拖拽直接驱动开合进度：右滑进度增大（开）、左滑减小（关），与模式无关
                    val raw = (openFraction.value + dragAmount / drawerWidthPx).coerceIn(0f, 1f)
                    scope.launch { openFraction.snapTo(raw) }
                }
            },
    ) {
        // __drawer：关态整体左移自身宽度藏出屏。
        // 菜单点击后自动关抽屉（对照 Web 层 onDrawerNavigation）
        val drawerPrimary = primaryItems.map { item ->
            item.copy(onClick = { item.onClick(); closeDrawer() })
        }
        val drawerSecondary = secondaryItems.map { item ->
            item.copy(onClick = { item.onClick(); closeDrawer() })
        }
        DrawerPanel(
            primaryItems = drawerPrimary,
            secondaryItems = drawerSecondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(drawerWidth)
                .fillMaxHeight()
                .offsetX { ((openFraction.value - 1f) * drawerWidthPx).roundToInt() },
        )

        // __main：开态右移一个抽屉宽；向页内 SaltNavbar 提供汉堡打开回调
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .offsetX { (openFraction.value * drawerWidthPx).roundToInt() },
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.muses.player.core.ui.components.LocalSaltOpenDrawer provides { openDrawer() },
            ) {
                content()
            }
        }

        // __drawer-dismiss：透明关闭交互区，覆盖被推开的主页面
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

        // MiniPlayer（z-index 1000）：层级高于 drawer-dismiss
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

/**
 * `.tabs-layout__nav-link`：min-height 64px、radius-sm 圆角、无涟漪；
 * 图标壳 flex 0 0 60px 居中、图标恒灰（--m-text-2）、文字 16px --m-text。
 * **激活态视觉与普通项完全一致**（08-16 定案）。
 *
 * [inDrawer] = `.tabs-layout__drawer-link` 变体：width 100% + padding-left 0
 * （18px 卡片空隙 + 60px 图标列使文字自 ~78px 起，对齐椒盐 x204px 实测）。
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

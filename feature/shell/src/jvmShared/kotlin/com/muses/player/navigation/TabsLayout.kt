package com.muses.player.navigation

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.components.LocalMusesOpenDrawer
import com.muses.player.feature.shell.platform.ShellBackHandler
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.squircle.squircleBackground

/**
 * `.tabs-layout` —— 主框架双形态导航（miuix 官方分工）。
 *
 * - 宽屏（≥768px，Web 断点口径）→ 官方 [NavigationRail]（宽屏专用侧轨，折叠图标+展开药丸二态）；
 * - 窄屏 → **推屏式侧滑抽屉**（内容随抽屉整体右移，椒盐/Salt Player 口径；顶栏汉堡按钮打开）；
 * - 两端同一套目的地集合，选中态同源（NavDestination.isActive），切 tab/子页面压栈逻辑见 MusesApp。
 */

/** Web 断点口径：viewportWidth >= 768 即平板形态（MiniPlayer 副标题宽窄形态共用） */
internal val TabletBreakpoint = 768.dp

/** 导航项（RouterLink 的 Compose 对应物入参） */
data class MusesNavItem(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

/**
 * 双形态主框架。
 *
 * @param navVisible false 时隐藏导航 chrome（播放页/队列页等覆盖路由全屏呈现）
 */
@Composable
fun TabsLayout(
    primaryItems: List<MusesNavItem>,
    secondaryItems: List<MusesNavItem>,
    navVisible: Boolean,
    modifier: Modifier = Modifier,
    /** 窄屏推屏抽屉状态；null = 退化为无抽屉（不应发生，仅兜底） */
    phoneDrawer: PhoneDrawerState? = null,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    androidx.compose.runtime.CompositionLocalProvider(
        // miuix-blur：内容容器 layerBackdrop 捕获背景，磨砂表面经 LocalMusesBackdrop 消费
        com.muses.player.core.ui.theme.LocalMusesBackdrop provides backdrop,
    ) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            androidx.compose.runtime.CompositionLocalProvider(
                // 底部悬浮件避让：窄屏 184dp / 宽屏 96dp（见 BottomChrome），列表页末项避让用，空态不留白
                com.muses.player.core.ui.theme.LocalBottomChromePadding provides if (maxWidth >= TabletBreakpoint) {
                    com.muses.player.core.ui.theme.TabletBottomChromePadding
                } else {
                    com.muses.player.core.ui.theme.PhoneBottomChromePadding
                },
            ) {
            if (!navVisible) {
                // 覆盖路由形态：无导航 chrome，全屏内容
                Box(Modifier.fillMaxSize()) { content() }
            } else {
                val isTablet = maxWidth >= TabletBreakpoint
                if (isTablet) {
                    TabletLayout(
                        primaryItems = primaryItems,
                        secondaryItems = secondaryItems,
                        backdrop = backdrop,
                        modifier = Modifier,
                        content = content,
                    )
                } else {
                    PhoneLayout(
                        primaryItems = primaryItems,
                        secondaryItems = secondaryItems,
                        backdrop = backdrop,
                        phoneDrawer = phoneDrawer,
                        content = content,
                    )
                }
            }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 宽屏 aside 形态
// ---------------------------------------------------------------------------

/**
 * 宽屏侧轨形态：官方 [NavigationRail]（折叠 80dp 图标+文 / 展开 240dp 药丸，内建切换钮）。
 *
 * 配色/分隔/边距全走官方默认（surface 底 + 内容侧分割线 + 自吃系统边衬）；
 * 与窄屏底栏同一套 5 目的地（次组为空时不画分割线）。
 * 迷你条已停靠根 Scaffold bottomBar，此处不再叠加悬浮层。
 */
@Composable
private fun TabletLayout(
    primaryItems: List<MusesNavItem>,
    secondaryItems: List<MusesNavItem>,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Box(modifier.background(scheme.surface)) {
        Row(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            NavigationRail(
                state = rememberNavigationRailState(),
                header = {
                    // 应用标识（对齐桌面标题栏 logo：primary 圆角底 + 音符）
                    Box(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(32.dp)
                            .squircleBackground(scheme.primary, 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TablerIcons.MusicNote,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            ) {
                primaryItems.forEach { item ->
                    NavigationRailItem(
                        selected = item.active,
                        onClick = item.onClick,
                        icon = item.icon,
                        label = item.label,
                    )
                }
                // 次组为空（5 目的地精简态）时不画分割线
                if (secondaryItems.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    secondaryItems.forEach { item ->
                        NavigationRailItem(
                            selected = item.active,
                            onClick = item.onClick,
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 窄屏侧滑抽屉形态
// ---------------------------------------------------------------------------

/**
 * 窄屏推屏抽屉宽度：**220dp**，与椒盐（Salt Player）侧栏同规格
 * （SaltUI 的 `SideBar` 就是 `fillMaxHeight().width(220.dp)`，见 .tmp/saltui/ui2/.../sidebar/SideBar.kt）。
 *
 * internal：壳层（MusesApp）算迷你条同步位移也要用同一个宽度。
 */
internal val PhoneDrawerWidth = 220.dp

/**
 * 窄屏推屏抽屉状态（由**壳层**持有）。
 *
 * 为什么不由 TabsLayout 内部自己管：推屏时「内容」与「底部迷你条」的位移必须完全一致，
 * 而迷你条在 Scaffold 的 bottomBar 槽、内容在 content 槽（TabsLayout 内）——两者是兄弟节点，
 * CompositionLocal 传不过去，只能把状态提到共同父层，用同一份 [progress] 驱动两处位移。
 */
class PhoneDrawerState(
    val open: Boolean,
    /** 0..1 推屏进度（内容与迷你条共用；拖拽中为实时值） */
    val progress: Float,
    val onOpen: () -> Unit,
    val onClose: () -> Unit,
    /** 拖拽中直接写进度（跟手）；松手后由 [onOpen]/[onClose] 按阈值结算 */
    val onDragProgress: (Float) -> Unit = {},
)

/** 拖拽松手时的开合阈值（位置判定；超过一半即展开/收起） */
private const val DrawerOpenThreshold = 0.5f

/**
 * 窄屏内容壳：**推屏式**侧滑抽屉（原悬浮底栏下线）。
 *
 * 交互口径（对照椒盐/Salt Player，与 SaltUI 示例的 `Row { SideBar(220dp); content }` 同源）：
 * 抽屉滑出时**内容整体右移**（被推开）而非被覆盖——
 * 所以无遮罩、抽屉在内容下层、两者共用 surface 同色，靠位移本身区分层次；
 * 这也意味着「点内容区收起」需要一个透明点击层（否则会误触内容）。
 *
 * 为什么自研而不引 material3 的 ModalNavigationDrawer：material3 只有「覆盖 + 遮罩」一种形态，
 * 做不出推屏；而推屏真正需要的只有「两条同步位移 + 一个点击层」，用 foundation 即可。
 *
 * 视觉按椒盐口径：**直角、无阴影、与页面同色**，宽度 220dp（SaltUI 的 `SideBar` 同值）。
 *
 * 手势刻意不做：安卓 10+ 的左右边缘是**系统返回手势**区，自研边缘拖拽会与之冲突；
 * 全局拖拽又会抢内容区横向滚动（首页平台/榜单胶囊等）——项目此前正是因此下线抽屉手势。
 * 入口统一走顶栏汉堡按钮（经 [LocalMusesOpenDrawer] 注入，见 MusesTopBar）。
 *
 * 状态经 [phoneDrawer] 由壳层传入（而非内部自管）：迷你条在 Scaffold 的 bottomBar 槽，
 * 必须与内容用同一份进度位移，详见 [PhoneDrawerState]。
 */
@Composable
private fun PhoneLayout(
    primaryItems: List<MusesNavItem>,
    secondaryItems: List<MusesNavItem>,
    backdrop: LayerBackdrop,
    phoneDrawer: PhoneDrawerState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    // 兜底：未提供状态时退化为「无抽屉」（内容全屏），保证不崩
    val drawer = phoneDrawer ?: remember {
        PhoneDrawerState(open = false, progress = 0f, onOpen = {}, onClose = {})
    }
    // 手势回调里需要读到**最新**状态（见下方 pointerInput 注释）
    val currentDrawer by rememberUpdatedState(drawer)

    /** 拖拽松手：按位置阈值结算开合 */
    fun settleDrag() {
        if (currentDrawer.progress > DrawerOpenThreshold) currentDrawer.onOpen() else currentDrawer.onClose()
    }

    // 抽屉展开时消费返回键（安卓）：本回调比 MusesApp 的页面返回处理器更深、后注册，
    // 故抽屉展开时优先关抽屉；否则按返回会直接 pop 页面而抽屉留在原地。
    ShellBackHandler(enabled = drawer.open) { drawer.onClose() }

    // 抽屉宽度像素（手势与位移共用；定义在 Box 外，手势 modifier 也要用）
    val drawerWidthPx = with(LocalDensity.current) { PhoneDrawerWidth.toPx() }

    Box(
        modifier
            .fillMaxSize()
            // 推屏手势：水平拖拽跟手开合（左滑关、右滑开）。
            //
            // 为什么不是「左边缘专属手势」：安卓 10+ 的左右边缘是**系统返回手势**区，
            // 应用在边缘做拖拽要么被系统抢走、要么把返回手势废掉，体验割裂；
            // 而全局水平拖拽不与系统冲突，且内层横向滚动（首页平台/榜单胶囊）在 Compose 手势的
            // Main pass 中会**先于**父层消费事件，所以不会被抢（已实测胶囊仍可正常横滑）。
            //
            // 用 rememberUpdatedState 读最新状态：拖拽中每次写进度都会重组出新 PhoneDrawerState，
            // 若直接捕获会一直读到开始拖拽时的旧值（拖拽会「卡住」）。
            .pointerInput(drawerWidthPx) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        currentDrawer.onDragProgress(currentDrawer.progress)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = currentDrawer.progress + dragAmount / drawerWidthPx
                        currentDrawer.onDragProgress(next.coerceIn(0f, 1f))
                    },
                    onDragEnd = { settleDrag() },
                    onDragCancel = { settleDrag() },
                )
            },
    ) {
        // ① 侧栏（下层）：从左侧滑出；未被内容盖住的部分可见，被盖住的不可见
        Column(
            modifier = Modifier
                .width(PhoneDrawerWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = -drawerWidthPx + drawerWidthPx * drawer.progress }
                .background(scheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { },
                ),
        ) {
            PhoneDrawerContent(
                primaryItems = primaryItems,
                secondaryItems = secondaryItems,
                onNavigate = { item ->
                    item.onClick()
                    drawer.onClose()
                },
            )
        }

        // ② 内容（上层）：随抽屉同步右移（推屏）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = drawerWidthPx * drawer.progress },
        ) {
            CompositionLocalProvider(
                // 顶栏汉堡按钮（MusesTopBar 消费本 Local）：仅窄屏抽屉形态非空，宽屏侧轨形态为 null
                LocalMusesOpenDrawer provides drawer.onOpen,
            ) {
                Box(
                    modifier
                        .fillMaxSize()
                        .background(scheme.surface)
                        .layerBackdrop(backdrop),
                ) {
                    content()
                }
            }

            // 推屏式没有遮罩，必须留一个「点内容区收起」的出口；
            // 同时它把点击拦下来，避免抽屉展开时误触到下层内容
            if (drawer.progress > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { drawer.onClose() }
                        },
                )
            }
        }
    }
}

/** 抽屉内容：应用标识 + 导航项（主/次分组，次组为空时不画分隔线） */
@Composable
private fun PhoneDrawerContent(
    primaryItems: List<MusesNavItem>,
    secondaryItems: List<MusesNavItem>,
    onNavigate: (MusesNavItem) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        // 应用标识（与宽屏侧轨 header 同口径：primary 圆角底 + 音符）
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .squircleBackground(scheme.primary, 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TablerIcons.MusicNote,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Muses",
                fontSize = 18.sp,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }

        primaryItems.forEach { item -> PhoneDrawerItem(item, onNavigate) }
        if (secondaryItems.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            secondaryItems.forEach { item -> PhoneDrawerItem(item, onNavigate) }
        }
    }
}

/**
 * 抽屉导航项。
 *
 * miuix 没有抽屉行组件，故按官方 [NavigationRailItem] 的选中态口径手绘
 * （选中：primary 12% 底 + primary 图标/文字；未选：透明底 + onSurface）。
 */
@Composable
private fun PhoneDrawerItem(
    item: MusesNavItem,
    onNavigate: (MusesNavItem) -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (item.active) scheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onNavigate(item) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.active) scheme.primary else scheme.onSurfaceVariantSummary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = item.label,
            fontSize = 15.sp,
            color = if (item.active) scheme.primary else scheme.onSurface,
            fontWeight = if (item.active) FontWeight.Medium else FontWeight.Normal,
        )
    }
}


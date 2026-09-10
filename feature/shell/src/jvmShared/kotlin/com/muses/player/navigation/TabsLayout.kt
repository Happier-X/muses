package com.muses.player.navigation

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleBorder

/**
 * `.tabs-layout` —— 主框架双形态导航。
 *
 * 结构对照：
 * - 宽屏（≥768px，Web 断点口径）→ `.tabs-layout__aside`：固定左侧栏，宽 260px，
 *   主/次菜单各一张圆角卡；
 * - 窄屏 → miuix NavigationBar 底部导航（抽屉推屏手势已下线）；
 * - 导航项 `.tabs-layout__nav-link`（aside 内）：min-height 64px、图标壳固定 60px、
 *   文字 16px；**激活态与普通项完全一致**（08-16 用户定案）。
 */

/** Web 断点口径：viewportWidth >= 768 即平板形态（MiniPlayer 副标题宽窄形态共用） */
internal val TabletBreakpoint = 768.dp

/** `.tabs-layout__aside { width: 260px }` */
private val AsideWidth = 260.dp

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
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    androidx.compose.runtime.CompositionLocalProvider(
        // miuix-blur：内容容器 layerBackdrop 捕获背景，磨砂表面经 LocalMusesBackdrop 消费
        com.muses.player.core.ui.theme.LocalMusesBackdrop provides backdrop,
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
                    backdrop = backdrop,
                    modifier = Modifier,
                    content = content,
                )
            } else {
                PhoneLayout(
                    backdrop = backdrop,
                    content = content,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 宽屏 aside 形态
// ---------------------------------------------------------------------------

/**
 * `.tabs-layout__aside` + `.tabs-layout__panel`（非卡片分组形态）。
 *
 * 迷你条已停靠根 Scaffold bottomBar（阶段二），此处不再叠加悬浮层。
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
    Box(modifier.background(scheme.background)) {
        Row(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            // 平板 aside 改为卡片形态（对齐手机抽屉的 NavGroupCard），
            // 主/次菜单各为一张圆角卡：surface-1 底、1px hairline、16dp 圆角、
            // 左 18 右 12 空隙，两组间距 18dp（原版仅抽屉用卡片；现统一为卡片）。
            // aside 整栏底用 surface（与窄屏抽屉空隙区同色，卡片 surface-1 浮于其上），不用 surface1
            Column(
                Modifier
                    .width(AsideWidth)
                    .fillMaxHeight()
                    .background(scheme.background),
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
            Box(Modifier.weight(1f).fillMaxHeight()) {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 窄屏底部导航形态（抽屉推屏手势已下线，改 miuix NavigationBar）
// ---------------------------------------------------------------------------

/**
 * 窄屏内容壳：导航栏已上收根 Scaffold bottomBar，此处仅承载内容 + hazeSource。
 * 平板 aside 形态不受影响（仍用 [NavGroupCard]）。
 */
@Composable
private fun PhoneLayout(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
            .layerBackdrop(backdrop),
    ) {
        content()
    }
}

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
    items: List<MusesNavItem>,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    Column(
        modifier
            .padding(start = 18.dp, end = 12.dp)
            .squircleBackground(scheme.surface, 16.dp)
            .squircleBorder(1.dp, scheme.dividerLine, 16.dp)
            .padding(vertical = 8.dp),
    ) {
        items.forEach { item -> MusesNavLink(item, inDrawer = true) }
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
private fun MusesNavLink(
    item: MusesNavItem,
    inDrawer: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
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
            .squircleClip(8.dp)
            .padding(
                start = if (inDrawer) 0.dp else 16.dp,
                end = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // __nav-icon-shell：flex 0 0 60px，图标居中于 30px 处
        Box(
            Modifier.size(width = 60.dp, height = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null, // aria-hidden
                tint = scheme.onBackgroundVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        // __nav-label：font-size 16px / --m-text
        Text(
            text = item.label,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Normal,
            color = scheme.onBackground,
        )
    }
}

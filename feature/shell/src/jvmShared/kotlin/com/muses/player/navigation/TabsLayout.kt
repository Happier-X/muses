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
 * - 宽屏（≥768px，Web 断点口径）→ 官方 [NavigationRail]（宽屏专用侧轨，折叠图标+展开药丸二态，
 *   主/次菜单以分割线分组，7 个目的地竖排全收纳）；
 * - 窄屏 → 官方 FloatingNavigationBar 底部胶囊（见 MusesApp bottomBar，图标-only）；
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
 * 宽屏侧轨形态：官方 [NavigationRail]（折叠 80dp 图标+文 / 展开 240dp 药丸，内建切换钮）。
 *
 * 配色/分隔/边距全走官方默认（surface 底 + 内容侧分割线 + 自吃系统边衬）；
 * 主/次菜单以一条分割线分组，7 个目的地竖排全收纳，与窄屏底栏同一集合。
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
    Box(modifier.background(scheme.background)) {
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


package com.muses.player.navigation

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * 主框架内容壳：手机和平板共用底部悬浮导航，页面绘制在底栏背后。
 */

/** 宽屏副标题断点，导航布局不再随宽度切换。 */
internal val TabletBreakpoint = 768.dp

/** 底部导航项 */
data class MusesNavItem(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun TabsLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    androidx.compose.runtime.CompositionLocalProvider(
        com.muses.player.core.ui.theme.LocalMusesBackdrop provides backdrop,
        com.muses.player.core.ui.theme.LocalBottomChromePadding provides
            com.muses.player.core.ui.theme.PhoneBottomChromePadding +
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
    ) {
        Box(
            modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .layerBackdrop(backdrop),
        ) {
            content()
        }
    }
}

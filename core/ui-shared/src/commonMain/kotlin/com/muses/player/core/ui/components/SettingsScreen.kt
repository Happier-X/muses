package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 跨平台设置页共用组件（U4 设置页共用化；U15 移除内置音源管理区块——
 * 音源有独立模块（feature:sources 音源页），设置页不再重复承载，
 * 历史遗留的「Android 端 emptyList 占位死区块」随之消除）。
 *
 * 纯 UI 容器：吸顶 MusesNavbar + 滚动容器 + 底部 MiniPlayer 避让；
 * - [extraContent]：平台专属扩展区域（Android 放「关于/反馈」，Desktop 留空）。
 *
 * 约束：commonMain 零安卓 import。
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme

    // ---- navbar 顶部避让 ----
    // 与 MusesNavbar 同口径：CMP WindowInsets 跨平台取真实状态栏高度，桌面返回 0
    val statusBarTop = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val navbarPt = maxOf(16.dp, statusBarTop) + 44.dp

    Box(modifier = modifier.fillMaxSize().background(scheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = navbarPt + 8.dp),
        ) {
            // ---- 平台扩展区域 ----
            extraContent()

            // ---- 底部避让 MiniPlayer ----
            Spacer(Modifier.height(96.dp))
        }

        // ---- 吸顶导航栏 ----
        MusesNavbar(
            title = "设置",
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

// ---- 公共辅助组件（供外部扩展区域复用） ----

/**
 * 设置分组标题（miuix SmallTitle；原 `.m-block-title--default` 语义）。
 */
@Composable
fun SettingsBlockTitle(text: String) {
    SmallTitle(text = text)
}

/**
 * 设置项左侧图标容器 —— 36dp 圆角方形 + primary 浅底。
 * （rgba(var(--m-primary-rgb), 0.12)，明暗主题自动跟随）。
 *
 * 注意 Web margin-right → Compose `padding(end)` 必须放链最外层（先留间距再画壳），
 * 放 size/background 之后会收缩背景本身（布局陷阱 #7）。
 */
@Composable
fun SettingsIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(36.dp)
            .background(scheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

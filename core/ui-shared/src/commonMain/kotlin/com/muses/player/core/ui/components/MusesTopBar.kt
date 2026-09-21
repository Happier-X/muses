package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

/**
 * 原生顶栏封装（阶段二顶栏换原生：MusesNavbar 自绘玻璃退役）。
 *
 * 映射关系（调用方由 MusesNavbar 平移）：
 * - `title` → 固定小标题（[SmallTopAppBar]）。本栏**不提供可折叠大标题**：
 *   大标题会占掉过多首屏高度，产品侧已统一改回普通模式
 *   （原 `largeTitle` 参数与 miuix `TopAppBar` 分支已一并移除）；
 * - 原 `left` 插槽 → [onBack]（返回键）或 [navigationIcon]（自定义，如文字返回键）；
 *   两者皆空且处在抽屉导航内时自动渲染汉堡（对齐原 MusesNavbar 内建行为）；
 * - 原 `right` 插槽 → [actions]（同为 RowScope，逐行平移）；
 * - 原 `subnavbar` 插槽（搜索栏/工具条）→ [bottomContent]（miuix 原生槽位）。
 */
@Composable
fun MusesTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    /**
     * false = 不渲染（供「容器已提供统一顶栏 + Tab」的嵌入场景，如曲库页的三个子页）。
     * 在函数入口直接 return，比在每个调用处包 `if` 简单得多（调用点带有几十行的
     * actions / bottomContent lambda，包 if 会很难改）。
     */
    visible: Boolean = true,
) {
    if (!visible) return
    val openDrawer = LocalMusesOpenDrawer.current
    val icon: @Composable () -> Unit = navigationIcon ?: {
        if (onBack != null) {
            MusesIconButton(onClick = onBack, contentDescription = "返回") {
                Icon(TablerIcons.ArrowBack, contentDescription = null)
            }
        } else if (openDrawer != null) {
            MusesIconButton(onClick = openDrawer, contentDescription = "打开导航菜单") {
                Icon(TablerIcons.Menu, contentDescription = null)
            }
        }
    }
    SmallTopAppBar(
        title = title,
        subtitle = subtitle,
        navigationIcon = icon,
        actions = actions,
        bottomContent = bottomContent,
        modifier = modifier,
    )
}

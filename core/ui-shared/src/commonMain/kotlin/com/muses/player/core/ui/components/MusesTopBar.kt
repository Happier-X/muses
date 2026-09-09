package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 原生顶栏封装（阶段二顶栏换原生：MusesNavbar 自绘玻璃退役）。
 *
 * 映射关系（调用方由 MusesNavbar 平移）：
 * - `title` → 小标题；`largeTitle != null` → 可折叠大标题 TopAppBar（主列表页），
 *   为空 → 固定小标题 SmallTopAppBar（详情/表单/审核页）；
 * - 原 `left` 插槽 → [onBack]（返回键）或 [navigationIcon]（自定义，如文字返回键）；
 *   两者皆空且处在抽屉导航内时自动渲染汉堡（对齐原 MusesNavbar 内建行为）；
 * - 原 `right` 插槽 → [actions]（同为 RowScope，逐行平移）；
 * - 原 `subnavbar` 插槽（搜索栏/工具条）→ [bottomContent]（miuix 原生槽位）。
 *
 * 大标题折叠联动：调用方提升
 * `val behavior = MiuixScrollBehavior(rememberTopAppBarState())` 后同时传入本栏
 * 与列表 `Modifier.nestedScroll(behavior.nestedScrollConnection)`。
 */
@Composable
fun MusesTopBar(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: String? = null,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
) {
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
    if (largeTitle != null) {
        TopAppBar(
            title = title,
            largeTitle = largeTitle,
            subtitle = subtitle,
            navigationIcon = icon,
            actions = actions,
            bottomContent = bottomContent,
            scrollBehavior = scrollBehavior,
            modifier = modifier,
        )
    } else {
        SmallTopAppBar(
            title = title,
            subtitle = subtitle,
            navigationIcon = icon,
            actions = actions,
            bottomContent = bottomContent,
            scrollBehavior = scrollBehavior,
            modifier = modifier,
        )
    }
}

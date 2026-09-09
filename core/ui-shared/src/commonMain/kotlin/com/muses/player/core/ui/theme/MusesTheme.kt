package com.muses.player.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * Muses 根主题（唯一真相源 = miuix，官方 `ThemeController` 写法）。
 *
 * 双端根入口（安卓 MainActivity / 桌面 MusesDesktopApp）直挂本主题；
 * `ColorSchemeMode.System` 内置跟随系统深色模式，可用 [useDarkTheme] 经 `isDark` 显式覆盖。
 *
 * 后续如需动态取色，把 [ColorSchemeMode] 换成 `MonetSystem`（可配 `keyColor` 种子色）即可。
 *
 * 视觉精确层一律走 `MiuixTheme.colorScheme`。
 */
@Composable
fun MusesTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val controller = remember(useDarkTheme) {
        ThemeController(
            colorSchemeMode = ColorSchemeMode.System,
            isDark = useDarkTheme,
        )
    }
    MiuixTheme(controller = controller) {
        content()
    }
}

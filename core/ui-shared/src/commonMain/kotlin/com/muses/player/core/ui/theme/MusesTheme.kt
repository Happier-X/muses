package com.muses.player.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColors
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColors

/**
 * Muses 根主题（唯一真相源 = miuix，全量迁移后无 M3 桥接）。
 *
 * 双端根入口（安卓 MainActivity / 桌面 MusesDesktopApp）直挂本主题；
 * 明暗跟随系统，可用 [useDarkTheme] 显式覆盖。
 *
 * 历史说明：早期采用主题桥接策略，内层附带 MaterialTheme 为残留 M3 基础件兜底；
 * 全量迁移完成后 M3 组件已清零，桥接层同步下线，视觉精确层一律走 `MiuixTheme.colorScheme`。
 */
@Composable
fun MusesTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = useDarkTheme ?: isSystemInDarkTheme()
    val miuixColors = if (dark) miuixDarkColors() else miuixLightColors()
    MiuixTheme(colors = miuixColors) {
        content()
    }
}

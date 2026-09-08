package com.muses.player.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColors
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColors

/**
 * Muses 根主题（唯一真相源 = miuix）。
 *
 * 双端根入口（安卓 MainActivity / 桌面 MusesDesktopApp）直挂本主题；
 * 明暗跟随系统，可用 [useDarkTheme] 显式覆盖。
 *
 * 内层附带的 MaterialTheme 不是第二套主题：miuix 不提供 M3 ColorScheme，
 * 残留的 M3 基础件（Text/Icon/ProgressIndicator…）在无 MaterialTheme 时会取
 * M3 默认浅色值（深色下发紫/发黑）。此处将其槽位全部派生自 miuix 色板，
 * 保证明暗正确；视觉精确层一律走 `MiuixTheme.colorScheme`。
 */
@Composable
fun MusesTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = useDarkTheme ?: isSystemInDarkTheme()
    val miuixColors = if (dark) miuixDarkColors() else miuixLightColors()
    // M3 槽位派生自 miuix（仅兜底用，不做视觉精确表达）
    val m3 = remember(miuixColors) {
        val base = if (dark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = miuixColors.primary,
            onPrimary = miuixColors.onPrimary,
            background = miuixColors.background,
            onBackground = miuixColors.onBackground,
            surface = miuixColors.surface,
            onSurface = miuixColors.onSurface,
            surfaceVariant = miuixColors.surfaceVariant,
            error = miuixColors.error,
            onError = miuixColors.onError,
            outline = miuixColors.dividerLine,
        )
    }
    MiuixTheme(colors = miuixColors) {
        androidx.compose.material3.MaterialTheme(colorScheme = m3) {
            content()
        }
    }
}

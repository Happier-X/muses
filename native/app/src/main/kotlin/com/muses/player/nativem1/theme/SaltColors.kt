package com.muses.player.nativem1.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 玻璃透明度梯度（design.md 第 4 节：GlassSurface 的核心参数）。
 */
object GlassAlpha {
    const val FAINT = 0.06f
    const val SUBTLE = 0.10f
    const val MEDIUM = 0.16f
    const val STRONG = 0.24f
}

/** 表面层级（由低到高），对应 Salt 风格的层级递进 */
data class SaltColors(
    val background: Color,
    val surfaceLow: Color,
    val surfaceMid: Color,
    val surfaceHigh: Color,
    val primary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    /** 各玻璃层级的实际颜色 = 白/黑 + GlassAlpha，随主题切换 */
    val glassFaint: Color,
    val glassSubtle: Color,
    val glassMedium: Color,
    val glassStrong: Color,
)

private fun glass(baseLight: Boolean, alpha: Float): Color =
    if (baseLight) Color(0xFFFFFFFF).copy(alpha = alpha) else Color(0xFF000000).copy(alpha = alpha)

val SaltDarkColors: SaltColors = SaltColors(
    background = Color(0xFF0E0E12),
    surfaceLow = Color(0xFF16161C),
    surfaceMid = Color(0xFF1D1D25),
    surfaceHigh = Color(0xFF262630),
    primary = Color(0xFF7FB5FF),
    onSurface = Color(0xFFECECF1),
    onSurfaceVariant = Color(0xFFA2A2AE),
    glassFaint = glass(false, GlassAlpha.FAINT),
    glassSubtle = glass(true, GlassAlpha.SUBTLE),
    glassMedium = glass(true, GlassAlpha.MEDIUM),
    glassStrong = glass(true, GlassAlpha.STRONG),
)

val SaltLightColors: SaltColors = SaltColors(
    background = Color(0xFFF4F4F8),
    surfaceLow = Color(0xFFFAFAFE),
    surfaceMid = Color(0xFFEFEFF5),
    surfaceHigh = Color(0xFFE4E4EC),
    primary = Color(0xFF2C62C6),
    onSurface = Color(0xFF1A1A20),
    onSurfaceVariant = Color(0xFF5B5B66),
    glassFaint = glass(false, GlassAlpha.FAINT),
    glassSubtle = glass(false, GlassAlpha.SUBTLE),
    glassMedium = glass(false, GlassAlpha.MEDIUM),
    glassStrong = glass(false, GlassAlpha.STRONG),
)

/** 挂载在 MaterialTheme 之上以继承无障碍行为（design.md 第 4 节） */
val SaltDarkScheme: ColorScheme = darkColorScheme(
    primary = SaltDarkColors.primary,
    background = SaltDarkColors.background,
    surface = SaltDarkColors.surfaceMid,
    surfaceContainerLowest = SaltDarkColors.background,
    surfaceContainerLow = SaltDarkColors.surfaceLow,
    surfaceContainer = SaltDarkColors.surfaceMid,
    surfaceContainerHigh = SaltDarkColors.surfaceHigh,
    onPrimary = Color(0xFF001B44),
    onBackground = SaltDarkColors.onSurface,
    onSurface = SaltDarkColors.onSurface,
    onSurfaceVariant = SaltDarkColors.onSurfaceVariant,
)

val SaltLightScheme: ColorScheme = lightColorScheme(
    primary = SaltLightColors.primary,
    background = SaltLightColors.background,
    surface = SaltLightColors.surfaceMid,
    surfaceContainerLowest = SaltLightColors.background,
    surfaceContainerLow = SaltLightColors.surfaceLow,
    surfaceContainer = SaltLightColors.surfaceMid,
    surfaceContainerHigh = SaltLightColors.surfaceHigh,
    onPrimary = Color(0xFFFFFFFF),
    onBackground = SaltLightColors.onSurface,
    onSurface = SaltLightColors.onSurface,
    onSurfaceVariant = SaltLightColors.onSurfaceVariant,
)

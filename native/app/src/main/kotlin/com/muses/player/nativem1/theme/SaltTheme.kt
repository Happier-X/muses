package com.muses.player.nativem1.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** 页面取色统一入口：LocalSaltColors.current 拿到当前明/暗套的完整 SCSS 令牌调色板 */
val LocalSaltColors = staticCompositionLocalOf { SaltDarkColors }

/** 全局访问 Salt 调色板 */
object SaltThemeDefaults {
    val colors: SaltColors
        @Composable get() = LocalSaltColors.current
}

/**
 * SaltTheme：挂载于 MaterialTheme 之上以继承无障碍行为，
 * 同时经 CompositionLocal 暴露玻璃梯度等 Salt 专属颜色（design.md 第 4 节）。
 *
 * 明暗切换：默认 [isSystemInDarkTheme] 跟随系统（SCSS :root / .dark 双套变量 →
 * SaltLightColors / SaltDarkColors 双套令牌）；调用方可用 [useDarkTheme] 显式覆盖。
 */
@Composable
fun SaltTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = useDarkTheme ?: isSystemInDarkTheme()
    val saltColors = if (dark) SaltDarkColors else SaltLightColors
    val scheme = if (dark) SaltDarkScheme else SaltLightScheme

    CompositionLocalProvider(LocalSaltColors provides saltColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SaltTypography,
            shapes = SaltShapes,
            content = content,
        )
    }
}

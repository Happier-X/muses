package com.muses.player.nativem1.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSaltColors = staticCompositionLocalOf { SaltDarkColors }

/** 全局访问 Salt 调色板 */
object SaltThemeDefaults {
    val colors: SaltColors
        @Composable get() = LocalSaltColors.current
}

/**
 * SaltTheme：挂载于 MaterialTheme 之上以继承无障碍行为，
 * 同时经 CompositionLocal 暴露玻璃梯度等 Salt 专属颜色（design.md 第 4 节）。
 */
@Composable
fun SaltTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val saltColors = if (useDarkTheme) SaltDarkColors else SaltLightColors
    val scheme = if (useDarkTheme) SaltDarkScheme else SaltLightScheme

    CompositionLocalProvider(LocalSaltColors provides saltColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SaltTypography,
            shapes = SaltShapes,
            content = content,
        )
    }
}

package com.muses.player.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

/**
 * 底部 MiniPlayer / 顶部导航 的真磨砂（Haze）上下文。
 *
 * - 由 [com.muses.player.navigation.TabsLayout] 在根部 `rememberHazeState()` 并通过
 *   CompositionLocal 透传，`hazeSource` 打在内容区，`hazeEffect` 打在悬浮玻璃上。
 * - 预览或无 Haze 环境下为 null，玻璃回退为半透明 [SaltColors.glassBg]。
 */
val LocalMusesHazeState = compositionLocalOf<HazeState?> { null }

/**
 * 底部胶囊 / 顶部导航 的 Haze 风格。
 *
 * - blur 20.dp 对齐 Web 版 `backdrop-filter: blur(20px)`；
 * - 亮色在 surface 基底上叠一层半透白，暗色叠半透黑，既保证文字对比度又保留下层模糊；
 * - noiseFactor 0.01 轻微噪点，避免大面积纯色带状。
 */
@Composable
@ReadOnlyComposable
fun musesBottomBarHazeStyle(isDark: Boolean): HazeBlurStyle {
    // 底部与顶部一致：统一用顶部同款透明度（用户定案）
    return musesNavbarHazeStyle(isDark)
}

@Composable
@ReadOnlyComposable
fun musesNavbarHazeStyle(isDark: Boolean): HazeBlurStyle {
    val salt = LocalSaltColors.current
    // 顶部恢复此前无问题态：贴合列表背景 surface 极轻 tint，不再加白
    return if (isDark) {
        HazeBlurStyle(
            backgroundColor = salt.surface,
            colorEffects = listOf(
                HazeColorEffect.tint(Color.Black.copy(alpha = 0.08f)),
            ),
            blurRadius = 20.dp,
            noiseFactor = 0.01f,
        )
    } else {
        HazeBlurStyle(
            backgroundColor = salt.surface,
            colorEffects = listOf(
                HazeColorEffect.tint(Color.White.copy(alpha = 0.08f)),
            ),
            blurRadius = 20.dp,
            noiseFactor = 0.01f,
        )
    }
}

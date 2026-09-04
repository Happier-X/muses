package com.muses.player.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * 底部胶囊 / 顶部导航 的 Haze 风格数据载体。
 *
 * commonMain 无法引用 `dev.chrisbanes.haze.HazeBlurStyle`（Android-only），
 * 因此用此 data class 传递数值，Android actual 转换为 `HazeBlurStyle` 消费。
 */
data class HazeBlurStyleData(
    val backgroundColor: Color,
    val tint: Color,
    val blurRadiusDp: Float,
)

/**
 * 底部胶囊 / 顶部导航 的 Haze 风格。
 *
 * - blur 20.dp 对齐 Web 版 `backdrop-filter: blur(20px)`；
 * - 亮色在 surface 基底上叠一层半透白，暗色叠半透黑，既保证文字对比度又保留下层模糊；
 * - noiseFactor 0.01 轻微噪点，避免大面积纯色带状。
 */
@Composable
@ReadOnlyComposable
fun musesNavbarHazeStyle(isDark: Boolean): HazeBlurStyleData {
    val salt = LocalSaltColors.current
    return if (isDark) {
        HazeBlurStyleData(
            backgroundColor = salt.surface,
            tint = Color.Black.copy(alpha = 0.08f),
            blurRadiusDp = 20f,
        )
    } else {
        HazeBlurStyleData(
            backgroundColor = salt.surface,
            tint = Color.White.copy(alpha = 0.08f),
            blurRadiusDp = 20f,
        )
    }
}

@Composable
@ReadOnlyComposable
fun musesBottomBarHazeStyle(isDark: Boolean): HazeBlurStyleData {
    return musesNavbarHazeStyle(isDark)
}

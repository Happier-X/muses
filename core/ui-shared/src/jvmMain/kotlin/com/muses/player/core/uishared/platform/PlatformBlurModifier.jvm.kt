package com.muses.player.core.uishared.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muses.player.core.ui.theme.HazeBlurStyleData

/**
 * JVM actual（桌面）：无真模糊，纯色降级。
 *
 * - 桌面端不引入 Haze 依赖，模糊效果由 [backgroundColor] 半透明背景替代；
 * - hazeState / hazeStyleData 参数在桌面端忽略（签名保持跨平台一致）。
 */
@Composable
@ReadOnlyComposable
actual fun platformBlurModifier(
    isDark: Boolean,
    backgroundColor: Color,
    hazeState: Any?,
    hazeStyleData: HazeBlurStyleData?,
): Modifier {
    return Modifier.background(color = backgroundColor)
}

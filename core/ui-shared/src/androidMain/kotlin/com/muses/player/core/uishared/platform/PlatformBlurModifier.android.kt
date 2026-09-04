package com.muses.player.core.uishared.platform

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.theme.HazeBlurStyleData
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Android actual：Haze 真模糊。
 *
 * - [hazeState] 由上层导航（TabsLayout）通过 `LocalHazeBlurState` provide，
 *   此处转为 `dev.chrisbanes.haze.HazeState` 消费；
 * - 不可用（null）时回退为半透明纯色背景，行为与旧 SaltNavbar 一致。
 */
@Composable
@ReadOnlyComposable
actual fun platformBlurModifier(
    isDark: Boolean,
    backgroundColor: Color,
    hazeState: Any?,
    hazeStyleData: HazeBlurStyleData?,
): Modifier {
    // 类型擦除桥接：commonMain 传入 Any?，Android actual 转为 HazeState
    @Suppress("UNCHECKED_CAST")
    val hzState = hazeState as? dev.chrisbanes.haze.HazeState

    return if (hzState != null && hazeStyleData != null) {
        // 将跨平台风格数据转换为 HazeBlurStyle
        val hazeStyle = HazeBlurStyle(
            backgroundColor = hazeStyleData.backgroundColor,
            colorEffects = listOf(
                HazeColorEffect.tint(hazeStyleData.tint),
            ),
            blurRadius = hazeStyleData.blurRadiusDp.dp,
            noiseFactor = 0.01f,
        )
        Modifier.hazeBlur(input = HazeInput.Sources(hzState), style = hazeStyle)
    } else {
        Modifier.background(color = backgroundColor)
    }
}

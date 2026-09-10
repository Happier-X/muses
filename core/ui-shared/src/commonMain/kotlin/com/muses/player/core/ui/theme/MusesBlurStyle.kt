package com.muses.player.core.ui.theme

import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import androidx.compose.foundation.background
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * 磨砂表面（底部胶囊 / 顶部导航）的模糊风格数据。
 */
data class MusesBlurStyleData(
    val backgroundColor: Color,
    val tint: Color,
    val blurRadius: Float,
)

/**
 * 磨砂表面模糊风格。
 *
 * - blur 20f 对齐 Web 版 `backdrop-filter: blur(20px)`；
 * - 亮色在 surface 基底上叠一层半透白，暗色叠半透黑，既保证文字对比度又保留下层模糊
 *   （映射为 miuix-blur 的 SrcOver BlendColorEntry，见 musesBackdropBlur）；
 * - 噪点 0.01 轻微噪点，避免大面积纯色带状（在消费处传 textureBlur noiseCoefficient）。
 */
@Composable
@ReadOnlyComposable
fun musesNavbarBlurStyle(isDark: Boolean): MusesBlurStyleData {
    val scheme = MiuixTheme.colorScheme
    return if (isDark) {
        MusesBlurStyleData(
            backgroundColor = scheme.background,
            tint = Color.Black.copy(alpha = 0.08f),
            blurRadius = 20f,
        )
    } else {
        MusesBlurStyleData(
            backgroundColor = scheme.background,
            tint = Color.White.copy(alpha = 0.08f),
            blurRadius = 20f,
        )
    }
}

@Composable
@ReadOnlyComposable
fun musesBottomBarBlurStyle(isDark: Boolean): MusesBlurStyleData {
    return musesNavbarBlurStyle(isDark)
}

/**
 * 磨砂表面背景模糊（miuix-blur 官方工具）。
 *
 * - [backdrop] + [style] 齐备且运行时支持 RuntimeShader（Android API 33+；桌面恒支持）
 *   时走 textureBlur 真磨砂：blur 半径取 [MusesBlurStyleData.blurRadius]，tint 以 SrcOver
 *   混合层叠加，噪点 0.01 防纯色带状；
 * - 任一不满足（未提供 backdrop / 门控不过）回退 [backgroundColor] 纯色。
 *
 * 需由 app 层先在内容容器上 Modifier.layerBackdrop(backdrop) 捕获背景
 * （见 LocalMusesBackdrop），本修饰符只负责消费。
 */
@Composable
@ReadOnlyComposable
fun musesBackdropBlur(
    backdrop: LayerBackdrop?,
    isDark: Boolean,
    backgroundColor: Color,
    style: MusesBlurStyleData?,
    shape: Shape,
): Modifier {
    if (backdrop == null || style == null || !isRuntimeShaderSupported()) {
        return Modifier.background(backgroundColor)
    }
    val colors = BlurColors(
        blendColors = listOf(BlendColorEntry(style.tint, BlurBlendMode.SrcOver)),
    )
    return Modifier.textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = style.blurRadius,
        colors = colors,
        noiseCoefficient = 0.01f,
    )
}


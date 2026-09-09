package com.muses.player.core.ui.theme

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** 玻璃层级，映射 GlassAlpha 梯度 */
enum class GlassLevel { Faint, Subtle, Medium, Strong }

/**
 * GlassSurface 原型（design.md 第 4 节）：
 * 半透明表面 + 细描边 + 可选背景模糊。
 * 性能约束：列表页禁用 blurEnabled=true；模糊仅播放页可用。
 * Modifier.blur 在 API < 31 上为 no-op，无需手动分支。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    level: GlassLevel = GlassLevel.Subtle,
    tint: Color? = null,
    borderEnabled: Boolean = true,
    blurEnabled: Boolean = false,
    blurRadius: Dp = 16.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MiuixTheme.colorScheme
    // 原 GlassAlpha 梯度（Faint .06 / Subtle .10 / Medium .16 / Strong .24），基底改 miuix surface
    val surfaceColor = tint ?: when (level) {
        GlassLevel.Faint -> scheme.surface.copy(alpha = 0.06f)
        GlassLevel.Subtle -> scheme.surface.copy(alpha = 0.10f)
        GlassLevel.Medium -> scheme.surface.copy(alpha = 0.16f)
        GlassLevel.Strong -> scheme.surface.copy(alpha = 0.24f)
    }

    var backgroundModifier: Modifier = Modifier
    if (blurEnabled) {
        backgroundModifier = backgroundModifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded)
    }
    backgroundModifier = backgroundModifier.background(
        brush = Brush.verticalGradient(
            colors = listOf(surfaceColor, surfaceColor.copy(alpha = surfaceColor.alpha * 0.6f)),
        ),
        shape = shape,
    )

    Box(
        modifier = modifier
            .clip(shape)
            .then(backgroundModifier)
            .then(
                if (borderEnabled) {
                    Modifier.border(
                        border = BorderStroke(0.5.dp, scheme.outline),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

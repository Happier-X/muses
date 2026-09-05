package com.muses.player.core.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape

/**
 * JVM（桌面）actual：简化实现。
 *
 * 桌面端无 BlurMaskFilter 等安卓绘制设施，降级策略：
 * - 仅绘制**最后一层外投影**（令牌约定末层即主外投影），无高斯模糊；
 * - 内阴影（inset）层整体跳过（玻璃内浮雕暂缺，由半透明底/描边承担基础观感）。
 *
 * TODO(09-05 V2/V3)：桌面端多层阴影精细实现（逐层无模糊叠加或引入平台模糊等价物）。
 */
actual fun Modifier.saltShadow(shape: Shape, layers: List<SaltShadowLayer>): Modifier =
    drawBehind {
        val layer = layers.lastOrNull { !it.inset } ?: return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        val cornerRadius = when (outline) {
            is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x.coerceAtLeast(0f)
            else -> 0f
        }
        val spreadPx = layer.spread.toPx()
        val radius = (cornerRadius + spreadPx).coerceAtLeast(0f)
        drawRoundRect(
            color = layer.color,
            topLeft = Offset(layer.offsetX.toPx() - spreadPx, layer.offsetY.toPx() - spreadPx),
            size = Size(size.width + spreadPx * 2f, size.height + spreadPx * 2f),
            cornerRadius = CornerRadius(radius, radius),
        )
    }

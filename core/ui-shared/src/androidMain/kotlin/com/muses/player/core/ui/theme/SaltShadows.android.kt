package com.muses.player.core.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/** 绘制外圈矩形时超出组件边界的保底 padding，保证模糊边缘不被自身路径裁掉 */
private const val SHADOW_PAD_PX = 64f

/**
 * Android actual：多层 box-shadow 绘制（drawBehind + nativeCanvas(Paint + BlurMaskFilter)）。
 *
 * - 外阴影：矩形按 offset 平移、spread 扩边后，用 BlurMaskFilter(sigma = blur/2) 高斯模糊
 *   （CSS blur radius ≈ 2σ，与浏览器渲染口径一致）；
 * - 内阴影（inset）：clip 到目标 shape 后，用 EVEN_ODD 路径挖出「内缩矩形洞」并整体模糊绘制，
 *   洞边缘的羽化即 CSS inset shadow 的内侧渐隐；负 spread 使洞扩张到边界外，形成细窄高光条。
 */
actual fun Modifier.saltShadow(shape: androidx.compose.ui.graphics.Shape, layers: List<SaltShadowLayer>): Modifier =
    drawBehind {
        if (layers.isEmpty()) return@drawBehind
        drawIntoCanvas { canvas ->
            val outline = shape.createOutline(size, layoutDirection, this)
            val cornerRadius = when (outline) {
                is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x.coerceAtLeast(0f)
                else -> 0f
            }
            val shapePath = when (outline) {
                is Outline.Rectangle -> Path().apply { addRect(Rect(Offset.Zero, size)) }
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Generic -> outline.path
            }
            val nativeCanvas = canvas.nativeCanvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // CSS：先声明的层在最上面 → 从最后一层开始画，让首层覆盖在上
            for (layer in layers.asReversed()) {
                val sigmaPx = layer.blurRadius.toPx() / 2f
                paint.maskFilter =
                    if (sigmaPx > 0f) BlurMaskFilter(sigmaPx, BlurMaskFilter.Blur.NORMAL) else null
                paint.color = layer.color.toArgb()
                if (layer.inset) {
                    drawInsetLayer(nativeCanvas, paint, layer, shapePath, cornerRadius)
                } else {
                    drawOutsetLayer(nativeCanvas, paint, layer, cornerRadius)
                }
            }
        }
    }

/** 外阴影：元素矩形平移 offset、按 spread 扩边后模糊绘制 */
private fun DrawScope.drawOutsetLayer(
    canvas: android.graphics.Canvas,
    paint: Paint,
    layer: SaltShadowLayer,
    cornerRadius: Float,
) {
    val spreadPx = layer.spread.toPx()
    val radius = (cornerRadius + spreadPx).coerceAtLeast(0f)
    canvas.drawRoundRect(
        0f + layer.offsetX.toPx() - spreadPx,
        0f + layer.offsetY.toPx() - spreadPx,
        size.width + layer.offsetX.toPx() + spreadPx,
        size.height + layer.offsetY.toPx() + spreadPx,
        radius,
        radius,
        paint,
    )
}

/**
 * 内阴影：clip 进 shape 后，绘制「大矩形 ⊖ 内缩矩形」的 EVEN_ODD 路径。
 * 内缩矩形 = 元素矩形按 spread 收缩（负值扩张）、再按 (-dx, -dy) 反向平移 —— 对应
 * CSS inset shadow 的几何定义（正 dx 让阴影带出现在左/上内侧）。
 */
private fun DrawScope.drawInsetLayer(
    canvas: android.graphics.Canvas,
    paint: Paint,
    layer: SaltShadowLayer,
    shapePath: Path,
    cornerRadius: Float,
) {
    val spreadPx = layer.spread.toPx()
    val holeLeft = -layer.offsetX.toPx() + spreadPx
    val holeTop = -layer.offsetY.toPx() + spreadPx
    val holeRight = size.width - layer.offsetX.toPx() - spreadPx
    val holeBottom = size.height - layer.offsetY.toPx() - spreadPx

    // 洞矩形可能因负 spread / 大 offset 越界成空，需归一化为合法矩形
    val left = minOf(holeLeft, holeRight)
    val top = minOf(holeTop, holeBottom)
    val right = maxOf(holeLeft, holeRight)
    val bottom = maxOf(holeTop, holeBottom)
    // 半径不得超过洞短边的一半
    val radius = cornerRadius.coerceAtMost(minOf(right - left, bottom - top) / 2f).coerceAtLeast(0f)

    // 外圈远大于组件边界 + 最大模糊半径，其羽化边缘会被 clip 全部裁掉
    val big = layer.blurRadius.toPx() * 2f
    val path = android.graphics.Path().apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
        addRect(makeRectF(-SHADOW_PAD_PX - big, -SHADOW_PAD_PX - big, size.width + SHADOW_PAD_PX + big, size.height + SHADOW_PAD_PX + big), android.graphics.Path.Direction.CW)
        addRoundRect(makeRectF(left, top, right, bottom), radius, radius, android.graphics.Path.Direction.CCW)
    }

    canvas.save()
    canvas.clipPath(shapePath.asAndroidPath())
    canvas.drawPath(path, paint)
    canvas.restore()
}

private fun makeRectF(left: Float, top: Float, right: Float, bottom: Float) =
    android.graphics.RectF(left, top, right, bottom)

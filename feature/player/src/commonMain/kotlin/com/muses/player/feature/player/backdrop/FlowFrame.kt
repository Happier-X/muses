package com.muses.player.feature.player.backdrop

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 封面采样的低分辨率动态色场：颜色缓慢流动而不是把封面旋转放大。
 * 亮度在离屏帧内限制，浅色封面也能形成有层次的深色播放页。
 */
internal fun renderFlowFrame(cover: CoverPixels, viewportWidth: Int, viewportHeight: Int, timeSeconds: Double): CoverPixels {
    require(cover.width > 0 && cover.height > 0 && cover.pixels.size >= cover.width * cover.height)
    val width = (viewportWidth / 16).coerceIn(1, 160)
    val height = (viewportHeight / 16).coerceIn(1, 240)
    val grid = IntArray(16)
    for (row in 0 until 4) {
        for (column in 0 until 4) {
            val phase = row * 1.7 + column * 2.3
            val positionX = (column + 0.5) / 4 + sin(timeSeconds * 2 * PI / 13 + phase) * 0.12
            val positionY = (row + 0.5) / 4 + cos(timeSeconds * 2 * PI / 17 + phase) * 0.12
            grid[row * 4 + column] = sampleRegion(cover, positionX, positionY)
        }
    }

    val pixels = IntArray(width * height)
    for (row in 0 until height) {
        val vertical = (row + 0.5f) / height * 3
        val top = vertical.toInt().coerceIn(0, 2)
        val verticalBlend = smooth((vertical - top).coerceIn(0f, 1f))
        for (column in 0 until width) {
            val horizontal = (column + 0.5f) / width * 3
            val left = horizontal.toInt().coerceIn(0, 2)
            val horizontalBlend = smooth((horizontal - left).coerceIn(0f, 1f))
            val first = grid[top * 4 + left]
            val second = grid[top * 4 + left + 1]
            val third = grid[(top + 1) * 4 + left]
            val fourth = grid[(top + 1) * 4 + left + 1]
            fun channel(shift: Int): Float {
                val upper = mix(((first shr shift) and 255).toFloat(), ((second shr shift) and 255).toFloat(), horizontalBlend)
                val lower = mix(((third shr shift) and 255).toFloat(), ((fourth shr shift) and 255).toFloat(), horizontalBlend)
                return mix(upper, lower, verticalBlend)
            }
            val red = channel(16)
            val green = channel(8)
            val blue = channel(0)
            val peak = maxOf(red, green, blue)
            val brightness = if (peak > 158f) 158f / peak else 1f
            pixels[row * width + column] = (255 shl 24) or
                ((red * brightness).roundToInt() shl 16) or
                ((green * brightness).roundToInt() shl 8) or
                (blue * brightness).roundToInt()
        }
    }
    val radius = (max(width, height) / 18).coerceIn(1, 12)
    return CoverPixels(blurFlowPixels(pixels, width, height, radius), width, height)
}

private fun mix(first: Float, second: Float, fraction: Float): Float = first + (second - first) * fraction

private fun smooth(fraction: Float): Float = fraction * fraction * (3f - 2f * fraction)

private fun sampleRegion(cover: CoverPixels, horizontal: Double, vertical: Double): Int {
    val centerX = (horizontal * cover.width).toInt()
    val centerY = (vertical * cover.height).toInt()
    val stepX = (cover.width / 30).coerceAtLeast(1)
    val stepY = (cover.height / 30).coerceAtLeast(1)
    var red = 0
    var green = 0
    var blue = 0
    for (row in -2..2) {
        for (column in -2..2) {
            val x = (centerX + column * stepX).coerceIn(0, cover.width - 1)
            val y = (centerY + row * stepY).coerceIn(0, cover.height - 1)
            val color = cover.pixels[y * cover.width + x]
            red += (color shr 16) and 255
            green += (color shr 8) and 255
            blue += color and 255
        }
    }
    return (255 shl 24) or ((red / 25) shl 16) or ((green / 25) shl 8) or (blue / 25)
}

private fun blurFlowPixels(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
    val horizontal = IntArray(source.size)
    val vertical = IntArray(source.size)
    for (row in 0 until height) {
        for (column in 0 until width) {
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val color = source[row * width + (column + offset).coerceIn(0, width - 1)]
                red += (color shr 16) and 255
                green += (color shr 8) and 255
                blue += color and 255
            }
            val count = radius * 2 + 1
            horizontal[row * width + column] = (255 shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
        }
    }
    for (row in 0 until height) {
        for (column in 0 until width) {
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val color = horizontal[(row + offset).coerceIn(0, height - 1) * width + column]
                red += (color shr 16) and 255
                green += (color shr 8) and 255
                blue += color and 255
            }
            val count = radius * 2 + 1
            vertical[row * width + column] = (255 shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
        }
    }
    return vertical
}

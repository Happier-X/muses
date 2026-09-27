package com.muses.player.feature.player.backdrop

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

internal fun coverContentColor(cover: CoverPixels?): Color {
    if (cover == null || cover.width <= 0 || cover.height <= 0 ||
        cover.pixels.size < cover.width * cover.height) return Color.White

    val buckets = HashMap<Int, LongArray>()
    val fallback = LongArray(4)
    var sampled = 0
    var brightNeutral = 0
    var eligible = 0
    val step = (minOf(cover.width, cover.height) / 36).coerceAtLeast(1)
    for (row in 0 until cover.height step step) {
        for (column in 0 until cover.width step step) {
            val pixel = cover.pixels[row * cover.width + column]
            if ((pixel ushr 24) <= 24) continue
            val red = (pixel shr 16) and 255
            val green = (pixel shr 8) and 255
            val blue = pixel and 255
            val value = maxOf(red, green, blue) / 255f
            val saturation = if (value == 0f) 0f else (maxOf(red, green, blue) - minOf(red, green, blue)) / (value * 255f)
            sampled++
            fallback[0]++
            fallback[1] += red.toLong()
            fallback[2] += green.toLong()
            fallback[3] += blue.toLong()
            if (value > 0.78f && saturation < 0.18f) brightNeutral++
            if (value > 0.08f && !(value > 0.94f && saturation < 0.20f)) {
                eligible++
                val key = ((red ushr 4) shl 8) or ((green ushr 4) shl 4) or (blue ushr 4)
                val bucket = buckets.getOrPut(key) { LongArray(4) }
                bucket[0]++
                bucket[1] += red.toLong()
                bucket[2] += green.toLong()
                bucket[3] += blue.toLong()
            }
        }
    }
    if (sampled == 0) return Color.White
    val neutralCover = brightNeutral.toFloat() / sampled > 0.56f && eligible.toFloat() / sampled < 0.24f
    val best = if (neutralCover) fallback else buckets.values.maxByOrNull { bucket ->
        val count = bucket[0].coerceAtLeast(1L)
        val red = bucket[1] / count / 255f
        val green = bucket[2] / count / 255f
        val blue = bucket[3] / count / 255f
        val peak = maxOf(red, green, blue)
        val saturation = if (peak == 0f) 0f else (peak - minOf(red, green, blue)) / peak
        val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
        val balance = 1f - abs(luminance - 0.50f).coerceIn(0f, 0.50f) * 1.25f
        count * (0.55f + saturation * 1.65f) * (0.75f + balance * 0.55f)
    } ?: fallback
    val count = best[0].coerceAtLeast(1L)
    val red = best[1] / count / 255f
    val green = best[2] / count / 255f
    val blue = best[3] / count / 255f
    val peak = maxOf(red, green, blue).coerceAtLeast(0.01f)
    val boosted = (0.86f / peak).coerceIn(1f, 2.4f)
    fun pastel(channel: Float) = 0.78f + 0.22f * (channel * boosted).coerceAtMost(1f)
    return Color(pastel(red), pastel(green), pastel(blue))
}

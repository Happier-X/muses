package com.muses.player.feature.player.backdrop

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 封面位图链路（对齐 AMLL PixiRenderer.setAlbum + MeshGradientRenderer.setAlbum）。
 *
 * 背景渲染直接消费整张封面（多图层旋转叠加），而不是几个提取色——这才是
 * “流光”质感的来源。解码后的像素在共用层做 AMLL 同款预处理：
 * 饱和提升 1.2 → 亮度压到 0.6 → 对比度 0.3（soft-light 近似）→ 小半径盒式模糊。
 *
 * 平台层只负责把 coverUri 解成 ARGB 像素（裸路径 / file:// / content:// / http(s) 均可，
 * 最长边不超过 192px，够背景旋转层用）；
 * 预处理纯 Kotlin 共用；转 ImageBitmap 走平台 actual（双端位图 API 不同）。
 */
data class CoverPixels(
    /** ARGB 打包 int（0xAARRGGBB），行优先，长度 = width * height */
    val pixels: IntArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CoverPixels) return false
        return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int = 31 * (31 * width + height) + pixels.contentHashCode()
}

/** 平台解码：coverUri 解成小尺寸 ARGB 像素，失败返回 null。内部已切 IO 线程。 */
expect suspend fun decodeCoverPixels(coverUri: String): CoverPixels?

/** 平台转换：预处理后的 ARGB 像素转 Compose ImageBitmap（供 Canvas 绘制）。 */
expect fun coverPixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap

/** 背景用封面入口：解码 + 预处理 + 转 ImageBitmap，失败返回 null（调用方回落默认背景）。 */
suspend fun loadBackdropCover(coverUri: String): ImageBitmap? {
    if (coverUri.isBlank()) return null
    return withContext(Dispatchers.Default) {
        val decoded = runCatching { decodeCoverPixels(coverUri) }.getOrNull() ?: return@withContext null
        val processed = preprocessCoverPixels(decoded)
        runCatching { coverPixelsToImageBitmap(processed.pixels, processed.width, processed.height) }
            .getOrNull()
    }
}

/**
 * AMLL 同款封面预处理（setAlbum 像素循环 + 多层 BlurFilter 的 CPU 近似）。
 *
 * - 采样到 96px 短边（保留足够细节供旋转层错开，太大浪费显存）
 * - 按像素：饱和提升到 1.2（流光色彩鲜明）；亮度/对比度保持原样——
 *   背景变暗靠上层的暗色遮罩，而不是压封面本身（否则旋转层色彩被提前抹掉）
 * - 盒式模糊 radius 2，跑 3 遍（近似高斯，消除旋转时的生硬边缘）
 */
fun preprocessCoverPixels(src: CoverPixels): CoverPixels {
    val srcW = src.width
    val srcH = src.height
    if (srcW <= 0 || srcH <= 0 || src.pixels.size < srcW * srcH) return src

    // 最近邻缩到短边 96
    val shortSide = minOf(srcW, srcH)
    val scale = 96f / shortSide
    val dstW = (srcW * scale).roundToInt().coerceIn(1, 256)
    val dstH = (srcH * scale).roundToInt().coerceIn(1, 256)
    var pixels = IntArray(dstW * dstH)
    for (y in 0 until dstH) {
        for (x in 0 until dstW) {
            val sx = (x / scale).toInt().coerceIn(0, srcW - 1)
            val sy = (y / scale).toInt().coerceIn(0, srcH - 1)
            pixels[y * dstW + x] = src.pixels[sy * srcW + sx]
        }
    }

    // 逐像素：仅饱和提升到 1.2；亮度/对比度不动（变暗交给暗色遮罩）
    for (i in pixels.indices) {
        val argb = pixels[i]
        val a = (argb ushr 24) and 0xFF
        var r = (argb shr 16) and 0xFF
        var g = (argb shr 8) and 0xFF
        var b = argb and 0xFF

        val gray = r * 0.3f + g * 0.59f + b * 0.11f
        r = (gray * -0.2f + r * 1.2f).roundToInt()
        g = (gray * -0.2f + g * 1.2f).roundToInt()
        b = (gray * -0.2f + b * 1.2f).roundToInt()

        pixels[i] = (a shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)
    }

    // 盒式模糊 radius 2 × 3 遍，近似高斯
    repeat(3) { pixels = boxBlur(pixels, dstW, dstH) }
    return CoverPixels(pixels, dstW, dstH)
}

/** 分离式盒式模糊（radius 2），返回新数组。 */
private fun boxBlur(src: IntArray, w: Int, h: Int): IntArray {
    if (w <= 1 || h <= 1 || src.size < w * h) return src
    val radius = 2
    val window = radius * 2 + 1
    val tmp = IntArray(w * h)
    val dst = IntArray(w * h)

    fun channel(v: Int, shift: Int) = (v shr shift) and 0xFF
    fun pack(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    // 横向
    for (y in 0 until h) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (k in -radius..radius) {
            val v = src[y * w + k.coerceIn(0, w - 1)]
            sumA += channel(v, 24)
            sumR += channel(v, 16)
            sumG += channel(v, 8)
            sumB += channel(v, 0)
        }
        for (x in 0 until w) {
            tmp[y * w + x] = pack(sumA / window, sumR / window, sumG / window, sumB / window)
            val sub = src[y * w + (x - radius).coerceIn(0, w - 1)]
            val add = src[y * w + (x + radius + 1).coerceIn(0, w - 1)]
            sumA += channel(add, 24) - channel(sub, 24)
            sumR += channel(add, 16) - channel(sub, 16)
            sumG += channel(add, 8) - channel(sub, 8)
            sumB += channel(add, 0) - channel(sub, 0)
        }
    }
    // 纵向
    for (x in 0 until w) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0
        for (k in -radius..radius) {
            val v = tmp[k.coerceIn(0, h - 1) * w + x]
            sumA += channel(v, 24)
            sumR += channel(v, 16)
            sumG += channel(v, 8)
            sumB += channel(v, 0)
        }
        for (y in 0 until h) {
            dst[y * w + x] = pack(sumA / window, sumR / window, sumG / window, sumB / window)
            val sub = tmp[(y - radius).coerceIn(0, h - 1) * w + x]
            val add = tmp[(y + radius + 1).coerceIn(0, h - 1) * w + x]
            sumA += channel(add, 24) - channel(sub, 24)
            sumR += channel(add, 16) - channel(sub, 16)
            sumG += channel(add, 8) - channel(sub, 8)
            sumB += channel(add, 0) - channel(sub, 0)
        }
    }
    return dst
}

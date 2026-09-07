package com.muses.player.feature.player.backdrop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO

/**
 * 桌面解码：ImageIO 读图后最近邻缩到最长边 ≤ 192px，取 ARGB 像素。
 *
 * 桌面 coverUri 只有裸路径 / file:// / http(s) 三种（无 content:// 概念）；
 * ImageIO 原生支持 jpg/png/bmp/gif，webp 等读不出时返回 null 回落默认背景。
 */
actual suspend fun decodeCoverPixels(coverUri: String): CoverPixels? = runCatching {
    val image = when {
        coverUri.startsWith("http://") || coverUri.startsWith("https://") -> {
            val connection = URL(coverUri).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.getInputStream().use { ImageIO.read(it) }
        }
        else -> {
            val path = if (coverUri.startsWith("file:")) {
                runCatching { File(URI(coverUri)) }.getOrNull() ?: File(coverUri.removePrefix("file://"))
            } else {
                File(coverUri)
            }
            if (!path.isFile || !path.canRead()) return null
            // 过大文件先拒收（封面通常几百 KB，32MB 上限防误读整轨音频）
            if (path.length() > 32 * 1024 * 1024L) return null
            path.inputStream().use { ImageIO.read(it) }
        }
    } ?: return null

    val w = image.width
    val h = image.height
    if (w <= 0 || h <= 0) return null

    // 步长采样到最长边 ≤ 192（最近邻，背景旋转层够用）
    val step = maxOf(1, maxOf(w, h) / 192)
    val sw = (w + step - 1) / step
    val sh = (h + step - 1) / step
    val rgb = image.getRGB(0, 0, w, h, null, 0, w) ?: return null
    val pixels = IntArray(sw * sh)
    for (y in 0 until sh) {
        for (x in 0 until sw) {
            pixels[y * sw + x] = rgb[(y * step) * w + (x * step)]
        }
    }
    CoverPixels(pixels, sw, sh)
}.getOrNull()

/** ARGB 像素经 BufferedImage 转 Compose ImageBitmap（公开 API，无 Skia 直接引用）。 */
actual fun coverPixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    buffered.setRGB(0, 0, width, height, pixels, 0, width)
    return buffered.toComposeImageBitmap()
}

package com.muses.player.feature.player.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.koin.core.context.GlobalContext
import java.io.ByteArrayOutputStream
import java.net.URL

/**
 * 安卓解码：BitmapFactory 降采样到最长边 ≤ 192px 后取 ARGB 像素。
 *
 * coverUri 形态：裸路径（AudioTagReader 落盘）/ file://（CoverCacheWriter/PlayerConnection 落盘）/
 * content://（Media3 metadata 兜底）/ http(s)（刮削网络图）。
 * content:// 需 ContentResolver，经 Koin 全局 Context 获取；Koin 未就绪则回 null。
 */
actual suspend fun decodeCoverPixels(coverUri: String): CoverPixels? = runCatching {
    // content:// 走 ContentResolver 流，其余先解成字节（file/裸路径/http）
    val directStream: (() -> java.io.InputStream?)? = when {
        coverUri.startsWith("content://") -> {
            val appContext: Context = GlobalContext.get().get()
            val uri = android.net.Uri.parse(coverUri)
            ({ appContext.contentResolver.openInputStream(uri) })
        }
        else -> null
    }

    // 先探尺寸定采样率（content 走两次 openInputStream，file/http 读字节后二次解码）
    var sampleSize = 1
    var bytes: ByteArray? = null
    if (directStream != null) {
        val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        directStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        sampleSize = sampleFor(bounds.outWidth, bounds.outHeight)
    } else {
        bytes = when {
            coverUri.startsWith("http://") || coverUri.startsWith("https://") -> {
                val connection = URL(coverUri).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.getInputStream().use { it.readBytesCapped() }
            }
            else -> {
                val path = if (coverUri.startsWith("file://")) android.net.Uri.parse(coverUri).path else coverUri
                val file = if (path != null) java.io.File(path) else return null
                if (!file.isFile || !file.canRead()) return null
                file.inputStream().use { it.readBytesCapped() }
            }
        }
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        sampleSize = sampleFor(bounds.outWidth, bounds.outHeight)
    }

    val opts = BitmapFactory.Options().also {
        it.inSampleSize = sampleSize
        it.inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = if (directStream != null) {
        directStream().use { BitmapFactory.decodeStream(it, null, opts) }
    } else {
        BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size, opts)
    } ?: return null

    try {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        CoverPixels(pixels, w, h)
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}.getOrNull()

/** 采样率：最长边压到 ≤ 192（2 的幂）。 */
private fun sampleFor(w: Int, h: Int): Int {
    if (w <= 0 || h <= 0) return 1
    var sample = 1
    while (maxOf(w, h) / (sample * 2) >= 160) sample *= 2
    return sample
}

/** 读流兜底：封面通常几百 KB，8MB 截断防恶意声明拖垮。 */
private fun java.io.InputStream.readBytesCapped(limit: Int = 8 * 1024 * 1024): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(32 * 1024)
    var total = 0
    while (true) {
        val n = read(buf)
        if (n < 0) break
        total += n
        if (total > limit) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

/** ARGB 像素转 Compose ImageBitmap。 */
actual fun coverPixelsToImageBitmap(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}

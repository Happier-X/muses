package com.muses.player.feature.player.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 贝塞尔网格渐变（movingparts.io 网格渐变 + AMLL mesh-renderer 移植）。
 *
 * 结构：
 * - 4×4 控制点：位置固定铺满，颜色从封面缩略图采样（封面色即网格色，
 *   对齐 AMLL“顶点色全白 + 全图纹理”——Compose 无自定义 UV 纹理采样，
 *   改为顶点色直给，GPU 插值，视觉等价）。
 * - 色点游走：每格控制点叠加双正弦偏移（周期 17s/23s，边界点固定），
 *   色彩在原地呼吸交融，而非整图旋转。
 * - 细分：每格 12×12 双三次 Hermite 面片求值（切向量由相邻点差分，
 *   Catmull-Rom 转 Hermite，twist 置零），顶点写入三角面片。
 *
 * 网格总数：3×3 格 × 12×12×2 三角 = 2592 三角，全屏 1 个 drawVertices，
 * iPad 级 GPU 无压力；低端机靠外层 64dp 模糊 + 0.9 alpha 本就柔和。
 */
internal const val MESH_POINTS = 4

/** 网格色：4×4 ARGB int（行优先），由封面采样 + 游走得到。 */
internal data class MeshColors(val argb: IntArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshColors) return false
        return argb.contentEquals(other.argb)
    }

    override fun hashCode(): Int = argb.contentHashCode()
}

/**
 * 网格色采样：封面缩略图 8×8 区域平均 → 4×4 网格色，叠加时间游走。
 *
 * 游走（对齐 AMLL cp-generate 噪声偏移思想，确定性实现保证循环无缝）：
 * 内部点颜色向相邻点插值漂移，边界点固定。漂移量 = sin/cos(2πt/T)，
 * T 取 17s/23s 质数周期，与旋转无关，视觉上是色彩呼吸而非位移。
 */
@Composable
internal fun rememberMeshColors(cover: ImageBitmap?, timeSec: Double): MeshColors? {
    if (cover == null) return null
    // 采样只做一次（封面不变则缩略图不变），游走每帧算
    val thumb = remember(cover) { sampleThumb(cover) }
    return remember(thumb, timeSec) { driftColors(thumb, timeSec) }
}

/** 封面 → 8×8 区域平均色（预处理过的背景封面已饱和+模糊，直接采）。 */
private fun sampleThumb(cover: ImageBitmap): IntArray {
    val pixels = cover.toPixelMap()
    val w = cover.width
    val h = cover.height
    val out = IntArray(64)
    for (gy in 0 until 8) {
        for (gx in 0 until 8) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            var count = 0
            val x0 = gx * w / 8
            val x1 = ((gx + 1) * w / 8).coerceAtMost(w)
            val y0 = gy * h / 8
            val y1 = ((gy + 1) * h / 8).coerceAtMost(h)
            var y = y0
            while (y < y1) {
                var x = x0
                while (x < x1) {
                    val c: Color = pixels[x, y]
                    sumR += (c.red * 255).toInt()
                    sumG += (c.green * 255).toInt()
                    sumB += (c.blue * 255).toInt()
                    count++
                    x += 2 // 步长 2 足够（缩略图本就模糊过）
                }
                y += 2
            }
            if (count == 0) count = 1
            out[gy * 8 + gx] = (0xFF shl 24) or
                (sumR / count shl 16) or
                (sumG / count shl 8) or
                (sumB / count)
        }
    }
    return out
}

/**
 * 8×8 缩略图 → 4×4 网格色 + 时间游走。
 *
 * 基础色：2×2 区域平均；游走：内部 4 点向对角邻居借色 +
 * 边中 4 点向对边中点借色，借色权重随时间正弦变化，边界 4 角固定。
 * 主频 7s、次频 11s（质数互质，长时间不重复），全是时间正弦函数，无缝循环。
 */
private fun driftColors(thumb: IntArray, timeSec: Double): MeshColors {
    val grid = IntArray(16)
    for (gy in 0 until 4) {
        for (gx in 0 until 4) {
            var sumA = 0
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (dy in 0 until 2) {
                for (dx in 0 until 2) {
                    val v = thumb[(gy * 2 + dy) * 8 + (gx * 2 + dx)]
                    sumA += (v ushr 24) and 0xFF
                    sumR += (v shr 16) and 0xFF
                    sumG += (v shr 8) and 0xFF
                    sumB += v and 0xFF
                }
            }
            grid[gy * 4 + gx] = (sumA / 4 shl 24) or (sumR / 4 shl 16) or (sumG / 4 shl 8) or (sumB / 4)
        }
    }
    // 8 点游走：内部 4 点对角借色 + 边中 4 点对边借色，四角固定。
    // 主频 7s 保证肉眼可见的流动，次频 11s 叠加避免机械感
    val pairs = arrayOf(
        intArrayOf(1 * 4 + 1, 2 * 4 + 2, 0),
        intArrayOf(1 * 4 + 2, 2 * 4 + 1, 2),
        intArrayOf(2 * 4 + 1, 1 * 4 + 2, 4),
        intArrayOf(2 * 4 + 2, 1 * 4 + 1, 6),
        intArrayOf(0 * 4 + 1, 3 * 4 + 1, 1),
        intArrayOf(1 * 4 + 0, 1 * 4 + 3, 3),
        intArrayOf(2 * 4 + 0, 2 * 4 + 3, 5),
        intArrayOf(3 * 4 + 1, 0 * 4 + 1, 7),
    )
    for ((dst, src, phase) in pairs) {
        val w = 0.38f * (0.5 + 0.5 * sin(timeSec * 2 * PI / 7.0 + phase)).toFloat()
        grid[dst] = mixArgb(grid[dst], grid[src], w)
        // 叠加第二频率呼吸，避免单一正弦的机械感
        val w2 = 0.18f * (0.5 + 0.5 * cos(timeSec * 2 * PI / 11.0 + phase * 1.7)).toFloat()
        grid[dst] = mixArgb(grid[dst], grid[src], w2)
    }
    return MeshColors(grid)
}

private fun mixArgb(a: Int, b: Int, t: Float): Int {
    val clamped = t.coerceIn(0f, 1f)
    val aa = (a ushr 24) and 0xFF
    val ar = (a shr 16) and 0xFF
    val ag = (a shr 8) and 0xFF
    val ab = a and 0xFF
    val ba = (b ushr 24) and 0xFF
    val br = (b shr 16) and 0xFF
    val bg = (b shr 8) and 0xFF
    val bb = b and 0xFF
    fun lerp(x: Int, y: Int) = (x + (y - x) * clamped).toInt().coerceIn(0, 255)
    return (lerp(aa, ba) shl 24) or (lerp(ar, br) shl 16) or (lerp(ag, bg) shl 8) or lerp(ab, bb)
}

/**
 * 绘制网格渐变：4×4 控制点 → 每格 SUBDIV×SUBDIV Hermite 面片 → 光栅化到小位图。
 *
 * 为什么是小位图而不是三角面片：本工程 Compose 版本的 DrawScope 没有
 * drawVertices，只能走位图路线。SUBDIV=8 → 25×25 小图，GPU 放大时双线性
 * 插值免费平滑，外层再叠 64dp 模糊，等价于 AMLL 的纹理预模糊。
 * 位置面片恒等（控制点铺满），只有颜色面片需要求值。
 *
 * [target] 由调用方 remember 复用（25×25），避免每帧分配。
 * 颜色面片：RGB 三通道各自 Hermite 插值（movingparts 做法：切向量置零，
 * 得到缓入缓出，避免黑白突变），alpha 线性。
 * 暗角：边缘乘 0.55 + 中心 1.0（AMLL mesh.frag vignette：0.6 + smoothstep×0.4）。
 */
internal const val MESH_RASTER = 25

internal fun rasterizeMesh(colors: MeshColors, target: ImageBitmap) {
    val n = MESH_POINTS
    // 控制点颜色（float RGB）与暗角系数
    val cr = FloatArray(n * n)
    val cg = FloatArray(n * n)
    val cb = FloatArray(n * n)
    val shade = FloatArray(n * n)
    for (gy in 0 until n) {
        for (gx in 0 until n) {
            val i = gy * n + gx
            val v = colors.argb[i]
            cr[i] = ((v shr 16) and 0xFF) / 255f
            cg[i] = ((v shr 8) and 0xFF) / 255f
            cb[i] = (v and 0xFF) / 255f
            val dx = gx / (n - 1f) * 2f - 1f
            val dy = gy / (n - 1f) * 2f - 1f
            val dist = kotlin.math.sqrt(dx * dx + dy * dy) / 1.4142f
            shade[i] = (1f - dist * 0.45f).coerceIn(0.55f, 1f)
        }
    }

    val size = MESH_RASTER
    val canvas = Canvas(target)
    // 清底（复用位图时擦掉上一帧）
    canvas.drawRect(
        0f, 0f, size.toFloat(), size.toFloat(),
        Paint().apply { color = Color.Transparent; blendMode = androidx.compose.ui.graphics.BlendMode.Clear },
    )
    val paint = Paint()
    // 小图每像素对应 (u,v) 全网格坐标 → 定位到格 → 格内 Hermite
    for (py in 0 until size) {
        val gv = py / (size - 1f) * (n - 1)
        val cy = gv.toInt().coerceIn(0, n - 2)
        val v = (gv - cy).coerceIn(0f, 1f)
        for (px in 0 until size) {
            val gu = px / (size - 1f) * (n - 1)
            val cx = gu.toInt().coerceIn(0, n - 2)
            val u = (gu - cx).coerceIn(0f, 1f)
            val p00 = cy * n + cx
            val p10 = p00 + 1
            val p01 = p00 + n
            val p11 = p01 + 1
            val fr = hermiteColor(u, v, cr[p00], cr[p10], cr[p01], cr[p11])
            val fg = hermiteColor(u, v, cg[p00], cg[p10], cg[p01], cg[p11])
            val fb = hermiteColor(u, v, cb[p00], cb[p10], cb[p01], cb[p11])
            val fs = hermiteColor(u, v, shade[p00], shade[p10], shade[p01], shade[p11])
            paint.color = Color(fr * fs, fg * fs, fb * fs)
            canvas.drawRect(px.toFloat(), py.toFloat(), px + 1f, py + 1f, paint)
        }
    }
}

/** 颜色 Hermite（切向量置零，缓入缓出）。 */
private fun hermiteColor(u: Float, v: Float, c00: Float, c10: Float, c01: Float, c11: Float): Float {
    fun h00(t: Float) = 2 * t * t * t - 3 * t * t + 1
    fun h01(t: Float) = -2 * t * t * t + 3 * t * t
    val top = h00(u) * c00 + h01(u) * c10
    val bottom = h00(u) * c01 + h01(u) * c11
    return (h00(v) * top + h01(v) * bottom).coerceIn(0f, 1f)
}

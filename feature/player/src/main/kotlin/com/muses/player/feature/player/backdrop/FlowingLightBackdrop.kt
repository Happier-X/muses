package com.muses.player.feature.player.backdrop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Immersive 流体背景 — 直接复刻 Immersive-Android 的 FlowingLightBackdrop
 *
 * 职责（对齐 Immersive 源码语义）：
 * - 流动渐变（flowing light）：多 Blob 径向渐变 + 线性无限循环位移，flowSpeed 控制周期
 * - 封面虚化：高斯模糊 + 放大 + 叠加暗色渐变保证前景可读，粘性 coverUri（null 时沿用旧帧）
 * - fallback-background：无封面时深色占位纵向渐变，hasLyric 仅作语义保留（背景始终渲染，不因无词卸载）
 *
 * 与 PlayerViewModel 的粘性封面契约一致：coverUri = stickyCover（null 表示沿用），hasLyric = parsedLines.isNotEmpty()
 * 调用方保证传入 stickyCover，不在此处做二次粘性。
 */
@Composable
fun FlowingLightBackdrop(
    coverUri: String?,
    hasLyric: Boolean,
    modifier: Modifier = Modifier,
    flowSpeed: Float = 2f,
) {
    @Suppress("UNUSED_PARAMETER") val _hasLyric = hasLyric

    // 流体周期：Immersive flowSpeed=2 约 12s 一圈，此处 duration = 12000 / flowSpeed
    val durationMs = remember(flowSpeed) { (12000 / flowSpeed.coerceAtLeast(0.2f)).toInt().coerceIn(4000, 30000) }
    val transition = rememberInfiniteTransition(label = "flow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Box(
        modifier = modifier
            // 对齐原版 .player-page__bg { overflow: hidden }：封面虚化层 scale 1.08 + blur 28dp
            // 的绘制会向边界外溢出约一个模糊半径，不裁剪时沉浸页下滑会越界叠出半透明“第二层”
            .clipToBounds()
            .background(Color(0xFF05070D)),
    ) {
        // 底层 fallback：无封面时可见（对齐 .player-overlay .fallback-background：
        // 径向紫光 50%/18% rgba(148,120,255,0.28) → transparent 42% + 线性 165deg #171b2b/#0a0c14/#05070d）
        if (coverUri.isNullOrBlank()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF171B2B), Color(0xFF0A0C14), Color(0xFF05070D)),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                        )
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(Color(0x479478FF), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.18f),
                                radius = size.width * 0.9f,
                            ),
                        )
                    },
            )
        }

        // 封面虚化层：对齐 Capacitor player-page__bg opacity 0.75 + blur 28dp scale 1.08
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.08f, scaleY = 1.08f)
                    .blur(28.dp)
                    .alpha(0.75f),
            )
        }

        // 流动光层：Canvas 径向 Blob，缓慢位移，模拟 MeshGradientRenderer 的流体
        // 3 枚主 Blob + 1 高光，颜色取 Immersive 常见紫蓝粉系，alpha 低避免过艳
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f),
        ) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            // Blob 1：紫蓝，左上
            val cx1 = w * (0.32f + 0.10f * sin(phase.toDouble()).toFloat())
            val cy1 = h * (0.28f + 0.06f * cos((phase * 0.9f).toDouble()).toFloat())
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x596A5BFF), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx1, cy1),
                    radius = minDim * 0.85f,
                ),
            )
            // Blob 2：粉紫，右中
            val cx2 = w * (0.72f + 0.08f * cos((phase * 1.1f).toDouble()).toFloat())
            val cy2 = h * (0.58f + 0.07f * sin((phase * 0.85f).toDouble()).toFloat())
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4D9B59D6), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx2, cy2),
                    radius = minDim * 0.78f,
                ),
            )
            // Blob 3：玫粉，左下
            val cx3 = w * (0.22f + 0.07f * cos(phase.toDouble()).toFloat())
            val cy3 = h * (0.74f + 0.05f * sin((phase * 1.2f).toDouble()).toFloat())
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x40FF6B9D), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx3, cy3),
                    radius = minDim * 0.72f,
                ),
            )
        }

        // 暗色遮罩：保证前景文字可读（顶部浅、底部深，复刻 Immersive 沉浸页 scrim）
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color(0xFF05070D).copy(alpha = 0.52f),
                            Color(0xFF05070D).copy(alpha = 0.90f),
                        ),
                    ),
                )
                .alpha(0.92f),
        )

        // 顶部高光：径向白光，对齐 Capacitor 高光 alpha 0.07（原 0.075）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.07f),
        ) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = maxOf(size.width, size.height) * 0.55f,
                ),
            )
        }
    }
}

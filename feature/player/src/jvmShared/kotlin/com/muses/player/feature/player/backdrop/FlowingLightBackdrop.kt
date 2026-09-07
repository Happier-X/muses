package com.muses.player.feature.player.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.sin

/**
 * Immersive 流体背景 — 对齐 Pear-Wall（Apple Music 系网格流光的 Compose 还原）的动态流光。
 *
 * 拆解结论（movingparts.io 网格渐变 + AMLL + Pear-Wall 三方交叉验证）：
 * - 真机是贝塞尔网格渐变 + 全图封面纹理采样 + 缓慢旋转 + 低频呼吸；
 *   Web 兼容路线（AMLL Pixi / Pear-Wall rotation shader）是同一封面多实例旋转叠加 + 强模糊。
 * - 本实现走旋转叠加路线（Compose Canvas 无自定义顶点网格 API，着色器路线在 minSdk 26 下
 *   无法全版本覆盖），参数对齐 Pear-Wall rotation 着色器。
 *
 * 原理（Pear-Wall rotation.vert / rotation.frag 移植）：
 * - 同一张封面画 3 层：底层铺满全屏（uArtworkFill），上层 2 个旋转实例
 * - 实例 0：1.4 倍、120 秒一圈；实例 1：0.7 倍、左上偏移、70 秒一圈、逆向；
 *   实例 2：0.7 倍、右下偏移、90 秒一圈，并额外叠加实例 0 的父旋转（嵌套视差）
 * - 呼吸：三路缩放以鼓点强度（或模拟脉冲）为目标做平方加成，最大 1.33 倍
 * - 切歌：新旧封面 0.5 秒线性混合（transitionMix），旋转相位连续不跳变
 * - 封面先做预处理（饱和 1.2 + 模糊），见 BackdropCover
 *
 * 职责：
 * - 流光层：3 实例旋转封面，单调时钟驱动，绝对无缝循环，flowSpeed 控制流速
 * - 封面虚化：底层封面虚化铺底，保证无旋转层时的色彩基调
 * - 暗色遮罩 + 顶部高光：保证前景文字可读
 * - fallback-background：无封面时深色占位纵向渐变，hasLyric 仅作语义保留
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

    // 背景用封面：切歌时解码 + 预处理一次，内存缓存避免来回切歌重复解码。
    // 过渡状态机（对齐 Pear-Wall 0.5s transitionMix）：切歌时旧封面保留，新封面淡入，
    // 旋转相位走单调时钟不重置，杜绝切歌跳变
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var previous by remember { mutableStateOf<ImageBitmap?>(null) }
    var transition by remember { mutableStateOf(1f) }
    val coverCache = remember { mutableMapOf<String, ImageBitmap?>() }
    LaunchedEffect(coverUri) {
        if (coverUri.isNullOrBlank()) {
            previous = current
            current = null
            transition = 0f
        } else {
            val bitmap = if (coverCache.containsKey(coverUri)) {
                coverCache[coverUri]
            } else {
                loadBackdropCover(coverUri).also { coverCache[coverUri] = it }
            }
            if (bitmap !== current) {
                previous = current
                current = bitmap
                transition = 0f
            }
        }
        // 0.5 秒线性混合（Pear-Wall ARTWORK_TRANSITION_SECONDS）
        val start = withFrameNanos { it }
        var elapsed: Long
        do {
            elapsed = withFrameNanos { it } - start
            transition = (elapsed / 500_000_000f).coerceIn(0f, 1f)
        } while (transition < 1f)
        previous = null
    }

    // 单调时钟驱动（对齐 Pear-Wall render(time)：uTime = 秒级单调时间 × flowSpeed）。
    // 不用 0→2π 循环动画——旋转周期取 70/90/120 秒（Pear-Wall rotationTimeScale），
    // 各层角度 = time × 2π / 周期，天然连续，不存在循环边界跳变。
    // flowSpeed=1 标准速、=2 快速（Pear-Wall FastFlowSpeed ×2）。
    //
    // 绘制时钟下沉到 FlowLayers 独立 composable（见文件末尾）：帧时间 State 只被它读取，
    // 父级 FlowingLightBackdrop 不再订阅时间，不再每帧重组——之前时间 State 放在父级，
    // 每帧重组整棵沉浸页（含歌词/控制区），重组开销挤占帧时间，造成肉眼可见的跳变
    val flowSpeedState = rememberUpdatedState(flowSpeed)

    Box(
        modifier = modifier
            // .player-page__bg { overflow: hidden }：旋转层放大后会向边界外溢出，不裁剪时
            // 沉浸页下滑会越界叠出半透明“第二层”
            .clipToBounds()
            .background(Color(0xFF05070D)),
    ) {
        // 底层 fallback：无封面时可见（径向紫光 + 深色纵向渐变）
        if (current == null && previous == null) {
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

        // 封面虚化铺底：底层色彩基调（铺底即可，流光主体靠旋转层）
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .alpha(0.5f),
            )
        }

        // 流光层下沉到独立 composable：时间 State 只在它内部读写，
        // 父级不订阅、不重组。transition 由上面的切歌协程推进（0.5s），
        // 此处只读一次传入，动画期间的重组仅来自过渡本身（0.5s 结束）
        FlowLayers(
            current = current,
            previous = previous,
            transition = transition,
            flowSpeed = flowSpeedState.value,
        )

        // 暗色遮罩：保证前景文字可读（顶部透流光、底部保控制区可读）
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color(0xFF05070D).copy(alpha = 0.35f),
                            Color(0xFF05070D).copy(alpha = 0.90f),
                        ),
                    ),
                )
                .alpha(0.92f),
        )

        // 顶部高光：径向白光 alpha 0.07
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.07f),
        ) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = maxOf(size.width, size.height) * 0.55f,
                ),
            )
        }
    }
}

/**
 * 流光层（独立 composable，贝塞尔网格渐变 + 封面纹理采样）。
 *
 * 下沉的原因：帧时间 State 只在这里读写，父级 FlowingLightBackdrop 不订阅，
 * 动画帧只重组这个小子树（1 个 Canvas），不碰歌词/控制区/封面。
 *
 * 原理（movingparts.io 网格渐变 + AMLL mesh-renderer 移植）：
 * - 4×4 控制点网格：网格点位置固定（铺满全屏，归一化 [-1,1]），
 *   网格点颜色 = 封面 8×8 缩略图对应位置像素（AMLL 网格纹理链路的顶点色版本）。
 *   颜色每 1.2s 向目标滑动一次（网格色点游走），不是整层旋转——
 *   这才是真机 mesh gradient 的质感：色彩在原地呼吸交融，而非图片转圈。
 * - 贝塞尔面片细分：每格 12×12 细分的双三次 Hermite 面片求值，
 *   顶点位置 + 顶点色写入三角面片（Vertices.Triangles），GPU 线性插值。
 *   面片切向量由相邻控制点差分得到（Catmull-Rom 转 Hermite），twist 置零。
 * - 暗角：径向压暗边缘（AMLL mesh.frag vignette），抖动抗色带靠 64dp 模糊 natural dither。
 * - 封面纹理：网格点颜色已承载封面色彩，不再需要整图旋转实例；
 *   底层仍保留虚化封面铺底（色彩基调 + 无网格时的 fallback）。
 * - 切歌：新旧两套网格色 0.5s 混合（transitionMix），位置网格不变，无跳变。
 */
@Composable
private fun FlowLayers(
    current: ImageBitmap?,
    previous: ImageBitmap?,
    transition: Float,
    flowSpeed: Float,
    modifier: Modifier = Modifier,
) {
    // 帧时钟：每帧写一次，触发本函数重组（只含 1 个 Canvas，不碰父级）
    val frameTime = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> frameTime.longValue = now - start }
        }
    }
    val speed = flowSpeed.coerceAtLeast(0.2f)
    // 纳秒 → 秒 × 流速
    val timeSec = frameTime.longValue / 1_000_000_000.0 * speed

    // 网格色点：从封面缩略图采样 + 缓慢游走（同一帧时间驱动，保证同步）
    val meshA = rememberMeshColors(current, timeSec)
    val meshB = rememberMeshColors(previous, timeSec)

    val layers = listOfNotNull(
        meshA?.let { it to transition },
        meshB?.let { it to (1f - transition) },
    )
    // 网格光栅位图：每帧新对象（25×25×4=2.5KB，GC 无压力）。
    // 不复用的原因：同一位图对象内容变化时，RenderNode 缓存的 GPU 纹理不刷新，
    // 画面会定格在第一帧；新对象必走上传，逐帧更新
    val raster = remember(timeSec) { ImageBitmap(MESH_RASTER, MESH_RASTER) }
    for ((colors, layerAlpha) in layers) {
        if (layerAlpha <= 0.01f) continue
        // CPU 光栅化（625 像素 Hermite 求值，毫秒级）→ GPU 放大铺满 → 强模糊
        rasterizeMesh(colors, raster)
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .blur(64.dp)
                .alpha(0.9f * layerAlpha),
        ) {
            drawImage(
                image = raster,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
            )
        }
    }
}


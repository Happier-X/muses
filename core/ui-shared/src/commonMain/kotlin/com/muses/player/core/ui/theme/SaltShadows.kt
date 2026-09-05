package com.muses.player.core.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 单层 box-shadow 描述 —— 与 CSS `box-shadow: <x> <y> <blur> <spread> <color>` 逐字段对应；
 * [inset] = true 表示 CSS `inset` 关键字。
 *
 * 数值规则：px → dp 原样抄数值；颜色/透明度原样抄。
 */
data class SaltShadowLayer(
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val blurRadius: Dp = 0.dp,
    val spread: Dp = 0.dp,
    val color: Color,
    /** CSS `inset` 内阴影 */
    val inset: Boolean = false,
)

/**
 * SCSS 阴影令牌清单（`src/theme/index.scss` §1「阴影」段）—— 每组多层阴影逐层翻译，
 * 层顺序保持与 SCSS 一致（CSS 规定先声明的层绘制在最上层）。
 */
object SaltShadowTokens {

    private val BLACK_12 = Color(0x1F000000)   // rgba(0,0,0,0.12)
    private val WHITE = Color(0xFFFFFFFF)
    private val WHITE_50 = Color(0x80FFFFFF)   // rgba(255,255,255,0.5)
    private val WHITE_25 = Color(0x40FFFFFF)   // rgba(255,255,255,0.25)
    private val WHITE_10 = Color(0x1AFFFFFF)   // rgba(255,255,255,0.1)
    private val WHITE_15 = Color(0x26FFFFFF)   // rgba(255,255,255,0.15)
    private val WHITE_075 = Color(0x13FFFFFF)  // rgba(255,255,255,0.075)
    private val WHITE_05 = Color(0x0DFFFFFF)   // rgba(255,255,255,0.05)
    private val WHITE_40 = Color(0x66FFFFFF)   // rgba(255,255,255,0.4)
    private val BLACK_20 = Color(0x33000000)   // rgba(0,0,0,0.2)
    private val BLACK_10 = Color(0x1A000000)   // rgba(0,0,0,0.1)
    private val BLACK_15 = Color(0x26000000)   // rgba(0,0,0,0.15)
    private val BLACK_25 = Color(0x40000000)   // rgba(0,0,0,0.25)
    private val EEE = Color(0xFFEEEEEE)
    private val DDD = Color(0xFFDDDDDD)
    private val DARK_141414 = Color(0xFF141414)
    private val DARK_262626 = Color(0xFF262626)

    /** `--m-shadow-ios-thumb`（外阴影，明暗同值）：iOS 滑块/开关 thumb 投影 */
    val IosThumb: List<SaltShadowLayer> = listOf(
        SaltShadowLayer(offsetY = 0.5.dp, blurRadius = 4.dp, color = BLACK_12),
        SaltShadowLayer(offsetY = 6.dp, blurRadius = 13.dp, color = BLACK_12),
    )

    /** `--m-shadow-ios-light-glass-thumb`：液态玻璃 thumb 明色内浮雕 */
    val LightGlassThumb: List<SaltShadowLayer> = listOf(
        SaltShadowLayer(-3.dp, -3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 0.dp, 0.5.dp, WHITE_50, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 10.dp, -2.dp, EEE, inset = true),
        SaltShadowLayer(-3.dp, -3.dp, 10.dp, -2.dp, EEE, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 5.dp, 1.dp, WHITE, inset = true),
    )

    /** `--m-shadow-ios-dark-glass-thumb`：液态玻璃 thumb 暗色内浮雕 */
    val DarkGlassThumb: List<SaltShadowLayer> = listOf(
        SaltShadowLayer(3.dp, 3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer(-3.dp, -3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer((-0.5).dp, (-0.5).dp, 0.dp, 0.dp, WHITE_50, inset = true),
        SaltShadowLayer(0.5.dp, 0.5.dp, 0.dp, 0.dp, WHITE_10, inset = true),
        SaltShadowLayer(-3.dp, 3.dp, 0.dp, (-3.5).dp, WHITE_25, inset = true),
        SaltShadowLayer(0.dp, -5.dp, 0.dp, (-3.5).dp, WHITE_25, inset = true),
        SaltShadowLayer(0.dp, -5.dp, 5.dp, 0.dp, WHITE_25, inset = true),
    )

    /** `--m-shadow-ios-light-glass`：玻璃容器明色配方（含最后一层外投影） */
    val LightGlass: List<SaltShadowLayer> = listOf(
        SaltShadowLayer(-1.dp, -1.dp, 0.dp, (-0.5).dp, WHITE, inset = true),
        SaltShadowLayer(1.dp, 1.dp, 0.dp, (-0.5).dp, WHITE, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 10.dp, -3.dp, DDD, inset = true),
        SaltShadowLayer(-3.dp, -3.dp, 10.dp, -3.dp, DDD, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 5.dp, 1.dp, WHITE, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 0.dp, 0.5.dp, BLACK_25, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 24.dp, 0.dp, BLACK_10, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 25.dp, 0.dp, BLACK_20),
    )

    /** `--m-shadow-ios-dark-glass`：玻璃容器暗色配方（含最后一层外投影） */
    val DarkGlass: List<SaltShadowLayer> = listOf(
        SaltShadowLayer(-2.dp, -2.dp, 0.5.dp, (-2.5).dp, WHITE_40, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 0.5.dp, (-3.5).dp, WHITE_40, inset = true),
        SaltShadowLayer(2.dp, 2.dp, 0.5.dp, -2.dp, DARK_262626, inset = true),
        SaltShadowLayer(-2.dp, -2.dp, 0.5.dp, -2.dp, DARK_262626, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 5.dp, 1.dp, DARK_141414, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 0.dp, 1.dp, WHITE_15, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 10.dp, 0.dp, WHITE_075, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 24.dp, 0.dp, WHITE_05, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 25.dp, 0.dp, BLACK_15),
    )

    /**
     * `--m-shadow-ios-light-glass-fab`：FAB 玻璃明色配方。
     * SCSS 中 `var(--m-primary)` 引用 → 以参数注入当前主题 primary。
     */
    fun LightGlassFab(primary: Color): List<SaltShadowLayer> = listOf(
        SaltShadowLayer(-3.dp, -3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 0.dp, (-3.5).dp, WHITE, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 0.dp, 0.5.dp, WHITE_50, inset = true),
        SaltShadowLayer(3.dp, 3.dp, 10.dp, -2.dp, primary, inset = true),
        SaltShadowLayer(-3.dp, -3.dp, 10.dp, -2.dp, primary, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 5.dp, 1.dp, WHITE, inset = true),
        SaltShadowLayer(0.dp, 0.dp, 15.dp, 4.dp, BLACK_20),
    )

    /** `--m-shadow-ios-dark-glass-fab`：FAB 玻璃暗色配方（primary 半透层对应 rgba(var(--m-primary-rgb), 0.5)） */
    fun DarkGlassFab(primary: Color): List<SaltShadowLayer> = listOf(
        SaltShadowLayer(3.dp, 3.dp, 0.dp, (-3.5).dp, primary, inset = true),
        SaltShadowLayer(-3.dp, -3.dp, 0.dp, (-3.5).dp, primary, inset = true),
        SaltShadowLayer((-0.5).dp, (-0.5).dp, 0.dp, 0.dp, WHITE_50, inset = true),
        SaltShadowLayer(0.5.dp, 0.5.dp, 0.dp, 0.dp, WHITE_10, inset = true),
        SaltShadowLayer(-3.dp, 3.dp, 0.dp, (-3.5).dp, WHITE_25, inset = true),
        SaltShadowLayer(0.dp, -5.dp, 0.dp, (-3.5).dp, primary.copy(alpha = 0.5f), inset = true),
        SaltShadowLayer(0.dp, -5.dp, 5.dp, 0.dp, primary.copy(alpha = 0.5f), inset = true),
        SaltShadowLayer(0.dp, 0.dp, 15.dp, 4.dp, BLACK_20),
    )
}

/**
 * 多层 box-shadow 绘制（09-05 T2 上收，expect/actual）：
 *
 * - Android actual：drawBehind + nativeCanvas(Paint + BlurMaskFilter)——
 *   外阴影矩形按 offset 平移、spread 扩边后高斯模糊（CSS blur radius ≈ 2σ）；
 *   内阴影（inset）clip 到目标 shape 后用 EVEN_ODD 路径挖「内缩矩形洞」整体模糊绘制。
 * - 桌面（JVM）actual：简化实现——仅绘制最后一层外投影（无模糊），内阴影层跳过。
 *
 * 用法：
 * ```
 * Box(
 *     modifier = Modifier
 *         .clip(RoundedCornerShape(24.dp))
 *         .saltShadow(shape, SaltShadowTokens.LightGlass)
 * )
 * ```
 * 注意：模糊像素会溢出组件边界绘制（Compose 默认不裁剪），父级不要对其使用 clipToBounds。
 */
expect fun Modifier.saltShadow(shape: Shape, layers: List<SaltShadowLayer>): Modifier

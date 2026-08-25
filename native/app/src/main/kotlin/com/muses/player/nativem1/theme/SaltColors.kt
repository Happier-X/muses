package com.muses.player.nativem1.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Salt 设计令牌调色板 —— 一比一翻译自 Web 层 `src/theme/index.scss`。
 *
 * 翻译规则（design.md §1）：
 * - 每个字段的 KDoc 第一行保留原 CSS 变量名；
 * - rgba/透明度原样抄（alpha 字节 = round(alpha * 255)）；
 * - 暗色块对应 SCSS `.dark` 选择器内重新定义的变量，
 *   未在 `.dark` 中重定义的变量（danger/success 等）两套取值相同。
 *
 * 覆盖的 SCSS 变量清单（light :root / .dark 双套）：
 *   --m-glass-bg --m-navbar-glass-bg
 *   --m-surface --m-surface-1 --m-surface-1-shade --m-surface-1-tint
 *   --m-surface-2 --m-surface-3 --m-surface-variant
 *   --m-text --m-text-2(--m-text-secondary) --m-text-3(--m-text-tertiary)
 *   --m-primary --m-primary-tint --m-primary-shade --m-on-primary
 *   --m-danger --m-danger-shade --m-success
 *   --m-disabled-text --m-disabled-bg --m-disabled-border
 *   --m-hairline(--m-stroke) --m-glass-light --m-glass-dark
 *
 * 未入本 data class 的 SCSS 变量及其归宿：
 *   - --m-shadow-ios-*：多层 box-shadow → [SaltShadowTokens]（SaltShadows.kt）
 *   - --m-radius-* / --m-font-size-* / --m-spacing 等：→ SaltRadius/SaltFontSize/SaltSpacing（SaltTypeAndShape.kt）
 *   - --m-safe-area-* / --m-navbar-pt / --m-content-pb：布局期 insets，由页面按
 *     WindowInsets 实时计算，静态令牌只保留公式基准值（见 [SaltSpacing]）
 *   - --m-primary-rgb：仅服务 CSS rgba(var()) 内插，Compose 直接用 Color.copy(alpha) 等价
 */
data class SaltColors(
    /** `--m-glass-bg`：液态玻璃背景（MiniPlayer/FAB 共用，统一 alpha） */
    val glassBg: Color,
    /** `--m-navbar-glass-bg`：navbar 灰底磨砂玻璃（基底 = --m-surface 的 0.65 alpha + blur） */
    val navbarGlassBg: Color,
    /** `--m-surface`：页面底色（Salt background） */
    val surface: Color,
    /** `--m-surface-1`：卡片/导航底（Salt subBackground） */
    val surface1: Color,
    /** `--m-surface-1-shade`：surface-1 暗化档 */
    val surface1Shade: Color,
    /** `--m-surface-1-tint`：surface-1 亮化档 */
    val surface1Tint: Color,
    /** `--m-surface-2`：略深于 surface-1 */
    val surface2: Color,
    /** `--m-surface-3` */
    val surface3: Color,
    /** `--m-surface-variant` */
    val surfaceVariant: Color,
    /** `--m-text`：主文字色 */
    val text: Color,
    /** `--m-text-2` / `--m-text-secondary`：次级文字（Salt subText） */
    val text2: Color,
    /** `--m-text-3` / `--m-text-tertiary`：三级文字 / placeholder */
    val text3: Color,
    /** `--m-primary`：品牌高亮色（Salt highlight） */
    val primary: Color,
    /** `--m-primary-tint`：highlight 亮化 */
    val primaryTint: Color,
    /** `--m-primary-shade`：highlight 暗化 */
    val primaryShade: Color,
    /** `--m-on-primary`：highlight 上文字/图标（Salt onHighlight） */
    val onPrimary: Color,
    /** `--m-danger`：危险语义色（`.dark` 未重定义，两套同值） */
    val danger: Color,
    /** `--m-danger-shade`（`.dark` 未重定义，两套同值） */
    val dangerShade: Color,
    /** `--m-success`：成功语义色（`.dark` 未重定义，两套同值） */
    val success: Color,
    /** `--m-disabled-text`：禁用态文字 */
    val disabledText: Color,
    /** `--m-disabled-bg`：禁用态背景 */
    val disabledBg: Color,
    /** `--m-disabled-border`：禁用态描边 */
    val disabledBorder: Color,
    /** `--m-hairline` / `--m-stroke`：分隔线（Salt stroke） */
    val hairline: Color,
    /** `--m-glass-light`：白玻璃（保留类使用） */
    val glassLight: Color,
    /** `--m-glass-dark`：深色玻璃（保留类使用；`.dark` 下与 light 同值） */
    val glassDark: Color,
    // ---- 过渡兼容字段：GlassSurface(GlassLevel) 旧 API 仍在使用（MusesApp 播放入口）。
    // 后续 m-* 组件落地时逐个替换为上方 glassBg/navbarGlassBg/glassLight 后删除。
    val glassFaint: Color,
    val glassSubtle: Color,
    val glassMedium: Color,
    val glassStrong: Color,
)

/**
 * GlassSurface 过渡兼容用的通用玻璃梯度 alpha（原 M1 占位值，非 SCSS 令牌）。
 */
object GlassAlpha {
    const val FAINT = 0.06f
    const val SUBTLE = 0.10f
    const val MEDIUM = 0.16f
    const val STRONG = 0.24f
}

private fun overlayGlass(baseLight: Boolean, alpha: Float): Color =
    if (baseLight) Color(0xFFFFFFFF).copy(alpha = alpha) else Color(0xFF000000).copy(alpha = alpha)

/** 明色全套（SCSS `:root` 块） */
val SaltLightColors: SaltColors = SaltColors(
    glassBg = Color(0x73FFFFFF),            // rgba(255,255,255,0.45)
    navbarGlassBg = Color(0xA6F3F3F3),      // rgba(243,243,243,0.65)
    surface = Color(0xFFF3F3F3),
    surface1 = Color(0xFFF9F9F9),
    surface1Shade = Color(0xFFEEF0F2),
    surface1Tint = Color(0xFFFDFDFD),
    surface2 = Color(0xFFECECEC),
    surface3 = Color(0xFFF9F9F9),
    surfaceVariant = Color(0xFFECECEC),
    text = Color(0xFF191919),
    text2 = Color(0xFF8C8C8C),
    text3 = Color(0x4D191919),              // rgba(25,25,25,0.3)
    primary = Color(0xFF0470E6),
    primaryTint = Color(0xFF2F8AF0),
    primaryShade = Color(0xFF035FC2),
    onPrimary = Color(0xFFFFFFFF),
    danger = Color(0xFFFF3B30),
    dangerShade = Color(0xFFD70015),
    success = Color(0xFF34C759),
    disabledText = Color(0x4D1E1715),       // rgba(30,23,21,0.3)
    disabledBg = Color(0x141E1715),         // rgba(30,23,21,0.08)
    disabledBorder = Color(0x1A1E1715),     // rgba(30,23,21,0.1)
    hairline = Color(0x268C8C8C),           // rgba(140,140,140,0.15)
    glassLight = Color(0xBFFFFFFF),          // rgba(255,255,255,0.75)
    glassDark = Color(0x80323232),          // rgba(50,50,50,0.5)
    glassFaint = overlayGlass(false, GlassAlpha.FAINT),
    glassSubtle = overlayGlass(true, GlassAlpha.SUBTLE),
    glassMedium = overlayGlass(true, GlassAlpha.MEDIUM),
    glassStrong = overlayGlass(true, GlassAlpha.STRONG),
)

/** 暗色全套（SCSS `.dark` 块；未重定义变量沿用明色值） */
val SaltDarkColors: SaltColors = SaltColors(
    glassBg = Color(0x731E1E1E),            // rgba(30,30,30,0.45)
    navbarGlassBg = Color(0xA6202020),      // rgba(32,32,32,0.65)
    surface = Color(0xFF202020),
    surface1 = Color(0xFF262626),
    surface1Shade = Color(0xFF1A1A1A),
    surface1Tint = Color(0xFF2E2E2E),
    surface2 = Color(0xFF2B2B2B),
    surface3 = Color(0xFF262626),
    surfaceVariant = Color(0xFF2B2B2B),
    text = Color(0xFFEBEEF1),
    text2 = Color(0xBFE1E6EB),              // rgba(225,230,235,0.75)
    text3 = Color(0x4DE1E6EB),              // rgba(225,230,235,0.3)
    primary = Color(0xFF0088FF),
    primaryTint = Color(0xFF2B9CFF),
    primaryShade = Color(0xFF0070D6),
    onPrimary = Color(0xFFFFFFFF),
    danger = Color(0xFFFF3B30),
    dangerShade = Color(0xFFD70015),
    success = Color(0xFF34C759),
    disabledText = Color(0x4DE1E6EB),       // rgba(225,230,235,0.3)
    disabledBg = Color(0x14E1E6EB),         // rgba(225,230,235,0.08)
    disabledBorder = Color(0x1AE1E6EB),     // rgba(225,230,235,0.1)
    hairline = Color(0x1AE1E6EB),           // rgba(225,230,235,0.1)
    glassLight = Color(0x80323232),          // rgba(50,50,50,0.5)
    glassDark = Color(0x80323232),          // rgba(50,50,50,0.5)
    glassFaint = overlayGlass(false, GlassAlpha.FAINT),
    glassSubtle = overlayGlass(true, GlassAlpha.SUBTLE),
    glassMedium = overlayGlass(true, GlassAlpha.MEDIUM),
    glassStrong = overlayGlass(true, GlassAlpha.STRONG),
)

/**
 * Material3 近似槽位映射：保证 Material 行为组件（Scaffold/Button 底座等）
 * 在复刻页面上底色不突兀。视觉精确层一律走 [LocalSaltColors]。
 */
val SaltLightScheme: ColorScheme = lightColorScheme(
    primary = SaltLightColors.primary,
    onPrimary = SaltLightColors.onPrimary,
    secondary = SaltLightColors.text2,
    error = SaltLightColors.danger,
    background = SaltLightColors.surface,           // 页面底 #F3F3F3
    onBackground = SaltLightColors.text,
    surface = SaltLightColors.surface1,             // 卡片/导航底 #F9F9F9
    onSurface = SaltLightColors.text,
    surfaceVariant = SaltLightColors.surfaceVariant,
    onSurfaceVariant = SaltLightColors.text2,
    surfaceContainerLowest = SaltLightColors.surface1Tint,
    surfaceContainerLow = SaltLightColors.surface1,
    surfaceContainer = SaltLightColors.surface,
    surfaceContainerHigh = SaltLightColors.surface1Shade,
    surfaceContainerHighest = SaltLightColors.surfaceVariant,
    outline = SaltLightColors.hairline,
    outlineVariant = SaltLightColors.hairline,
)

val SaltDarkScheme: ColorScheme = darkColorScheme(
    primary = SaltDarkColors.primary,
    onPrimary = SaltDarkColors.onPrimary,
    secondary = SaltDarkColors.text2,
    error = SaltDarkColors.danger,
    background = SaltDarkColors.surface,             // 页面底 #202020
    onBackground = SaltDarkColors.text,
    surface = SaltDarkColors.surface1,               // 卡片/导航底 #262626
    onSurface = SaltDarkColors.text,
    surfaceVariant = SaltDarkColors.surfaceVariant,
    onSurfaceVariant = SaltDarkColors.text2,
    surfaceContainerLowest = SaltDarkColors.surface1Tint,
    surfaceContainerLow = SaltDarkColors.surface1,
    surfaceContainer = SaltDarkColors.surface,
    surfaceContainerHigh = SaltDarkColors.surface1Shade,
    surfaceContainerHighest = SaltDarkColors.surface1Shade, // 无更暗令牌，复用 shade
    outline = SaltDarkColors.hairline,
    outlineVariant = SaltDarkColors.hairline,
)

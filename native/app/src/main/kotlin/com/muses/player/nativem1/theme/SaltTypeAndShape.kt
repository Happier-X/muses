package com.muses.player.nativem1.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 圆角令牌 —— 翻译自 `src/theme/index.scss`：
 *   --m-radius-sm: 8px / --m-radius-md: 16px / --m-radius-lg: 24px
 *   --m-radius-card: 12px / --m-radius-dialog: 20px
 */
object SaltRadius {
    val sm = 8.dp        // --m-radius-sm
    val md = 16.dp       // --m-radius-md（按钮）
    val lg = 24.dp       // --m-radius-lg
    val card = 12.dp     // --m-radius-card（卡片 corner）
    val dialog = 20.dp   // --m-radius-dialog（dialogCorner）
}

/**
 * 字号令牌 —— 翻译自 `--m-font-size-*`（字重沿用现有 Material 映射，SCSS 未定义全局字重变量）。
 */
object SaltFontSize {
    val sm = 12.sp       // --m-font-size-sm（small）
    val md = 16.sp       // --m-font-size-md（body）
    val lg = 24.sp       // --m-font-size-lg（title）
}

/**
 * 间距/尺寸令牌 —— 翻译自 `--m-spacing* / --m-list-*`，
 * 以及 index.scss §3 `.m-app` 的内容避让公式基准值。
 *
 * 安全区相关（--m-safe-area-* / --m-navbar-pt / --m-content-pb）是布局期 insets：
 * 页面用 WindowInsets 实时计算，此处只保留公式中的静态基准值。
 */
object SaltSpacing {
    val spacing = 16.dp          // --m-spacing（padding）
    val spacingSub = 12.dp       // --m-spacing-sub（subPadding）
    val listRowHeight = 56.dp    // --m-list-row-h（item 行高）
    val listIcon = 24.dp         // --m-list-icon（行图标）

    /** MiniPlayer 高度基准：--m-content-pb = calc(72px + safe-area-bottom) 中的 72px */
    val miniPlayerHeight = 72.dp

    /** navbar 顶部避让下限：--m-navbar-pt = max(16px, safe-area-top) 中的 16px */
    val navbarTopPaddingMin = 16.dp
}

/**
 * Material3 Shapes 槽位 ← SaltRadius 对齐：
 *   extraSmall ← radius-sm(8) / small ← radius-card(12) / medium ← radius-md(16)
 *   large ← radius-dialog(20) / extraLarge ← radius-lg(24)
 */
val SaltShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(SaltRadius.sm),
    small = RoundedCornerShape(SaltRadius.card),
    medium = RoundedCornerShape(SaltRadius.md),
    large = RoundedCornerShape(SaltRadius.dialog),
    extraLarge = RoundedCornerShape(SaltRadius.lg),
)

/** Salt 字阶（关键槽位对齐 SaltFontSize；其余槽位维持 M1 过渡值） */
val SaltTypography: Typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = SaltFontSize.lg, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = SaltFontSize.md, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = SaltFontSize.md, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = SaltFontSize.sm, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

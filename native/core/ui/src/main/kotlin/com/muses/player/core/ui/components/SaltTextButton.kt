package com.muses.player.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius

/** MButton 尺寸档位（`.m-button--small/--md/--large`） */
enum class SaltTextButtonSize(val height: Dp, val fontSize: Int, val fontWeight: FontWeight) {
    SMALL(28.dp, 14, FontWeight.Medium),
    MD(34.dp, 15, FontWeight.Medium),
    LARGE(48.dp, 17, FontWeight.SemiBold),
}

/**
 * `.m-button--clear.m-button--inline` —— MButton.vue clear 变体一比一翻译。
 *
 * 视觉契约：
 * - `color: var(--m-primary)`；`background-color: transparent`；
 * - `:active` → `rgba(var(--m-primary-rgb), 0.15)`；
 * - 高度/字号按 [SaltTextButtonSize]；圆角 `--m-radius-md`(16px)；
 * - 水平 padding `0 8px`；inline 不占满容器宽度；
 * - disabled：文字 `--m-disabled-text`，clear 保持透明底
 *   （`&.m-button--clear { background-color: transparent }`）。
 *
 * 其余变体（fill/outline/tonal）按映射表在首个使用页落地时再扩展。
 */
@Composable
fun SaltTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: SaltTextButtonSize = SaltTextButtonSize.MD,
    enabled: Boolean = true,
) {
    val salt = LocalSaltColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // :active 背景过渡（transition: background-color 0.1s ease）
    val bgAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "m-button-clear-active-bg",
    )

    Box(
        modifier = modifier
            .height(size.height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(SaltRadius.md))
            .background(
                // rgba(var(--m-primary-rgb), 0.15)
                color = salt.primary.copy(alpha = 0.15f * bgAlpha),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) salt.primary else salt.disabledText,
            fontSize = size.fontSize.sp,
            fontWeight = size.fontWeight,
            lineHeight = (size.fontSize + 3).sp, // 近似 Web 默认行高观感
            maxLines = 1,
        )
    }
}

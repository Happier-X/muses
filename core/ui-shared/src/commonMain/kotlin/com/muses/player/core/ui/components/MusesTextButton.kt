package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 文本按钮尺寸档位（沿用原 Salt 三档：small/md/large） */
enum class MusesTextButtonSize(val height: Dp, val fontSize: Int, val fontWeight: FontWeight) {
    SMALL(28.dp, 14, FontWeight.Medium),
    MD(34.dp, 15, FontWeight.Medium),
    LARGE(48.dp, 17, FontWeight.SemiBold),
}

/**
 * 文本按钮（miuix TextButton；原 SaltTextButton 的 clear 语义：透明底 + 主色文字，
 * 按压由 miuix 承担；danger 档文字走 error）。
 */
@Composable
fun MusesTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: MusesTextButtonSize = MusesTextButtonSize.MD,
    enabled: Boolean = true,
    /** 危险动作（多选条「永久删除」）：文字用 error */
    destructive: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        minWidth = 0.dp,
        minHeight = size.height,
        colors = ButtonDefaults.textButtonColors(
            textColor = if (destructive) scheme.error else scheme.primary,
        ),
        insideMargin = PaddingValues(horizontal = 8.dp),
        textStyle = TextStyle(
            fontSize = size.fontSize.sp,
            fontWeight = size.fontWeight,
            lineHeight = (size.fontSize + 3).sp,
        ),
    )
}

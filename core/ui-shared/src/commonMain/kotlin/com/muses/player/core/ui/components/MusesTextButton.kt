package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme


/**
 * 基于 Miuix 官方 TextButton 的轻量兼容封装。
 * 常规调用沿用官方默认样式；紧凑尺寸与危险操作仅在调用方明确指定时覆盖。
 */
@Composable
fun MusesTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** 危险动作（多选条「永久删除」）：文字用 error */
    destructive: Boolean = false,
    /** 主确认动作：使用 Miuix 主文字按钮颜色 */
    primary: Boolean = false,
    loading: Boolean = false,
) {
    val scheme = MiuixTheme.colorScheme
    val label = if (loading) "$text…" else text

    if (destructive) {
        TextButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
            colors = ButtonDefaults.textButtonColors(textColor = scheme.error),
        )
    } else if (primary) {
        TextButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    } else {
        TextButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
        )
    }
}

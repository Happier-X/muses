package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Button

/**
 * 填充按钮封装（miuix Button；替换 M3 Button）。
 *
 * 内容槽保持 `RowScope.() -> Unit`，原来 `Button(onClick) { Text("xxx") }`
 * 的写法零改动，只需把导入换成 [MusesButton]。
 * 颜色圆角等走 miuix 默认 HyperOS 风格，不再逐处指定。
 */
@Composable
fun MusesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

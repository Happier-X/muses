package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 复选框封装（miuix Checkbox；替换 M3 Checkbox）。
 *
 * 参数映射：
 * - M3 的 `checked: Boolean + onCheckedChange: (Boolean) -> Unit` 收敛为
 *   `checked + onToggle: () -> Unit`，调用方原来就是忽略新值直接取反，
 *   与 miuix 的 `state + onClick` 语义一致。
 * - M3 的 `CheckboxDefaults.colors(checkedColor = primary)` 对应
 *   miuix 的 `checkboxColors(checkedBackgroundColor = primary)`。
 */
@Composable
fun MusesCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MiuixTheme.colorScheme.primary,
) {
    Checkbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = onToggle,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.checkboxColors(
            checkedBackgroundColor = checkedColor,
        ),
    )
}

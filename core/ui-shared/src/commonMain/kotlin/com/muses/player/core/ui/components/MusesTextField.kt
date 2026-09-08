package com.muses.player.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import top.yukonga.miuix.kmp.basic.TextField

/**
 * 输入框封装（miuix TextField；替换 M3 OutlinedTextField）。
 *
 * 参数映射：
 * - M3 的 `label = { Text("xxx") }` 直接传 `label = "xxx"` 字符串即可。
 * - M3 的 `placeholder` 在 miuix 侧无独立槽位，这里用 `useLabelAsPlaceholder = true`
 *   让标签在空值时充当占位提示；原来 `label` 与 `placeholder` 同文案的场景零差异。
 * - 原来 `placeholder` 为示例文案（如网址示例）而 `label` 另有说明的场景，
 *   示例文案会被收敛掉，以标签为准，符合 HyperOS 单标签输入风格。
 */
@Composable
fun MusesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = true,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

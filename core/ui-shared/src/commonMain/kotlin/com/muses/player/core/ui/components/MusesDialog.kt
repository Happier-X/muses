package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 对话框封装（miuix OverlayDialog；替换 M3 AlertDialog）。
 *
 * 设计说明：
 * - 调用方保持原有 `?.let { MusesDialog(...) }` 条件组合写法，内部固定 `show = true`，
 *   与 [MusesActionsSheet] 的 `opened` 模式区分：对话框由是否组合决定显隐。
 * - `title` 对应原 `AlertDialog.title` 的纯文本部分，原先 `Text(title)` 直接传字符串即可。
 * - `message` 对应原 `text` 为纯文本的场景，走 `summary` 槽位；富文本或表单场景走 [content]。
 * - 按钮区统一用 [MusesTextButton] 横排右对齐，危险确认走 `destructive = true`（红色）。
 * - 不可取消场景（如扫描进度）：传 `onDismiss = {}` 且不传 `dismissText`，与原来保持一致。
 */
@Composable
fun MusesDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    destructiveConfirm: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    OverlayDialog(
        show = true,
        title = title,
        summary = message,
        onDismissRequest = onDismiss,
        renderInRootScaffold = false,
        content = {
            Column(Modifier.fillMaxWidth()) {
                content()
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (dismissText != null) {
                        MusesTextButton(
                            text = dismissText,
                            onClick = onDismiss,
                        )
                    }
                    if (confirmText != null) {
                        MusesTextButton(
                            text = confirmText,
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            destructive = destructiveConfirm,
                        )
                    }
                }
            }
        },
    )
}

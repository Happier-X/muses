package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

/**
 * 底部弹窗封装（miuix OverlayBottomSheet；替换 M3 ModalBottomSheet）。
 *
 * 调用方保持原有条件组合写法（`if (show) MusesBottomSheet(...)`），内部固定 `show = true`，
 * 与 [MusesDialog] 一致；`renderInRootScaffold = false` 与 [MusesActionsSheet] 一致。
 * 原来写在内容区的标题 `Text` 请上移到 [title] 参数，内容只留表单和按钮。
 */
@Composable
fun MusesBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OverlayBottomSheet(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        renderInRootScaffold = false,
        modifier = modifier,
        content = content,
    )
}

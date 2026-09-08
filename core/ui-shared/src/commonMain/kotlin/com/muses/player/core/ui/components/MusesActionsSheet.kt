package com.muses.player.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底部弹出的操作单（miuix OverlayBottomSheet；原 SaltActionsSheet 的 Konsta Actions 语义）。
 *
 * label → sheet 标题；每行一个动作按钮（居中 17sp，危险动作用 error 色）；
 * 取消按钮独立在末尾。`renderInRootScaffold = false`：Muses 未使用 miuix Scaffold，
 * sheet 自带 Dialog 容器即可。
 */
data class MusesActionItem(
    val label: String,
    /** 危险动作（如删除）：红色文字 */
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun MusesActionsSheet(
    opened: Boolean,
    onDismiss: () -> Unit,
    label: String,
    items: List<MusesActionItem>,
) {
    val scheme = MiuixTheme.colorScheme
    OverlayBottomSheet(
        show = opened,
        title = label,
        onDismissRequest = onDismiss,
        renderInRootScaffold = false,
        content = {
            Column(Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable(onClick = item.onClick)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = item.label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (item.destructive) scheme.error else scheme.primary,
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(onClick = onDismiss),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "取消",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        },
    )
}

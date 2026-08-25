package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors

/**
 * `m-actions` / `m-actions-group` / `m-actions-label` / `m-actions-button` ——
 * 底部弹出的操作单（Konsta Actions 风格）。
 *
 * 对照 Web 结构：圆角卡分组 + label 小字头 + 每行一个动作按钮（居中、17sp）、
 * 组间距 8dp、底部安全区避让。backdrop 点击关闭由 ModalBottomSheet 承担。
 */
data class SaltActionItem(
    val label: String,
    val onClick: () -> Unit,
    /** 危险动作（如删除）：红色文字 */
    val destructive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaltActionsSheet(
    opened: Boolean,
    onDismiss: () -> Unit,
    label: String,
    items: List<SaltActionItem>,
) {
    if (!opened) return
    val salt = LocalSaltColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            // m-actions-group：surface-2 圆角卡
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(salt.surface2, RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // m-actions-label：小字组头
                Text(
                    text = label,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = salt.text2,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
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
                            color = if (item.destructive) salt.danger else salt.primary,
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // 取消按钮独立一张卡（Konsta actions 惯例）
            Column(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(salt.surface2, RoundedCornerShape(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                        color = salt.text,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


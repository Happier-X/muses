package com.muses.player.feature.playlist

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.components.MusesTextField

/** 新建/重命名共用对话框（m-dialog + mListInput；U10 从 PlaylistsPage 上收 commonMain） */
@Composable
internal fun NameEditDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = "名称",
) {
    var name by remember { mutableStateOf(initialName) }
    MusesDialog(
        onDismiss = onDismiss,
        title = title,
        confirmText = "确定",
        onConfirm = { onConfirm(name) },
        confirmEnabled = name.isNotBlank(),
        dismissText = "取消",
        content = {
            MusesTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

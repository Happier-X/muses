package com.muses.player.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.muses.player.core.ui.components.MusesDialog

/** 下载列表开始下载后显示；放在当前可见 Scaffold 内，切换页面仍能处理确认。 */
@Composable
fun DownloadLibraryDuplicateDialog(manager: DownloadManager) {
    val prompt by manager.libraryDuplicatePrompt.collectAsState()
    val sources by manager.availableSources.collectAsState()
    prompt?.let { duplicate ->
        MusesDialog(
            onDismiss = { manager.respondToLibraryDuplicate(duplicate.id, false) },
            title = "曲库已存在这首歌",
            message = duplicate.message(sources.associate { it.id to it.name }),
            dismissText = "取消",
            confirmText = "确定",
            onConfirm = { manager.respondToLibraryDuplicate(duplicate.id, true) },
        )
    }
}

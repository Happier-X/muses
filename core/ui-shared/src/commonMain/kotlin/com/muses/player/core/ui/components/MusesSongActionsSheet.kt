package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable

/** 曲库和沉浸式播放页共用的歌曲操作菜单。 */
@Composable
fun MusesSongActionsSheet(
    songId: String?,
    onDismiss: () -> Unit,
    onEnqueueScrape: (List<String>) -> Unit,
    // 播放端口尚未提供追加队列能力，沿用曲库现有的预留入口。
    onAddToQueue: (String) -> Unit = {},
    onEditSong: (() -> Unit)? = null,
) {
    MusesActionsSheet(
        opened = songId != null,
        onDismiss = onDismiss,
        label = "歌曲操作",
        items = listOfNotNull(
            onEditSong?.let { edit ->
                MusesActionItem(label = "编辑歌曲信息", onClick = {
                    onDismiss()
                    edit()
                })
            },
            MusesActionItem(label = "加入待刮削", onClick = {
                songId?.let {
                    onEnqueueScrape(listOf(it))
                    MusesSnackbar.show("已加入待刮削队列")
                }
                onDismiss()
            }),
            MusesActionItem(label = "添加到队列", onClick = {
                songId?.let(onAddToQueue)
                onDismiss()
            }),
        ),
    )
}

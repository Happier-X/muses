package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable

/** 曲库和沉浸式播放页共用的歌曲操作菜单。 */
@Composable
fun MusesSongActionsSheet(
    songId: String?,
    onDismiss: () -> Unit,
    onEnqueueScrape: (List<String>) -> Unit,
    /** 追加到播放队列（PlaybackPort.addToQueue）；提示文案由调用方给出，空列表不算成功。 */
    onAddToQueue: (String) -> Unit,
    onEnqueueDownload: (() -> Unit)? = null,
) {
    MusesActionsSheet(
        opened = songId != null,
        onDismiss = onDismiss,
        label = "歌曲操作",
        items = listOfNotNull(
            onEnqueueDownload?.let { enqueue -> MusesActionItem("添加到下载队列", onClick = { enqueue(); onDismiss() }) },
            MusesActionItem(label = "添加到刮削队列", onClick = {
                songId?.let {
                    onEnqueueScrape(listOf(it))
                    MusesSnackbar.show("添加成功")
                }
                onDismiss()
            }),
            MusesActionItem(label = "添加到播放队列", onClick = {
                songId?.let(onAddToQueue)
                onDismiss()
            }),
        ),
    )
}

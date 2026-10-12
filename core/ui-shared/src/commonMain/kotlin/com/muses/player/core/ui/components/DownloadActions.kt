package com.muses.player.core.ui.components

import androidx.compose.runtime.*
import com.muses.player.core.model.Song

/** 导航壳提供入队动作，列表不会直接获取音频。 */
val LocalDownloadSong = staticCompositionLocalOf<((Song) -> Unit)?> { null }

@Composable
fun OnlineSongDownloadAction(song: Song, onDismissRecommendation: (() -> Unit)? = null) {
    val enqueue = LocalDownloadSong.current
    if (enqueue == null && onDismissRecommendation == null) return
    var opened by remember { mutableStateOf(false) }
    SongMoreButton(onClick = { opened = true })
    MusesActionsSheet(opened = opened, onDismiss = { opened = false }, label = "歌曲操作",
        items = buildList {
            if (enqueue != null) add(MusesActionItem("添加到下载队列", onClick = { enqueue(song); opened = false }))
            if (onDismissRecommendation != null) add(MusesActionItem("无感", onClick = { onDismissRecommendation(); opened = false }))
        })
}

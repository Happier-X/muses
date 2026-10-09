package com.muses.player.core.ui.components

import androidx.compose.runtime.*
import com.muses.player.core.model.Song

/** 导航壳提供入队动作，列表不会直接获取音频。 */
val LocalDownloadSong = staticCompositionLocalOf<((Song) -> Unit)?> { null }

@Composable
fun OnlineSongDownloadAction(song: Song) {
    val enqueue = LocalDownloadSong.current ?: return
    var opened by remember { mutableStateOf(false) }
    SongMoreButton(onClick = { opened = true })
    MusesActionsSheet(opened = opened, onDismiss = { opened = false }, label = "歌曲操作",
        items = listOf(MusesActionItem("添加到下载队列", onClick = { enqueue(song); opened = false })))
}

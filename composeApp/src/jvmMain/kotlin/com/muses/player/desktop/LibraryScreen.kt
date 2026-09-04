package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.components.SongListItem
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.data.db.SongEntity
import com.muses.player.desktop.playback.DesktopPlayerHook

/**
 * 桌面库房页（S3b 最小版）：音源列表 + 曲目列表 + 播放。
 * 复用平板曲库视觉（自适应网格语义），首版用列表保证可用。
 *
 * U5 曲目列表共用化：SongListItem 从 :core:ui-shared 平台无关共用组件消费，
 * 桌面复刻 SongRow 已移除。
 */
@Composable
fun LibraryScreen(playerHook: DesktopPlayerHook?) {
    val salt = LocalSaltColors.current
    val hook = remember { playerHook ?: DesktopPlayerHook() }
    val songs by hook.songs.collectAsState()
    val sources by hook.sources.collectAsState()
    val status by hook.status.collectAsState()
    val currentSongId by hook.currentSongId.collectAsState()

    LaunchedEffect(Unit) { hook.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().background(salt.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "曲库",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (sources.isNotEmpty()) {
            Text(
                text = "音源 ${sources.size} 个 · 歌曲 ${songs.size} 首",
                color = salt.text2,
                fontSize = 13.sp,
            )
        }
        if (status.isNotBlank()) {
            Text(text = status, color = salt.danger, fontSize = 13.sp)
        }
        if (songs.isEmpty()) {
            Spacer(modifier = Modifier.height(48.dp))
            SaltEmpty(
                title = "曲库为空",
                description = "请在设置页添加 WebDAV 音源后扫描",
                icon = TablerIcons.MusicNote,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song.toSongItem(),
                        isCurrent = song.id == currentSongId,
                        onClick = { hook.play(song.id) },
                    )
                }
            }
        }
    }
}

/** Room SongEntity → 跨平台 SongItem 映射 */
private fun SongEntity.toSongItem() = SongItem(
    id = id,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
)

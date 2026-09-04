package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.db.SongEntity
import com.muses.player.desktop.playback.DesktopPlayerHook

/**
 * 桌面库房页（S3b 最小版）：音源列表 + 曲目列表 + 播放。
 * 复用平板曲库视觉（自适应网格语义），首版用列表保证可用。
 */
@Composable
fun LibraryScreen(playerHook: DesktopPlayerHook?) {
    val hook = remember { playerHook ?: DesktopPlayerHook() }
    val songs by hook.songs.collectAsState()
    val sources by hook.sources.collectAsState()
    val status by hook.status.collectAsState()
    val currentSongId by hook.currentSongId.collectAsState()

    LaunchedEffect(Unit) { hook.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "曲库",
            color = Color(0xFFCDD6F4),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (sources.isNotEmpty()) {
            Text(
                text = "音源 ${sources.size} 个 · 歌曲 ${songs.size} 首",
                color = Color(0xFF7F849C),
                fontSize = 13.sp,
            )
        }
        if (status.isNotBlank()) {
            Text(text = status, color = Color(0xFFF38BA8), fontSize = 13.sp)
        }
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "曲库为空\n请在设置页添加 WebDAV 音源后扫描",
                    color = Color(0xFF585B70),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { hook.play(song.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: SongEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) Color(0xFF313244) else Color(0xFF181825))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) Color(0xFF89B4FA) else Color(0xFFCDD6F4),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(song.artist, song.albumTitle)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color(0xFF7F849C),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isCurrent) {
            Text(text = "▶", color = Color(0xFF89B4FA), fontSize = 14.sp)
        }
    }
}

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.desktop.playback.DesktopPlayerHook

/**
 * 桌面播放页（S3b 最小版）：封面占位 + 标题 + 进度 + 控制栏 + 音量。
 * 复用平板沉浸布局视觉（双栏语义），首版单栏保证可用。
 */
@Composable
fun PlayerScreen(playerHook: DesktopPlayerHook?) {
    val hook = remember { playerHook ?: DesktopPlayerHook() }
    val songs by hook.songs.collectAsState()
    val currentSongId by hook.currentSongId.collectAsState()
    val isPlaying by hook.isPlaying.collectAsState()
    val positionMs by hook.positionMs.collectAsState()
    val durationMs by hook.durationMs.collectAsState()
    val volume by hook.volume.collectAsState()
    val status by hook.status.collectAsState()

    val currentSong = songs.firstOrNull { it.id == currentSongId }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "正在播放",
            color = Color(0xFF7F849C),
            fontSize = 13.sp,
        )
        // 封面占位（首版无 Coil 图片链，纯色块 + 首字）
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF313244)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currentSong?.title?.firstOrNull()?.toString() ?: "♪",
                color = Color(0xFF585B70),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // 标题区
        Text(
            text = currentSong?.title ?: "未在播放",
            color = Color(0xFFCDD6F4),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        val subtitle = listOfNotNull(currentSong?.artist, currentSong?.albumTitle)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        if (subtitle.isNotBlank()) {
            Text(text = subtitle, color = Color(0xFF7F849C), fontSize = 14.sp)
        }
        // 进度条
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            var sliderValue by remember(positionMs) { mutableStateOf(positionMs.toFloat()) }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { hook.seekTo(sliderValue.toLong()) },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatMs(positionMs), color = Color(0xFF7F849C), fontSize = 12.sp)
                Text(text = formatMs(durationMs), color = Color(0xFF7F849C), fontSize = 12.sp)
            }
        }
        // 控制栏
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton("⏮", 44.dp) { hook.previous() }
            ControlButton(if (isPlaying) "⏸" else "▶", 64.dp, primary = true) {
                hook.togglePlayPause()
            }
            ControlButton("⏭", 44.dp) { hook.next() }
        }
        // 音量
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(text = "音量", color = Color(0xFF7F849C), fontSize = 13.sp)
            var vol by remember(volume) { mutableStateOf(volume.toFloat()) }
            Slider(
                value = vol,
                onValueChange = { vol = it },
                onValueChangeFinished = { hook.setVolume(vol.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
            )
            Text(text = "$volume", color = Color(0xFF7F849C), fontSize = 13.sp)
        }
        if (status.isNotBlank()) {
            Text(text = status, color = Color(0xFFF38BA8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    size: androidx.compose.ui.unit.Dp,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) Color(0xFF89B4FA) else Color(0xFF313244))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) Color(0xFF11111B) else Color(0xFFCDD6F4),
            fontSize = if (primary) 24.sp else 18.sp,
        )
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

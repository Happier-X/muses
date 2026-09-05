package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.muses.player.desktop.playback.DesktopLyricsSearchState
import com.muses.player.desktop.playback.DesktopLyricsState
import com.muses.player.desktop.playback.DesktopPlayerHook
import com.muses.player.feature.player.lyric.SimpleLyricsPanel

/** 桌面歌词来源展示名（在线命中源 wire → 友好名） */
private val lyricsSourceLabels = mapOf(
    "amll" to "AMLL TTML",
    "kw" to "酷我",
    "tx" to "QQ音乐",
    "wy" to "网易云",
    "kg" to "酷狗",
    "mg" to "咪咕",
    "lrclib" to "LRCLIB",
)

/**
 * 桌面播放页：封面/歌词双面板（任务 09-05-desktop-player-lyrics Y2/Y3）。
 * 左侧封面 + 标题 + 进度 + 控制栏 + 音量（控制行为与 S3b 单栏版一致）；
 * 右侧歌词面板复用安卓 SimpleLyricsPanel（随播放进度滚动定位，点击行 seek），
 * 无歌词时提供「在线搜索」补充链（LyricsMatcher：AMLL+五源+LRCLIB），命中仅内存展示。
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
    val lyrics by hook.lyrics.collectAsState()
    val lyricsSearch by hook.lyricsSearch.collectAsState()

    val currentSong = songs.firstOrNull { it.id == currentSongId }

    Row(
        modifier = Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 左面板：封面 + 控制（保持原控制区行为不变）
        Column(
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
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
        // 分隔线
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF313244)),
        )
        // 右面板：歌词（SimpleLyricsPanel 上收版；随进度滚动定位）
        Box(
            modifier = Modifier.weight(0.58f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (lyrics.lines.isEmpty()) {
                LyricsEmptyState(
                    hasSong = currentSong != null,
                    searchState = lyricsSearch,
                    onSearch = hook::searchOnlineLyrics,
                )
            } else {
                SimpleLyricsPanel(
                    lines = lyrics.lines,
                    positionMs = positionMs,
                    isPlaying = isPlaying,
                    onSeek = { hook.seekTo(it) },
                    modifier = Modifier.fillMaxSize(),
                )
                // 来源标签（库内歌词不标注；在线命中标注来源）
                val sourceTag = when (val source = lyrics.source) {
                    null, DesktopLyricsState.SOURCE_LIBRARY -> null
                    else -> "来源：${lyricsSourceLabels[source] ?: source}"
                }
                if (sourceTag != null) {
                    Text(
                        text = sourceTag,
                        color = Color(0xFF7F849C),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color(0xFF313244).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** 歌词空态：提示 + 在线搜索按钮（Y3；搜索中/失败态就地反馈） */
@Composable
private fun LyricsEmptyState(
    hasSong: Boolean,
    searchState: DesktopLyricsSearchState,
    onSearch: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "暂无歌词",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
        )
        when (searchState) {
            is DesktopLyricsSearchState.Searching -> {
                CircularProgressIndicator(
                    color = Color(0xFF89B4FA),
                    modifier = Modifier.size(22.dp),
                )
                Text(text = "正在在线搜索歌词…", color = Color(0xFF7F849C), fontSize = 13.sp)
            }
            is DesktopLyricsSearchState.Failed -> {
                Text(text = searchState.message, color = Color(0xFFF38BA8), fontSize = 13.sp)
                SearchButton(enabled = hasSong, onSearch = onSearch)
            }
            is DesktopLyricsSearchState.Idle -> {
                SearchButton(enabled = hasSong, onSearch = onSearch)
            }
        }
    }
}

@Composable
private fun SearchButton(enabled: Boolean, onSearch: () -> Unit) {
    Button(
        onClick = onSearch,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF89B4FA),
            contentColor = Color(0xFF11111B),
        ),
    ) {
        Text(text = "在线搜索")
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

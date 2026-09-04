package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import com.muses.player.core.playback.PlayerPort
import com.muses.player.core.model.playback.RepeatMode

/**
 * 桌面应用主 Composable（S3a 占位版）：
 * 自绘标题栏 + 内容区（播放器状态占位文本）。
 * S3b 将内容区替换为三屏导航（库房/播放/设置）。
 */
@Composable
fun MusesDesktopApp(
    windowState: WindowState,
    onClose: () -> Unit,
    playerPort: PlayerPort? = null,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF11111B))) {
        DesktopTitleBar(
            windowState = windowState,
            onClose = onClose,
        )
        // S3a 占位内容区
        ContentPlaceholder(playerPort)
    }
}

@Composable
private fun ContentPlaceholder(playerPort: PlayerPort?) {
    if (playerPort == null) {
        Text(
            text = "Muses Desktop (S3a 占位)\n播放器未连接",
            color = Color(0xFFCDD6F4),
            fontSize = 16.sp,
            modifier = Modifier.background(Color(0xFF1E1E2E)).fillMaxSize(),
            lineHeight = 24.sp,
        )
    } else {
        val state by playerPort.playbackState.collectAsState()
        val config by playerPort.playerConfig.collectAsState()
        val error by playerPort.playbackError.collectAsState()
        Text(
            text = buildString {
                appendLine("Muses Desktop (S3a 占位)")
                appendLine("播放状态: $state")
                appendLine("循环: ${config.repeatMode.name}, 随机: ${config.shuffleEnabled}")
                error?.let { appendLine("错误: $it") }
            },
            color = Color(0xFFCDD6F4),
            fontSize = 16.sp,
            modifier = Modifier.background(Color(0xFF1E1E2E)).fillMaxSize(),
            lineHeight = 24.sp,
        )
    }
}

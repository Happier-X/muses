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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import com.muses.player.core.ui.theme.SaltTheme
import com.muses.player.desktop.playback.DesktopPlayerHook

/**
 * 桌面主界面（S3b）：标题栏 + 平板双栏（侧边导航 + 内容区）。
 * 桌面宽度默认 1280dp，天然落在平板断点（>=768dp）之上。
 *
 * U5 曲目列表共用化：包裹 SaltTheme 使共享组件可正确取色。
 */
@Composable
fun MusesDesktopApp(
    windowState: WindowState,
    onClose: () -> Unit,
    viewModel: DesktopViewModel = remember { DesktopViewModel() },
    playerHook: DesktopPlayerHook? = null,
) {
    val destination by viewModel.destination.collectAsState()

    SaltTheme {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF11111B))) {
            DesktopTitleBar(
                windowState = windowState,
                onClose = onClose,
            )
            Row(modifier = Modifier.fillMaxSize()) {
                // 侧边导航（TabletLayout 双栏：260px 侧边栏）
                DesktopSidebar(
                    current = destination,
                    onNavigate = viewModel::navigate,
                )
                // 内容区
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (destination) {
                        DesktopDestination.LIBRARY -> LibraryScreen(playerHook = playerHook)
                        DesktopDestination.PLAYER -> PlayerScreen(playerHook = playerHook)
                        DesktopDestination.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSidebar(
    current: DesktopDestination,
    onNavigate: (DesktopDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(Color(0xFF181825))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Muses",
            color = Color(0xFFCDD6F4),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
        SidebarItem(
            label = "曲库",
            selected = current == DesktopDestination.LIBRARY,
            onClick = { onNavigate(DesktopDestination.LIBRARY) },
        )
        SidebarItem(
            label = "播放",
            selected = current == DesktopDestination.PLAYER,
            onClick = { onNavigate(DesktopDestination.PLAYER) },
        )
        SidebarItem(
            label = "设置",
            selected = current == DesktopDestination.SETTINGS,
            onClick = { onNavigate(DesktopDestination.SETTINGS) },
        )
    }
}

@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF313244) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF89B4FA) else Color(0xFFBAC2DE),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

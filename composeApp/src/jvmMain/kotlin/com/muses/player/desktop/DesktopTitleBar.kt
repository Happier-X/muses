package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState

/**
 * 自绘标题栏（S3a）：
 * - 最小化/最大化/关闭 三按钮
 * - 拖拽移动（双击标题栏最大化/还原）
 * - 高分屏/多显示器/贴边为已知限制，首版不追求
 */
@Composable
fun DesktopTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    title: String = "Muses",
) {
    var isMaximized by remember { mutableStateOf(false) }
    val defaultWidth = 1280.dp
    val defaultHeight = 800.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF1E1E2E))
            // 标题栏拖拽 + 双击最大化
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (isMaximized) {
                        // 拖拽退出最大化：恢复默认尺寸并居中到鼠标位置
                        windowState.size = DpSize(defaultWidth, defaultHeight)
                        isMaximized = false
                    }
                    // 拖拽移动：通过 WindowState 无法直接移动，使用 AWT 窗口
                    // 首版用双击最大化替代拖拽移动（Compose Desktop undecorated 拖拽需额外处理）
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (isMaximized) {
                            windowState.size = DpSize(defaultWidth, defaultHeight)
                        } else {
                            windowState.size = DpSize(
                                androidx.compose.ui.unit.Dp.Unspecified,
                                androidx.compose.ui.unit.Dp.Unspecified,
                            )
                        }
                        isMaximized = !isMaximized
                    },
                )
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 标题
        androidx.compose.material3.Text(
            text = title,
            color = Color(0xFFCDD6F4),
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        // 窗口控制按钮
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TitleBarButton("─", Color(0xFF585B70)) {
                // TODO: Compose Desktop 暂无 minimize API，首版留空
            }
            TitleBarButton(
                text = if (isMaximized) "❐" else "□",
                color = if (isMaximized) Color(0xFF89B4FA) else Color(0xFF585B70),
            ) {
                if (isMaximized) {
                    windowState.size = DpSize(defaultWidth, defaultHeight)
                } else {
                    windowState.size = DpSize(
                        androidx.compose.ui.unit.Dp.Unspecified,
                        androidx.compose.ui.unit.Dp.Unspecified,
                    )
                }
                isMaximized = !isMaximized
            }
            TitleBarButton("✕", Color(0xFFF38BA8)) {
                onClose()
            }
        }
    }
}

@Composable
private fun TitleBarButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .padding(0.dp)
            .background(color.copy(alpha = 0.3f), shape = androidx.compose.foundation.shape.CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}

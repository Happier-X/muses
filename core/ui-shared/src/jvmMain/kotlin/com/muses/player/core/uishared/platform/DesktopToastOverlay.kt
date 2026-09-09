package com.muses.player.core.uishared.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 桌面 Toast 浮层（U2）：消费 [PlatformToast] 消息总线，底部居中短提示，
 * 2.2s 自动消退。对照安卓 Toast.LENGTH_SHORT 的停留节奏；样式走 miuix 官方
 * 默认颜色 token（surfaceVariant 胶囊 + onSurface 文本，明暗随主题）。
 *
 * 覆盖层不参与点击命中（无 pointerInput），浮层显示期间交互照常穿透。
 * 由桌面壳在内容顶层挂载一次：`DesktopToastOverlay()`。
 */
@Composable
fun DesktopToastOverlay(modifier: Modifier = Modifier) {
    var visibleText by remember { mutableStateOf<String?>(null) }
    // 消退令牌：每次新消息递增，旧延迟任务过期后不再清屏，避免新消息被吞
    var dismissToken by remember { mutableIntStateOf(0) }

    // 挂载前攒的消息兜底：首帧消费一次最近一条（壳层晚挂载不丢提示）
    val pendingMessage by desktopToastMessage.collectAsState()
    LaunchedEffect(pendingMessage) {
        if (visibleText == null && pendingMessage != null) {
            visibleText = pendingMessage
            desktopToastMessage.value = null
            val token = ++dismissToken
            delay(2_200)
            if (dismissToken == token) visibleText = null
        }
    }

    // 事件总线：SharedFlow 无去重，同文案连续触发也每次都显示
    LaunchedEffect(Unit) {
        desktopToastEvents.collect { message ->
            visibleText = message
            desktopToastMessage.value = null
            val token = ++dismissToken
            delay(2_200)
            if (dismissToken == token) visibleText = null
        }
    }

    visibleText?.let { text ->
        val scheme = MiuixTheme.colorScheme
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = text,
                    color = scheme.onSurface,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

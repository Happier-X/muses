package com.muses.player.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部悬浮件避让（不透明实色盖在内容之上，列表滚到底时末项需停在其上）。
 *
 * 高度口径（测量值，与位移/动画无关）：
 * - 迷你条 80dp = 高 64 + 上下边距 8×2；
 * - 悬浮底栏 88dp = minHeight 52 + 底部留白 36（整体位移只改变绘制不改变测量）；
 * - 呼吸感 16dp。
 *
 * 窄屏两件套全在 → 184dp；宽屏只有迷你条（Rail 在侧边）→ 96dp。
 * 主框架按断点提供，列表页经 [LocalBottomChromePadding] 消费，加到各自滚动容器底部（末项避让；空态/短内容不留白）。
 */
val PhoneBottomChromePadding: Dp = 184.dp
val TabletBottomChromePadding: Dp = 96.dp

val LocalBottomChromePadding = compositionLocalOf { PhoneBottomChromePadding }

package com.muses.player.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部悬浮件避让（不透明实色盖在内容之上，列表滚到底时末项需停在其上）。
 *
 * 高度口径（测量值，与位移/动画无关）：
 * - 迷你条 80dp = 高 64 + 上下边距 8×2；
 * - 呼吸感 16dp。
 *
 * 窄屏已改为**侧滑抽屉**导航（原悬浮底栏下线）：两屏都只有迷你条 → 均为 96dp。
 * 若将来窄屏重新加入底部 chrome，需同步上调 [PhoneBottomChromePadding]，
 * 否则列表末项会被新 chrome 盖住。
 * 主框架按断点提供，列表页经 [LocalBottomChromePadding] 消费，加到各自滚动容器底部（末项避让；空态/短内容不留白）。
 */
val PhoneBottomChromePadding: Dp = 96.dp
val TabletBottomChromePadding: Dp = 96.dp

val LocalBottomChromePadding = compositionLocalOf { PhoneBottomChromePadding }

package com.muses.player.feature.shell.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 底部胶囊导航条的智能系统避让：
 * - 三键/双键导航（含读取失败的安全兜底）→ 完整 navigationBars inset，避让系统按钮
 * - 手势导航 → 压缩到 8dp 小边距（全面屏手势区无需整条避让，MuMu 等模拟器 inset 虚高时尤其明显）
 * - 桌面端 → 无系统导航条，零内边距
 */
@Composable
expect fun Modifier.smartBottomBarInsetsPadding(): Modifier

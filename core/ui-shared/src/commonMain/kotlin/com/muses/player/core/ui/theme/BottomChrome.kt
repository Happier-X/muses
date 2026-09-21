package com.muses.player.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部悬浮件避让（不透明实色盖在内容之上，列表滚到底时末项需停在其上）。
 *
 * 高度口径（测量值，不含呼吸感——消费方一般自行 +16dp）：
 * - 迷你条 56dp + 上下边距 8×2 = 72dp（对齐 Halcyon 后的尺寸）；
 * - 悬浮底栏 56dp（自研 MusesBottomDock，图标 + 文字；与迷你条等高）。
 *
 * 窄屏两件套全在 → 128dp（净高）；宽屏只有迷你条（Rail 在侧边）→ 80dp。
 * 系统导航栏/手势条 inset 由壳层统一叠加（bottomBar 用 navigationBarsPadding + 8dp 保底抬升），
 * 见 TabsLayout 的 `chromeBottomInset`。
 *
 * 避让值**恒定**（不随滚动融合插值）——一旦随融合变化，就会出现「避让变小后列表滚动位置不跟着变
 * → 展开回去时底部内容被多出的 chrome 盖住且已到底滚不出来」的死角（对齐 Halcyon：其内容页写死
 * navInset + 116/120/130/160dp，完全不感知 dock 融合态）。
 */
val PhoneBottomChromePadding: Dp = 128.dp
val TabletBottomChromePadding: Dp = 80.dp

val LocalBottomChromePadding = compositionLocalOf { PhoneBottomChromePadding }

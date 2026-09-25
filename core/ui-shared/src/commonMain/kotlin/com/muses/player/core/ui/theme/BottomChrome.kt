package com.muses.player.core.ui.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部悬浮件避让（不透明实色盖在内容之上，列表滚到底时末项需停在其上）。
 *
 * 高度口径（测量值，不含呼吸感——消费方一般自行 +16dp）：
 * - 迷你条 56dp + 上下边距 8×2 = 72dp（对齐 Halcyon 后的尺寸）；
 * - 悬浮底栏 56dp（自研 MusesBottomDock，图标 + 文字；与迷你条等高）。
 *
 * 手机和平板都显示两件套 → 128dp（净高）。
 * 系统导航栏/手势条 inset 由壳层统一叠加（bottomBar 用 navigationBarsPadding + 8dp 保底抬升），
 * 见 TabsLayout 的 `chromeBottomInset`。
 *
 * 避让值**恒定**（不随滚动融合插值）——一旦随融合变化，就会出现「避让变小后列表滚动位置不跟着变
 * → 展开回去时底部内容被多出的 chrome 盖住且已到底滚不出来」的死角（对齐 Halcyon：其内容页写死
 * navInset + 116/120/130/160dp，完全不感知 dock 融合态）。
 */
val PhoneBottomChromePadding: Dp = 128.dp

val LocalBottomChromePadding = compositionLocalOf { PhoneBottomChromePadding }

/**
 * 底部 chrome 可见顶的**窗口坐标**（从窗口顶量到迷你条可见顶），由壳层从迷你条窗口矩形
 *（miniBarBounds，融合/展开两态都上报，且两态它都是最靠上的可见件）逐帧上报（见 MusesApp）。
 *
 * 用窗口坐标而非「距屏底高度」：FAB slot（miuix Scaffold）自带内缩，实测底/右各缩 12dp，
 * 且该 inset 与 WindowInsets.navigationBars 无关（MuMu 手势导航下 nav=0）静态拿不到；
 * FAB 侧用同一窗口坐标系实测自己 slot 的底，两者相减即可，与 inset 来源无关。
 * 也不能用 bottomBar 节点总高：节点含透明 padding（实测比可见胶囊顶高 20dp），
 * FAB 会跟着悬空。
 *
 * 仅供悬浮 FAB 定位（歌曲页「跳转当前曲」）：展开 / 滚动融合两态都贴着**可见** chrome
 * 顶 + 16dp 呼吸（对齐 Halcyon：FAB bottom=176dp ≈ 其展开态 chrome 顶 + 少量呼吸）。
 * 列表 contentPadding 避让**不得**用它——仍用恒定的 [LocalBottomChromePadding]（防滚动死角）。
 *
 * 以 [State] 形态下发：壳层逐帧写入时只有真正读 `.value` 的 FAB 会重组。
 */
val LocalBottomChromeElevation = compositionLocalOf<State<Dp>> { mutableStateOf(PhoneBottomChromePadding) }

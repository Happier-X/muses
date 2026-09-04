package com.muses.player.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * 底部 MiniPlayer / 顶部导航 的真磨砂（Haze）上下文。
 *
 * - 由 [com.muses.player.navigation.TabsLayout] 在根部 `rememberHazeState()` 并通过
 *   CompositionLocal 透传，`hazeSource` 打在内容区，`hazeEffect` 打在悬浮玻璃上。
 * - 预览或无 Haze 环境下为 null，玻璃回退为半透明 [SaltColors.glassBg]。
 *
 * 注意：Haze 风格函数（musesNavbarHazeStyle / musesBottomBarHazeStyle）
 * 已上收至 ui-shared（返回 HazeBlurStyleData），本文件仅保留 CompositionLocal。
 */
val LocalMusesHazeState = compositionLocalOf<HazeState?> { null }

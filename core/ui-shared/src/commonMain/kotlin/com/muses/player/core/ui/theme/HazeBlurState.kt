package com.muses.player.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Haze 模糊状态供给（U3 T1 跨平台桥接）。
 *
 * - ui-shared commonMain 无法引用 `dev.chrisbanes.haze.HazeState`（Android-only），
 *   因此通过 `Any?` 类型擦除传递；Android actual 内部转为 `HazeState` 消费。
 * - app 层（TabsLayout / SettingsScreen）在 `CompositionLocalProvider` 中同时提供
 *   `LocalMusesHazeState`（feature:/app 消费）和 `LocalHazeBlurState`（ui-shared 消费），
 *   值相同，仅类型不同。
 * - 桌面端始终为 null（无真模糊，走纯色降级）。
 */
val LocalHazeBlurState = staticCompositionLocalOf<Any?> { null }

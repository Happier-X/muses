package com.muses.player.core.uishared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.muses.player.core.ui.theme.HazeBlurStyleData

/**
 * 平台模糊 Modifier 供给（U3 T1）。
 *
 * commonMain 声明语义：为导航栏 / 底部迷你条提供背景模糊效果。
 * - 安卓侧：Haze 真模糊（Modifier.hazeBlur）
 * - 桌面侧：纯色降级（Modifier.background）
 *
 * [hazeState] 在 commonMain 中为 `Any?`（类型擦除），Android actual 内部
 * 转为 `dev.chrisbanes.haze.HazeState` 消费；桌面端忽略。
 */
@Composable
@ReadOnlyComposable
expect fun platformBlurModifier(
    isDark: Boolean,
    backgroundColor: Color,
    hazeState: Any?,
    hazeStyleData: HazeBlurStyleData?,
): Modifier

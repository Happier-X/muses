package com.muses.player.core.uishared.platform

/**
 * U2 androidMain 真实现：安卓侧提供真模糊能力（Haze 依赖已声明在 androidMain）。
 *
 * - [enabled] = true，组件层（U3 T1）据此决定是否应用 Haze `Modifier.hazeBlur()`；
 * - [radiusDp] = 20dp，对齐 Web `backdrop-filter: blur(20px)`；
 * - Haze 的 HazeState / HazeBlurStyle 由组件层在 Composable 中创建与消费，
 *   本对象仅暴露元数据（commonMain 零安卓 import 约束）。
 */
actual object PlatformBlur {
    actual val enabled: Boolean = true
    actual val radiusDp: Float = 20f
}

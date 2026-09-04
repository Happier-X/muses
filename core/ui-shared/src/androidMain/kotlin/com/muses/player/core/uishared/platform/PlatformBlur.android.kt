package com.muses.player.core.uishared.platform

/**
 * U0 androidMain 最小实现：安卓侧提供真模糊（Haze 接线在 U3 T1 落地，
 * 此处只声明能力位，不引 Haze 依赖）。
 */
actual object PlatformBlur {
    actual val enabled: Boolean = true
    actual val radiusDp: Float = 20f
}

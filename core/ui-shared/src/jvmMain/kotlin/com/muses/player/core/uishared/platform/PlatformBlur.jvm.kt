package com.muses.player.core.uishared.platform

/** U0 jvmMain（桌面）占位实现：无真模糊，调用方按 enabled=false 走纯色降级。 */
actual object PlatformBlur {
    actual val enabled: Boolean = false
    actual val radiusDp: Float = 0f
}

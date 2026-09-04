package com.muses.player.core.uishared.platform

/**
 * U2 jvmMain（桌面）实现：无真模糊，调用方按 [enabled]=false 走纯色降级。
 *
 * - 桌面端不引入 Haze 依赖，模糊由 SaltColors.glassBg 半透明背景替代；
 * - 组件层检查 [enabled] 后走 `Modifier.background(salt.glassBg)` 降级分支。
 */
actual object PlatformBlur {
    actual val enabled: Boolean = false
    actual val radiusDp: Float = 0f
}

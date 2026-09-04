package com.muses.player.core.uishared.platform

/**
 * U0 jvmMain（桌面）占位实现：边衬给 0，模糊降级关闭，Toast 静默丢弃。
 * 真实现 U2 落地（标题栏高/桌面浮层），供 composeApp/desktop 消费。
 */
actual object PlatformInsets {
    actual fun statusBarHeightDp(): Float = 0f

    actual fun navigationBarHeightDp(): Float = 0f
}

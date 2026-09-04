package com.muses.player.core.uishared.platform

/**
 * U2 jvmMain（桌面）实现：静默丢弃。
 *
 * - 桌面端无系统 Toast 通道；后续可接入状态栏文案或小浮层（composeApp 层）；
 * - 当前保持 no-op，不影响功能正确性。
 */
actual object PlatformToast {
    actual fun show(message: String) {
        // U2 桌面实现：静默丢弃，无系统 Toast 通道
    }
}

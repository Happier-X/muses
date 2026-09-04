package com.muses.player.core.uishared.platform

/** U0 jvmMain（桌面）占位实现：静默丢弃，真实现（状态栏文案/小浮层）U2 落地。 */
actual object PlatformToast {
    actual fun show(message: String) {
        // U0 占位：桌面提示通道 U2 接入前静默丢弃，避免引入桌面 UI 依赖
    }
}

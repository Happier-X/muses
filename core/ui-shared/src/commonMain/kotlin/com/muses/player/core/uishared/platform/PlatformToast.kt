package com.muses.player.core.uishared.platform

/**
 * 平台短提示供给（设计 §3）。
 *
 * commonMain 只声明语义：一条短提示文案；安卓侧走 Toast，
 * 桌面侧静默丢弃（后续可接状态栏文案/小浮层）。
 */
expect object PlatformToast {
    /** 显示一条短提示 */
    fun show(message: String)
}

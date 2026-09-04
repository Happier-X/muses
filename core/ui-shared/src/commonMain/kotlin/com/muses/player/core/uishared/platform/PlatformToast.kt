package com.muses.player.core.uishared.platform

/**
 * 平台短提示供给（设计 §3）。
 *
 * commonMain 只声明语义：一条短提示文案；安卓侧走 Toast，
 * 桌面侧走状态栏文案/小浮层（U2 落地）。
 * U0 仅做空定义保证编译过。文件选择（PlatformFilePicker）设置页暂不用，U2 预留。
 */
expect object PlatformToast {
    /** 显示一条短提示 */
    fun show(message: String)
}

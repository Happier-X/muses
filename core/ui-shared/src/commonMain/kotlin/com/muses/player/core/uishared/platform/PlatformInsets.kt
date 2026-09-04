package com.muses.player.core.uishared.platform

/**
 * 平台边衬供给（设计 §3）。
 *
 * commonMain 只声明语义：状态栏/导航栏高度（dp 数值，不含 Compose 运行时）；
 * 安卓侧 U2 接 WindowInsets 真实值，桌面侧给 0/标题栏高。
 * U0 仅做空定义保证编译过，真实现 U2 落地。
 */
expect object PlatformInsets {
    /** 状态栏高度（dp） */
    fun statusBarHeightDp(): Float

    /** 导航栏高度（dp） */
    fun navigationBarHeightDp(): Float
}

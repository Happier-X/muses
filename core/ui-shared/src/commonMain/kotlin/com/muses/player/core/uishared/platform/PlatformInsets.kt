package com.muses.player.core.uishared.platform

/**
 * 平台边衬供给（设计 §3）。
 *
 * commonMain 只声明语义：状态栏/导航栏高度（dp 数值，不含 Compose 运行时）；
 * 安卓侧通过 [com.muses.player.core.uishared.platform.PlatformInsets.init] 注入 Activity 读取真实值，
 * 桌面侧给 0（标题栏高度由 composeApp 自行处理）。
 */
expect object PlatformInsets {
    /** 状态栏高度（dp） */
    fun statusBarHeightDp(): Float

    /** 导航栏高度（dp） */
    fun navigationBarHeightDp(): Float
}

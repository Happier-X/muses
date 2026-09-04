package com.muses.player.core.uishared.platform

/**
 * U2 jvmMain（桌面）实现：边衬给 0，标题栏高度由 composeApp 自行处理。
 *
 * - 桌面窗口无系统状态栏/导航栏概念，标题栏由 composeApp 自绘；
 * - 组件层按此值 0 不做额外顶部/底部避让，由页面布局自行安排。
 */
actual object PlatformInsets {
    actual fun statusBarHeightDp(): Float = 0f

    actual fun navigationBarHeightDp(): Float = 0f
}

package com.muses.player.core.uishared.platform

/**
 * U0 androidMain 最小实现：默认值占位，保证编译过。
 * 真实现 U2 落地（读 WindowInsets 真实值）；调用方 U2 前不得依赖具体数值。
 */
actual object PlatformInsets {
    @Volatile
    private var statusBarDp: Float = 0f

    @Volatile
    private var navigationBarDp: Float = 0f

    /** U2 前的手动注入点；重复传入不同值抛错防配错（与 PlatformDirs 口径一致）。 */
    fun init(statusBar: Float, navigationBar: Float) {
        val curStatus = statusBarDp
        val curNav = navigationBarDp
        if (curStatus == 0f && curNav == 0f) {
            statusBarDp = statusBar
            navigationBarDp = navigationBar
        } else {
            check(curStatus == statusBar && curNav == navigationBar) {
                "PlatformInsets 重复初始化且数值不一致"
            }
        }
    }

    actual fun statusBarHeightDp(): Float = statusBarDp

    actual fun navigationBarHeightDp(): Float = navigationBarDp
}

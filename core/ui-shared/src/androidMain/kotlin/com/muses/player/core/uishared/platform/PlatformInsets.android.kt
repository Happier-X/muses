package com.muses.player.core.uishared.platform

import android.app.Activity
import android.content.res.Resources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * U2 androidMain 真实现：通过 [init] 注入 [Activity]，
 * 读取 WindowInsets 真实状态栏/导航栏高度（px → dp），供组件层消费。
 *
 * - 调用方在 Application.onCreate 或首个 Activity.onCreate 处调一次 [init]；
 * - [init] 仅首个值生效；重复传入相同 Activity 幂等，不同实例抛错防配错。
 */
actual object PlatformInsets {

    @Volatile
    private var statusBarDp: Float = 0f

    @Volatile
    private var navigationBarDp: Float = 0f

    /**
     * 从 Activity 窗口读取真实系统边衬高度（dp）。
     *
     * 底层调用 [ViewCompat.getRootWindowInsets]，首次生效；
     * 后续传入不同 Activity 实例抛错（与 PlatformDirs.initPlatformDirs 口径一致）。
     */
    fun init(activity: Activity) {
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView) ?: return
        val density = activity.resources.displayMetrics.density
        val statusBarPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val navigationBarPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        init(statusBarPx / density, navigationBarPx / density)
    }

    /**
     * 手动注入 dp 值（U0 兼容路径；测试或特殊场景可用）。
     * 重复传入不同值抛错。
     */
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

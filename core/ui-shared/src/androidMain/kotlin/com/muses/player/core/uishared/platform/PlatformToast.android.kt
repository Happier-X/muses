package com.muses.player.core.uishared.platform

import android.content.Context
import android.widget.Toast

/**
 * U0 androidMain 最小实现：Toast 需应用上下文，调用方在 Application.onCreate
 * 处调一次 [init]（与 PlatformDirs.initPlatformDirs 口径一致）；未初始化时静默丢弃，
 * U2 补齐桌面浮层与统一调用点。
 */
actual object PlatformToast {

    @Volatile
    private var appContext: Context? = null

    /** 幂等初始化：首个值生效；重复传入不同实例抛错。 */
    fun init(context: Context) {
        val app = context.applicationContext
        val cur = appContext
        if (cur == null) {
            appContext = app
        } else {
            check(cur === app) { "PlatformToast 重复初始化且上下文不一致" }
        }
    }

    actual fun show(message: String) {
        val ctx = appContext ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }
}

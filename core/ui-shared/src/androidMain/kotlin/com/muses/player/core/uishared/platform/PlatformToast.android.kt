package com.muses.player.core.uishared.platform

import android.content.Context
import android.widget.Toast

/**
 * U2 androidMain 真实现：通过 [init] 注入应用 [Context]，使用系统 Toast 显示短提示。
 *
 * - 调用方在 Application.onCreate 处调一次 [init]；
 * - [init] 幂等：首个值生效；重复传入不同实例抛错；
 * - 未初始化时 [show] 静默丢弃，不抛异常。
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

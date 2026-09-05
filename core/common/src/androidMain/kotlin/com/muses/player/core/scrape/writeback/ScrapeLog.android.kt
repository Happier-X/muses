package com.muses.player.core.scrape.writeback

/** 安卓 actual：android.util.Log（JVM 单测无桩时回退 println，原 safeLog 语义） */
internal actual fun safeLogW(tag: String, msg: String) {
    try {
        android.util.Log.w(tag, msg)
    } catch (_: Throwable) {
        println("[$tag] $msg")
    }
}

internal actual fun safeLogE(tag: String, msg: String, throwable: Throwable?) {
    try {
        android.util.Log.e(tag, msg, throwable)
    } catch (_: Throwable) {
        println("[$tag] $msg ${throwable?.message}")
        throwable?.printStackTrace()
    }
}

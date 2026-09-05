package com.muses.player.core.scrape.writeback

/** 桌面 actual：无 Android Log，直接 stdout */
internal actual fun safeLogW(tag: String, msg: String) {
    println("[$tag] $msg")
}

internal actual fun safeLogE(tag: String, msg: String, throwable: Throwable?) {
    println("[$tag] $msg ${throwable?.message}")
    throwable?.printStackTrace()
}

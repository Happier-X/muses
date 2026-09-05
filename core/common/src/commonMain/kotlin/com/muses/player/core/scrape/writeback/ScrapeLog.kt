package com.muses.player.core.scrape.writeback

/**
 * 写回链日志安全封装（W3 上收 commonMain，原 SongFileWriters.kt 内联实现 expect/actual 化）：
 * 单元测试/桌面端无 Android Log 桩时回退 println，避免 Stub! 异常（spec 踩坑记录）。
 */
internal expect fun safeLogW(tag: String, msg: String)

internal expect fun safeLogE(tag: String, msg: String, throwable: Throwable?)

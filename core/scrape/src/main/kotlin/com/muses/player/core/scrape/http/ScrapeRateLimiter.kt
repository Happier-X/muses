package com.muses.player.core.scrape.http

/**
 * 刮削限流器别名：08-27-webdav-playback-429 起提升至 core:webdav 共享单例。
 *
 * 原实现已迁移至 [com.muses.player.core.webdav.WebDavRateLimiter]，
 * 本别名保留以兼容存量 import 与单测，编译期等价于共享实现。
 */
typealias ScrapeRateLimiter = com.muses.player.core.webdav.WebDavRateLimiter

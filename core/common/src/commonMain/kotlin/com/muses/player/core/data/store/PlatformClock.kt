package com.muses.player.core.data.store

/**
 * P2b-S2 平台时钟（ScrapeQueueStore/ScrapeHistoryStore 的 nowIso 默认值供给）。
 * commonMain 无 java.time；androidMain/jvmMain 均有 JDK，用真实实现。
 */
expect fun platformNowIso(): String

/**
 * P2c 毫秒时钟（ScrapeHttp/WebDavClient 的 parseRetryAfterMs HTTP-date 差值 + 限流器默认时钟）。
 * commonMain 无 System.currentTimeMillis；双端 actual 均为 JDK 真实时间。
 */
expect fun platformNowMs(): Long

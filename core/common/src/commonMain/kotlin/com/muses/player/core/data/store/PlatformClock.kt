package com.muses.player.core.data.store

/**
 * P2b-S2 平台时钟（ScrapeQueueStore/ScrapeHistoryStore 的 nowIso 默认值供给）。
 * commonMain 无 java.time；androidMain/jvmMain 均有 JDK，用真实实现。
 */
expect fun platformNowIso(): String

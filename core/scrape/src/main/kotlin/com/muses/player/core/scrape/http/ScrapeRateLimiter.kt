package com.muses.player.core.scrape.http

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 刮削全局限流器（漏桶/令牌桶简化版）。
 *
 * - 默认 4 rps（约 250ms 间隔），跨文本/封面/封面字节获取共享；
 * - [acquire] 在请求入口调用，使用 [delay] 非阻塞协程；
 * - 线程安全：[Mutex] 保证并发下串行更新 [nextAvailableMs]；
 * - 测试可构造自定义间隔或使用 [Unlimited] 跳过限流。
 */
class ScrapeRateLimiter(
    /** 许可间隔毫秒，默认 250ms 即 4 rps。 */
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {

    private val mutex = Mutex()
    private var nextAvailableMs: Long = 0L

    /**
     * 获取一次许可。
     *
     * 若距离上次许可不足 [intervalMs]，则挂起等待。
     * 首个许可立即放行（[nextAvailableMs] 初始 0）。
     */
    suspend fun acquire() {
        if (intervalMs <= 0L) return
        val delayMs = mutex.withLock {
            val now = nowMs()
            if (now >= nextAvailableMs) {
                nextAvailableMs = now + intervalMs
                0L
            } else {
                val wait = nextAvailableMs - now
                nextAvailableMs += intervalMs
                wait
            }
        }
        if (delayMs > 0L) {
            delay(delayMs)
        }
    }

    companion object {
        /** 默认 4 rps 对应间隔。 */
        const val DEFAULT_INTERVAL_MS: Long = 250L

        /** 默认 4 rps 实例（与 Module 单例等效）。 */
        fun default(): ScrapeRateLimiter = ScrapeRateLimiter(DEFAULT_INTERVAL_MS)

        /** 测试用：不限流实例。 */
        val Unlimited: ScrapeRateLimiter = ScrapeRateLimiter(intervalMs = 0L)
    }
}

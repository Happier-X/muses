package com.muses.player.core.scrape.http

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 限流器节流单测（任务 08-27-scrape-throttle-429）。
 */
class ScrapeRateLimiterTest {

    @Test
    fun `默认4rps约250ms间隔_连续acquire产生延迟`() = runTest {
        val limiter = ScrapeRateLimiter(intervalMs = 100L, nowMs = { currentTime })
        limiter.acquire() // 首次立即：虚拟时间仍为 0
        assertEquals(0L, currentTime)

        limiter.acquire() // 二次应延迟约 100ms（虚拟时间推进）
        assertEquals(100L, currentTime)
        limiter.acquire() // 三次再 100ms
        assertEquals(200L, currentTime)
    }

    @Test
    fun `Unlimited不限流_连续acquire无延迟`() = runTest {
        val limiter = ScrapeRateLimiter(intervalMs = 0L, nowMs = { currentTime })
        repeat(10) { limiter.acquire() }
        assertEquals(0L, currentTime)
        // 同时验证预置 Unlimited 单例（真实时钟，间隔 0）也不延迟
        repeat(10) { ScrapeRateLimiter.Unlimited.acquire() }
        assertEquals(0L, currentTime)
    }

    @Test
    fun `自定义间隔_限流生效`() = runTest {
        val limiter = ScrapeRateLimiter(intervalMs = 250L, nowMs = { currentTime })
        limiter.acquire()
        assertEquals(0L, currentTime)
        limiter.acquire()
        assertEquals(250L, currentTime)
        limiter.acquire()
        assertEquals(500L, currentTime)
    }
}

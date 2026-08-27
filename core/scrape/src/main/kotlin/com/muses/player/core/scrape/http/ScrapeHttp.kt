package com.muses.player.core.scrape.http

import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.min

/**
 * 刮削引擎共享 HTTP GET 工具。
 *
 * 规格书 = src/features/cover/http.ts 的 httpGetText / httpGetJson：
 * - 非 2xx 直接抛错（错误消息 `http <status>`），不重试、不回退；
 * - JSON 解析失败向上抛出（kw provider 自行捕获返回 null 的行为在 provider 内对齐）。
 *
 * 限流与退避（任务 08-27-scrape-throttle-429）：
 * - 全局 [ScrapeRateLimiter]（默认 4 rps）在每次请求前 [ScrapeRateLimiter.acquire]；
 * - 命中 429 时解析 Retry-After（秒数或 HTTP-date），delay(min(computed, 8s)) 后重试 1 次；
 * - 二次 429 抛 IOException("http 429") 交上层归为 NETWORK。
 */
class ScrapeHttp(
    private val client: OkHttpClient = OkHttpClient(),
    private val rateLimiter: ScrapeRateLimiter = ScrapeRateLimiter(),
) {

    /** 跨端文本 GET：非 2xx 抛 IOException("http <status>")，429 自动退避重试 1 次 */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        executeWithRetry(url, headers) { res -> res.body?.string() ?: "" }

    /** 文本 GET + JSON 解析；解析失败抛序列化异常（与 Web JSON.parse 抛错一致） */
    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement {
        val text = getText(url, headers)
        // 响应结构不稳定，调用方用 JsonElement 松散解析，不做强类型映射
        return Json.parseToJsonElement(text)
    }

    /** 二进制 GET（远程封面字节）；非 2xx 抛 IOException，429 同 getText 退避重试 1 次 */
    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
        executeWithRetry(url, headers) { res -> res.body?.bytes() ?: ByteArray(0) }

    /** 共享的 429 感知重试骨架：限流 acquire + Retry-After 退避 + 最多 1 次重试。 */
    private suspend fun <T> executeWithRetry(
        url: String,
        headers: Map<String, String>,
        onSuccess: (okhttp3.Response) -> T,
    ): T {
        try {
            rateLimiter.acquire()
        } catch (e: CancellationException) {
            throw e
        }
        var attempt = 0
        while (true) {
            val request = buildRequest(url, headers)
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (response.code != 429) {
                response.use { res ->
                    if (!res.isSuccessful) {
                        throw IOException("http ${res.code}")
                    }
                    return onSuccess(res)
                }
            }
            val retryAfterMs = parseRetryAfterMs(response.header("Retry-After"))
            response.close()
            if (attempt >= 1) {
                throw IOException("http 429")
            }
            val delayMs = retryAfterMs?.let { min(it, MAX_RETRY_AFTER_MS) } ?: DEFAULT_429_DELAY_MS
            val clamped = delayMs.coerceIn(0L, MAX_RETRY_AFTER_MS)
            try {
                delay(clamped)
            } catch (e: CancellationException) {
                throw e
            }
            try {
                rateLimiter.acquire()
            } catch (e: CancellationException) {
                throw e
            }
            attempt++
        }
    }

    private fun buildRequest(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        for ((name, value) in headers) {
            builder.header(name, value)
        }
        return builder.build()
    }

    companion object {
        /** 二次 429 退避上限 8s（design.md）。 */
        const val MAX_RETRY_AFTER_MS: Long = 8000L

        /** 无 Retry-After 时的默认退避 1s。 */
        const val DEFAULT_429_DELAY_MS: Long = 1000L

        /**
         * 解析 Retry-After 头（秒数或 HTTP-date）。
         * - 秒数：直接 *1000；
         * - HTTP-date：按 RFC1123 解析与当前时间差值；
         * - 解析失败返回 null 由调用方回退默认延迟。
         */
        internal fun parseRetryAfterMs(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim()
            // 1) 尝试秒数（整数）
            trimmed.toLongOrNull()?.let { sec ->
                return (sec * 1000L).coerceAtLeast(0L)
            }
            // 2) 尝试 HTTP-date（RFC1123，如 Wed, 21 Oct 2015 07:28:00 GMT）
            return try {
                val formatter = DateTimeFormatter.RFC_1123_DATE_TIME
                val dateTime = ZonedDateTime.parse(trimmed, formatter)
                val diff = dateTime.toInstant().toEpochMilli() - System.currentTimeMillis()
                diff.coerceAtLeast(0L)
            } catch (_: DateTimeParseException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
}

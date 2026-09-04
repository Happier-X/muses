package com.muses.player.core.scrape.http

import com.muses.player.core.data.store.platformNowMs
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.math.min

/**
 * 刮削引擎共享 HTTP GET 工具（P2c：OkHttp → Ktor-client CIO，搬入 commonMain）。
 *
 * 规格书 = src/features/cover/http.ts 的 httpGetText / httpGetJson：
 * - 非 2xx 直接抛错（错误消息 `http <status>`），不重试、不回退；
 * - JSON 解析失败向上抛出（kw provider 自行捕获返回 null 的行为在 provider 内对齐）。
 *
 * 限流与退避（任务 08-27-scrape-throttle-429，铁律冻结）：
 * - 全局 [WebDavRateLimiter]（默认 4 rps）在每次请求前 acquire；
 * - 命中 429 时解析 Retry-After（秒数或 HTTP-date），delay(min(computed, 8s)) 后重试 1 次；
 * - 二次 429 抛 IOException("http 429") 交上层归为 NETWORK。
 *
 * P2c 变更点（仅传输层，骨架逐行平移）：
 * - `parseRetryAfterMs` 的 java.time 改 `nowMs` 注入（commonMain 无 java.time）；
 * - `java.io.IOException` → `kotlinx.io.IOException`（provider 全为泛 catch，安全）。
 */
class ScrapeHttp(
    private val client: HttpClient = defaultScrapeHttpClient(),
    private val rateLimiter: WebDavRateLimiter = WebDavRateLimiter(),
    private val nowMs: () -> Long = { platformNowMs() },
) {

    /** 跨端文本 GET：非 2xx 抛 IOException("http <status>")，429 自动退避重试 1 次 */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        executeWithRetry(url, headers) { bodyAsText() }

    /** 文本 GET + JSON 解析；解析失败抛序列化异常（与 Web JSON.parse 抛错一致） */
    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement {
        val text = getText(url, headers)
        // 响应结构不稳定，调用方用 JsonElement 松散解析，不做强类型映射
        return Json.parseToJsonElement(text)
    }

    /** 二进制 GET（远程封面字节）；非 2xx 抛 IOException，429 同 getText 退避重试 1 次 */
    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
        executeWithRetry(url, headers) { readRawBytes() }

    /** 共享的 429 感知重试骨架：限流 acquire + Retry-After 退避 + 最多 1 次重试。 */
    private suspend fun <T> executeWithRetry(
        url: String,
        headers: Map<String, String>,
        onSuccess: suspend io.ktor.client.statement.HttpResponse.() -> T,
    ): T {
        try {
            rateLimiter.acquire()
        } catch (e: CancellationException) {
            throw e
        }
        var attempt = 0
        while (true) {
            val response = client.get(url) {
                headers.forEach { (name, value) -> header(name, value) }
            }
            if (response.status.value != 429) {
                if (!response.status.isSuccess()) {
                    throw kotlinx.io.IOException("http ${response.status.value}")
                }
                return response.onSuccess()
            }
            val retryAfterMs = parseRetryAfterMs(response.headers["Retry-After"], nowMs)
            // 429 空体消费后关闭复用连接（MockEngine/真实引擎均安全）
            runCatching { response.bodyAsText() }
            if (attempt >= 1) {
                throw kotlinx.io.IOException("http 429")
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

    companion object {
        /** 二次 429 退避上限 8s（design.md）。 */
        const val MAX_RETRY_AFTER_MS: Long = 8000L

        /** 无 Retry-After 时的默认退避 1s。 */
        const val DEFAULT_429_DELAY_MS: Long = 1000L

        /** 默认 CIO 客户端（原 `OkHttpClient()` 默认 10s 连接/读超时对齐）。 */
        fun defaultScrapeHttpClient(): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000L
                socketTimeoutMillis = 10_000L
            }
        }

        /**
         * 解析 Retry-After 头（秒数或 HTTP-date）。
         * - 秒数：直接 *1000；
         * - HTTP-date：按 RFC1123 解析与当前时间差值；
         * - 解析失败返回 null 由调用方回退默认延迟。
         *
         * P2c：commonMain 无 java.time，HTTP-date 改手动解析（RFC1123 固定格式，
         * 日/月映射表 + GMT epoch 换算，不依赖平台时区库）。
         * public 可见性：下游 :core:scrape 单测直接断言（原同模块 internal 语义）。
         */
        fun parseRetryAfterMs(value: String?, nowMs: () -> Long = { platformNowMs() }): Long? {
            if (value.isNullOrBlank()) return null
            val trimmed = value.trim()
            // 1) 尝试秒数（整数）
            trimmed.toLongOrNull()?.let { sec ->
                return (sec * 1000L).coerceAtLeast(0L)
            }
            // 2) 尝试 HTTP-date（RFC1123，如 Wed, 21 Oct 2015 07:28:00 GMT）
            return parseHttpDateToEpochMs(trimmed)?.let { epochMs ->
                (epochMs - nowMs()).coerceAtLeast(0L)
            }
        }

        private val HTTP_DATE_PATTERN =
            // 日允许 1-2 位（java.time RFC_1123 输出天数不补零，如 "Fri, 4 Sep 2026"）
            Regex("""^[A-Za-z]{3}, (\d{1,2}) ([A-Za-z]{3}) (\d{4}) (\d{2}):(\d{2}):(\d{2}) (GMT|UTC|[+-]\d{4})$""")

        private val MONTHS = mapOf(
            "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
            "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12,
        )

        /** RFC1123 HTTP-date → epoch 毫秒；格式不符返回 null（调用方回退默认延迟）。
         * P2c 注：与 WebDavClient.parseHttpDateToEpochMs 逐字节一致（copy，避免 scrape↔webdav 成环）；单边改动须同步另一边。 */
        fun parseHttpDateToEpochMs(value: String): Long? {
            val match = HTTP_DATE_PATTERN.matchEntire(value.trim()) ?: return null
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val month = MONTHS[match.groupValues[2]] ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: return null
            val hour = match.groupValues[4].toIntOrNull() ?: return null
            val minute = match.groupValues[5].toIntOrNull() ?: return null
            val second = match.groupValues[6].toIntOrNull() ?: return null
            if (day !in 1..31 || hour > 23 || minute > 59 || second > 60) return null
            val days = daysSinceEpoch(year, month, day) ?: return null
            val zoneMs = parseZoneOffsetMs(match.groupValues[7]) ?: return null
            return days * 86_400_000L + hour * 3_600_000L + minute * 60_000L + second * 1000L - zoneMs
        }

        /** GMT/UTC → 0；±HHMM → 对应毫秒偏移（java.time ZoneOffset 语义的纯算术版）。 */
        private fun parseZoneOffsetMs(zone: String): Long? {
            if (zone == "GMT" || zone == "UTC") return 0L
            if (zone.length != 5) return null
            val sign = when (zone[0]) {
                '+' -> 1L
                '-' -> -1L
                else -> return null
            }
            val hh = zone.substring(1, 3).toIntOrNull() ?: return null
            val mm = zone.substring(3, 5).toIntOrNull() ?: return null
            if (hh > 23 || mm > 59) return null
            return sign * (hh * 3_600_000L + mm * 60_000L)
        }

        /** Howard Hinnant days_from_civil（纯算术，无平台库依赖）。 */
        private fun daysSinceEpoch(y: Int, m: Int, d: Int): Long? {
            if (m !in 1..12 || y < 1970 || y > 2100) return null
            val yAdj = if (m <= 2) y - 1 else y
            val era = (if (yAdj >= 0) yAdj else yAdj - 399) / 400
            val yoe = yAdj - era * 400
            val mp = (m + 9) % 12
            val doy = (153 * mp + 2) / 5 + d - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097L + doe - 719468L
        }
    }
}

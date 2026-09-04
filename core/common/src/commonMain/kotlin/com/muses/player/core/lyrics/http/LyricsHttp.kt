package com.muses.player.core.lyrics.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * 歌词域共享 HTTP 工具（P2c：OkHttp → Ktor-client CIO，搬入 commonMain）。
 *
 * 契约冻结（对齐 src/features/cover/http.ts 的 httpGetText/GetJson/PostText 语义）：
 * - 非 2xx 抛 IOException("http <code>")，不重试、不回退；
 * - JSON 解析失败向上抛（provider 自行决定是否吞掉）；
 * - 支持 per-call 读超时（AMLL 索引 20s / TTML 12s，经 HttpTimeout socketTimeout 逐个对齐）。
 * 独立于 core:scrape 的 ScrapeHttp 以保持 core:lyrics 无反向依赖。
 *
 * Ktor 说明：suspend 调用主线程安全，无需 Dispatchers.IO 包裹；
 * 超时异常（HttpRequestTimeoutException）本身即 IOException 子类，provider 侧泛 catch 语义不变。
 */
class LyricsHttp(
    private val client: HttpClient = defaultLyricsHttpClient(),
    private val connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
) {

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        execText(url, headers, readTimeoutMs)

    /** 带 per-call 读超时的 GET（秒） */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap(), timeoutSec: Long): String =
        execText(url, headers, timeoutSec * 1000L)

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement =
        Json.parseToJsonElement(getText(url, headers))

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap(), timeoutSec: Long): JsonElement =
        Json.parseToJsonElement(getText(url, headers, timeoutSec))

    /** POST 已序列化 body 字符串（如 form / raw json），返回文本 */
    suspend fun postText(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
        val response = client.post(url) {
            headers.forEach { (name, value) -> header(name, value) }
            headers["Content-Type"]?.let { contentType(ContentType.parse(it)) }
            timeout {
                connectTimeoutMillis = connectTimeoutMs
                socketTimeoutMillis = readTimeoutMs
            }
            setBody(body)
        }
        if (!response.status.isSuccess()) throw IOException("http ${response.status.value}")
        return response.bodyAsText()
    }

    suspend fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()): JsonElement =
        Json.parseToJsonElement(postText(url, body, headers))

    private suspend fun execText(url: String, headers: Map<String, String>, timeoutMs: Long): String {
        val response = client.get(url) {
            headers.forEach { (name, value) -> header(name, value) }
            timeout {
                connectTimeoutMillis = connectTimeoutMs
                socketTimeoutMillis = timeoutMs
            }
        }
        if (!response.status.isSuccess()) throw IOException("http ${response.status.value}")
        return response.bodyAsText()
    }

    companion object {
        /** 默认连接超时 15s（原 OkHttpClient connectTimeout 对齐）。 */
        const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 15_000L

        /** 默认读超时 20s（原 OkHttpClient readTimeout 对齐）。 */
        const val DEFAULT_READ_TIMEOUT_MS: Long = 20_000L

        /** 默认 CIO 客户端：双端同引擎（commonMain 可用，无需 expect）。 */
        fun defaultLyricsHttpClient(
            connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
            readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
        ): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = connectTimeoutMs
                socketTimeoutMillis = readTimeoutMs
            }
        }
    }
}

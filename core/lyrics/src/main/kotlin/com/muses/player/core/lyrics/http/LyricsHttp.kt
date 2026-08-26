package com.muses.player.core.lyrics.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 歌词域共享 HTTP 工具（对齐 src/features/cover/http.ts 的 httpGetText/GetJson/PostText 语义）：
 * - 非 2xx 抛 IOException("http <code>")，不重试、不回退；
 * - JSON 解析失败向上抛（provider 自行决定是否吞掉）；
 * - 支持 per-call 读超时（AMLL 索引 20s / TTML 12s）。
 * 独立于 core:scrape 的 ScrapeHttp 以保持 core:lyrics 无反向依赖。
 */
class LyricsHttp(
    connectTimeoutSec: Long = 15,
    readTimeoutSec: Long = 20,
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .build()

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        execText(buildRequest(url, headers), client)

    /** 带 per-call 读超时的 GET（秒） */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap(), timeoutSec: Long): String =
        execText(buildRequest(url, headers), clientWithReadTimeout(timeoutSec))

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement =
        Json.parseToJsonElement(getText(url, headers))

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap(), timeoutSec: Long): JsonElement =
        Json.parseToJsonElement(getText(url, headers, timeoutSec))

    /** POST 已序列化 body 字符串（如 form / raw json），返回文本 */
    suspend fun postText(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
        val contentType = headers["Content-Type"]?.toMediaTypeOrNull()
        val request = buildRequest(url, headers)
            .newBuilder()
            .post(body.toRequestBody(contentType))
            .build()
        return execText(request, client)
    }

    suspend fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()): JsonElement =
        Json.parseToJsonElement(postText(url, body, headers))

    private fun clientWithReadTimeout(timeoutSec: Long): OkHttpClient =
        client.newBuilder().readTimeout(timeoutSec, TimeUnit.SECONDS).build()

    private fun buildRequest(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        for ((name, value) in headers) {
            builder.header(name, value)
        }
        return builder.build()
    }

    private suspend fun execText(request: Request, targetClient: OkHttpClient): String =
        withContext(Dispatchers.IO) {
            targetClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("http ${response.code}")
                response.body?.string() ?: ""
            }
        }
}

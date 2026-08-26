package com.muses.player.core.scrape.http

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 刮削引擎共享 HTTP GET 工具。
 *
 * 规格书 = src/features/cover/http.ts 的 httpGetText / httpGetJson：
 * - 非 2xx 直接抛错（错误消息 `http <status>`），不重试、不回退；
 * - JSON 解析失败向上抛出（kw provider 自行捕获返回 null 的行为在 provider 内对齐）。
 */
class ScrapeHttp(private val client: OkHttpClient = OkHttpClient()) {

    /** 跨端文本 GET：非 2xx 抛 IOException("http <status>") */
    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val request = buildRequest(url, headers)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("http ${response.code}")
                }
                response.body?.string() ?: ""
            }
        }

    /** 文本 GET + JSON 解析；解析失败抛序列化异常（与 Web JSON.parse 抛错一致） */
    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement {
        val text = getText(url, headers)
        // 响应结构不稳定，调用方用 JsonElement 松散解析，不做强类型映射
        return Json.parseToJsonElement(text)
    }

    /** 二进制 GET（远程封面字节）；非 2xx 抛 IOException，不重试 */
    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
        withContext(Dispatchers.IO) {
            val request = buildRequest(url, headers)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("http ${response.code}")
                }
                response.body?.bytes() ?: ByteArray(0)
            }
        }

    private fun buildRequest(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        for ((name, value) in headers) {
            builder.header(name, value)
        }
        return builder.build()
    }
}

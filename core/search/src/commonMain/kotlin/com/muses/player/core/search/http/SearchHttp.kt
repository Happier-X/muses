package com.muses.player.core.search.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 在线搜索的 HTTP 客户端。
 *
 * 各平台接口都是「逆向接口」，对 header 敏感（Referer/UA 缺失常直接 403 或返回空），
 * 故按平台需要注入默认 header；响应体解析一律走 [JsonElement] 松散模式
 * （字段结构不稳定，强类型映射易碎）。
 */
class SearchHttp(
    private val client: HttpClient = defaultSearchHttpClient(),
    private val json: Json = SearchJson,
) {

    /** 桌面 UA：多数平台对移动 UA 返回的字段结构不同，统一桌面口径 */
    private val desktopUa: String = DEFAULT_UA

    /** GET 文本（自动带 UA；[referer] 非空时注入） */
    suspend fun getText(
        url: String,
        referer: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String {
        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.UserAgent, desktopUa)
            referer?.let { header(HttpHeaders.Referrer, it) }
            extraHeaders.forEach { (k, v) -> header(k, v) }
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("http ${response.status.value}")
        }
        return response.bodyAsText()
    }

    /** POST JSON（QQ 音乐 musicu.fcg 用） */
    suspend fun postJson(
        url: String,
        body: String,
        referer: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String {
        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.UserAgent, desktopUa)
            referer?.let { header(HttpHeaders.Referrer, it) }
            contentType(ContentType.Application.Json)
            extraHeaders.forEach { (k, v) -> header(k, v) }
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("http ${response.status.value}")
        }
        return response.bodyAsText()
    }

    /**
     * 解析为 JSON 对象。
     *
     * 注意：酷我 `search.kuwo.cn/r.s` 返回的是**单引号伪 JSON**（键值用 `'`），
     * 需先做引号替换再解析；这里是统一的兜底处理。
     */
    fun parseObject(text: String): JsonObject {
        // 先 normalize 再解析：
        // - 对单引号伪 JSON（酷我）是必需的正规化；
        // - 对标准 JSON 是幂等的（字符串内的单引号不会被替换，见 normalizeSingleQuotedJson）。
        // 不能「先直接解析」：isLenient 会把单引号 JSON **解析成错误结构**（数组退化为标量），
        // 且不报错，导致兜底分支永远不触发。
        val normalized = normalizeSingleQuotedJson(text)
        val obj = runCatching { json.parseToJsonElement(normalized) as? JsonObject }.getOrNull()
        if (obj != null && obj.isNotEmpty()) return obj
        // 极致兜底：原文再试（应对 normalize 误伤的非典型文本）
        return runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: obj
            ?: JsonObject(emptyMap())
    }

    fun parseElement(text: String): JsonElement? =
        runCatching { json.parseToJsonElement(text) }.getOrNull()

    /**
     * 单引号伪 JSON → 标准 JSON。
     *
     * 保守实现：只在字符串外替换引号，避免破坏内容里的单引号
     * （如歌名 "Don't Stop"）。同时去掉 JS 里常见的 `undefined` 字面量。
     */
    private fun normalizeSingleQuotedJson(text: String): String {
        val sb = StringBuilder(text.length + 16)
        var inDouble = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' && i + 1 < text.length -> {
                    sb.append(c).append(text[i + 1]); i += 2; continue
                }
                c == '"' -> {
                    inDouble = !inDouble
                    sb.append(c)
                }
                c == '\'' && !inDouble -> sb.append('"')
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
            .replace(":undefined", ":null")
            .replace(": undefined", ": null")
    }

    companion object {
        const val DEFAULT_UA: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        fun defaultSearchHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 8_000
                socketTimeoutMillis = 15_000
            }
        }
    }
}

/** 搜索响应结构不稳定：宽松解析 */
internal val SearchJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}
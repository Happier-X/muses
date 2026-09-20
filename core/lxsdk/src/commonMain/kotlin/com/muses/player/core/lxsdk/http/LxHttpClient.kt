package com.muses.player.core.lxsdk.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 洛雪 `lx.request(url, options, callback)` 的 Ktor 实现。
 *
 * options 支持（对齐洛雪规范）：`method` / `headers` / `body` / `form` / `formData` / `timeout`。
 * 返回响应体文本（由 JS shim 负责 JSON 解析，与洛雪宿主行为一致）。
 *
 * 说明：洛雪源普遍依赖真实网络与特定 UA/Referer，本层不做重试与限流——
 * 「保留失败语义」交脚本自行 catch（脚本里大量 `request(...).catch(...)` 逻辑）。
 */
class LxHttpClient(
    private val client: HttpClient = defaultLxHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {

    /**
     * 执行请求，返回响应体文本。
     * 非 2xx 不抛错（脚本可能依赖错误响应体内容），仅网络异常向上抛。
     */
    suspend fun request(url: String, optionsJson: String?): String {
        val options = parseOptions(optionsJson)
        val method = options.method.uppercase()
        val response: HttpResponse = client.request(url) {
            this.method = HttpMethod.parse(method)
            applyHeaders(options)
            applyBody(options)
        }
        return response.bodyAsText()
    }

    private fun HttpRequestBuilder.applyHeaders(options: LxRequestOptions) {
        options.headers.forEach { (name, value) -> header(name, value) }
        // 未显式指定 UA 时补默认值：多数音源接口对 UA 敏感
        if (options.headers.keys.none { it.equals(HttpHeaders.UserAgent, ignoreCase = true) }) {
            header(HttpHeaders.UserAgent, DEFAULT_UA)
        }
    }

    private suspend fun HttpRequestBuilder.applyBody(options: LxRequestOptions) {
        when {
            options.form != null -> {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            options.form.forEach { (k, v) -> append(k, v) }
                        },
                    ),
                )
            }
            options.formData != null -> {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            options.formData.forEach { (k, v) -> append(k, v) }
                        },
                    ),
                )
            }
            options.body != null -> {
                if (options.headers.keys.none { it.equals(HttpHeaders.ContentType, ignoreCase = true) }) {
                    contentType(ContentType.Application.Json)
                }
                setBody(options.body)
            }
        }
    }

    /** 松散解析 options：字段缺失/类型不符时回退默认值，不因脚本传入畸形 options 而崩 */
    private fun parseOptions(optionsJson: String?): LxRequestOptions {
        if (optionsJson.isNullOrBlank()) return LxRequestOptions()
        val obj: JsonObject = try {
            json.parseToJsonElement(optionsJson).jsonObject
        } catch (e: Exception) {
            return LxRequestOptions()
        }
        fun str(key: String): String? =
            (obj[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

        fun strMap(key: String): Map<String, String> =
            (obj[key] as? JsonObject)?.mapNotNull { (k, v) ->
                (v as? JsonPrimitive)?.contentOrNull?.let { k to it }
            }?.toMap() ?: emptyMap()

        fun anyElement(key: String): JsonElement? = obj[key]

        return LxRequestOptions(
            method = str("method") ?: "GET",
            headers = strMap("headers"),
            body = anyElement("body")?.let { if (it is JsonPrimitive) it.contentOrNull else it.toString() },
            form = (anyElement("form") as? JsonObject)?.let { fo ->
                fo.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap()
            },
            formData = (anyElement("formData") as? JsonObject)?.let { fo ->
                fo.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap()
            },
            timeout = str("timeout")?.toLongOrNull(),
        )
    }

    companion object {
        /** 洛雪源广泛依赖桌面 UA，默认值与之对齐 */
        const val DEFAULT_UA: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        fun defaultLxHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
        }
    }
}

/** `lx.request` 的 options 解析结果 */
internal data class LxRequestOptions(
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val form: Map<String, String>? = null,
    val formData: Map<String, String>? = null,
    val timeout: Long? = null,
)

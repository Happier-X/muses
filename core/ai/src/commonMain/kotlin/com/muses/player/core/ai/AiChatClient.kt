package com.muses.player.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * OpenAI 兼容的 Chat Completions 客户端。
 *
 * 只实现推荐所需的最小面：非流式、单轮 system+user、取 `choices[0].message.content`。
 * 不引各家 SDK、不做流式/工具调用——推荐场景一次请求即完成，越薄越稳。
 *
 * 鉴权：`Authorization: Bearer <API Key>`（OpenAI 兼容统一口径）。
 */
class AiChatClient(
    private val client: HttpClient = defaultAiHttpClient(),
) {

    /**
     * 发起一次补全，返回助手文本内容。
     *
     * @throws AiException 网络失败/鉴权失败/返回结构不含内容
     */
    suspend fun complete(
        config: AiRecommendConfig,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val url = "${config.resolvedBaseUrl}/chat/completions"
        val body = buildJsonObject {
            put("model", config.resolvedModel)
            put("temperature", RECOMMEND_TEMPERATURE)
            put("stream", false)
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
            })
        }.toString()

        val response = try {
            client.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                setBody(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AiException("AI 服务连接失败：${e.message}", e)
        }

        val text = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AiException("AI 响应读取失败：${e.message}", e)
        }

        if (!response.status.isSuccess()) {
            throw AiException("AI 服务返回 ${response.status.value}：${text.take(200)}")
        }

        val root = runCatching { AiJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw AiException("AI 响应不是合法 JSON：${text.take(200)}")
        val content = root["choices"]
            ?.let { it as? JsonArray }
            ?.firstOrNull()
            ?.let { it as? JsonObject }
            ?.get("message")
            ?.let { it as? JsonObject }
            ?.get("content")
            ?.let { it as? JsonPrimitive }
            ?.contentOrNull

        if (content.isNullOrBlank()) {
            // 兼容部分服务商的错误信封：{ "error": { "message": "..." } }
            val err = root["error"]
                ?.let { it as? JsonObject }
                ?.get("message")
                ?.let { it as? JsonPrimitive }
                ?.contentOrNull
            throw AiException(err?.let { "AI 服务报错：$it" } ?: "AI 响应缺少 content：${text.take(200)}")
        }
        return content
    }

    companion object {
        /** 推荐需要一定发散性，但过高会编造：0.7 为实测较平衡值 */
        private const val RECOMMEND_TEMPERATURE = 0.7

        fun defaultAiHttpClient(): HttpClient = HttpClient(CIO) {
            expectSuccess = false
            install(HttpTimeout) {
                // LLM 生成比普通接口慢，超时放宽（20 首要十来秒）
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}

/** LLM 返回文本偶带 markdown 代码块，解析统一走宽松模式 */
internal val AiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

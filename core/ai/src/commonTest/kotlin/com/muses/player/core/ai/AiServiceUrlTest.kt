package com.muses.player.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AiServiceUrlTest {
    @Test
    fun `粘贴换行制表符和不可见字符可清理`() {
        val pasted = " \uFEFFhttps://\r\napi.deepseek.com/\tv1\u200B/ \n"
        assertEquals("https://api.deepseek.com/v1", validateAiBaseUrl(pasted))
        assertEquals("https://api.deepseek.com/v1", AiRecommendConfig(pasted).resolvedBaseUrl)
    }

    @Test
    fun `合法版本代理路径与本地端口保持不变`() {
        listOf("https://api.deepseek.com", "https://example.com/proxy/v1", "http://localhost:11434/v1").forEach {
            assertEquals(it, validateAiBaseUrl(it))
        }
    }

    @Test
    fun `非法地址明确报错且不猜测目标`() {
        listOf("", "https://", "https:///v1", "api.deepseek.com", "..https://api.deepseek.com", "ftp://example.com", "https://api.deep seek.com").forEach {
            val error = assertFailsWith<AiServiceUrlException> { validateAiBaseUrl(it) }
            assertTrue(error.message.orEmpty().startsWith("服务地址无效"))
        }
        assertEquals("..https://api.deepseek.com", normalizeAiBaseUrl("..https://api.deepseek.com"))
    }

    @Test
    fun `不能把参数或登录信息当作服务基址`() {
        listOf("https://example.com/v1?key=value", "https://example.com/v1#part", "https://user:pass@example.com/v1").forEach {
            assertFailsWith<AiException> { validateAiBaseUrl(it) }
        }
    }

    @Test
    fun `补全与模型列表使用相同的清理规则`() = runTest {
        val targets = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            targets += request.url.toString()
            val body = if (request.url.encodedPath.endsWith("/models")) {
                """{"data":[{"id":"deepseek-chat"}]}"""
            } else {
                """{"choices":[{"message":{"content":"正常"}}]}"""
            }
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val chat = AiChatClient(client)
            val pasted = " https://\napi.deepseek.com/\tv1/ "
            assertEquals("正常", chat.complete(AiRecommendConfig(pasted, "deepseek-chat", "fake-key"), "测试", "测试"))
            assertEquals(listOf("deepseek-chat"), chat.listModels(pasted, "fake-key"))
            assertEquals(listOf("https://api.deepseek.com/v1/chat/completions", "https://api.deepseek.com/v1/models"), targets)
        } finally {
            client.close()
        }
    }

    @Test
    fun `无效地址在发送任何请求前被拒绝`() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("{}") })
        try {
            val chat = AiChatClient(client)
            assertFailsWith<AiServiceUrlException> {
                chat.complete(AiRecommendConfig("..https://api.deepseek.com", "model", "fake-key"), "测试", "测试")
            }
            assertFailsWith<AiServiceUrlException> { chat.listModels("https://", "fake-key") }
            assertEquals(0, requests)
        } finally {
            client.close()
        }
    }
}

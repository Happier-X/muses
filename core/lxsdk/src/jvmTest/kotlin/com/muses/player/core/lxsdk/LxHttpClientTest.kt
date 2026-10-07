package com.muses.player.core.lxsdk

import com.muses.player.core.lxsdk.http.LxHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LxHttpClientTest {
    @Test
    fun `脚本超时生效且超时后可再次请求`() = runBlocking {
        val mock = MockEngine { request ->
            if (request.url.encodedPath == "/slow") delay(2000)
            respond("正常响应")
        }
        val client = HttpClient(mock) {
            install(HttpTimeout) { requestTimeoutMillis = 5000 }
        }
        val http = LxHttpClient(client)
        try {
            assertFailsWith<HttpRequestTimeoutException> {
                http.request("http://api.test/slow", """{"timeout":50}""")
            }
            assertEquals("正常响应", http.request("http://api.test/fast", """{"timeout":1000}"""))
        } finally { client.close() }
    }
}

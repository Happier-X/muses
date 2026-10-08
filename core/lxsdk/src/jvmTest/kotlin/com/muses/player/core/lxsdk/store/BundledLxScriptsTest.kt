package com.muses.player.core.lxsdk.store

import com.muses.player.core.lxsdk.LxScriptEngine
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundledLxScriptsTest {
    @Test
    fun `内置脚本在真实引擎中声明可播放平台`() = runBlocking {
        val failures = mutableListOf<String>()
        val bundled = BundledLxScripts.load()
        assertEquals(18, bundled.size)
        for (script in bundled) {
            val client = HttpClient(MockEngine {
                respond(
                    """{"code":200,"data":{"update":{"version":"0"},"init":{"sources":{"wy":{"name":"网易","type":"music","actions":["musicUrl"],"qualitys":["128k"]}}}}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            })
            val engine = LxScriptEngine(script.source, LxCryptoJvm(), LxHttpClient(client), requestTimeoutMs = 15_000)
            try {
                val descriptor = engine.load()
                assertTrue(descriptor.sources.values.any { "musicUrl" in it.actions }, script.id)
            } catch (e: Exception) {
                failures += "${script.id}: ${e.message}"
            } finally {
                engine.close()
                client.close()
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `首次安装后保留禁用编辑和删除状态`() {
        val root = Files.createTempDirectory("muses-bundled-test").toFile()
        val bundled = listOf(BundledLxScript("builtin-test", "/** @name 测试 */", "https://example.test/source.js"))
        fun store() = FileLxScriptStore({ root }, { bundled })
        try {
            assertEquals(1, store().list().size)
            store().setEnabled("builtin-test", false)
            assertFalse(store().get("builtin-test")!!.enabled)
            store().save("builtin-test", "用户编辑", false)
            assertEquals("用户编辑", store().get("builtin-test")!!.source)
            store().delete("builtin-test")
            assertTrue(store().list().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}

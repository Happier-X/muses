package com.muses.player.core.webdav

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KtorWebDavClient 单测（P2c：OkHttp/MockWebServer → Ktor MockEngine 重写，语义冻结）。
 *
 * MockEngine 不走真实 socket：多跳按队列顺序应答；requestCount 改计数器；
 * 去 Robolectric（纯 JVM 即可，File 用真实临时文件）。
 */
class KtorWebDavClientTest {

    private data class MockResp(
        val status: Int,
        val body: String = "",
        val headers: Map<String, String> = emptyMap(),
    )

    private class Harness(vararg resps: MockResp) {
        private val queue = ArrayDeque(resps.toList())
        val requests = mutableListOf<HttpRequestData>()
        val logs = mutableListOf<String>()

        private val errorLogStore = object : com.muses.player.core.data.log.ErrorLogStore {
            override val latestSummary = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
            override fun log(
                level: com.muses.player.core.data.log.ErrorLogStore.Level,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) {
                logs += "$tag:$message"
            }
            override suspend fun dump(): String? = logs.joinToString("\n").ifEmpty { null }
        }

        /** 空仓库 stub：Registry 无任何注册源 */
        private val emptySourceRepo = object : com.muses.player.core.data.repository.SourceRepository {
            override fun observeSources() =
                kotlinx.coroutines.flow.flowOf(emptyList<com.muses.player.core.model.Source>())
            override suspend fun getSource(id: String): com.muses.player.core.model.Source? = null
            override suspend fun upsert(source: com.muses.player.core.model.Source) = Unit
            override suspend fun deleteById(id: String) = Unit
        }
        private val emptyCredRepo = object : com.muses.player.core.data.repository.CredentialsRepository {
            override suspend fun savePassword(sourceId: String, password: String) = Unit
            override suspend fun getPassword(sourceId: String): String? = null
            override suspend fun clearPassword(sourceId: String) = Unit
        }

        val client = KtorWebDavClient(
            HttpClient(
                MockEngine { req ->
                    requests += req
                    val r = queue.removeFirst()
                    respond(
                        r.body,
                        HttpStatusCode.fromValue(r.status),
                        headersOf(*r.headers.map { (k, v) -> k to listOf(v) }.toTypedArray()),
                    )
                },
            ),
            // 空 Registry（无注册源 → authorizationHeader 恒 null）；本测试用显式 authenticate，不会查它
            WebDavAuthRegistry(emptySourceRepo, emptyCredRepo),
            WebDavRateLimiter.Unlimited,
            errorLogStore,
        ).apply { authenticate("user", "pass") }
    }

    private fun ok(body: String) = MockResp(200, body)
    private fun status(status: Int, body: String = "") = MockResp(status, body)
    private fun multistatus(body: String) = MockResp(207, body)
    private fun throttled() = MockResp(429, "", mapOf("Retry-After" to "0"))

    @Test
    fun probe_returns_true_on_success() = runTest {
        val h = Harness(multistatus(propfindRootResponse))
        assertTrue(h.client.probe("https://example.com/dav/"))
    }

    @Test
    fun probe_returns_false_on_error() = runTest {
        val h = Harness(status(401))
        assertFalse(h.client.probe("https://example.com/dav/"))
    }

    @Test
    fun list_parses_multistatus_response() = runTest {
        val h = Harness(multistatus(propfindDepthOneResponse))
        val items = h.client.list("https://example.com/dav/music/")
        assertEquals(3, items.size)

        // 文件
        val song = items.first { it.name == "song.mp3" }
        assertFalse(song.isDirectory)
        assertEquals(1024000L, song.contentLength)

        // 子目录
        val subdir = items.first { it.name == "rock" }
        assertTrue(subdir.isDirectory)

        // 无扩展名文件
        val unknown = items.first { it.name == "readme" }
        assertFalse(unknown.isDirectory)
    }

    @Test
    fun list_throws_auth_exception_on_401() = runTest {
        val h = Harness(status(401))
        try {
            h.client.list("https://example.com/dav/music/")
            assert(false) { "Expected WebDavAuthException" }
        } catch (e: WebDavAuthException) {
            assertTrue(e.message?.contains("401") == true)
        }
    }

    @Test
    fun get_downloads_file() = runTest {
        val h = Harness(ok("fake audio content"))
        val dest = java.io.File.createTempFile("test-", ".mp3")
        dest.deleteOnExit()

        val result = h.client.get("https://example.com/dav/music/song.mp3", dest)
        assertEquals("fake audio content", result.readText())
    }

    @Test
    fun get_throws_on_error() = runTest {
        val h = Harness(status(404))
        val dest = java.io.File.createTempFile("test-", ".mp3")
        dest.deleteOnExit()

        try {
            h.client.get("https://example.com/dav/music/missing.mp3", dest)
            assert(false) { "Expected WebDavRequestException" }
        } catch (e: WebDavRequestException) {
            assertEquals(404, e.code)
        }
    }

    @Test
    fun put_uploads_file() = runTest {
        val h = Harness(MockResp(201))
        val source = java.io.File.createTempFile("upload-", ".mp3")
        source.writeText("test data")
        source.deleteOnExit()

        h.client.put("https://example.com/dav/music/uploaded.mp3", source)

        val recorded = h.requests.single()
        assertEquals("PUT", recorded.method.value)
        assertEquals("test data", (recorded.body as ByteArrayContent).bytes().decodeToString())
    }

    @Test
    fun delete_sends_delete_request() = runTest {
        val h = Harness(MockResp(204))

        h.client.delete("https://example.com/dav/music/old.mp3")

        val recorded = h.requests.single()
        assertEquals("DELETE", recorded.method.value)
    }

    @Test
    fun move_sends_move_request() = runTest {
        val h = Harness(MockResp(201))
        val source = "https://example.com/dav/music/old.mp3"
        val dest = "https://example.com/dav/music/new.mp3"

        h.client.move(source, dest)

        val recorded = h.requests.single()
        assertEquals("MOVE", recorded.method.value)
        assertEquals(dest, recorded.headers["Destination"])
        assertEquals("T", recorded.headers["Overwrite"])
    }

    @Test
    fun getString_returns_text_content() = runTest {
        val h = Harness(ok("lyrics content"))

        val result = h.client.getString("https://example.com/dav/music/song.lrc")
        assertEquals("lyrics content", result)
    }

    @Test
    fun getString_returns_null_on_error() = runTest {
        val h = Harness(status(404))

        val result = h.client.getString("https://example.com/dav/music/missing.lrc")
        assertEquals(null, result)
    }

    @Test
    fun chinese_filename_parsed_correctly() = runTest {
        val h = Harness(multistatus(propfindChineseNameResponse))
        val items = h.client.list("https://example.com/dav/music/")

        val chinese = items.firstOrNull { it.name.contains("晴天") }
        assertEquals(true, chinese?.name?.contains("晴天"))
        assertTrue(chinese?.isDirectory == false)
    }

    @Test
    fun list_tolerates_no_prefix_uppercase_and_entities() = runTest {
        // 无前缀 + 全大写标签 + 百分号编码 + 命名/数字 XML 实体 + 缺字段回退
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <MULTISTATUS xmlns="DAV:">
                <RESPONSE>
                    <HREF>/dav/music/</HREF>
                    <PROPSTAT><PROP><COLLECTION/></PROP></PROPSTAT>
                </RESPONSE>
                <response>
                    <href>/dav/music/%E6%99%B4%E5%A4%A9.mp3</href>
                    <propstat><prop>
                        <getcontentlength>10</getcontentlength>
                    </prop></propstat>
                </response>
                <D:response xmlns:D="DAV:">
                    <D:href>/dav/music/rock &amp; roll&#x2F;live.mp3</D:href>
                    <D:propstat><D:prop>
                        <D:getcontentlength>20</D:getcontentlength>
                    </D:prop></D:propstat>
                </D:response>
                <d:response xmlns:d="DAV:">
                    <d:href>/dav/music/A&#65;B.mp3</d:href>
                    <d:propstat><d:prop/></d:propstat>
                </d:response>
            </MULTISTATUS>
        """.trimIndent()
        val h = Harness(multistatus(body))
        val items = h.client.list("https://example.com/dav/music/")
        assertEquals(3, items.size)
        // depth0 目录自身跳过；百分号解码
        assertTrue(items.any { it.name == "晴天.mp3" && !it.isDirectory })
        // &amp; + &#x2F;(=/) 还原（/ 参与路径切分，name 取末段）
        assertTrue(items.any { it.name == "live.mp3" })
        // &#65; 还原为 A；缺字段回退零值
        val aab = items.first { it.name == "AAB.mp3" }
        assertEquals(0L, aab.contentLength)
        assertEquals(null, aab.lastModified)
        assertEquals(null, aab.eTag)
    }

    // ── 429 退避与限流埋点（任务 08-27-webdav-playback-429） ─────────────────

    @Test
    fun get_429_retry_once_then_success() = runTest {
        val h = Harness(throttled(), ok("ok-after-429"))
        val dest = java.io.File.createTempFile("test-429-", ".mp3")
        dest.deleteOnExit()
        val result = h.client.get("https://example.com/dav/music/song.mp3", dest)
        assertEquals("ok-after-429", result.readText())
        assertEquals(2, h.requests.size)
        assertTrue(h.logs.any { it.contains("429") })
    }

    @Test
    fun get_429_second_attempt_throws_and_logs() = runTest {
        val h = Harness(throttled(), throttled())
        val dest = java.io.File.createTempFile("test-429-fail-", ".mp3")
        dest.deleteOnExit()
        try {
            h.client.get("https://example.com/dav/music/song.mp3", dest)
            assert(false) { "应抛 http 429" }
        } catch (e: java.io.IOException) {
            assertEquals("http 429", e.message)
        }
        assertEquals(2, h.requests.size)
        assertTrue(h.logs.any { it.contains("429") })
    }

    @Test
    fun list_429_retry_once_then_success() = runTest {
        val h = Harness(throttled(), multistatus(propfindDepthOneResponse))
        val items = h.client.list("https://example.com/dav/music/")
        assertEquals(3, items.size)
        assertEquals(2, h.requests.size)
    }

    @Test
    fun get_429_without_retry_after_uses_default_delay_and_retries() = runTest {
        val h = Harness(status(429), ok("ok-default"))
        val dest = java.io.File.createTempFile("test-429-default-", ".mp3")
        dest.deleteOnExit()
        val result = h.client.get("https://example.com/dav/music/song2.mp3", dest)
        assertEquals("ok-default", result.readText())
        assertEquals(2, h.requests.size)
    }

    // ── Test XML responses ──────────────────────────────

    private val propfindRootResponse = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
            <d:response>
                <d:href>/dav/</d:href>
                <d:propstat>
                    <d:prop>
                        <d:collection/>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
        </d:multistatus>
    """.trimIndent()

    private val propfindDepthOneResponse = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
            <d:response>
                <d:href>/dav/music/</d:href>
                <d:propstat>
                    <d:prop>
                        <d:collection/>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
            <d:response>
                <d:href>/dav/music/song.mp3</d:href>
                <d:propstat>
                    <d:prop>
                        <d:getcontentlength>1024000</d:getcontentlength>
                        <d:getlastmodified>Mon, 01 Jan 2024 00:00:00 GMT</d:getlastmodified>
                        <d:getetag>"abc123"</d:getetag>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
            <d:response>
                <d:href>/dav/music/rock/</d:href>
                <d:propstat>
                    <d:prop>
                        <d:collection/>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
            <d:response>
                <d:href>/dav/music/readme</d:href>
                <d:propstat>
                    <d:prop>
                        <d:getcontentlength>128</d:getcontentlength>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
        </d:multistatus>
    """.trimIndent()

    private val propfindChineseNameResponse = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
            <d:response>
                <d:href>/dav/music/%E6%99%B4%E5%A4%A9.mp3</d:href>
                <d:propstat>
                    <d:prop>
                        <d:getcontentlength>512000</d:getcontentlength>
                    </d:prop>
                    <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
            </d:response>
        </d:multistatus>
    """.trimIndent()
}

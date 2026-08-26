package com.muses.player.core.webdav

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OkHttpWebDavClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpWebDavClient

    /** 空仓库 stub：Registry 无任何注册源 */
    private val emptySourceRepo = object : com.muses.player.core.data.repository.SourceRepository {
        override fun observeSources() = kotlinx.coroutines.flow.flowOf(emptyList<com.muses.player.core.model.Source>())
        override suspend fun getSource(id: String): com.muses.player.core.model.Source? = null
        override suspend fun upsert(source: com.muses.player.core.model.Source) = Unit
        override suspend fun deleteById(id: String) = Unit
    }
    private val emptyCredRepo = object : com.muses.player.core.data.repository.CredentialsRepository {
        override suspend fun savePassword(sourceId: String, password: String) = Unit
        override suspend fun getPassword(sourceId: String): String? = null
        override suspend fun clearPassword(sourceId: String) = Unit
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        client = OkHttpWebDavClient(
            httpClient,
            // 空 Registry（无注册源 → authorizationHeader 恒 null）；本测试用显式 authenticate，不会查它
            WebDavAuthRegistry(emptySourceRepo, emptyCredRepo),
        )
        client.authenticate("user", "pass")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun probe_returns_true_on_success() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody(propfindRootResponse))
        val url = server.url("/dav/").toString()
        assertTrue(client.probe(url))
    }

    @Test
    fun probe_returns_false_on_error() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val url = server.url("/dav/").toString()
        assertFalse(client.probe(url))
    }

    @Test
    fun list_parses_multistatus_response() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody(propfindDepthOneResponse))
        val url = server.url("/dav/music/").toString()
        val items = client.list(url)
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
        server.enqueue(MockResponse().setResponseCode(401))
        val url = server.url("/dav/music/").toString()
        try {
            client.list(url)
            assert(false) { "Expected WebDavAuthException" }
        } catch (e: WebDavAuthException) {
            assertTrue(e.message?.contains("401") == true)
        }
    }

    @Test
    fun get_downloads_file() = runTest {
        val body = "fake audio content"
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
        val url = server.url("/dav/music/song.mp3").toString()
        val dest = java.io.File.createTempFile("test-", ".mp3")
        dest.deleteOnExit()

        val result = client.get(url, dest)
        assertEquals(body, result.readText())
    }

    @Test
    fun get_throws_on_error() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val url = server.url("/dav/music/missing.mp3").toString()
        val dest = java.io.File.createTempFile("test-", ".mp3")
        dest.deleteOnExit()

        try {
            client.get(url, dest)
            assert(false) { "Expected WebDavRequestException" }
        } catch (e: WebDavRequestException) {
            assertEquals(404, e.code)
        }
    }

    @Test
    fun put_uploads_file() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val source = java.io.File.createTempFile("upload-", ".mp3")
        source.writeText("test data")
        source.deleteOnExit()
        val url = server.url("/dav/music/uploaded.mp3").toString()

        client.put(url, source)

        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("test data", recorded.body.readUtf8())
    }

    @Test
    fun delete_sends_delete_request() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val url = server.url("/dav/music/old.mp3").toString()

        client.delete(url)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
    }

    @Test
    fun move_sends_move_request() = runTest {
        server.enqueue(MockResponse().setResponseCode(201))
        val source = server.url("/dav/music/old.mp3").toString()
        val dest = server.url("/dav/music/new.mp3").toString()

        client.move(source, dest)

        val recorded = server.takeRequest()
        assertEquals("MOVE", recorded.method)
        assertEquals(dest, recorded.getHeader("Destination"))
        assertEquals("T", recorded.getHeader("Overwrite"))
    }

    @Test
    fun getString_returns_text_content() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("lyrics content"))
        val url = server.url("/dav/music/song.lrc").toString()

        val result = client.getString(url)
        assertEquals("lyrics content", result)
    }

    @Test
    fun getString_returns_null_on_error() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val url = server.url("/dav/music/missing.lrc").toString()

        val result = client.getString(url)
        assertEquals(null, result)
    }

    @Test
    fun chinese_filename_parsed_correctly() = runTest {
        server.enqueue(MockResponse().setResponseCode(207).setBody(propfindChineseNameResponse))
        val url = server.url("/dav/music/").toString()
        val items = client.list(url)

        val chinese = items.firstOrNull { it.name.contains("晴天") }
        assertEquals(true, chinese?.name?.contains("晴天"))
        assertTrue(chinese?.isDirectory == false)
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

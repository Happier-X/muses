package com.muses.player.core.search

import com.muses.player.core.search.http.SearchHttp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnlinePlayableUrlProbeTest {
    @Test fun `小范围请求识别音频并拒绝403与错误网页`() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("bytes=0-0", request.headers[HttpHeaders.Range])
            assertEquals(com.muses.player.core.model.online.OnlinePlaybackHttp.USER_AGENT,
                request.headers[HttpHeaders.UserAgent])
            when (request.url.encodedPath) {
                "/audio" -> respond("a", HttpStatusCode.PartialContent, headersOf(HttpHeaders.ContentType, "audio/mpeg"))
                "/forbidden" -> respond("denied", HttpStatusCode.Forbidden)
                else -> respond("<html>error</html>", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
            }
        })
        val http = SearchHttp(client)
        try {
            assertTrue(http.canOpenAudio("https://cdn.test/audio"))
            assertFalse(http.canOpenAudio("https://cdn.test/forbidden"))
            assertFalse(http.canOpenAudio("https://cdn.test/error"))
        } finally {
            client.close()
        }
    }
}

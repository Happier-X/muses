package com.muses.player.core.scrape.international

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.muses.player.core.model.scrape.*
import com.muses.player.core.network.ProviderRequestGate
import com.muses.player.core.scrape.cover.*
import com.muses.player.core.scrape.editmeta.*
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.*

class InternationalProvidersTest {
    private val query = EditCloudMetaQuery("歌曲", "童年", "罗大佑", "专辑乙")
    private val recordings = """{"recordings":[{"id":"recording-1","length":180000,"title":"童年","artist-credit":[{"name":"罗大佑"}],"releases":[
        {"id":"11111111-1111-1111-1111-111111111111","title":"专辑甲"},
        {"id":"22222222-2222-2222-2222-222222222222","title":"专辑乙"}]}]}"""

    private suspend fun client(
        response: (HttpRequestData) -> Pair<String, HttpStatusCode>,
        block: suspend (ScrapeHttp, List<HttpRequestData>) -> Unit,
    ) {
        val requests = mutableListOf<HttpRequestData>()
        val dispatcher = currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
        val client = HttpClient(MockEngine(MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler { request ->
                requests += request
                val (body, status) = response(request)
                respond(body, status)
            }
        }))
        try { block(ScrapeHttp(client, rateLimiter = WebDavRateLimiter.Unlimited, requestGate = ProviderRequestGate(0)), requests) }
        finally { client.close() }
    }

    @Test fun `诊断绕过MusicBrainz缓存而不覆盖正常审核缓存`() = runTest {
        var body = recordings
        client({ body to HttpStatusCode.OK }) { http, requests ->
            val mb = MusicBrainzClient(http)
            val q = OnlineTextQuery("歌曲", "童年", "罗大佑")
            val original = mb.search(q)
            body = recordings.replace("童年", "诊断新结果")
            val fresh = kotlinx.coroutines.withContext(com.muses.player.core.network.ProviderDiagnosticContext) { mb.search(q) }
            assertEquals("诊断新结果", fresh.first().hit.title)
            assertEquals(original, mb.search(q))
            assertEquals(2, requests.size)
        }
    }

    @Test fun `iTunes选择确实返回歌曲信息并保留不同发行`() = runTest {
        client({ """{"results":[{"kind":"song","trackName":"童年","artistName":"罗大佑","collectionName":"专辑甲","trackTimeMillis":180123,"trackId":111,"collectionId":222},
            {"kind":"song","trackName":"童年","artistName":"罗大佑","collectionName":"专辑乙"},
            {"kind":"music-video","trackName":"不要采纳"}]}""" to HttpStatusCode.OK }) { http, requests ->
            val result = EditCloudMetaSearch(listOf(ItunesTextProvider(http)), emptyList()).search(
                query, SearchOptions(platform = CloudPlatformId.ITUNES))
            assertEquals(2, result.text.items.size)
            assertTrue(result.text.items.all { it.source == OnlineTextSource.ITUNES })
            val firstRelease = result.text.items.single { it.album == "专辑甲" }
            assertEquals(180.123, firstRelease.durationSec)
            assertEquals("itunes:111", firstRelease.candidateId)
            assertEquals("itunes:222", firstRelease.releaseId)
            assertNull(firstRelease.recordingId)
            assertEquals("song", requests.single().url.parameters["entity"])
            assertEquals("童年 罗大佑", requests.single().url.parameters["term"])
        }
    }

    @Test fun `MusicBrainz共享录音搜索并按匹配发行获取正面封面`() = runTest {
        client({ request ->
            (if (request.url.host == "musicbrainz.org") recordings else
                """{"images":[{"front":false,"image":"https://cdn.test/back.jpg"},{"front":true,"thumbnails":{"1200":"https://cdn.test/front.jpg"}}]}""") to HttpStatusCode.OK
        }) { http, requests ->
            val mb = MusicBrainzClient(http)
            val result = EditCloudMetaSearch(listOf(MusicBrainzTextProvider(mb)), listOf(MusicBrainzCoverProvider(mb)))
                .search(query, SearchOptions(platform = CloudPlatformId.MUSICBRAINZ))
            assertEquals(setOf("专辑甲", "专辑乙"), result.text.items.map { it.album }.toSet())
            assertTrue(result.text.items.all { it.durationSec == 180.0 && it.recordingId == "musicbrainz:recording-1" })
            assertEquals(2, result.text.items.map { it.releaseId }.distinct().size)
            assertEquals("https://cdn.test/front.jpg", result.cover.items.single().remoteUrl)
            assertEquals(1, requests.count { it.url.host == "musicbrainz.org" })
            assertTrue(requests.first().headers["User-Agent"].orEmpty().startsWith("Muses/"))
            assertEquals("recording:\"童年\" AND artist:\"罗大佑\"", requests.first().url.parameters["query"])
            assertTrue(requests.last().url.encodedPath.endsWith("22222222-2222-2222-2222-222222222222"))
        }
    }

    @Test fun `MusicBrainz保留来源录音说明而不改写歌名`() = runTest {
        client({ recordings.replace("\"length\":180000", "\"length\":180000,\"disambiguation\":\"Live at Wembley\"") to HttpStatusCode.OK }) { http, _ ->
            val result = EditCloudMetaSearch(listOf(MusicBrainzTextProvider(MusicBrainzClient(http))), emptyList())
                .search(query, SearchOptions(platform = CloudPlatformId.MUSICBRAINZ))
            assertEquals(2, result.text.items.size)
            assertTrue(result.text.items.all { it.versionInfo == "Live at Wembley" && it.title == "童年" })
            assertTrue(result.text.items.all { com.muses.player.core.scrape.text.evaluateTextMatch(it,
                OnlineTextQuery("曲", "童年", artist = "罗大佑")).requiresReview })
        }
    }

    @Test fun `封面未收录是无结果而服务器异常可以重试`() = runTest {
        for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.ServiceUnavailable)) client({ request ->
            if (request.url.host == "musicbrainz.org") recordings to HttpStatusCode.OK else "" to status
        }) { http, _ ->
            val result = EditCloudMetaSearch(emptyList(), listOf(MusicBrainzCoverProvider(MusicBrainzClient(http))))
                .search(query, SearchOptions(platform = CloudPlatformId.MUSICBRAINZ))
            assertEquals(if (status == HttpStatusCode.NotFound) EditDimStatus.NO_MATCH else EditDimStatus.NETWORK, result.cover.status)
        }
    }

    @Test fun `LrcApi编码查询不传本机路径并拒绝错误页面`() = runTest {
        for (text in listOf("[00:01.00]歌词", "<html>服务器出错</html>", "Lyrics not found.")) client({ text to HttpStatusCode.OK }) { http, requests ->
            val result = EditCloudMetaSearch(emptyList(), listOf(LrcApiCoverProvider(http)), listOf(LrcApiLyricsPort(http)))
                .search(query, SearchOptions(platform = CloudPlatformId.LRCAPI, lyricsPlatform = CloudLyricsPlatformId.LRCAPI))
            assertEquals(if (text.startsWith("[")) 1 else 0, result.lyrics.items.size)
            assertTrue(result.cover.items.isEmpty())
            assertEquals(EditDimStatus.NETWORK, result.cover.status)
            assertTrue(requests.isNotEmpty())
            assertTrue(requests.all { it.url.parameters["artist"] == "罗大佑" })
            assertTrue(requests.none { it.url.parameters.contains("path") })
        }
    }

    @Test fun `LrcApi未收录不显示网络失败但拒绝访问保留错误`() = runTest {
        for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.Forbidden)) client({ "" to status }) { http, _ ->
            val result = EditCloudMetaSearch(emptyList(), emptyList(), listOf(LrcApiLyricsPort(http)))
                .search(query, SearchOptions(lyricsPlatform = CloudLyricsPlatformId.LRCAPI))
            assertEquals(if (status == HttpStatusCode.NotFound) EditDimStatus.NO_MATCH else EditDimStatus.NETWORK, result.lyrics.status)
        }
    }

    @Test fun `LrcApi封面通过图片头检查才列为候选且错误可重试`() = runTest {
        for (status in listOf(HttpStatusCode.OK, HttpStatusCode.NotFound, HttpStatusCode.Forbidden)) {
            client({ "GIF89a1234567890" to status }) { http, requests ->
                val result = EditCloudMetaSearch(emptyList(), listOf(LrcApiCoverProvider(http)))
                    .search(query, SearchOptions(platform = CloudPlatformId.LRCAPI))
                when (status) {
                    HttpStatusCode.OK -> assertEquals("lrcapi", result.cover.items.single().source.wire)
                    HttpStatusCode.NotFound -> assertEquals(EditDimStatus.NO_MATCH, result.cover.status)
                    else -> assertEquals(EditDimStatus.NETWORK, result.cover.status)
                }
                assertTrue(requests.all { it.url.encodedPath == "/cover" })
            }
        }
    }

    @Test fun `明确选择其他来源不会请求新增来源`() = runTest {
        client({ error("不应请求") }) { http, requests ->
            val result = EditCloudMetaSearch(listOf(ItunesTextProvider(http), MusicBrainzTextProvider(MusicBrainzClient(http))),
                listOf(LrcApiCoverProvider(http)), listOf(LrcApiLyricsPort(http)))
                .search(query, SearchOptions(platform = CloudPlatformId.WY, lyricsPlatform = CloudLyricsPlatformId.WY))
            assertTrue(result.text.items.isEmpty()); assertTrue(result.cover.items.isEmpty()); assertTrue(result.lyrics.items.isEmpty())
            assertTrue(requests.isEmpty())
        }
    }

    @Test fun `自托管地址可持久化并拒绝带凭据或参数的地址`() = runTest {
        val dataStore = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(data.value).also { data.value = it }
        }
        val settings = LrcApiSettingsStore(dataStore)
        assertEquals(DEFAULT_LRC_API_URL, settings.readBaseUrl())
        settings.setBaseUrl(" http://localhost:28883/prefix/ ")
        assertEquals("http://localhost:28883/prefix", LrcApiSettingsStore(dataStore).readBaseUrl())
        for (url in listOf("file:///music", "https://user:pass@test/", "https://test/?key=secret")) {
            assertFailsWith<IllegalArgumentException> { settings.setBaseUrl(url) }
        }
        settings.setBaseUrl(""); assertEquals(DEFAULT_LRC_API_URL, settings.readBaseUrl())
    }
}

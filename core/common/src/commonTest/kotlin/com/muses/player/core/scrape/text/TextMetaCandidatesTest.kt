package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.provider.*
import com.muses.player.core.webdav.WebDavRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TextMetaCandidatesTest {
    @Test fun `五个平台保留多条结果且目录名专辑不参与审核检索`() = runTest {
        val bodies = mapOf(
            "search.kuwo.cn" to """{"abslist":[{"SONGNAME":"曲名","ARTIST":"歌手","ALBUM":"真实专辑"},{"SONGNAME":"曲名 (Live)","ARTIST":"歌手","ALBUM":"现场专辑"}]}""",
            "music.163.com" to """{"result":{"songs":[{"name":"曲名","artists":[{"name":"歌手"}],"album":{"name":"真实专辑"}},{"name":"曲名 (Live)","artists":[{"name":"歌手"}],"album":{"name":"现场专辑"}}]}}""",
            "c.y.qq.com" to """{"data":{"song":{"list":[{"songname":"曲名","singer":[{"name":"歌手"}],"albumname":"真实专辑"},{"songname":"曲名 (Live)","singer":[{"name":"歌手"}],"albumname":"现场专辑"}]}}}""",
            "songsearch.kugou.com" to """{"data":{"lists":[{"SongName":"曲名","SingerName":"歌手","AlbumName":"真实专辑"},{"SongName":"曲名 (Live)","SingerName":"歌手","AlbumName":"现场专辑"}]}}""",
            "m.music.migu.cn" to """{"musics":[{"songName":"曲名","singerName":"歌手","albumName":"真实专辑"},{"songName":"曲名 (Live)","singerName":"歌手","albumName":"现场专辑"}]}""",
        )
        val client = HttpClient(MockEngine { request ->
            val keyword = listOf("all", "s", "w", "keyword").firstNotNullOf { request.url.parameters[it] }
            assertEquals("曲名 歌手", keyword)
            assertFalse(keyword.contains("我的音乐"))
            respond(bodies.getValue(request.url.host))
        })
        try {
            val http = ScrapeHttp(client, WebDavRateLimiter.Unlimited)
            val query = OnlineTextQuery(songId = "歌曲", title = "曲名", artist = "歌手", album = "我的音乐")
            for (provider in listOf(KwProvider(http), WyProvider(http), TxProvider(http), KgProvider(http), MgProvider(http))) {
                val candidates = provider.searchCandidates(query)
                assertEquals(listOf("曲名", "曲名 (Live)"), candidates.map { it.title })
            }
        } finally { client.close() }
    }
}

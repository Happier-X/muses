package com.muses.player.core.lyrics

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.provider.ScoreableHit
import com.muses.player.core.lyrics.provider.pickBest
import com.muses.player.core.lyrics.provider.platformId
import com.muses.player.core.lyrics.provider.searchWyLyrics
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.model.online.OnlineTrackRef
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformLyricsIdentityTest {
    private data class Hit(override val title: String?, override val artist: String?,
        override val album: String? = null, override val durationSec: Double? = null) : ScoreableHit

    @Test fun `无关歌曲同名翻唱和现场版都不能冒充嗜好`() {
        val query = OnlineLyricsQuery("online:wy:123", "嗜好", "颜人中")
        assertNull(pickBest(listOf(Hit("别的歌", "其他歌手")), query))
        assertNull(pickBest(listOf(Hit("嗜好", "其他歌手")), query))
        assertNull(pickBest(listOf(Hit("嗜好 (Live)", "颜人中")), query))
        val original = Hit("嗜好", "颜人中")
        assertEquals(original, pickBest(listOf(Hit("嗜好 (Live)", "颜人中"), original), query))
    }

    @Test fun `不同平台不能复用网易歌曲 ID`() {
        val query = OnlineLyricsQuery("track", "嗜好", trackRef = OnlineTrackRef("wy", """{"songmid":123}""", "source"))
        assertEquals("123", query.platformId("wy", "songmid"))
        assertNull(query.platformId("tx", "songmid"))
    }

    @Test fun `歌名歌手相同但时长不符也拒绝`() {
        val query = OnlineLyricsQuery("track", "嗜好", "颜人中", durationSec = 240.0)
        assertNull(pickBest(listOf(Hit("嗜好", "颜人中", durationSec = 160.0)), query))
    }

    @Test fun `网易原歌曲 ID 直接获取歌词不再搜索同名版本`() = runTest {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requests += request.url.toString()
            respond(if (request.url.toString().contains("/eapi/")) "{}"
                else """{"lrc":{"lyric":"[00:01.00]原平台歌词"}}""", HttpStatusCode.OK)
        })
        try {
            val hit = searchWyLyrics(LyricsHttp(client), OnlineLyricsQuery("track", "嗜好", "颜人中",
                trackRef = OnlineTrackRef("wy", """{"songmid":123}""", "source")))
            assertEquals("[00:01.00]原平台歌词", hit?.text)
            assertTrue(requests.none { it.contains("search") })
            assertTrue(requests.first().contains("/song/lyric"))
            assertTrue(requests.last().contains("id=123"))
        } finally { client.close() }
    }
}

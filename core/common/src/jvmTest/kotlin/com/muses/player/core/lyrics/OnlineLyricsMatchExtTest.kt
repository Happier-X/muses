package com.muses.player.core.lyrics

import com.muses.player.core.lyrics.amll.AmllIndexRepository
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.model.lyrics.AmllFailReason
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.AmllMatchResult
import com.muses.player.core.model.lyrics.LyricsProvider
import com.muses.player.core.model.lyrics.OnlineLyricsFormat
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.model.lyrics.OnlineLyricsSource
import com.muses.player.core.model.scrape.MatchConfidence
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [matchDocument] 测试：在线曲目歌词回退链的「匹配结果 → LyricsDocument」落地口径。
 *
 * 覆盖 AMLL 命中（TTML 走通用解析链）与平台源命中（LRC + timed 译文走合并链）两条路径。
 */
class OnlineLyricsMatchExtTest {

    private class FakeAmll(var result: AmllMatchResult = AmllMatchResult.Fail(AmllFailReason.NO_MATCH)) :
        AmllTtmlDbClient(http = LyricsHttp(), indexRepository = AmllIndexRepository { "" }) {
        override suspend fun match(query: AmllMatchQuery): AmllMatchResult = result
    }

    private class FakeProvider(
        override val id: OnlineLyricsSource,
        val hit: OnlineLyricsProviderHit?,
    ) : LyricsProvider {
        override suspend fun searchLyrics(query: OnlineLyricsQuery): OnlineLyricsProviderHit? = hit
    }

    private val ttml = """
        <tt xmlns="http://www.w3.org/ns/ttml">
          <body><div>
            <p begin="00:00.000" end="00:02.000">第一行</p>
            <p begin="00:02.000" end="00:04.000">第二行</p>
          </div></body>
        </tt>
    """.trimIndent()

    @Test
    fun `AMLL 命中 TTML 时解析成歌词文档`() = runTest {
        val matcher = LyricsMatcher(
            FakeAmll(
                AmllMatchResult.Ok(
                    ttml = ttml,
                    rawLyricFile = "a.ttml",
                    score = 120,
                    confidence = MatchConfidence.HIGH,
                ),
            ),
            emptyList(),
        )

        val document = matcher.matchDocument(
            songId = "online:kw:1",
            title = "测试曲",
            artist = "测试歌手",
        )
        assertNotNull(document)
        assertEquals(2, document!!.lines.size)
        assertEquals("第一行", document.lines[0].text)
        assertEquals(0L, document.lines[0].timeMs)
        assertEquals(2000L, document.lines[1].timeMs)
    }

    @Test
    fun `平台源命中时 LRC 与 timed 译文合并`() = runTest {
        val matcher = LyricsMatcher(
            FakeAmll(),
            listOf(
                FakeProvider(
                    OnlineLyricsSource.KW,
                    OnlineLyricsProviderHit(
                        text = "[00:01.000]Hello",
                        format = OnlineLyricsFormat.LRC,
                        translationText = "[00:01.000]你好",
                    ),
                ),
            ),
        )

        val document = matcher.matchDocument(songId = "online:kw:2", title = "Hello")
        assertNotNull(document)
        assertEquals("Hello", document!!.lines.first().text)
        assertEquals("你好", document.lines.first().translation)
    }

    @Test
    fun `全链未命中返回 null`() = runTest {
        val matcher = LyricsMatcher(FakeAmll(), listOf(FakeProvider(OnlineLyricsSource.KW, null)))
        assertNull(matcher.matchDocument(songId = "online:kw:3", title = "无此歌"))
    }

    @Test
    fun `时长秒与毫秒两种入参都能参与匹配`() = runTest {
        var seen: OnlineLyricsQuery? = null
        val provider = object : LyricsProvider {
            override val id = OnlineLyricsSource.LRCLIB
            override suspend fun searchLyrics(query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
                seen = query
                return null
            }
        }
        val matcher = LyricsMatcher(FakeAmll(), listOf(provider))

        matcher.matchDocument(songId = "s", title = "t", durationMs = 215_000L)
        assertEquals(215.0, seen?.durationSec)

        matcher.matchDocument(songId = "s", title = "t", durationSec = 180L)
        assertEquals(180.0, seen?.durationSec)
    }
}

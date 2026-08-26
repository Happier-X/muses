package com.muses.player.core.lyrics

import com.muses.player.core.model.lyrics.LyricsProvider
import com.muses.player.core.model.lyrics.OnlineLyricsFailReason
import com.muses.player.core.model.lyrics.OnlineLyricsFormat
import com.muses.player.core.model.lyrics.OnlineLyricsMatchResult
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import com.muses.player.core.model.lyrics.OnlineLyricsSource
import com.muses.player.core.model.scrape.MatchConfidence
import com.muses.player.core.lyrics.amll.AmllTtmlDbClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 规格 = src/features/lyrics/match.ts matchOnlineLyrics 主流程 */
class LyricsMatcherTest {

    private class FakeAmll(
        var result: com.muses.player.core.model.lyrics.AmllMatchResult =
            com.muses.player.core.model.lyrics.AmllMatchResult.Fail(com.muses.player.core.model.lyrics.AmllFailReason.NO_MATCH),
    ) : AmllTtmlDbClient(http = com.muses.player.core.lyrics.http.LyricsHttp(), indexRepository = com.muses.player.core.lyrics.amll.AmllIndexRepository { "" }) {
        override suspend fun match(query: com.muses.player.core.model.lyrics.AmllMatchQuery): com.muses.player.core.model.lyrics.AmllMatchResult {
            return result
        }
    }

    private class FakeProvider(
        override val id: OnlineLyricsSource,
        val result: (OnlineLyricsQuery) -> OnlineLyricsProviderHit? = { null },
    ) : LyricsProvider {
        var calls = 0
        override suspend fun searchLyrics(query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
            calls++
            return result(query)
        }
    }

    private fun query() = OnlineLyricsQuery(songId = "s1", title = "Love Story", artist = "Taylor")

    @Test
    fun `title或songId为空_no-match`() = runTest {
        val matcher = LyricsMatcher(FakeAmll(), emptyList())
        assertEquals(
            OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NO_MATCH),
            matcher.match(OnlineLyricsQuery(songId = "", title = "x")),
        )
    }

    @Test
    fun `amll命中即返回ttml且不再走fallback`() = runTest {
        val amll = FakeAmll().apply {
            result = com.muses.player.core.model.lyrics.AmllMatchResult.Ok(
                ttml = "<tt/>", rawLyricFile = "a.ttml", score = 125, confidence = MatchConfidence.HIGH,
            )
        }
        val fallback = FakeProvider(OnlineLyricsSource.KW) { OnlineLyricsProviderHit("lrc", OnlineLyricsFormat.LRC) }
        val matcher = LyricsMatcher(amll, listOf(fallback))

        val result = matcher.match(query())
        result as OnlineLyricsMatchResult.Ok
        assertEquals(OnlineLyricsFormat.TTML, result.format)
        assertEquals(OnlineLyricsSource.AMLL, result.source)
        assertEquals(MatchConfidence.HIGH, result.confidence)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `amll miss后fallback首个命中即停并携带译文`() = runTest {
        val kw = FakeProvider(OnlineLyricsSource.KW) {
            OnlineLyricsProviderHit(text = "[00:01.00]a", format = OnlineLyricsFormat.LRC, translationText = "[00:01.00]译")
        }
        val tx = FakeProvider(OnlineLyricsSource.TX)
        val matcher = LyricsMatcher(FakeAmll(), listOf(kw, tx))

        val result = matcher.match(query())
        result as OnlineLyricsMatchResult.Ok
        assertEquals(OnlineLyricsSource.KW, result.source)
        assertEquals("[00:01.00]译", result.translationText)
        assertEquals(0, tx.calls)
    }

    @Test
    fun `全miss归no-match`() = runTest {
        val matcher = LyricsMatcher(FakeAmll(), listOf(FakeProvider(OnlineLyricsSource.KW)))
        assertEquals(
            OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NO_MATCH),
            matcher.match(query()),
        )
    }

    @Test
    fun `amll网络失败加fallback异常归network`() = runTest {
        val amll = FakeAmll().apply {
            result = com.muses.player.core.model.lyrics.AmllMatchResult.Fail(com.muses.player.core.model.lyrics.AmllFailReason.NETWORK)
        }
        val failing = FakeProvider(OnlineLyricsSource.KW) { throw IllegalStateException("down") }
        val matcher = LyricsMatcher(amll, listOf(failing))
        assertEquals(
            OnlineLyricsMatchResult.Fail(OnlineLyricsFailReason.NETWORK),
            matcher.match(query()),
        )
    }
}

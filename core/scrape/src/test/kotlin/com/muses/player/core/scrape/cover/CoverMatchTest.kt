package com.muses.player.core.scrape.cover

import com.muses.player.core.scrape.cover.CoverTypesTest.Dummy
import com.muses.player.core.scrape.text.NegativeCache
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规格 = src/features/cover/match.ts matchOnlineCoverRemote 主流程 */
class CoverMatchTest {

    private class FakeProvider(
        override val id: OnlineCoverSource,
        val result: () -> String? = { null },
    ) : CoverProvider {
        var calls: Int = 0

        override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
            calls++
            return result()
        }
    }

    private fun query(
        songId: String = "s1",
        title: String = "Love Story",
        artist: String? = null,
        album: String? = null,
    ) = OnlineCoverQuery(songId = songId, title = title, artist = artist, album = album)

    @Test
    fun `title空白早退no-match且不调provider`() = runTest {
        val provider = FakeProvider(OnlineCoverSource.KW)
        val matcher = CoverMatcher(listOf(provider))
        val result = matcher.match(query(title = "   "))
        assertEquals(OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NO_MATCH), result)
        assertEquals(0, provider.calls)
    }

    @Test
    fun `链序首个命中返回且来源为命中provider`() = runTest {
        val kwMiss = FakeProvider(OnlineCoverSource.KW)
        val txHit = FakeProvider(OnlineCoverSource.TX) {
            "https://y.gtimg.cn/music/photo_new/T002R500x500M000abc.jpg"
        }
        val wyNever = FakeProvider(OnlineCoverSource.WY)
        val matcher = CoverMatcher(listOf(kwMiss, txHit, wyNever))

        val result = matcher.match(query())
        assertEquals(
            OnlineCoverMatchResult.Ok(
                remoteUrl = "https://y.gtimg.cn/music/photo_new/T002R500x500M000abc.jpg",
                source = OnlineCoverSource.TX,
            ),
            result,
        )
        assertEquals(1, kwMiss.calls)
        assertEquals(1, txHit.calls)
        assertEquals(0, wyNever.calls)
    }

    @Test
    fun `非http开头URL不算命中继续下一源`() = runTest {
        // 命中校验 /^https?:\/\//i：非 http(s) 开头跳过
        val badScheme = FakeProvider(OnlineCoverSource.KW) { "ftp://example.com/cover.jpg" }
        val good = FakeProvider(OnlineCoverSource.ITUNES) { "https://is1-ssl.mzstatic.com/600x600bb.jpg" }
        val matcher = CoverMatcher(listOf(badScheme, good))

        val result = matcher.match(query())
        assertTrue(result is OnlineCoverMatchResult.Ok)
        assertEquals(OnlineCoverSource.ITUNES, (result as OnlineCoverMatchResult.Ok).source)
        assertEquals(1, badScheme.calls)
    }

    @Test
    fun `全miss写负缓存且同queryKey二次调用短路`() = runTest {
        val miss = FakeProvider(OnlineCoverSource.KW)
        val matcher = CoverMatcher(listOf(miss))
        val q = query()

        val first = matcher.match(q)
        assertEquals(OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NO_MATCH), first)
        assertEquals(1, matcher.negativeCache.size())

        // 同 songId + 同 queryKey：负缓存短路，不再调用 provider
        val second = matcher.match(q)
        assertEquals(OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NO_MATCH), second)
        assertEquals(1, miss.calls)
    }

    @Test
    fun `queryKey变化不短路旧负缓存`() = runTest {
        val miss = FakeProvider(OnlineCoverSource.KW)
        val matcher = CoverMatcher(listOf(miss))
        matcher.match(query(songId = "s1", title = "A"))
        // 同 songId 但不同 query（album 变化）→ 负缓存不命中
        matcher.match(query(songId = "s1", title = "A", album = "B"))
        assertEquals(2, miss.calls)
    }

    @Test
    fun `过期负缓存不短路`() = runTest {
        val miss = FakeProvider(OnlineCoverSource.KW)
        val matcher = CoverMatcher(listOf(miss))
        val q = query()
        // 预写一条已过期的负缓存（expiresAt < now）
        matcher.negativeCache.put(
            q.songId,
            NegativeCache.NegativeEntry(queryKey = Dummy.key(q), expiresAt = 0L),
        )
        matcher.match(q)
        assertEquals(1, miss.calls)
    }

    @Test
    fun `网络异常归network不写负缓存以支持重试`() = runTest {
        val failing = FakeProvider(OnlineCoverSource.KW) { throw IOException("http 500") }
        val matcher = CoverMatcher(listOf(failing))
        val result = matcher.match(query())
        assertEquals(OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK), result)
        // 任务 08-27：NETWORK（含 429）不入负缓存，由限流器控频，支持稍后重试
        assertEquals(0, matcher.negativeCache.size())
    }

    @Test
    fun `部分网络异常部分miss归network`() = runTest {
        val failing = FakeProvider(OnlineCoverSource.KW) { throw IOException("timeout") }
        val miss = FakeProvider(OnlineCoverSource.MG)
        val matcher = CoverMatcher(listOf(failing, miss))
        val result = matcher.match(query())
        assertEquals(OnlineCoverMatchResult.Fail(OnlineCoverMatchFailReason.NETWORK), result)
        assertEquals(1, failing.calls)
        assertEquals(1, miss.calls)
    }
}

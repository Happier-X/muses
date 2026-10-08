package com.muses.player.core.media.playback

import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.parser.LxLyricParser
import com.muses.player.core.lyrics.hasPreciseDesktopLyrics
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackLyrics
import com.muses.player.core.model.online.OnlineTrackRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineSessionLyricsLoaderTest {
    private fun song() = Song(id = "online:wy:1", sourceId = "script",
        path = OnlineTrackRef("wy", "{}", "script").encode(), title = "测试歌",
        sourceType = SourceType.ONLINE)

    @Test
    fun `复用同曲页面文档不重复请求`() = runTest {
        val document = LxLyricParser.parse(lxlyric = "[00:01.000]<1000,1000>已有歌词")!!
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = { error("不应请求脚本") }, matchLyrics = { error("不应匹配") },
            sharedLyrics = { assertEquals(song().id, it); document },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals(listOf(document), received)
    }

    @Test
    fun `歌曲已有逐字歌词不被页面普通歌词降级`() = runTest {
        val source = song().copy(lyrics = "[1000,1000](1000,500,0)已有(1500,500,0)歌词")
        val page = LyricsDocument(listOf(LyricLine(1000, text = "已有歌词")))
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = { error("已有逐字不应请求脚本") },
            matchLyrics = { error("已有逐字不应匹配") }, sharedLyrics = { page },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(source) { received += it }
        assertEquals(1, received.size)
        assertTrue(received.single().hasPreciseDesktopLyrics())
    }

    @Test
    fun `页面普通歌词不阻断迟到的脚本逐字歌词`() = runTest {
        val page = LyricsDocument(listOf(LyricLine(1000, text = "已有歌词")))
        var requests = 0
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = {
                if (++requests == 1) OnlineTrackLyrics(lyric = "[00:01.000]已有歌词")
                else OnlineTrackLyrics(lxlyric = "[00:01.000]<1000,500>已有<1500,500>歌词")
            }, matchLyrics = { null }, sharedLyrics = { page },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals(2, requests)
        assertEquals(page, received.first())
        assertTrue(received.last().hasPreciseDesktopLyrics())
    }

    @Test
    fun `已有行歌词仍可采用匹配到的真实逐字文档`() = runTest {
        val page = LyricsDocument(listOf(LyricLine(1000, text = "已有歌词")))
        val precise = LxLyricParser.parse(lxlyric = "[00:01.000]<1000,500>已有<1500,500>歌词")!!
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = { OnlineTrackLyrics(lyric = "[00:01.000]已有歌词") },
            matchLyrics = { precise }, sharedLyrics = { page },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals(precise, received.last())
    }

    @Test
    fun `没有页面时服务自行获取脚本歌词`() = runTest {
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = { OnlineTrackLyrics(lyric = "[00:01.250]在线歌词") },
            matchLyrics = { null }, sharedLyrics = { null },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals("在线歌词", received.last().lines.single().text)
        assertEquals("[00:01.250]在线歌词", received.last().sessionLrc())
    }

    @Test
    fun `直链晚到后重试获得脚本缓存歌词`() = runTest {
        var requests = 0
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = { if (++requests == 1) null else OnlineTrackLyrics(lyric = "[00:02.000]迟到歌词") },
            matchLyrics = { null }, sharedLyrics = { null },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals(3, requests)
        assertEquals("迟到歌词", received.last().lines.single().text)
    }

    @Test
    fun `单次脚本异常不阻断后续重试`() = runTest {
        var requests = 0
        val loader = OnlineSessionLyricsLoader(
            resolveLyrics = {
                if (++requests == 1) error("临时网络故障")
                OnlineTrackLyrics(lyric = "[00:01.000]恢复歌词")
            }, matchLyrics = { null }, sharedLyrics = { null },
        )
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals("恢复歌词", received.last().lines.single().text)
    }

    @Test
    fun `取消上一曲请求后不发布歌词`() = runTest {
        val received = mutableListOf<LyricsDocument>()
        val loader = OnlineSessionLyricsLoader(resolveLyrics = { awaitCancellation() },
            matchLyrics = { error("取消后不应匹配") }, sharedLyrics = { null })
        val job = launch { loader.load(song()) { received += it } }
        runCurrent()
        job.cancelAndJoin()
        advanceUntilIdle()
        assertTrue(received.isEmpty())
    }

    @Test
    fun `脚本无结果时复用匹配文档且保留毫秒时间轴`() = runTest {
        val matched = LyricsDocument(listOf(LyricLine(61234, text = "匹配歌词")))
        val loader = OnlineSessionLyricsLoader(resolveLyrics = { null }, matchLyrics = { matched }, sharedLyrics = { null })
        val received = mutableListOf<LyricsDocument>()
        loader.load(song()) { received += it }
        assertEquals(matched, received.last())
        assertEquals("[01:01.234]匹配歌词", received.last().sessionLrc())
    }
}

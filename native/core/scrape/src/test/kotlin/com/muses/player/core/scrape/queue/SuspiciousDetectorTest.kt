package com.muses.player.core.scrape.queue

import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规格 = src/features/scrape/suspicious.ts 逐条规则 */
class SuspiciousDetectorTest {

    private fun song(
        id: String = "s1",
        title: String = "Love Story",
        path: String = "/music/Love Story.mp3",
        artist: String? = "Taylor Swift",
        album: String? = "Fearless",
        coverUri: String? = "file:///c.jpg",
        lyrics: String? = "[00:01.00]x",
        lyricsSource: LyricsSource? = LyricsSource.EMBEDDED,
        metaSources: MetaSources? = MetaSources(title = MetaFieldSource.EMBEDDED),
    ) = Song(
        id = id,
        sourceId = "src",
        path = path,
        title = title,
        artist = artist,
        album = album,
        coverUri = coverUri,
        lyrics = lyrics,
        lyricsSource = lyricsSource,
        metaSources = metaSources,
    )

    @Test
    fun `字段齐备不可疑`() {
        assertFalse(isSuspiciousSongForScrape(song()))
    }

    @Test
    fun `artist缺失可疑`() {
        assertTrue(isSuspiciousSongForScrape(song(artist = null)))
    }

    @Test
    fun `album缺失可疑`() {
        assertTrue(isSuspiciousSongForScrape(song(album = null)))
    }

    @Test
    fun `cover缺失可疑`() {
        assertTrue(isSuspiciousSongForScrape(song(coverUri = null)))
    }

    @Test
    fun `lyrics缺失或空白可疑`() {
        assertTrue(isSuspiciousSongForScrape(song(lyrics = null)))
        assertTrue(isSuspiciousSongForScrape(song(lyrics = "   ")))
    }

    @Test
    fun `歌词来源scrape或历史遗留online可疑`() {
        assertTrue(isSuspiciousSongForScrape(song(lyricsSource = LyricsSource.SCRAPE)))
        assertTrue(isSuspiciousSongForScrape(song(lyricsSource = LyricsSource.ONLINE)))
    }

    @Test
    fun `includeCloudSources关闭时来源规则不生效`() {
        val opts = SuspiciousSongOptions(includeCloudSources = false)
        val s = song(metaSources = MetaSources(title = MetaFieldSource.CLOUD), coverUri = null, lyrics = null)
        // 缺 cover/lyrics 仍命中规则 1（与来源无关）
        assertTrue(isSuspiciousSongForScrape(s, opts))
        // 仅来源 cloud 可疑信号的字段：关掉后不可疑
        val onlyCloud = song(
            coverUri = "file:///c.jpg",
            lyrics = "x",
            metaSources = MetaSources(title = MetaFieldSource.CLOUD),
        )
        assertFalse(isSuspiciousSongForScrape(onlyCloud, opts))
    }

    @Test
    fun `弱title配合缺cover才可疑`() {
        // 弱 title（=文件名占位）+ 字段齐备：不可疑
        val weakOnly = song(title = "Untitled", path = "/music/Untitled.mp3")
        assertTrue(isWeakTitleCheck(weakOnly))
        assertFalse(isSuspiciousSongForScrape(weakOnly))
        // 弱 title + 缺 cover → 可疑
        val weakNoCover = song(title = "Untitled", path = "/music/Untitled.mp3", coverUri = null)
        assertTrue(isSuspiciousSongForScrape(weakNoCover))
    }

    private fun isWeakTitleCheck(s: Song): Boolean =
        com.muses.player.core.scrape.text.isWeakTitle(s.title, s.path)

    @Test
    fun `批量筛选返回新列表`() {
        val good = song("good")
        val bad = song("bad", artist = null)
        val picked = pickSuspiciousSongs(listOf(good, bad))
        assertEquals(listOf("bad"), picked.map { it.id })
    }
}

package com.muses.player.core.data.index

import com.muses.player.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryIndexerTest {

    private fun song(
        id: String,
        title: String = id,
        artist: String? = "Artist",
        album: String? = "Album",
    ) = Song(id = id, sourceId = "s1", path = "/$id", title = title, artist = artist, album = album)

    @Test
    fun `同专辑歌曲归入同一专辑且计数正确`() {
        val indexes = LibraryIndexer.build(
            listOf(song("a"), song("b"), song("c")),
        )
        assertEquals(1, indexes.albums.size)
        assertEquals(3, indexes.albums[0].songCount)
        // 三首歌 → 三个 song-album 关联
        assertEquals(setOf("a", "b", "c"), indexes.songAlbumRefs.map { it.first }.toSet())
    }

    @Test
    fun `同名专辑但艺术家不同不合并`() {
        val indexes = LibraryIndexer.build(
            listOf(song("a", artist = "A"), song("b", artist = "B")),
        )
        assertEquals(2, indexes.albums.size)
    }

    @Test
    fun `无艺术家的专辑归入合辑`() {
        val indexes = LibraryIndexer.build(listOf(song("a", artist = null)))
        assertEquals(1, indexes.albums.size)
        assertEquals(LibraryIndexer.VARIOUS_ARTISTS, indexes.albums[0].artist)
    }

    @Test
    fun `多艺术家分隔符拆分并各建索引`() {
        val indexes = LibraryIndexer.build(
            listOf(song("a", artist = "周杰伦; 费玉清")),
        )
        assertEquals(2, indexes.artists.size)
        assertEquals(setOf("周杰伦", "费玉清"), indexes.artists.map { it.name }.toSet())
        // 两名艺术家都关联到歌曲 a
        assertEquals(2, indexes.songArtistRefs.count { it.first == "a" })
    }

    @Test
    fun `无专辑歌曲不产生专辑索引但仍计入艺术家`() {
        val indexes = LibraryIndexer.build(
            listOf(song("a", album = null)),
        )
        assertEquals(0, indexes.albums.size)
        assertEquals(0, indexes.songAlbumRefs.size)
        assertEquals(1, indexes.artists.size)
        assertEquals(1, indexes.artists[0].songCount)
        assertEquals(0, indexes.artists[0].albumCount)
    }

    @Test
    fun `艺术家专辑计数按去重专辑统计`() {
        val indexes = LibraryIndexer.build(
            listOf(
                song("a", artist = "X", album = "One"),
                song("b", artist = "X", album = "Two"),
                song("c", artist = "X", album = "One"),
            ),
        )
        val x = indexes.artists.single()
        assertEquals(3, x.songCount)
        assertEquals(2, x.albumCount)
    }

    @Test
    fun `索引 ID 跨次构建保持稳定`() {
        val first = LibraryIndexer.build(listOf(song("a")))
        val second = LibraryIndexer.build(listOf(song("a")))
        assertEquals(first.albums[0].id, second.albums[0].id)
        assertEquals(first.artists[0].id, second.artists[0].id)
    }

    @Test
    fun `空曲库产出空索引`() {
        val indexes = LibraryIndexer.build(emptyList())
        assertEquals(0, indexes.albums.size)
        assertEquals(0, indexes.artists.size)
    }
}

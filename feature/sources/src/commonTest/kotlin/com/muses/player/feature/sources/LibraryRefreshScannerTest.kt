package com.muses.player.feature.sources

import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class LibraryRefreshScannerTest {
    private fun source(id: String, type: SourceType) = Source(id, id, type, createdAt = 0, updatedAt = 0)

    private class Sources(private val entries: List<Source>) : SourceRepository {
        override fun observeSources() = flowOf(entries)
        override suspend fun getSource(id: String) = entries.find { it.id == id }
        override suspend fun upsert(source: Source) = error("测试不应修改音源")
        override suspend fun deleteById(id: String) = error("测试不应删除音源")
    }

    private class Songs : SongRepository {
        val merged = mutableListOf<String>()
        val stored = mutableMapOf<String, Song>()
        override fun observeSongs() = flowOf(stored.values.toList())
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult {
            merged += sourceId
            val added = songs.count { it.id !in stored }
            songs.forEach { stored.getOrPut(it.id) { it } }
            return ScanMergeResult(songs.size, added = added)
        }
        override suspend fun deleteSourceSongs(sourceId: String) = error("测试不应删除歌曲")
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun getSong(id: String) = stored[id]
        override suspend fun upsert(song: Song) = error("测试应使用扫描合并")
    }

    private class Port(private val read: suspend (Source) -> List<Song>) : LibraryScanPort {
        val scanned = mutableListOf<String>()
        override fun progressFor(type: SourceType) = flowOf(ScanProgress())
        override suspend fun scan(source: Source, readTags: Boolean): List<Song> {
            scanned += source.id
            return read(source)
        }
    }

    @Test
    fun refresh_discovers_new_songs_without_scanning_online_sources() = runTest {
        val entries = listOf(source("local", SourceType.LOCAL), source("dav", SourceType.WEBDAV), source("online", SourceType.ONLINE))
        val songs = Songs()
        val existing = Song("existing", "local", "/existing.mp3", "已有歌曲", lyrics = "已有歌词")
        songs.stored[existing.id] = existing
        val port = Port { listOf(Song("new-${it.id}", it.id, "/new.mp3", "新增歌曲")) }
        val scanner = LibraryRefreshScanner(Sources(entries), songs, port)
        assertEquals("刷新完成，新增 2 首歌曲", scanner.refresh())
        assertEquals(listOf("local", "dav"), port.scanned)
        assertEquals(listOf("local", "dav"), songs.merged)
        assertEquals(existing, songs.stored[existing.id])
        assertEquals("刷新完成，新增 0 首歌曲", scanner.refresh())
    }

    @Test
    fun failed_source_does_not_block_other_sources_or_merge_failed_results() = runTest {
        val songs = Songs()
        val port = Port {
            if (it.id == "failed") error("连接失败")
            listOf(Song("new", it.id, "/new.mp3", "新增歌曲"))
        }
        val scanner = LibraryRefreshScanner(Sources(listOf(source("failed", SourceType.WEBDAV), source("ok", SourceType.LOCAL))), songs, port)
        val result = scanner.refresh()
        assertTrue(result.contains("新增 1 首歌曲"))
        assertTrue(result.contains("failed：扫描失败"))
        assertEquals(listOf("ok"), songs.merged)
    }

    @Test
    fun cancellation_stops_scanning_without_merging_partial_result() = runTest {
        val songs = Songs()
        val port = Port { throw CancellationException() }
        val scanner = LibraryRefreshScanner(Sources(listOf(source("cancel", SourceType.LOCAL), source("later", SourceType.WEBDAV))), songs, port)
        assertFailsWith<CancellationException> { scanner.refresh() }
        assertEquals(listOf("cancel"), port.scanned)
        assertTrue(songs.merged.isEmpty())
    }
}

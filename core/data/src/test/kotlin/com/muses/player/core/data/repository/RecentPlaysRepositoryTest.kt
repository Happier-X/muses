package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.model.playback.RecentPlayEntry
import com.muses.player.core.model.playback.toRecentPlayEntry
import com.muses.player.core.model.playback.onlineSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 最近播放记录仓库测试（同曲去重置顶 / 半年保留 / 元数据快照） */
class RecentPlaysRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newRepo(): RecentPlaysRepository {
        val file = File(tmp.root, "recent_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return RecentPlaysRepository(dataStore)
    }

    private fun entry(id: String, playedAt: Long = System.currentTimeMillis()) = RecentPlayEntry(
        songId = id,
        title = "T-$id",
        subtitle = "Artist - Album",
        coverUri = "file:///c.jpg",
        playedAt = playedAt,
    )

    @Test
    fun `在线历史持久保存引用并可重建播放歌曲`() = runTest {
        val repo = newRepo()
        val path = com.muses.player.core.model.online.OnlineTrackRef(
            "wy", """{"songmid":"34383004"}""", "lx-source", "320k",
        ).encode()
        val song = com.muses.player.core.model.Song(
            id = "online:wy:34383004", sourceId = "lx-source", path = path,
            title = "示例歌曲", artist = "示例歌手", album = "示例专辑", durationMs = 223000,
            coverUri = "https://example.com/cover.jpg", sourceType = com.muses.player.core.model.SourceType.ONLINE,
        )
        repo.record(song.toRecentPlayEntry(System.currentTimeMillis()))
        val restored = repo.load().single().onlineSong()!!
        assertEquals(song, restored.copy(durationSec = song.durationSec))
        assertEquals(223L, restored.durationSec)
        assertEquals("320k", com.muses.player.core.model.online.OnlineTrackRef.parse(restored.path)!!.quality)
    }

    @Test
    fun `旧曲库历史保持兼容且不生成在线歌曲`() = runTest {
        val repo = newRepo()
        repo.record(entry("library"))
        org.junit.Assert.assertNull(repo.load().single().onlineSong())
    }

    @Test
    fun `同曲去重置顶`() = runTest {
        val repo = newRepo()
        val now = System.currentTimeMillis()
        repo.record(entry("a", now - 2_000))
        repo.record(entry("b", now - 1_000))
        repo.record(entry("a", now))

        val loaded = repo.load()
        assertEquals(listOf("a", "b"), loaded.map { it.songId })
        // 元数据快照保留
        assertEquals("T-a", loaded.first().title)
    }

    @Test
    fun `半年内记录不按数量裁剪`() = runTest {
        val repo = newRepo()
        val base = System.currentTimeMillis() - 60_000
        for (i in 1..60) {
            repo.record(entry("s%03d".format(i), base + i * 500L))
        }
        val loaded = repo.load()
        assertEquals(60, loaded.size)
        assertEquals("s060", loaded.first().songId)
        assertEquals("s001", loaded.last().songId)
    }

    @Test
    fun `写入新记录时移除半年以前的记录`() = runTest {
        val repo = newRepo()
        val now = System.currentTimeMillis()
        repo.record(entry("old", now - RecentPlaysRepository.RETENTION_MS - 1))
        repo.record(entry("new", now))

        assertEquals(listOf("new"), repo.load().map { it.songId })
    }

    @Test
    fun `clear清空记录`() = runTest {
        val repo = newRepo()
        repo.record(entry("a"))
        repo.clear()
        assertTrue(repo.load().isEmpty())
    }

    @Test
    fun `observe响应式读取最新在前`() = runTest {
        val repo = newRepo()
        repo.record(entry("a"))
        repo.record(entry("b"))
        val observed = repo.observe()
        org.junit.Assert.assertEquals(
            listOf("b", "a"),
            observed.first().map { it.songId },
        )
    }
}

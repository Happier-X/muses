package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.model.playback.RecentPlayEntry
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

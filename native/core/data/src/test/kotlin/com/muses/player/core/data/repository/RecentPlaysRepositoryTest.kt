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

/** 规格 = src/features/player/recent.ts（同曲去重置顶 / 上限 50 / 元数据快照） */
class RecentPlaysRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newRepo(): RecentPlaysRepository {
        val file = File(tmp.root, "recent_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return RecentPlaysRepository(dataStore)
    }

    private fun entry(id: String, playedAt: Long = 0L) = RecentPlayEntry(
        songId = id,
        title = "T-$id",
        subtitle = "Artist - Album",
        coverUri = "file:///c.jpg",
        playedAt = playedAt,
    )

    @Test
    fun `同曲去重置顶`() = runTest {
        val repo = newRepo()
        repo.record(entry("a", 1))
        repo.record(entry("b", 2))
        repo.record(entry("a", 3))

        val loaded = repo.load()
        assertEquals(listOf("a", "b"), loaded.map { it.songId })
        // 元数据快照保留
        assertEquals("T-a", loaded.first().title)
    }

    @Test
    fun `超限裁尾_只保留最新50条`() = runTest {
        val repo = newRepo()
        for (i in 1..60) {
            repo.record(entry("s%03d".format(i)))
        }
        val loaded = repo.load()
        assertEquals(50, loaded.size)
        assertEquals("s060", loaded.first().songId)
        assertEquals("s011", loaded.last().songId)
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

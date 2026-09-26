package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 听歌统计仓库：按日累计、按曲累计、月汇总与高频榜；
 * 累计值（total）独立于逐日明细，明细裁剪不影响累计口径。
 */
class PlayStatsRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newRepo(): PlayStatsRepository {
        val file = File(tmp.root, "stats_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return PlayStatsRepository(dataStore)
    }

    @Test
    fun `登记播放同时累计当日与单曲次数`() = runTest {
        val repo = newRepo()
        repo.recordPlay("s1", "歌一", "歌手 - 专辑", "file:///c1.jpg", "2026-09-26")
        repo.recordPlay("s1", "歌一", "歌手 - 专辑", "file:///c1.jpg", "2026-09-26")
        repo.recordPlay("s2", "歌二", "", null, "2026-09-27")

        val stats = repo.load()
        assertEquals(3, stats.totalPlayCount)
        assertEquals(2, stats.day("2026-09-26").playCount)
        assertEquals(1, stats.day("2026-09-27").playCount)
        assertEquals(2, stats.songs.getValue("s1").playCount)
        assertEquals("歌一", stats.songs.getValue("s1").title)
        assertEquals("file:///c1.jpg", stats.songs.getValue("s1").coverUri)
    }

    @Test
    fun `听歌时长按日累计且忽略非正增量`() = runTest {
        val repo = newRepo()
        repo.addListenMs(60_000L, "2026-09-26")
        repo.addListenMs(30_000L, "2026-09-26")
        repo.addListenMs(0L, "2026-09-26")
        repo.addListenMs(-5L, "2026-09-26")

        val stats = repo.load()
        assertEquals(90_000L, stats.totalListenMs)
        assertEquals(90_000L, stats.day("2026-09-26").listenMs)
    }

    @Test
    fun `月汇总给出听歌天数时长与次数`() = runTest {
        val repo = newRepo()
        repo.recordPlay("s1", "A", "", null, "2026-09-01")
        repo.addListenMs(120_000L, "2026-09-01")
        repo.recordPlay("s2", "B", "", null, "2026-09-20")
        repo.recordPlay("s3", "C", "", null, "2026-08-31")

        val stats = repo.load()
        val month = stats.month("2026-09")
        assertEquals(2, month.listeningDays)
        assertEquals(2, month.playCount)
        assertEquals(120_000L, month.listenMs)
        assertEquals(2, stats.monthDays("2026-09").size)
        assertEquals("2026-08", stats.earliestMonth)
    }

    @Test
    fun `高频歌曲按次数降序取前十`() = runTest {
        val repo = newRepo()
        for (index in 1..12) {
            repeat(index) {
                repo.recordPlay("s$index", "曲$index", "", null, "2026-09-10")
            }
        }

        val top = repo.load().topSongs(10)
        assertEquals(10, top.size)
        assertEquals("s12", top.first().songId)
        assertEquals(12, top.first().playCount)
    }

    @Test
    fun `清空统计`() = runTest {
        val repo = newRepo()
        repo.recordPlay("s1", "A", "", null, "2026-09-26")
        repo.addListenMs(1_000L, "2026-09-26")

        repo.clear()

        val stats = repo.load()
        assertEquals(0, stats.totalPlayCount)
        assertEquals(0L, stats.totalListenMs)
        assertTrue(stats.days.isEmpty())
        assertTrue(stats.songs.isEmpty())
    }
}


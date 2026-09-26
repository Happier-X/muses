package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 听歌时长埋点内核：只累计播放中的墙上时间——暂停结算、恢复续段、切歌分段，
 * 次数只在 onSongStarted 计一次（重复上报状态不会重复计数）。
 */
class PlayStatsSessionTrackerTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    /** 假时钟：所有断言基于手动推进的毫秒数，避免真实等待 */
    private var clock = 1_700_000_000_000L

    private val day = "2026-09-26"

    private fun newTracker(): Pair<PlayStatsSessionTracker, PlayStatsRepository> {
        val file = File(tmp.root, "stats_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        val repository = PlayStatsRepository(dataStore)
        return PlayStatsSessionTracker(repository, now = { clock }, dayOf = { day }) to repository
    }

    @Test
    fun `暂停时段不计入听歌时长`() = runTest {
        val (tracker, repository) = newTracker()
        tracker.onSongStarted("s1", "A", "艺术家", null, isPlaying = true)
        clock += 60_000L
        tracker.onPlaybackState("s1", isPlaying = false)
        clock += 300_000L
        tracker.onPlaybackState("s1", isPlaying = true)
        clock += 30_000L
        tracker.flush()

        val stats = repository.load()
        assertEquals(90_000L, stats.day(day).listenMs)
        assertEquals(90_000L, stats.totalListenMs)
        assertEquals(1, stats.totalPlayCount)
    }

    @Test
    fun `节拍结算写入后继续累计不重复`() = runTest {
        val (tracker, repository) = newTracker()
        tracker.onSongStarted("s1", "A", "", null, isPlaying = true)

        clock += PlayStatsSessionTracker.TICK_MS
        tracker.checkpoint()
        assertEquals(PlayStatsSessionTracker.TICK_MS, repository.load().totalListenMs)

        clock += PlayStatsSessionTracker.TICK_MS
        tracker.checkpoint()
        assertEquals(PlayStatsSessionTracker.TICK_MS * 2, repository.load().totalListenMs)
    }

    @Test
    fun `切歌结算上一段并累计新曲次数`() = runTest {
        val (tracker, repository) = newTracker()
        tracker.onSongStarted("s1", "A", "", null, isPlaying = true)
        clock += 10_000L
        tracker.onSongStarted("s2", "B", "", null, isPlaying = true)
        clock += 5_000L
        tracker.flush()

        val stats = repository.load()
        assertEquals(15_000L, stats.day(day).listenMs)
        assertEquals(1, stats.songs.getValue("s1").playCount)
        assertEquals(1, stats.songs.getValue("s2").playCount)
        assertEquals(2, stats.totalPlayCount)
    }

    @Test
    fun `停止播放后再上报不计时长`() = runTest {
        val (tracker, repository) = newTracker()
        tracker.onSongStarted("s1", "A", "", null, isPlaying = true)
        clock += 20_000L
        tracker.onPlaybackState("s1", isPlaying = false)
        clock += 60_000L
        tracker.onPlaybackState(null, isPlaying = false)
        tracker.flush()

        assertEquals(20_000L, repository.load().totalListenMs)
    }
}


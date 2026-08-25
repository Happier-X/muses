package com.muses.player.core.scrape.queue

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import com.muses.player.core.model.scrape.ScrapeHistoryEntry
import com.muses.player.core.model.scrape.WritebackStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 规格 = src/features/scrape/history.ts（滚动 200 条 / at 倒序 / 歌名快照） */
class ScrapeHistoryStoreTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newStore(nowIso: () -> String = { java.time.Instant.now().toString() }): ScrapeHistoryStore {
        val file = File(tmp.root, "history_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return ScrapeHistoryStore(dataStore, nowIso = nowIso)
    }

    private fun entry(journalId: String, status: WritebackStatus = WritebackStatus.SUCCESS) =
        ScrapeHistoryEntry(
            id = "",
            journalId = journalId,
            songId = "s-$journalId",
            songTitle = "歌名快照 $journalId",
            songArtist = "Artist",
            at = "",
            status = status,
            failureReason = if (status == WritebackStatus.SUCCESS) null else "写回失败",
            changedFields = listOf("title", "cover"),
        )

    @Test
    fun `追加补id与at_按at倒序返回`() = runTest {
        var tick = 0
        val store = newStore { java.time.Instant.ofEpochSecond(1_700_000_000L + (tick++)).toString() }

        store.append(listOf(entry("j1")))
        store.append(listOf(entry("j2")))

        val loaded = store.load()
        assertEquals(2, loaded.size)
        // 最新在前；补 id/at 生效
        assertEquals("j2", loaded.first().journalId)
        assertTrue(loaded.all { it.id.isNotEmpty() && it.at.isNotEmpty() })
    }

    @Test
    fun `滚动清理只保留最新200条`() = runTest {
        var tick = 0
        val store = newStore { java.time.Instant.ofEpochSecond(1_700_000_000L + (tick++)).toString() }
        // 分 250 批追加
        for (i in 1..250) {
            store.append(listOf(entry("j$i")))
        }
        val loaded = store.load()
        assertEquals(200, loaded.size)
        // 最新在前：j250 在首，j51 是最旧保留项
        assertEquals("j250", loaded.first().journalId)
        assertEquals("j51", loaded.last().journalId)
    }

    @Test
    fun `空批次不写回`() = runTest {
        val store = newStore()
        store.append(emptyList())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `clear清空历史`() = runTest {
        val store = newStore()
        store.append(listOf(entry("j1")))
        store.clear()
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `坏数据宽松回退空表`() {
        org.junit.Assert.assertNull(
            com.muses.player.core.scrape.writeback.WritebackJson.decodeHistory("oops{"),
        )
    }
}

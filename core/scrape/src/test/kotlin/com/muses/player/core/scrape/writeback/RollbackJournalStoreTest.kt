package com.muses.player.core.scrape.writeback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import com.muses.player.core.model.scrape.RollbackEntry
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.RollbackSongSnapshot
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 规格 = src/features/scrape/writeback.ts 回滚 journal 存储语义（DataStore 替换 localStorage） */
class RollbackJournalStoreTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newStore(): RollbackJournalStore {
        val file = File(tmp.root, "test_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)) { file }
        return RollbackJournalStore(dataStore)
    }

    private fun entry(songId: String) = RollbackEntry(
        songId = songId,
        songBefore = RollbackSongSnapshot(
            title = "T-$songId",
            artist = "A",
            album = null,
            coverUri = "file:///c.jpg",
            lyrics = null,
            lyricsFormat = LyricsFormat.LRC,
            lyricsSource = LyricsSource.EMBEDDED,
            metaSources = MetaSources(title = MetaFieldSource.EMBEDDED),
        ),
        createdAt = "2026-08-25T00:00:00.000Z",
    )

    @Test
    fun `空存储读取返回null`() = runTest {
        assertNull(newStore().read())
    }

    @Test
    fun `写入后读回roundtrip一致`() = runTest {
        val store = newStore()
        val journal = RollbackJournal(version = 1, journalId = "journal-42", entries = listOf(entry("s1"), entry("s2")))
        store.write(journal)

        val read = store.read()
        assertEquals(journal, read)
        assertEquals("journal-42", read?.journalId)
        assertEquals(2, read?.entries?.size)
        assertEquals(LyricsFormat.LRC, read?.entries?.first()?.songBefore?.lyricsFormat)
        assertEquals(MetaFieldSource.EMBEDDED, read?.entries?.first()?.songBefore?.metaSources?.title)
    }

    @Test
    fun `clear后读取返回null`() = runTest {
        val store = newStore()
        store.write(RollbackJournal(1, "j", listOf(entry("s1"))))
        store.clear()
        assertNull(store.read())
    }

    @Test
    fun `坏数据宽松回退null不抛错`() {
        assertNull(WritebackJson.decodeJournal("not-json{"))
        assertNull(WritebackJson.decodeJournal("""{"foo":1}"""))
        assertNull(WritebackJson.decodeJournal(""))
    }

    @Test
    fun `坏条目跳过好条目保留`() {
        // 第二条缺 songBefore → 跳过；第一条完整 → 保留
        val raw = """
            {"version":"1","journalId":"j","entries":[
              {"songId":"s1","createdAt":"t","songBefore":{"title":"ok"}},
              {"songId":"s2","createdAt":"t"}
            ]}
        """.trimIndent()
        val journal = WritebackJson.decodeJournal(raw)
        assertEquals(1, journal?.entries?.size)
        assertEquals("s1", journal?.entries?.first()?.songId)
    }
}

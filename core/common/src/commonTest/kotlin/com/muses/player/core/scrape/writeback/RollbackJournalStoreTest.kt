package com.muses.player.core.scrape.writeback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.LyricsFormat
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.MetaSources
import com.muses.player.core.model.scrape.RollbackEntry
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.RollbackSongSnapshot
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** 规格 = src/features/scrape/writeback.ts 回滚 journal 存储语义（DataStore 替换 localStorage） */
class RollbackJournalStoreTest {

    private var testDir: File? = null

    @BeforeTest
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "rbtest-${System.nanoTime()}").apply { mkdirs() }
    }

    @AfterTest
    fun tearDown() {
        testDir?.deleteRecursively()
        testDir = null
    }

    private fun newStore(): RollbackJournalStore {
        val file = File(testDir!!, "test_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
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
    fun `撤销字段范围与封面写入标记持久化且兼容旧记录`() = runTest {
        val store = newStore()
        val scoped = entry("s1").copy(changedFields = setOf("title", "cover"), coverWritten = false, fileSaved = false)
        val journal = RollbackJournal(1, "scoped", listOf(scoped, entry("s2"), entry("s3").copy(changedFields = emptySet())))
        store.write(journal)
        assertEquals(journal, store.read())
        val legacy = WritebackJson.decodeJournal("""{"version":1,"journalId":"old","entries":[{"songId":"s1","songBefore":{"title":"旧标题"}}]}""")
        assertNull(legacy?.entries?.single()?.changedFields)
        assertEquals(true, legacy?.entries?.single()?.coverWritten)
        assertEquals(true, legacy?.entries?.single()?.fileSaved)
    }

    @Test
    fun `损坏字段范围不会降级为旧版整组恢复`() {
        val journal = WritebackJson.decodeJournal("""{"version":1,"journalId":"bad","entries":[{"songId":"s1","changedFields":"title","songBefore":{"title":"旧标题"}}]}""")
        assertEquals(emptyList(), journal?.entries)
    }

    @Test
    fun `损坏的写入标记按未确认处理`() {
        val journal = WritebackJson.decodeJournal("""{"version":1,"journalId":"bad","entries":[{"songId":"s1","fileSaved":{},"coverWritten":null,"songBefore":{"title":"旧标题"}}]}""")
        val entry = assertNotNull(journal?.entries?.single())
        assertEquals(false, entry.fileSaved)
        assertEquals(false, entry.coverWritten)
    }

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
    fun `条件清空只删除匹配记录`() = runTest {
        val store = newStore()
        assertEquals(false, store.clearIfMatches("旧记录"))
        val journal = RollbackJournal(1, "当前记录", listOf(entry("s1")))
        store.write(journal)
        assertEquals(false, store.clearIfMatches("旧记录"))
        assertEquals(journal, store.read())
        assertEquals(true, store.clearIfMatches("当前记录"))
        assertNull(store.read())
    }

    @Test
    fun `忽略操作等待存储期间新写入的记录不会被清空`() = runTest {
        val file = File(testDir!!, "并发.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val entered = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        val delayedStore = object : DataStore<Preferences> {
            override val data = dataStore.data
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                entered.complete(Unit)
                resume.await()
                return dataStore.updateData(transform)
            }
        }
        val store = RollbackJournalStore(dataStore)
        store.write(RollbackJournal(1, "旧记录", listOf(entry("s1"))))
        val orchestrator = WritebackOrchestrator(
            songRepository = object : SongRepository {
                override fun observeSongs() = error("本测试不读取曲库")
                override suspend fun getSong(id: String) = error("本测试不读取曲库")
                override suspend fun upsert(song: Song) = error("本测试不写曲库")
                override suspend fun rebuildDerivedIndexes() = Unit
                override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>) = error("本测试不扫描")
                override suspend fun deleteSourceSongs(sourceId: String) = error("本测试不删除歌曲")
            },
            journalStore = RollbackJournalStore(delayedStore),
            fileWriter = AudioTagFileWriter { _, _, _ -> error("本测试不写文件") },
        )
        val discard = launch { assertEquals(false, orchestrator.discardRollbackJournal("旧记录")) }
        entered.await()
        val newer = RollbackJournal(1, "新记录", listOf(entry("s2")))
        store.write(newer)
        resume.complete(Unit)
        discard.join()
        assertEquals(newer, store.read())
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
    @Test fun `历史跨存储重读保留最近二十批且过滤未确认记录`() = runTest {
        val store = newStore()
        repeat(25) { number ->
            val pending = RollbackJournal(1, "批次-$number", listOf(entry("歌曲").copy(fileSaved = false)))
            store.write(pending)
            store.write(pending.copy(entries = listOf(entry("歌曲").copy(coverSnapshot = "", fileSnapshotKnown = true))))
        }
        val history = store.readHistory()
        assertEquals(20, history.size)
        assertEquals((24 downTo 5).map { "批次-$it" }, history.map { it.journalId })
        assertEquals(history, store.readHistory())
        assertEquals("", history.first().entries.single().coverSnapshot)
        assertEquals(true, history.first().entries.single().fileSnapshotKnown)
        store.write(RollbackJournal(1, "未确认", listOf(entry("未知").copy(fileSaved = false))))
        store.write(RollbackJournal(1, "新的未确认", listOf(entry("未知").copy(fileSaved = false))))
        assertEquals(false, store.readHistory().any { it.journalId in setOf("未确认", "新的未确认") })
    }

}

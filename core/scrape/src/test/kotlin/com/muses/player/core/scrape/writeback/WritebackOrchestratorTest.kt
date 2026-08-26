package com.muses.player.core.scrape.writeback

import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.media.metadata.TagWriter
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackStatus
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 规格 = src/features/scrape/writeback.ts applyScrapeChanges / revertScrapeJournal 主流程 */
class WritebackOrchestratorTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    // ── fakes ──────────────────────────────────────────────

    private class InMemorySongRepository(initial: List<Song>) : SongRepository {
        val songs = initial.associateBy { it.id }.toMutableMap()
        val upserts = mutableListOf<Song>()

        override fun observeSongs() = throw UnsupportedOperationException()
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>) =
            throw UnsupportedOperationException()

        override suspend fun deleteSourceSongs(sourceId: String) {
            songs.entries.removeIf { it.value.sourceId == sourceId }
        }

        override suspend fun rebuildDerivedIndexes() = Unit

        override suspend fun getSong(id: String): Song? = songs[id]
        override suspend fun upsert(song: Song) {
            songs[song.id] = song
            upserts.add(song)
        }
    }

    private fun newJournalStore(): RollbackJournalStore {
        val file = File(tmp.root, "journal_${System.nanoTime()}.preferences_pb")
        val dataStore = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO),
        ) { file }
        return RollbackJournalStore(dataStore)
    }

    private class FakeWriter(
        var behavior: (Song) -> FileWriteResult,
    ) : AudioTagFileWriter {
        val calls = mutableListOf<String>()
        val orderLock = Object()

        override suspend fun write(song: Song, request: TagWriter.TagWriteRequest): FileWriteResult {
            synchronized(orderLock) { calls.add(song.id) }
            return behavior(song)
        }
    }

    private fun song(id: String, sourceType: SourceType = SourceType.LOCAL, title: String = "T-$id") = Song(
        id = id,
        sourceId = "src-1",
        path = "/music/$id.mp3",
        title = title,
        artist = "Artist",
        album = "Album",
        sourceType = sourceType,
    )

    private fun orchestrator(
        repo: InMemorySongRepository,
        writer: FakeWriter,
        historySink: suspend (List<com.muses.player.core.model.scrape.ScrapeHistoryEntry>) -> Unit = { },
    ) = WritebackOrchestrator(
        songRepository = repo,
        journalStore = newJournalStore(),
        fileWriter = writer,
        historySink = historySink,
    )

    private fun candidates(vararg songs: Song) = songs.map { ScrapeCandidate(it.id, it) }

    @Test
    fun `写文件成功_status_success且库标记embedded`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(
            candidates = candidates(song("s1")),
            checkedIds = setOf("s1"),
            changesMap = mapOf("s1" to ScrapeChanges(title = "New Title", lyrics = "[00:01.00]x")),
        )

        assertEquals(WritebackStatus.SUCCESS, result.results.single().status)
        assertTrue(result.results.single().libraryUpdated)

        val updated = repo.songs.getValue("s1")
        assertEquals("New Title", updated.title)
        assertEquals("[00:01.00]x", updated.lyrics)
        assertEquals(MetaFieldSource.EMBEDDED, updated.metaSources?.title)
        assertEquals(com.muses.player.core.model.scrape.LyricsSource.EMBEDDED, updated.lyricsSource)
        assertNotNull(result.journalId)
    }

    @Test
    fun `写文件失败_status_file_failed但库已更新并标记scrape`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { FileWriteResult(ok = false, code = "write_failed", message = "boom") }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(artist = "New Artist")),
        )

        assertEquals(WritebackStatus.FILE_FAILED, result.results.single().status)
        assertTrue(result.results.single().libraryUpdated)
        assertEquals(MetaFieldSource.SCRAPE, repo.songs.getValue("s1").metaSources?.artist)
    }

    @Test
    fun `写回抛异常_status_failed库不更新`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { throw IllegalStateException("io broken") }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(album = "X")),
        )

        val row = result.results.single()
        assertEquals(WritebackStatus.FAILED, row.status)
        assertTrue(!row.libraryUpdated)
        assertNull(row.fileResult.takeIf { it.ok })
        assertEquals("Album", repo.songs.getValue("s1").album) // 未被改写
    }

    @Test
    fun `未勾选的candidate不参与快照与写回`() = runTest {
        val s1 = song("s1"); val s2 = song("s2")
        val repo = InMemorySongRepository(listOf(s1, s2))
        val writer = FakeWriter { FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(candidates(s1, s2), setOf("s1"), emptyMap())

        assertEquals(1, result.results.size)
        assertEquals("s1", result.results.single().songId)
        val journal = orch.getCurrentRollbackJournal()
        assertEquals(listOf("s1"), journal?.entries?.map { it.songId })
    }

    @Test
    fun `本地与webdav混排_逐行独立结果`() = runTest {
        val local = song("local-1", SourceType.LOCAL)
        val webdav = song("wd-1", SourceType.WEBDAV)
        val repo = InMemorySongRepository(listOf(local, webdav))
        // WebDAV 条目抛错不影响本地条目
        val writer = FakeWriter { if (it.sourceType == SourceType.WEBDAV) throw IllegalStateException("dav down") else FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(
            candidates(local, webdav), setOf("local-1", "wd-1"),
            mapOf("local-1" to ScrapeChanges(title = "A"), "wd-1" to ScrapeChanges(title = "B")),
        )

        assertEquals(2, result.results.size)
        assertEquals(
            setOf(WritebackStatus.SUCCESS, WritebackStatus.FAILED),
            result.results.map { it.status }.toSet(),
        )
    }

    @Test
    fun `快照截断到上限200条`() = runTest {
        val songs = (1..250).map { song("s$it") }
        val repo = InMemorySongRepository(songs)
        val writer = FakeWriter { FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)

        orch.applyScrapeChanges(
            songs.map { ScrapeCandidate(it.id, it) },
            checkedIds = songs.map { it.id }.toSet(),
            changesMap = emptyMap(),
        )

        assertEquals(200, orch.getCurrentRollbackJournal()?.entries?.size)
        // 保留最新的 200 条（前 50 条被截断）
        assertEquals("s51", orch.getCurrentRollbackJournal()?.entries?.first()?.songId)
    }

    @Test
    fun `revert恢复曲库旧值并清空journal`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)

        val apply = orch.applyScrapeChanges(
            candidates(original), setOf("s1"),
            mapOf("s1" to ScrapeChanges(title = "Changed", artist = null)),
        )
        assertEquals("Changed", repo.songs.getValue("s1").title)

        val revert = orch.revertScrapeJournal(apply.journalId)
        assertEquals(1, revert.reverted)
        assertEquals("T-s1", repo.songs.getValue("s1").title)
        assertNull(orch.getCurrentRollbackJournal())
    }

    @Test
    fun `revert_journalId不匹配不动作`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { FileWriteResult(ok = true) }
        val orch = orchestrator(repo, writer)
        orch.applyScrapeChanges(candidates(song("s1")), setOf("s1"), mapOf("s1" to ScrapeChanges(title = "C")))

        val revert = orch.revertScrapeJournal("wrong-id")
        assertEquals(0, revert.reverted)
        assertEquals("C", repo.songs.getValue("s1").title)
        assertNotNull(orch.getCurrentRollbackJournal())
    }

    @Test
    fun `历史旁路收到条目且changedFields正确归并`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { FileWriteResult(ok = false, code = "no_password") }
        val received = mutableListOf<com.muses.player.core.model.scrape.ScrapeHistoryEntry>()
        val orch = orchestrator(repo, writer) { entries -> received.addAll(entries) }

        orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(title = "T", coverUri = "https://c.jpg", lyrics = "l")),
        )

        assertEquals(1, received.size)
        val entry = received.single()
        assertEquals(WritebackStatus.FILE_FAILED, entry.status)
        // coverRemoteUrl/coverUri → cover；lyrics/lyricsFormat → lyrics
        assertEquals(setOf("title", "cover", "lyrics"), entry.changedFields.toSet())
        // no_password 已知 code → 固定文案
        assertEquals("WebDAV 密码缺失，请到音源设置补全后重试", entry.failureReason)
        assertEquals("T-s1", entry.songTitle) // 歌名快照来自 candidate.song
    }

    @Test
    fun `远程封面字节成功时传入标签请求`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val captured = mutableListOf<TagWriter.TagWriteRequest>()
        val writer = AudioTagFileWriter { _, request ->
            captured.add(request); FileWriteResult(ok = true)
        }
        val fakeFetcher = CoverBytesFetcher { "https://fake".toByteArray() }
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer, coverBytesFetcher = fakeFetcher)

        orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(coverUri = "https://remote/c.jpg", coverRemoteUrl = "https://remote/c.jpg")),
        )

        assertEquals(1, captured.size)
        assertTrue(captured.single().coverBytes != null)
        // 库内 coverUri 保留远端地址（非清空）
        assertEquals("https://remote/c.jpg", repo.songs.getValue("s1").coverUri)
    }
}

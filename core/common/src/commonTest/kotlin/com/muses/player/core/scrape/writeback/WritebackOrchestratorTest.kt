package com.muses.player.core.scrape.writeback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.MetaFieldSource
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackStatus
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * 规格 = src/features/scrape/writeback.ts applyScrapeChanges / revertScrapeJournal 主流程。
 * W3 随实现迁 :core:common commonTest；TagPort 收口后 FakeWriter 捕获 ScrapeChanges/封面字节。
 */
class WritebackOrchestratorTest {

    private var testDir: File? = null

    @BeforeTest
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "wbtest-${System.nanoTime()}").apply { mkdirs() }
    }

    @AfterTest
    fun tearDown() {
        testDir?.deleteRecursively()
        testDir = null
    }

    // ── fakes ──────────────────────────────────────────────

    private class InMemorySongRepository(initial: List<Song>) : SongRepository {
        val songs = initial.associateBy { it.id }.toMutableMap()
        val upserts = mutableListOf<Song>()
        var failUpsert = false
        val failedReads = mutableSetOf<String>()
        val failedWrites = mutableSetOf<String>()

        override fun observeSongs() = throw UnsupportedOperationException()
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult =
            throw UnsupportedOperationException()

        override suspend fun deleteSourceSongs(sourceId: String) {
            songs.entries.removeIf { it.value.sourceId == sourceId }
        }

        override suspend fun rebuildDerivedIndexes() = Unit

        override suspend fun getSong(id: String): Song? {
            check(id !in failedReads) { "模拟单曲读取失败" }
            return songs[id]
        }
        override suspend fun upsert(song: Song) {
            check(!failUpsert) { "模拟曲库写入失败" }
            check(song.id !in failedWrites) { "模拟单曲更新失败" }
            songs[song.id] = song
            upserts.add(song)
        }
    }

    private fun newJournalStore(): RollbackJournalStore {
        val file = File(testDir!!, "journal_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO),
        ) { file }
        return RollbackJournalStore(dataStore)
    }

    private class FakeWriter(
        var behavior: (Song) -> FileWriteResult,
    ) : AudioTagFileWriter {
        val calls = mutableListOf<String>()
        val orderLock = Object()

        override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
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
    fun `写回期间其他写回恢复或忽略均不能改文件曲库或记录`() = exclusiveOperation(duringRestore = false)

    @Test
    fun `恢复期间其他写回恢复或忽略均不能改文件曲库或记录`() = exclusiveOperation(duringRestore = true)

    @Test
    fun `取消正在进行的操作后可以重新写回`() = exclusiveOperation(duringRestore = false, cancelActive = true)

    private fun exclusiveOperation(duringRestore: Boolean, cancelActive: Boolean = false) = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val entered = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        var blockWrites = false
        var fileCalls = 0
        val writer = AudioTagFileWriter { _, _, _ ->
            fileCalls += 1
            if (blockWrites) {
                entered.complete(Unit)
                resume.await()
            }
            FileWriteResult(true)
        }
        val orch = WritebackOrchestrator(repo, store, writer)
        val initial = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "首批标题")))
        blockWrites = true
        val active = async {
            if (duringRestore) orch.revertScrapeJournal(initial.journalId)
            else orch.applyScrapeChanges(candidates(repo.songs.getValue(original.id)), setOf(original.id),
                mapOf(original.id to ScrapeChanges(title = "处理中标题")))
        }
        entered.await()
        val journal = assertNotNull(store.read())
        val savedSong = repo.songs.getValue(original.id)
        val calls = fileCalls
        assertFailsWith<WritebackInProgressException> {
            orch.applyScrapeChanges(candidates(savedSong), setOf(original.id),
                mapOf(original.id to ScrapeChanges(title = "其他页面标题")))
        }
        assertFailsWith<WritebackInProgressException> { orch.revertScrapeJournal(journal.journalId) }
        assertFailsWith<WritebackInProgressException> { orch.discardRollbackJournal(journal.journalId) }
        assertEquals(journal, store.read())
        assertEquals(savedSong, repo.songs.getValue(original.id))
        assertEquals(calls, fileCalls)
        if (cancelActive) active.cancelAndJoin()
        else {
            resume.complete(Unit)
            active.await()
        }
        blockWrites = false
        val retried = orch.applyScrapeChanges(candidates(repo.songs.getValue(original.id)), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "重试标题")))
        assertEquals(WritebackStatus.SUCCESS, retried.results.single().status)
        assertEquals("重试标题", repo.songs.getValue(original.id).title)
    }

    @Test
    fun `时钟不变时连续写回的旧入口不能恢复或忽略新记录`() = journalIdsDoNotDependOnClock(recreate = false)

    @Test
    fun `时钟不变且重建编排器后旧入口仍不能操作新记录`() = journalIdsDoNotDependOnClock(recreate = true)

    private fun journalIdsDoNotDependOnClock(recreate: Boolean) = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = FakeWriter { FileWriteResult(true) }
        fun create() = WritebackOrchestrator(repo, store, writer, nowMs = { 1_700_000_000_000L })
        val firstOrchestrator = create()
        val first = firstOrchestrator.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "第一次标题")))
        val secondOrchestrator = if (recreate) create() else firstOrchestrator
        val second = secondOrchestrator.applyScrapeChanges(candidates(repo.songs.getValue(original.id)),
            setOf(original.id), mapOf(original.id to ScrapeChanges(title = "第二次标题")))
        assertNotEquals(first.journalId, second.journalId)
        val journal = assertNotNull(store.read())
        writer.calls.clear()
        repo.upserts.clear()
        assertEquals(0, firstOrchestrator.revertScrapeJournal(first.journalId).reverted)
        assertEquals(false, firstOrchestrator.discardRollbackJournal(first.journalId))
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals("第二次标题", repo.songs.getValue(original.id).title)
        assertEquals(journal, store.read())
        assertEquals(1, secondOrchestrator.revertScrapeJournal(second.journalId).reverted)
        assertEquals("第一次标题", repo.songs.getValue(original.id).title)
        assertNull(store.read())
    }

    @Test
    fun `旧时间戳编号的持久化记录仍可恢复`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val orch = WritebackOrchestrator(repo, store, FakeWriter { FileWriteResult(true) })
        orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "已保存标题")))
        val legacy = assertNotNull(store.read()).copy(journalId = "journal-1700000000000")
        store.write(legacy)
        val reloaded = WritebackOrchestrator(repo, store, FakeWriter { FileWriteResult(true) })
        assertEquals(1, reloaded.revertScrapeJournal(legacy.journalId).reverted)
        assertEquals(original, repo.songs.getValue(original.id))
        assertNull(store.read())
    }

    @Test
    fun `放弃撤销记录只对当前记录生效`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val orch = WritebackOrchestrator(repo, store, FakeWriter { FileWriteResult(true) })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        assertEquals(false, orch.discardRollbackJournal("wrong-id"))
        assertNotNull(store.read())
        assertEquals(true, orch.discardRollbackJournal(applied.journalId))
        assertNull(store.read())
        assertEquals(false, orch.discardRollbackJournal(applied.journalId))
    }

    @Test
    fun `封面未内嵌的结果标注coverSkipped而内嵌与清空不标注`() = runTest {
        val cases = listOf(
            Triple("内嵌", "https://covers/ok.png", false),
            Triple("下载失败", "https://covers/missing.png", true),
            Triple("下载异常", "https://covers/error.png", true),
        )
        for ((name, url, expectedSkipped) in cases) {
            val original = song("s1")
            val repo = InMemorySongRepository(listOf(original))
            val orch = WritebackOrchestrator(repo, newJournalStore(), FakeWriter { FileWriteResult(true) },
                coverBytesFetcher = CoverBytesFetcher {
                    when {
                        it.endsWith("ok.png") -> byteArrayOf(1, 2, 3)
                        it.endsWith("error.png") -> throw java.io.IOException("封面数据超过大小上限")
                        else -> null
                    }
                })
            val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
                mapOf(original.id to ScrapeChanges(title = "新标题", coverUri = url)))
            assertEquals(WritebackStatus.SUCCESS, result.results.single().status, name)
            assertEquals(expectedSkipped, result.results.single().coverSkipped, name)
        }

        val withCover = song("s2").copy(coverUri = "https://old/cover.png")
        val repo = InMemorySongRepository(listOf(withCover))
        val orch = WritebackOrchestrator(repo, newJournalStore(), FakeWriter { FileWriteResult(true) },
            coverBytesFetcher = CoverBytesFetcher { null })
        val cleared = orch.applyScrapeChanges(candidates(withCover), setOf(withCover.id),
            mapOf(withCover.id to ScrapeChanges(coverUri = "")))
        assertEquals(WritebackStatus.SUCCESS, cleared.results.single().status)
        assertEquals(false, cleared.results.single().coverSkipped)
    }

    @Test
    fun `批量本地保存最多两首并行且失败不阻断后续和远程串行`() = runTest {
        val originals = (1..6).map { song("l$it") } + (1..2).map { song("w$it", SourceType.WEBDAV) }
        val repo = InMemorySongRepository(originals)
        val written = mutableListOf<String>()
        var localActive = 0
        var localPeak = 0
        var remoteActive = 0
        var remotePeak = 0
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { saved, _, _ ->
            written.add(saved.id)
            if (saved.sourceType == SourceType.WEBDAV) {
                assertEquals(0, localActive)
                remoteActive++
                remotePeak = maxOf(remotePeak, remoteActive)
                delay(5)
                remoteActive--
            }
            val failed = saved.id == "l2"
            FileWriteResult(ok = !failed, code = if (failed) "write_failed" else null)
        }, coverBytesFetcher = CoverBytesFetcher { url ->
            val id = url.removePrefix("https://covers/")
            if (id.startsWith("l")) {
                localActive++
                localPeak = maxOf(localPeak, localActive)
                try { delay(10) } finally { localActive-- }
            }
            byteArrayOf(1, 2, 3)
        })

        val result = orch.applyScrapeChanges(
            originals.map { ScrapeCandidate(it.id, it) },
            originals.map { it.id }.toSet(),
            originals.associate { it.id to ScrapeChanges(title = "新标题", coverUri = "https://covers/${it.id}") },
        )

        assertEquals(2, localPeak)
        assertEquals(1, remotePeak)
        assertEquals(originals.map { it.id }, result.results.map { it.songId })
        assertEquals(originals.map { it.id }.toSet(), written.toSet())
        assertEquals(WritebackStatus.FILE_FAILED, result.results.single { it.songId == "l2" }.status)
        assertEquals(originals[1], repo.songs.getValue("l2"))
        assertTrue(result.results.filter { it.songId != "l2" }.all { it.status == WritebackStatus.SUCCESS })
        assertTrue(orch.getCurrentRollbackJournal()?.entries.orEmpty().none { it.songId == "l2" })
    }

    @Test
    fun `取消批量保存时等待许可的歌曲不再读取封面或写文件`() = runTest {
        val originals = (1..6).map { song("l$it") }
        val repo = InMemorySongRepository(originals)
        val gate = CompletableDeferred<Unit>()
        val started = Channel<String>(Channel.UNLIMITED)
        val fetched = mutableListOf<String>()
        val written = mutableListOf<String>()
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { saved, _, _ ->
            written.add(saved.id)
            FileWriteResult(true)
        }, coverBytesFetcher = CoverBytesFetcher { url ->
            fetched.add(url)
            started.send(url)
            gate.await()
            byteArrayOf(1, 2, 3)
        })

        val pending = async {
            orch.applyScrapeChanges(
                originals.map { ScrapeCandidate(it.id, it) },
                originals.map { it.id }.toSet(),
                originals.associate { it.id to ScrapeChanges(coverUri = "https://covers/${it.id}") },
            )
        }
        started.receive()
        started.receive()
        pending.cancelAndJoin()

        assertEquals(2, fetched.size)
        assertTrue(written.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        val entries = assertNotNull(orch.getCurrentRollbackJournal()).entries
        assertEquals(originals.map { it.id }.toSet(), entries.map { it.songId }.toSet())
        assertTrue(entries.all { !it.fileSaved })
    }

    @Test
    fun `本地文件地址封面能内嵌且超限时仅保存文本并保留展示地址`() = runTest {
        val file = File(testDir!!, "中文 封面 #%+.png")
        val bytes = byteArrayOf(1, 2, 3, 4)
        file.writeBytes(bytes)
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<Pair<ScrapeChanges, ByteArray?>>()
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { _, changes, cover ->
            captured.add(changes to cover); FileWriteResult(true)
        }, coverBytesFetcher = CoverBytesFetcher { error("本地封面不能调用网络下载") })
        val uri = file.toURI().toASCIIString()
        orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(coverUri = uri)))
        assertTrue(bytes.contentEquals(captured.single().second))
        assertEquals(MetaFieldSource.EMBEDDED, repo.songs.getValue(original.id).metaSources?.cover)

        java.io.RandomAccessFile(file, "rw").use {
            it.setLength(com.muses.player.core.scrape.http.ScrapeHttp.MAX_COVER_BYTES.toLong() + 1)
        }
        val current = repo.songs.getValue(original.id)
        orch.applyScrapeChanges(candidates(current), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题", coverUri = uri)))
        assertEquals(ScrapeChanges(title = "新标题"), captured.last().first)
        assertNull(captured.last().second)
        assertEquals(uri, repo.songs.getValue(original.id).coverUri)
        assertEquals(MetaFieldSource.SCRAPE, repo.songs.getValue(original.id).metaSources?.cover)
        assertEquals(false, orch.getCurrentRollbackJournal()?.entries?.single()?.coverWritten)
    }

    @Test
    fun `撤销旧封面下载失败保留真实音频与曲库并允许重试`() = runTest {
        val image = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
        for (failure in listOf("无数据", "空数据", "网络异常", "错误页", "无效签名")) {
            val file = File(testDir!!, "$failure.mp3")
            file.outputStream().use { output ->
                val frame = ByteArray(417)
                frame[0] = 0xff.toByte(); frame[1] = 0xfb.toByte(); frame[2] = 0x90.toByte()
                repeat(500) { output.write(frame) }
            }
            val original = song("s1").copy(path = file.absolutePath, coverUri = "https://old/cover.png")
            val port = com.muses.player.core.scrape.ports.JaudiotaggerTagPort
            assertTrue(port.writeAndVerify(file, ScrapeChanges(title = original.title,
                coverUri = original.coverUri), image).ok)
            val repo = InMemorySongRepository(listOf(original))
            var oldCoverAvailable = false
            val orch = WritebackOrchestrator(repo, newJournalStore(), LocalAudioTagFileWriter(port),
                coverBytesFetcher = CoverBytesFetcher { url ->
                    if (url != original.coverUri || oldCoverAvailable) image
                    else when (failure) {
                        "无数据" -> null
                        "空数据" -> byteArrayOf()
                        "错误页" -> "<html>封面地址已失效</html>".toByteArray()
                        "无效签名" -> byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)
                        else -> throw java.io.IOException("模拟旧封面下载失败")
                    }
                })
            val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
                mapOf(original.id to ScrapeChanges(title = "新标题", coverUri = "https://new/cover.png")))
            assertEquals(WritebackStatus.SUCCESS, applied.results.single().status, failure)
            val savedFile = file.readBytes()
            val savedSong = repo.songs.getValue(original.id)
            val savedJournal = orch.getCurrentRollbackJournal()
            repo.upserts.clear()

            val failed = orch.revertScrapeJournal(applied.journalId)
            assertEquals(0, failed.reverted, failure)
            assertEquals(1, failed.failed, failure)
            assertTrue(savedFile.contentEquals(file.readBytes()), failure)
            assertEquals(savedSong, repo.songs.getValue(original.id), failure)
            assertEquals(savedJournal, orch.getCurrentRollbackJournal(), failure)
            assertTrue(repo.upserts.isEmpty(), failure)

            oldCoverAvailable = true
            val retried = orch.revertScrapeJournal(applied.journalId)
            assertEquals(1, retried.reverted, failure)
            assertEquals(0, retried.failed, failure)
            assertEquals(original, repo.songs.getValue(original.id), failure)
            assertEquals(original.title, port.readTags(file)?.title, failure)
            assertTrue(image.contentEquals(port.readTags(file)?.cover), failure)
            assertNull(orch.getCurrentRollbackJournal(), failure)
        }
    }

    @Test
    fun `撤销下载旧封面取消不写文件曲库且保留快照`() = runTest {
        val original = song("s1").copy(coverUri = "https://old/cover.png")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(true) }
        var cancelDownload = false
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            coverBytesFetcher = CoverBytesFetcher {
                if (cancelDownload) throw CancellationException("模拟撤销下载取消")
                byteArrayOf(1, 2, 3)
            })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题", coverUri = "https://new/cover.png")))
        val journal = orch.getCurrentRollbackJournal()
        val saved = repo.songs.getValue(original.id)
        cancelDownload = true
        writer.calls.clear()
        repo.upserts.clear()
        assertFailsWith<CancellationException> { orch.revertScrapeJournal(applied.journalId) }
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals(saved, repo.songs.getValue(original.id))
        assertEquals(journal, orch.getCurrentRollbackJournal())
    }

    @Test
    fun `仅撤销标题不写其他文件标签且保留之后的未涉及曲库修改`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<ScrapeChanges>()
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { _, changes, _ ->
            captured.add(changes); FileWriteResult(true)
        }, coverBytesFetcher = CoverBytesFetcher { error("标题操作不应下载封面") })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        val later = repo.songs.getValue(original.id).copy(artist = "后来修改的歌手", album = "后来修改的专辑",
            coverUri = "https://later/cover.jpg", lyrics = "后来修改的歌词",
            lyricsFormat = com.muses.player.core.model.scrape.LyricsFormat.TTML,
            lyricsSource = com.muses.player.core.model.scrape.LyricsSource.SIDECAR,
            metaSources = com.muses.player.core.model.scrape.MetaSources(title = MetaFieldSource.EMBEDDED,
                artist = MetaFieldSource.SCRAPE, album = MetaFieldSource.SCRAPE, cover = MetaFieldSource.SCRAPE))
        repo.songs[original.id] = later
        assertEquals(setOf("title"), orch.getCurrentRollbackJournal()?.entries?.single()?.changedFields)
        assertEquals(1, orch.revertScrapeJournal(applied.journalId).reverted)
        assertEquals(ScrapeChanges(title = original.title), captured.last())
        assertEquals(later.copy(title = original.title, metaSources = later.metaSources?.copy(title = null)),
            repo.songs.getValue(original.id))
    }

    @Test
    fun `封面未内嵌时撤销只还原展示地址而不改写文件封面`() = runTest {
        val original = song("s1").copy(coverUri = "https://old/cover.jpg")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<ScrapeChanges>()
        var downloads = 0
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { _, changes, _ ->
            captured.add(changes); FileWriteResult(true)
        }, coverBytesFetcher = CoverBytesFetcher { downloads++; null })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题", coverRemoteUrl = "https://new/cover.jpg")))
        val entry = orch.getCurrentRollbackJournal()?.entries?.single()
        assertEquals(setOf("title", "cover"), entry?.changedFields)
        assertEquals(false, entry?.coverWritten)
        assertEquals(1, orch.revertScrapeJournal(applied.journalId).reverted)
        assertEquals(1, downloads)
        assertEquals(ScrapeChanges(title = original.title), captured.last())
        assertEquals(original, repo.songs.getValue(original.id))
    }

    @Test
    fun `仅更新封面展示地址的撤销不产生空文件写入`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            coverBytesFetcher = CoverBytesFetcher { null })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(coverRemoteUrl = "https://new/cover.jpg")))
        assertEquals(1, writer.calls.size)
        assertEquals(1, orch.revertScrapeJournal(applied.journalId).reverted)
        assertEquals(1, writer.calls.size)
        assertEquals(original, repo.songs.getValue(original.id))
    }

    @Test
    fun `写回缓存刷新取消后不再更新曲库或处理后续远程歌曲`() = runTest {
        val originals = listOf(song("s1", SourceType.WEBDAV), song("s2", SourceType.WEBDAV))
        val repo = InMemorySongRepository(originals)
        val writer = FakeWriter { FileWriteResult(true) }
        var historyCalled = false
        var cancelCacheRefresh = true
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            historySink = { historyCalled = true },
            audioTagCacheInvalidator = {
                if (cancelCacheRefresh) throw CancellationException("模拟缓存刷新取消")
            })
        assertFailsWith<CancellationException> {
            orch.applyScrapeChanges(originals.map { ScrapeCandidate(it.id, it) },
                originals.map { it.id }.toSet(), originals.associate { it.id to ScrapeChanges(title = "新标题") })
        }
        assertEquals(listOf("s1"), writer.calls)
        assertTrue(repo.upserts.isEmpty())
        assertTrue(!historyCalled)
        val entries = orch.getCurrentRollbackJournal()?.entries.orEmpty()
        assertEquals(listOf("s1", "s2"), entries.map { it.songId })
        assertEquals(true, entries.single { it.songId == "s1" }.fileSaved)
        assertEquals(false, entries.single { it.songId == "s2" }.fileSaved)
        cancelCacheRefresh = false
        writer.calls.clear()
        val later = originals[1].copy(title = "外部修改的标题")
        repo.songs[later.id] = later
        val reverted = orch.revertScrapeJournal(assertNotNull(orch.getCurrentRollbackJournal()).journalId)
        assertEquals(1, reverted.reverted)
        assertEquals(1, reverted.failed)
        assertEquals(1, reverted.unconfirmed)
        assertEquals(listOf("s1"), writer.calls)
        assertEquals(later, repo.songs[later.id])
        assertEquals(listOf("s2"), orch.getCurrentRollbackJournal()?.entries?.map { it.songId })
    }

    @Test
    fun `未确认的写前快照禁止自动恢复且保留记录`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(true) }
        val store = newJournalStore()
        val orch = WritebackOrchestrator(repo, store, writer)
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        val journal = assertNotNull(orch.getCurrentRollbackJournal())
        store.write(journal.copy(entries = journal.entries.map { it.copy(fileSaved = false) }))
        val later = repo.songs.getValue(original.id).copy(title = "中断后外部修改")
        repo.songs[original.id] = later
        writer.calls.clear()
        repo.upserts.clear()

        val result = orch.revertScrapeJournal(applied.journalId)
        assertEquals(0, result.reverted)
        assertEquals(1, result.failed)
        assertEquals(1, result.unconfirmed)
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals(later, repo.songs[original.id])
        assertEquals(false, orch.getCurrentRollbackJournal()?.entries?.single()?.fileSaved)
    }

    @Test
    fun `写回历史记录取消会传播且已保存歌曲仍可撤销`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val orch = orchestrator(repo, FakeWriter { FileWriteResult(true) }) {
            throw CancellationException("模拟历史记录取消")
        }
        assertFailsWith<CancellationException> {
            orch.applyScrapeChanges(candidates(original), setOf(original.id),
                mapOf(original.id to ScrapeChanges(title = "新标题")))
        }
        assertEquals("新标题", repo.songs.getValue(original.id).title)
        val journal = assertNotNull(orch.getCurrentRollbackJournal())
        assertEquals(original.title, journal.entries.single().songBefore.title)
        assertEquals(1, orch.revertScrapeJournal(journal.journalId).reverted)
        assertEquals(original.title, repo.songs.getValue(original.id).title)
    }

    @Test
    fun `缓存刷新和历史记录普通异常仍不影响写回成功`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        var historyCalled = false
        val orch = WritebackOrchestrator(repo, newJournalStore(), FakeWriter { FileWriteResult(true) },
            historySink = { historyCalled = true; error("模拟历史记录失败") },
            audioTagCacheInvalidator = { error("模拟缓存刷新失败") })
        val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        assertTrue(historyCalled)
        assertEquals(WritebackStatus.SUCCESS, result.results.single().status)
        assertTrue(result.results.single().libraryUpdated)
        assertEquals("新标题", repo.songs.getValue(original.id).title)
        assertEquals(original.id, orch.getCurrentRollbackJournal()?.entries?.single()?.songId)
    }

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
    fun `文件已保存但曲库异常时保留真实结果历史与撤销快照`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original)).apply { failUpsert = true }
        val history = mutableListOf<com.muses.player.core.model.scrape.ScrapeHistoryEntry>()
        val orch = orchestrator(repo, FakeWriter { FileWriteResult(true, message = "文件保存完成") }) {
            history.addAll(it)
        }
        val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        val row = result.results.single()
        assertEquals(WritebackStatus.FAILED, row.status)
        assertTrue(row.fileResult.ok)
        assertTrue(!row.libraryUpdated)
        assertEquals("文件标签已保存，但曲库更新失败，请重试", row.error)
        assertEquals(row.error, history.single().failureReason)
        assertEquals(original, repo.songs.getValue(original.id))
        assertEquals(original.title, orch.getCurrentRollbackJournal()?.entries?.single()?.songBefore?.title)

        repo.failUpsert = false
        val reverted = orch.revertScrapeJournal(result.journalId)
        assertEquals(1, reverted.reverted)
        assertEquals(original.title, repo.songs.getValue(original.id).title)
    }

    @Test
    fun `旧候选写回使用当前曲库路径来源和快照且撤销恢复最新旧值`() = runTest {
        val stale = song("s1", title = "审核时标题")
        val current = stale.copy(title = "后来更新的标题", album = "后来更新的专辑",
            path = "/迁移后的歌曲.mp3", sourceType = SourceType.WEBDAV)
        val repo = InMemorySongRepository(listOf(current))
        val received = mutableListOf<Song>()
        val history = mutableListOf<com.muses.player.core.model.scrape.ScrapeHistoryEntry>()
        val orch = WritebackOrchestrator(repo, newJournalStore(), AudioTagFileWriter { saved, _, _ ->
            received.add(saved)
            FileWriteResult(true)
        }, historySink = { history.addAll(it) })
        val result = orch.applyScrapeChanges(candidates(stale), setOf(stale.id),
            mapOf(stale.id to ScrapeChanges(title = "本次确认标题")))
        assertEquals(WritebackStatus.SUCCESS, result.results.single().status)
        assertEquals(current, received.single())
        assertEquals(current.title, orch.getCurrentRollbackJournal()?.entries?.single()?.songBefore?.title)
        assertEquals(current.title, history.single().songTitle)
        assertEquals(current.album, repo.songs.getValue(stale.id).album)
        assertEquals(1, orch.revertScrapeJournal(result.journalId).reverted)
        assertEquals(current, repo.songs.getValue(stale.id))
    }

    @Test
    fun `写前歌曲删除或读取失败不写该首文件且其余歌曲继续保存`() = runTest {
        val stale = listOf(song("读取失败"), song("已删除"), song("可保存"))
        val repo = InMemorySongRepository(stale.filter { it.id != "已删除" })
        repo.failedReads.add("读取失败")
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = orchestrator(repo, writer)
        val result = orch.applyScrapeChanges(stale.map { ScrapeCandidate(it.id, it) },
            stale.map { it.id }.toSet(), stale.associate { it.id to ScrapeChanges(title = "新标题") })
        assertEquals(3, result.results.size)
        assertEquals("song_read_failed", result.results.single { it.songId == "读取失败" }.fileResult.code)
        assertEquals("song_missing", result.results.single { it.songId == "已删除" }.fileResult.code)
        assertTrue(result.results.filter { it.songId != "可保存" }.all {
            it.status == WritebackStatus.FAILED && !it.fileResult.ok && !it.libraryUpdated
        })
        assertEquals(WritebackStatus.SUCCESS, result.results.single { it.songId == "可保存" }.status)
        assertEquals(listOf("可保存"), writer.calls)
        assertEquals(listOf("可保存"), repo.upserts.map { it.id })
        assertEquals(listOf("可保存"), orch.getCurrentRollbackJournal()?.entries?.map { it.songId })
    }

    @Test
    fun `全部歌曲无法读取时保留上次撤销记录且不写文件曲库`() = runTest {
        val originals = listOf(song("s1"), song("s2"))
        val repo = InMemorySongRepository(originals)
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = orchestrator(repo, writer)
        orch.applyScrapeChanges(candidates(originals.first()), setOf("s1"),
            mapOf("s1" to ScrapeChanges(title = "已保存标题")))
        val journal = assertNotNull(orch.getCurrentRollbackJournal())
        writer.calls.clear()
        repo.upserts.clear()
        repo.failedReads.add("s1")
        repo.songs.remove("s2")
        val result = orch.applyScrapeChanges(originals.map { ScrapeCandidate(it.id, it) },
            setOf("s1", "s2"), originals.associate { it.id to ScrapeChanges(title = "本次标题") })
        assertEquals("", result.journalId)
        assertEquals(2, result.results.size)
        assertTrue(result.results.all { it.status == WritebackStatus.FAILED && !it.fileResult.ok })
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals(journal, orch.getCurrentRollbackJournal())
        assertEquals("已保存标题", repo.songs.getValue("s1").title)
    }

    @Test
    fun `文件保存后歌曲不在曲库时不误报成功且保留快照`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val orch = orchestrator(repo, FakeWriter {
            repo.songs.remove(it.id)
            FileWriteResult(true)
        })
        val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        val row = result.results.single()
        assertEquals(WritebackStatus.FAILED, row.status)
        assertTrue(row.fileResult.ok)
        assertTrue(!row.libraryUpdated)
        assertTrue(repo.upserts.isEmpty())
        assertEquals(original.id, orch.getCurrentRollbackJournal()?.entries?.single()?.songId)
        val reverted = orch.revertScrapeJournal(result.journalId)
        assertEquals(0, reverted.reverted)
        assertEquals(1, reverted.failed)
        assertEquals(original.id, orch.getCurrentRollbackJournal()?.entries?.single()?.songId)
    }

    @Test
    fun `写文件失败保留曲库原值`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val writer = FakeWriter { FileWriteResult(ok = false, code = "write_failed", message = "boom") }
        val orch = orchestrator(repo, writer)

        val result = orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(artist = "New Artist")),
        )

        assertEquals(WritebackStatus.FILE_FAILED, result.results.single().status)
        assertTrue(!result.results.single().libraryUpdated)
        assertEquals(song("s1"), repo.songs.getValue("s1"))
    }

    @Test
    fun `待补传结果不覆盖已保存曲库`() = runTest {
        val original = song("s1", SourceType.WEBDAV)
        val repo = InMemorySongRepository(listOf(original))
        val orch = orchestrator(repo, FakeWriter { FileWriteResult(false, "pending_upload", "待上传") })
        val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "待上传标题", lyrics = "待上传歌词")))
        assertEquals(original, repo.songs.getValue(original.id))
        assertTrue(!result.results.single().libraryUpdated)
    }

    @Test
    fun `恢复标签失败也不只还原数据库`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = orchestrator(repo, writer)
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "已保存标题")))
        writer.behavior = { FileWriteResult(false, "write_failed") }
        val result = orch.revertScrapeJournal(applied.journalId)
        assertEquals(1, result.failed)
        assertEquals("已保存标题", repo.songs.getValue(original.id).title)
        assertNotNull(orch.getCurrentRollbackJournal())
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
    fun `撤销全部成功时不会清空期间产生的新记录`() = revertKeepsNewJournal(partialFailure = false)

    @Test
    fun `撤销部分失败时不会用剩余项覆盖期间产生的新记录`() = revertKeepsNewJournal(partialFailure = true)

    private fun revertKeepsNewJournal(partialFailure: Boolean) = runTest {
        for (sameId in listOf(false, true)) {
            val originals = listOf(song("s1"), song("s2"))
            val repo = InMemorySongRepository(originals)
            val store = newJournalStore()
            val started = CompletableDeferred<Unit>()
            val resume = CompletableDeferred<Unit>()
            var reverting = false
            val writer = AudioTagFileWriter { saved, _, _ ->
                if (reverting && saved.id == "s1") {
                    started.complete(Unit)
                    resume.await()
                }
                FileWriteResult(ok = !(reverting && partialFailure && saved.id == "s2"))
            }
            val orch = WritebackOrchestrator(repo, store, writer)
            val applied = orch.applyScrapeChanges(originals.map { ScrapeCandidate(it.id, it) },
                originals.map { it.id }.toSet(), originals.associate { it.id to ScrapeChanges(title = "新标题") })
            val before = assertNotNull(store.read())
            reverting = true
            val pending = async { orch.revertScrapeJournal(applied.journalId) }
            started.await()
            // 同编号但内容已变化时，也必须保留最新快照。
            val newer = before.copy(
                journalId = if (sameId) before.journalId else "期间产生的新记录",
                entries = before.entries.map { it.copy(songId = "新-${it.songId}") },
            )
            store.write(newer)
            resume.complete(Unit)
            val result = pending.await()
            assertEquals(if (partialFailure) 1 else 2, result.reverted)
            assertEquals(if (partialFailure) 1 else 0, result.failed)
            assertEquals("T-s1", repo.songs.getValue("s1").title)
            assertEquals(if (partialFailure) "新标题" else "T-s2", repo.songs.getValue("s2").title)
            assertEquals(newer, store.read(), "同编号=$sameId，部分失败=$partialFailure")
        }
    }

    @Test
    fun `撤销的单曲读取文件或曲库异常不阻断其他歌曲且只重试失败项`() = runTest {
        for (stage in listOf("读取", "文件", "曲库")) {
            val originals = listOf(song("s1"), song("s2"))
            val repo = InMemorySongRepository(originals)
            val writer = FakeWriter { FileWriteResult(true) }
            val orch = orchestrator(repo, writer)
            val applied = orch.applyScrapeChanges(originals.map { ScrapeCandidate(it.id, it) },
                originals.map { it.id }.toSet(), originals.associate { it.id to ScrapeChanges(title = "新标题") })
            when (stage) {
                "读取" -> repo.failedReads.add("s1")
                "曲库" -> repo.failedWrites.add("s1")
                "文件" -> writer.behavior = { if (it.id == "s1") error("模拟文件异常") else FileWriteResult(true) }
            }
            val result = orch.revertScrapeJournal(applied.journalId)
            assertEquals(1, result.reverted, stage)
            assertEquals(1, result.failed, stage)
            assertEquals("T-s2", repo.songs.getValue("s2").title, stage)
            assertEquals("新标题", repo.songs.getValue("s1").title, stage)
            assertEquals(listOf("s1"), orch.getCurrentRollbackJournal()?.entries?.map { it.songId }, stage)

            repo.failedReads.clear()
            repo.failedWrites.clear()
            writer.behavior = { FileWriteResult(true) }
            writer.calls.clear()
            val retried = orch.revertScrapeJournal(applied.journalId)
            assertEquals(1, retried.reverted, stage)
            assertEquals(0, retried.failed, stage)
            assertEquals(listOf("s1"), writer.calls, stage)
            assertEquals("T-s1", repo.songs.getValue("s1").title, stage)
            assertNull(orch.getCurrentRollbackJournal(), stage)
        }
    }

    @Test
    fun `撤销时缓存失效异常不阻断曲库恢复`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val orch = WritebackOrchestrator(repo, newJournalStore(), FakeWriter { FileWriteResult(true) },
            audioTagCacheInvalidator = { error("模拟缓存异常") })
        val applied = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题")))
        val result = orch.revertScrapeJournal(applied.journalId)
        assertEquals(1, result.reverted)
        assertEquals(0, result.failed)
        assertEquals(original.title, repo.songs.getValue(original.id).title)
        assertNull(orch.getCurrentRollbackJournal())
    }

    @Test
    fun `撤销取消时立即传播且保留原快照`() = runTest {
        val originals = listOf(song("s1"), song("s2"))
        val repo = InMemorySongRepository(originals)
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = orchestrator(repo, writer)
        val applied = orch.applyScrapeChanges(originals.map { ScrapeCandidate(it.id, it) },
            originals.map { it.id }.toSet(), originals.associate { it.id to ScrapeChanges(title = "新标题") })
        val journal = orch.getCurrentRollbackJournal()
        writer.calls.clear()
        writer.behavior = { throw CancellationException("模拟取消") }
        assertFailsWith<CancellationException> { orch.revertScrapeJournal(applied.journalId) }
        assertEquals(listOf("s1"), writer.calls)
        assertEquals(journal, orch.getCurrentRollbackJournal())
        assertTrue(repo.songs.values.all { it.title == "新标题" })
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
    fun `仅封面地址字段的网络封面也会下载并内嵌`() = runTest {
        for (url in listOf("https://remote/c.jpg", "http://remote/c.jpg")) {
            val original = song("s1")
            val repo = InMemorySongRepository(listOf(original))
            val bytes = byteArrayOf(1, 2, 3)
            val fetched = mutableListOf<String>()
            val captured = mutableListOf<Pair<ScrapeChanges, ByteArray?>>()
            val orch = WritebackOrchestrator(repo, newJournalStore(),
                AudioTagFileWriter { _, changes, coverBytes ->
                    captured.add(changes to coverBytes)
                    FileWriteResult(true)
                }, coverBytesFetcher = CoverBytesFetcher { fetched.add(it); bytes })
            val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
                mapOf(original.id to ScrapeChanges(coverUri = url)))
            assertEquals(listOf(url), fetched)
            assertEquals(url, captured.single().first.coverUri)
            assertTrue(bytes.contentEquals(captured.single().second))
            assertEquals(WritebackStatus.SUCCESS, result.results.single().status)
            assertEquals(url, repo.songs.getValue(original.id).coverUri)
            assertEquals(MetaFieldSource.EMBEDDED, repo.songs.getValue(original.id).metaSources?.cover)
        }
    }

    @Test
    fun `仅封面地址下载失败保留展示地址且不误标内嵌`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<ScrapeChanges>()
        val url = "https://remote/c.jpg"
        val fetched = mutableListOf<String>()
        val orch = WritebackOrchestrator(repo, newJournalStore(),
            AudioTagFileWriter { _, changes, bytes ->
                captured.add(changes)
                assertNull(bytes)
                FileWriteResult(true)
            }, coverBytesFetcher = CoverBytesFetcher { fetched.add(it); null })
        orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题", coverUri = url)))
        assertEquals(listOf(url), fetched)
        assertNull(captured.single().coverUri)
        assertEquals("新标题", captured.single().title)
        assertEquals(url, repo.songs.getValue(original.id).coverUri)
        assertEquals(MetaFieldSource.SCRAPE, repo.songs.getValue(original.id).metaSources?.cover)
    }

    @Test
    fun `封面地址下载取消立即传播且不写文件或曲库`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val writer = FakeWriter { FileWriteResult(true) }
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            coverBytesFetcher = CoverBytesFetcher { throw CancellationException("模拟下载取消") })
        assertFailsWith<CancellationException> {
            orch.applyScrapeChanges(candidates(original), setOf(original.id),
                mapOf(original.id to ScrapeChanges(coverUri = "https://remote/c.jpg")))
        }
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals(original, repo.songs.getValue(original.id))
    }

    @Test
    fun `远程封面字节成功时传入标签请求`() = runTest {
        val repo = InMemorySongRepository(listOf(song("s1")))
        val captured = mutableListOf<Pair<ScrapeChanges, ByteArray?>>()
        val writer = AudioTagFileWriter { _, changes, coverBytes ->
            captured.add(changes to coverBytes); FileWriteResult(ok = true)
        }
        val fakeFetcher = CoverBytesFetcher { "https://fake".toByteArray() }
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer, coverBytesFetcher = fakeFetcher)

        orch.applyScrapeChanges(
            candidates(song("s1")), setOf("s1"),
            mapOf("s1" to ScrapeChanges(coverUri = "https://remote/c.jpg", coverRemoteUrl = "https://remote/c.jpg")),
        )

        assertEquals(1, captured.size)
        assertTrue(captured.single().second != null)
        // 库内 coverUri 保留远端地址（非清空）
        assertEquals("https://remote/c.jpg", repo.songs.getValue("s1").coverUri)
        assertEquals(MetaFieldSource.EMBEDDED, repo.songs.getValue("s1").metaSources?.cover)
    }

    @Test
    fun `封面取不到仍保存文本标签并标记待内嵌`() = runTest {
        val original = song("s1")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<Pair<ScrapeChanges, ByteArray?>>()
        val writer = AudioTagFileWriter { _, changes, coverBytes ->
            captured.add(changes to coverBytes); FileWriteResult(ok = true)
        }
        // 封面抓取失败：写回不因此中断，文件只需保存文本标签。
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            coverBytesFetcher = CoverBytesFetcher { null })
        val result = orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新标题", coverRemoteUrl = "https://remote/c.jpg")))

        assertEquals(WritebackStatus.SUCCESS, result.results.single().status)
        assertEquals("新标题", repo.songs.getValue(original.id).title)
        assertEquals("https://remote/c.jpg", repo.songs.getValue(original.id).coverUri)
        assertEquals(MetaFieldSource.SCRAPE, repo.songs.getValue(original.id).metaSources?.cover)
        assertEquals(MetaFieldSource.EMBEDDED, repo.songs.getValue(original.id).metaSources?.title)
        // 传给文件写入器的请求里没有封面，避免写入端因缺封面字节失败。
        assertEquals(null, captured.single().first.coverRemoteUrl)
        assertEquals(null, captured.single().second)
    }

    @Test
    fun `显式清空封面照常写入且不依赖封面字节`() = runTest {
        val original = song("s1").copy(coverUri = "https://old/c.jpg")
        val repo = InMemorySongRepository(listOf(original))
        val captured = mutableListOf<Pair<ScrapeChanges, ByteArray?>>()
        val writer = AudioTagFileWriter { _, changes, coverBytes ->
            captured.add(changes to coverBytes); FileWriteResult(ok = true)
        }
        val orch = WritebackOrchestrator(repo, newJournalStore(), writer,
            coverBytesFetcher = CoverBytesFetcher { null })
        orch.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(coverUri = "")))

        // 清空请求原样传给写入器（写入端依 coverUri=="" 删除封面）
        assertEquals("", captured.single().first.coverUri)
        assertEquals(null, repo.songs.getValue(original.id).coverUri)
        assertEquals(MetaFieldSource.EMBEDDED, repo.songs.getValue(original.id).metaSources?.cover)
    }
    @Test fun `审核基准冲突在封面请求和撤销覆盖前拒绝`() = runTest {
        val original = song("review")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = FakeWriter { FileWriteResult(true) }
        val seed = WritebackOrchestrator(repo, store, writer)
        seed.applyScrapeChanges(candidates(original), setOf(original.id), mapOf(original.id to ScrapeChanges(title = "第一次")))
        val previous = store.read()
        val current = repo.songs.getValue(original.id)
        writer.calls.clear(); repo.upserts.clear()
        var coverCalls = 0
        for (changedSong in listOf(current, current.copy(album = "外部曲库修改"))) {
            repo.songs[original.id] = changedSong
            val checked = WritebackOrchestrator(repo, store, writer,
                coverBytesFetcher = CoverBytesFetcher { coverCalls++; byteArrayOf(1) },
                readReviewFileRevision = { "新文件版本" })
            val result = checked.applyScrapeChanges(listOf(ScrapeCandidate(current.id, current, "旧文件版本")),
                setOf(current.id), mapOf(current.id to ScrapeChanges(title = "过期标题", coverRemoteUrl = "https://test/cover")))
            assertEquals("review_changed", result.results.single().fileResult.code)
            assertEquals(previous, store.read())
        }
        assertTrue(writer.calls.isEmpty())
        assertTrue(repo.upserts.isEmpty())
        assertEquals(0, coverCalls)
    }

    @Test fun `写入器发现审核冲突后未写文件且保留旧撤销记录`() = runTest {
        val original = song("review")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        WritebackOrchestrator(repo, store, FakeWriter { FileWriteResult(true) }).applyScrapeChanges(
            candidates(original), setOf(original.id), mapOf(original.id to ScrapeChanges(title = "第一次")))
        val previous = store.read()
        val current = repo.songs.getValue(original.id)
        repo.upserts.clear()
        var reviewedCalls = 0
        for (conflictCode in listOf("review_changed", "file_changed")) {
        val checked = WritebackOrchestrator(repo, store, object : AudioTagFileWriter {
            override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult = error("不能退回普通写入")
            override suspend fun writeReviewed(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?, expectedRevision: String): FileWriteResult {
                reviewedCalls++
                assertEquals("审核版本", expectedRevision)
                return FileWriteResult(false, conflictCode, "封面读取期间已变化")
            }
        }, readReviewFileRevision = { "审核版本" })
        val result = checked.applyScrapeChanges(listOf(ScrapeCandidate(current.id, current, "审核版本")),
            setOf(current.id), mapOf(current.id to ScrapeChanges(title = "过期标题")))
        assertEquals(conflictCode, result.results.single().fileResult.code)
        assertEquals(if (conflictCode == "review_changed") 1 else 2, reviewedCalls)
        assertTrue(repo.upserts.isEmpty())
        assertEquals(previous, store.read())
        }
    }

    private class SnapshotWriter : AudioTagFileWriter {
        var tags = com.muses.player.core.scrape.ports.TagPortTags(title = "文件原名", artist = "歌手", album = "原专辑", cover = byteArrayOf(1, 2, 3))
        var knownCover = true
        var onReviewed: suspend () -> Unit = {}
        var writes = 0
        override suspend fun captureSnapshot(song: Song) = com.muses.player.core.scrape.ports.AudioFileSnapshot(tags, knownCover)
        override suspend fun writeReviewed(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?, expectedRevision: String): FileWriteResult {
            onReviewed()
            return write(song, changes, coverBytes)
        }
        override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
            writes++
            tags = tags.copy(title = changes.title ?: tags.title, artist = changes.artist ?: tags.artist,
                album = changes.album ?: tags.album, lyrics = changes.lyrics ?: tags.lyrics,
                cover = if (changes.coverUri == "") null else coverBytes ?: tags.cover)
            return FileWriteResult(true)
        }
    }

    @Test fun `历史精确恢复单曲原字段与封面且恢复本身可撤销`() = runTest {
        val original = song("history", title = "曲库旧名")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = SnapshotWriter()
        val snapshots = CoverSnapshotStore(File(testDir, "covers"))
        val wb = WritebackOrchestrator(repo, store, writer, coverBytesFetcher = CoverBytesFetcher { byteArrayOf(8, 9) },
            readReviewFileRevision = { "文件版本" }, coverSnapshotStore = snapshots)
        val first = wb.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "第二版", coverRemoteUrl = "https://test/cover")))
        val secondSong = repo.songs.getValue(original.id)
        wb.applyScrapeChanges(candidates(secondSong), setOf(original.id), mapOf(original.id to ScrapeChanges(title = "第三版")))
        assertEquals(2, wb.getRollbackHistory().size)
        repo.songs[original.id] = repo.songs.getValue(original.id).copy(album = "后来改动的专辑")
        writer.tags = writer.tags.copy(album = "后来改动的专辑")
        val preview = wb.previewHistoricalEntry(first.journalId, original.id)
        assertEquals("文件原名", preview.entry.songBefore.title)
        assertEquals("第三版", preview.currentValues.title)
        val restored = wb.restoreHistoricalEntry(preview)
        assertEquals(1, restored.reverted)
        assertEquals("文件原名", writer.tags.title)
        kotlin.test.assertContentEquals(byteArrayOf(1, 2, 3), writer.tags.cover)
        assertEquals("后来改动的专辑", repo.songs.getValue(original.id).album)
        val undo = requireNotNull(wb.getCurrentRollbackJournal())
        assertEquals("第三版", undo.entries.single().songBefore.title)
        assertEquals(1, wb.revertScrapeJournal(undo.journalId).reverted)
        assertEquals("第三版", writer.tags.title)
        kotlin.test.assertContentEquals(byteArrayOf(8, 9), writer.tags.cover)
        assertTrue(wb.getRollbackHistory().any { it.journalId == first.journalId })
    }

    @Test fun `损坏主封面或预览后文件变化均不启动历史恢复`() = runTest {
        val original = song("history")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = SnapshotWriter()
        val directory = File(testDir, "covers")
        var revision = "版本一"
        val wb = WritebackOrchestrator(repo, store, writer, readReviewFileRevision = { revision },
            coverSnapshotStore = CoverSnapshotStore(directory))
        val first = wb.applyScrapeChanges(candidates(original), setOf(original.id),
            mapOf(original.id to ScrapeChanges(title = "新名", coverUri = "")))
        val preview = wb.previewHistoricalEntry(first.journalId, original.id)
        val beforeWrites = writer.writes
        revision = "外部修改"
        kotlin.test.assertFailsWith<IllegalStateException> { wb.restoreHistoricalEntry(preview) }
        revision = "版本一"
        File(directory, requireNotNull(preview.entry.coverSnapshot)).writeText("损坏")
        kotlin.test.assertFailsWith<IllegalStateException> { wb.restoreHistoricalEntry(preview) }
        assertEquals(beforeWrites, writer.writes)
        assertEquals(first.journalId, wb.getCurrentRollbackJournal()?.journalId)
    }

    @Test fun `未知封面快照不按无封面清空且保留恢复记录`() = runTest {
        val original = song("unknown")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = SnapshotWriter().apply { knownCover = false }
        val wb = WritebackOrchestrator(repo, store, writer, readReviewFileRevision = { "版本" },
            coverSnapshotStore = CoverSnapshotStore(File(testDir, "covers")))
        val first = wb.applyScrapeChanges(candidates(original), setOf(original.id), mapOf(original.id to ScrapeChanges(coverUri = "")))
        val writes = writer.writes
        assertEquals(1, wb.revertScrapeJournal(first.journalId).failed)
        assertEquals(writes, writer.writes)
        assertTrue(wb.getCurrentRollbackJournal()?.entries?.isNotEmpty() == true)
    }

    @Test fun `历史恢复开始写入后取消仍确认新的撤销快照`() = runTest {
        val original = song("cancel-history")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val writer = SnapshotWriter()
        val wb = WritebackOrchestrator(repo, store, writer, readReviewFileRevision = { "版本" },
            coverSnapshotStore = CoverSnapshotStore(File(testDir, "covers")))
        val first = wb.applyScrapeChanges(candidates(original), setOf(original.id), mapOf(original.id to ScrapeChanges(title = "新名")))
        val preview = wb.previewHistoricalEntry(first.journalId, original.id)
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        writer.onReviewed = { started.complete(Unit); finish.await() }
        val operation = async { wb.restoreHistoricalEntry(preview) }
        started.await()
        operation.cancel()
        finish.complete(Unit)
        operation.join()
        assertEquals("文件原名", writer.tags.title)
        val audit = requireNotNull(store.read())
        assertTrue(audit.journalId != first.journalId)
        assertTrue(audit.entries.single().fileSaved)
        assertEquals("新名", audit.entries.single().songBefore.title)
    }

    @Test fun `历史只恢复展示封面时新撤销也不触碰文件封面`() = runTest {
        val original = song("display-cover").copy(coverUri = "当前展示地址")
        val repo = InMemorySongRepository(listOf(original))
        val store = newJournalStore()
        val entry = com.muses.player.core.model.scrape.RollbackEntry(original.id,
            com.muses.player.core.model.scrape.RollbackSongSnapshot(original.title, original.artist, original.album,
                "旧展示地址", null, null, null, null), "2026-10-16", setOf("cover"), coverWritten = false)
        store.write(com.muses.player.core.model.scrape.RollbackJournal(1, "display-history", listOf(entry)))
        val writer = SnapshotWriter().apply { knownCover = false }
        val wb = WritebackOrchestrator(repo, store, writer, readReviewFileRevision = { "版本" },
            coverSnapshotStore = CoverSnapshotStore(File(testDir, "covers")))
        assertEquals(1, wb.restoreHistoricalEntry(wb.previewHistoricalEntry("display-history", original.id)).reverted)
        assertEquals("旧展示地址", repo.songs.getValue(original.id).coverUri)
        val audit = requireNotNull(store.read())
        assertEquals(false, audit.entries.single().coverWritten)
        assertEquals(1, wb.revertScrapeJournal(audit.journalId).reverted)
        assertEquals("当前展示地址", repo.songs.getValue(original.id).coverUri)
        assertEquals(0, writer.writes)
    }

}

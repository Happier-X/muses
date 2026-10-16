package com.muses.player.feature.scrape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeCandidate
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackStatus
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.CoverBytesFetcher
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 不访问实际音频或网络，验证整批写回、出队和失败重试的协作行为。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrapeBatchWritebackTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val value = transform(data.value).toPreferences()
            data.value = value
            return value
        }
    }

    private class MemorySongs : SongRepository {
        val songs = listOf("甲", "乙").associateWith { Song(it, "来源", "/$it.mp3", "歌曲$it") }.toMutableMap()
        val updates = mutableListOf<String>()
        var readBehavior: suspend (String) -> Song? = { songs[it] }
        override fun observeSongs() = MutableStateFlow(songs.values.toList())
        override suspend fun getSong(id: String) = readBehavior(id)
        override suspend fun upsert(song: Song) { songs[song.id] = song; updates.add(song.id) }
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("本测试不扫描")
        override suspend fun deleteSourceSongs(sourceId: String): Unit = error("本测试不删除歌曲")
    }

    private class Fixture {
        val repo = MemorySongs()
        val queue = ScrapeQueueStore(MemoryStore(), existingSongIds = { repo.songs.keys.toSet() })
        val writes = mutableListOf<String>()
        var behavior: suspend (String) -> FileWriteResult = { FileWriteResult(true) }
        val journals = RollbackJournalStore(MemoryStore())
        val orchestrator = WritebackOrchestrator(repo, journals, AudioTagFileWriter { song, _, _ ->
            writes.add(song.id)
            behavior(song.id)
        }, coverBytesFetcher = CoverBytesFetcher { byteArrayOf(1, 2, 3) })
        val vm = ScrapeViewModel(queue, TextMetaMatcher(emptyList()), CoverMatcher(listOf(object : CoverProvider {
            override val id = OnlineCoverSource.ITUNES
            override suspend fun searchCoverUrl(query: OnlineCoverQuery) = "https://example.com/${query.songId}.jpg"
        })), orchestrator, repo)

        fun preview() = vm.pageState.value as ScrapePageState.Preview
        fun result() = vm.pageState.value as ScrapePageState.Result
    }

    private fun workflow(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        try {
            fixture.queue.enqueue(listOf("甲", "乙"))
            advanceUntilIdle()
            fixture.vm.startMatching()
            advanceUntilIdle()
            assertEquals(2, fixture.preview().items.size)
            fixture.vm.setAllChecked(true)
            block(fixture)
        } finally {
            fixture.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `停止匹配后迟到的歌曲读取不能覆盖写回结果`() = stoppedReadDoesNotReplaceResult(false)

    @Test
    fun `停止匹配后迟到的读取异常不能覆盖写回结果或显示匹配错误`() = stoppedReadDoesNotReplaceResult(true)

    private fun stoppedReadDoesNotReplaceResult(fail: Boolean) = workflow { f ->
        val response = CompletableDeferred<Unit>()
        f.repo.readBehavior = { id ->
            withContext(NonCancellable) {
                response.await()
                if (fail) error("模拟迟到的读取失败")
                f.repo.songs[id]
            }
        }
        f.vm.retrySingle("甲")
        runCurrent()
        assertTrue(f.vm.pageState.value is ScrapePageState.Matching)
        f.vm.stopMatching()
        assertEquals(2, f.preview().items.size)
        f.repo.readBehavior = { f.repo.songs[it] }
        f.vm.confirmWriteback()
        runCurrent()
        val result = f.result()
        assertTrue(result.results.all { it.status == WritebackStatus.SUCCESS })
        response.complete(Unit)
        advanceUntilIdle()
        assertEquals(result, f.vm.pageState.value)
        assertEquals(null, f.vm.errorMessage.value)
        assertEquals(2, f.writes.size)
        assertTrue(f.queue.load().isEmpty())
    }

    @Test
    fun `其他页面写回时批量保存保留候选提示等待且完成后可重试`() = workflow { f ->
        val pending = CompletableDeferred<Unit>()
        f.behavior = { pending.await(); FileWriteResult(true) }
        val song = f.repo.songs.getValue("甲")
        val external = async {
            f.orchestrator.applyScrapeChanges(listOf(ScrapeCandidate(song.id, song)), setOf(song.id),
                mapOf(song.id to ScrapeChanges(title = "其他页面标题")))
        }
        runCurrent()
        val preview = f.preview()
        val journal = f.journals.read()
        f.vm.confirmWriteback()
        runCurrent()
        assertEquals(preview, f.preview())
        assertTrue(f.vm.errorMessage.value?.contains("请等待完成后重试") == true)
        assertEquals(listOf("甲"), f.writes)
        assertEquals(journal, f.journals.read())
        assertEquals(listOf("甲", "乙"), f.queue.load().map { it.songId })
        pending.complete(Unit)
        external.await()
        f.vm.confirmWriteback()
        advanceUntilIdle()
        assertTrue(f.result().results.all { it.status == WritebackStatus.SUCCESS })
        assertEquals(null, f.vm.errorMessage.value)
        assertTrue(f.queue.load().isEmpty())
    }

    @Test
    fun `全部写前读取失败时本次不能恢复且上次记录仍能在队列查看`() = workflow { f ->
        val song = f.repo.songs.getValue("甲")
        f.orchestrator.applyScrapeChanges(listOf(ScrapeCandidate(song.id, song)), setOf(song.id),
            mapOf(song.id to ScrapeChanges(title = "上次保存标题")))
        val previousJournal = f.journals.read()
        f.writes.clear()
        var reads = 0
        f.repo.readBehavior = { id ->
            reads += 1
            // 批量页先读取两首歌曲；编排器再次读取时模拟失败。
            if (reads in 3..4) error("模拟写前读取失败")
            f.repo.songs[id]
        }
        f.vm.confirmWriteback()
        advanceUntilIdle()
        val result = f.result()
        assertEquals("", result.journalId)
        assertTrue(result.results.all { it.status == WritebackStatus.FAILED && !it.fileResult.ok })
        assertTrue(f.writes.isEmpty())
        assertEquals(previousJournal, f.journals.read())
        f.vm.undoLastWriteback()
        advanceUntilIdle()
        assertEquals(result, f.result())
        assertEquals(previousJournal, f.journals.read())
        f.vm.backToQueue()
        advanceUntilIdle()
        assertEquals(previousJournal?.journalId, f.vm.pendingUndo.value?.journalId)
    }

    @Test
    fun `批量写回连点及写回期间返回均不会启动第二批保存`() = workflow { f ->
        val pending = CompletableDeferred<Unit>()
        f.behavior = { pending.await(); FileWriteResult(true) }
        f.vm.confirmWriteback()
        assertEquals(ScrapePageState.Writing(2), f.vm.pageState.value)
        f.vm.confirmWriteback()
        runCurrent()
        assertEquals(setOf("甲", "乙"), f.writes.toSet())
        f.vm.backToQueue()
        f.vm.startMatching()
        f.vm.confirmWriteback()
        runCurrent()
        assertEquals(ScrapePageState.Writing(2), f.vm.pageState.value)
        assertEquals(2, f.writes.size)
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, f.result().results.size)
        assertTrue(f.result().results.all { it.status == WritebackStatus.SUCCESS })
        assertEquals(setOf("甲", "乙"), f.repo.updates.toSet())
        assertTrue(f.queue.load().isEmpty())
        assertEquals(2, f.writes.size)
    }

    @Test
    fun `批量部分失败仅成功歌曲出队且重试保留失败歌曲选择`() = workflow { f ->
        val failedPreview = f.preview().items.single { it.songId == "乙" }
        f.behavior = { if (it == "乙") FileWriteResult(false, "write_failed", "模拟失败") else FileWriteResult(true) }
        f.vm.confirmWriteback()
        advanceUntilIdle()
        assertEquals(WritebackStatus.SUCCESS, f.result().results.single { it.songId == "甲" }.status)
        assertEquals(WritebackStatus.FILE_FAILED, f.result().results.single { it.songId == "乙" }.status)
        assertEquals(listOf("乙"), f.queue.load().map { it.songId })
        f.vm.reviewWritebackFailure("乙")
        assertEquals(listOf(failedPreview), f.preview().items)
        f.behavior = { FileWriteResult(true) }
        f.vm.confirmWriteback()
        advanceUntilIdle()
        assertEquals(listOf("乙"), f.result().results.map { it.songId })
        assertEquals(WritebackStatus.SUCCESS, f.result().results.single().status)
        assertEquals(1, f.writes.count { it == "甲" })
        assertEquals(2, f.writes.count { it == "乙" })
        assertEquals(setOf("甲", "乙"), f.repo.updates.toSet())
        assertTrue(f.queue.load().isEmpty())
    }

    @Test
    fun `写回中停止不会出队且只保留已确认保存的记录`() = workflow { f ->
        val firstSaved = CompletableDeferred<Unit>()
        val blocked = CompletableDeferred<Unit>()
        f.behavior = { id ->
            if (id == "甲") {
                firstSaved.complete(Unit)
                FileWriteResult(true)
            } else {
                blocked.await()
                FileWriteResult(true)
            }
        }
        f.vm.confirmWriteback()
        runCurrent()
        assertEquals(ScrapePageState.Writing(2), f.vm.pageState.value)
        firstSaved.await()
        f.vm.cancelWriteback()
        advanceUntilIdle()

        assertTrue(f.vm.pageState.value is ScrapePageState.Preview)
        assertEquals(listOf("甲", "乙"), f.queue.load().map { it.songId })
        val entries = f.journals.read()?.entries.orEmpty()
        assertEquals(setOf("甲", "乙"), entries.map { it.songId }.toSet())
        assertEquals(listOf("甲"), entries.filter { it.fileSaved }.map { it.songId })
        assertEquals(false, entries.single { it.songId == "乙" }.fileSaved)
        assertTrue(f.vm.errorMessage.value?.contains("已停止写回") == true)
        assertEquals(listOf("甲"), f.repo.updates)
    }
}

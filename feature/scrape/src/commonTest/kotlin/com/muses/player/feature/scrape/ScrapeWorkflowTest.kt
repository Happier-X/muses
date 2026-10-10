package com.muses.player.feature.scrape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.WritebackStatus
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 只使用内存曲库、存储和匹配源，验证用户的选择不会在异步操作中丢失。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrapeWorkflowTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(data.value).toPreferences()
            data.value = updated
            return updated
        }
    }

    private class MemorySongs : SongRepository {
        val songs = listOf("a", "b", "c").associateWith {
            Song(id = it, sourceId = "source", path = "/$it.mp3", title = "歌曲 $it", artist = "歌手", album = "专辑")
        }.toMutableMap()
        var readError: String? = null
        override fun observeSongs() = MutableStateFlow(songs.values.toList())
        override suspend fun getSong(id: String): Song? {
            check(id != readError) { "模拟读取失败" }
            return songs[id]
        }
        override suspend fun upsert(song: Song) { songs[song.id] = song }
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("本测试不扫描")
        override suspend fun deleteSourceSongs(sourceId: String): Unit = error("本测试不删除歌曲")
    }

    private class Fixture {
        val repo = MemorySongs()
        val queue = ScrapeQueueStore(MemoryStore(), existingSongIds = { repo.songs.keys })
        var search: suspend (OnlineCoverQuery) -> String? = { "https://example.com/${it.songId}.jpg" }
        var searchText: suspend (OnlineTextQuery) -> TextMetaHit? = { null }
        var writeResult: (Song) -> FileWriteResult = { FileWriteResult(true) }
        val writes = mutableListOf<Pair<String, ScrapeChanges>>()
        val provider = object : CoverProvider {
            override val id = OnlineCoverSource.ITUNES
            override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? = search(query)
        }
        val textProvider = object : TextMetaProvider {
            override val id = OnlineTextSource.KW
            override suspend fun search(query: OnlineTextQuery): TextMetaHit? = searchText(query)
        }
        lateinit var vm: ScrapeViewModel
        suspend fun create() {
            queue.enqueue(listOf("a", "b", "c"))
            vm = ScrapeViewModel(
                queue, TextMetaMatcher(listOf(textProvider)), CoverMatcher(listOf(provider)),
                WritebackOrchestrator(
                    repo, RollbackJournalStore(MemoryStore()),
                    AudioTagFileWriter { song, changes, _ ->
                        writes.add(song.id to changes)
                        writeResult(song)
                    },
                ), repo,
            )
        }
        fun preview() = vm.pageState.value as ScrapePageState.Preview
    }

    private fun workflow(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        try {
            fixture.create()
            advanceUntilIdle()
            block(fixture)
        } finally {
            fixture.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `单曲重试成功保留其它候选与勾选`() = workflow { f ->
        f.search = { if (it.songId == "b") null else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "cover")
        f.search = { "https://example.com/${it.songId}.jpg" }
        f.vm.retrySingle("b")
        advanceUntilIdle()
        assertEquals(setOf("a", "b", "c"), f.preview().items.map { it.songId }.toSet())
        assertEquals(setOf("cover"), f.preview().items.first { it.songId == "a" }.checkedFields)
        assertTrue(f.preview().items.first { it.songId == "b" }.checkedFields.isEmpty())
        assertTrue(f.preview().noMatchIds.isEmpty())
    }

    @Test
    fun `重试仍无匹配时保留候选和未匹配入口`() = workflow { f ->
        f.search = { if (it.songId == "b") null else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "cover")
        f.vm.retrySingle("b")
        advanceUntilIdle()
        assertEquals(listOf("a", "c"), f.preview().items.map { it.songId })
        assertEquals(listOf("b"), f.preview().noMatchIds)
        assertEquals(setOf("cover"), f.preview().items.first().checkedFields)
    }

    @Test
    fun `批量网络重试不自动勾选新结果`() = workflow { f ->
        f.search = { if (it.songId == "b") error("模拟网络错误") else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        advanceUntilIdle()
        assertEquals(listOf("b"), f.vm.throttledIds.value)
        f.vm.toggleField("a", "cover")
        f.search = { "https://example.com/${it.songId}.jpg" }
        f.vm.retryThrottled()
        advanceUntilIdle()
        assertTrue(f.vm.throttledIds.value.isEmpty())
        assertTrue(f.preview().items.first { it.songId == "b" }.checkedFields.isEmpty())
        assertEquals(setOf("cover"), f.preview().items.first { it.songId == "a" }.checkedFields)
    }

    @Test
    fun `网络失败重试无结果会转到未匹配组`() = workflow { f ->
        f.search = { error("模拟网络错误") }
        f.vm.startMatching()
        advanceUntilIdle()
        f.search = { null }
        f.vm.retrySingle("b")
        advanceUntilIdle()
        assertEquals(listOf("b"), f.preview().noMatchIds)
        assertEquals(listOf("a", "c"), f.vm.throttledIds.value)
    }

    @Test
    fun `部分请求失败仍显示已匹配候选并可重试`() = workflow { f ->
        f.repo.songs["a"] = f.repo.songs.getValue("a").copy(album = null)
        f.searchText = { TextMetaHit(title = it.title, artist = it.artist, album = "新专辑", source = OnlineTextSource.KW) }
        f.search = { if (it.songId == "a") error("模拟封面网络失败") else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        advanceUntilIdle()
        val candidate = f.preview().items.first { it.songId == "a" }
        assertEquals("新专辑", candidate.matchedAlbum)
        assertEquals(setOf("cover"), candidate.failedRequests)
        assertEquals(listOf("a"), f.vm.throttledIds.value)
        f.vm.toggleField("a", "album")
        f.search = { "https://example.com/${it.songId}.jpg" }
        f.vm.retryThrottled()
        advanceUntilIdle()
        assertEquals(setOf("album"), f.preview().items.first { it.songId == "a" }.checkedFields)
        assertTrue(f.vm.throttledIds.value.isEmpty())
    }

    @Test
    fun `重试部分请求失败不会抹掉已选字段的旧候选`() = workflow { f ->
        f.repo.songs["a"] = f.repo.songs.getValue("a").copy(album = null)
        f.searchText = { TextMetaHit(title = it.title, artist = it.artist, album = "新专辑", source = OnlineTextSource.KW) }
        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "album")
        f.searchText = { error("模拟歌曲信息请求失败") }
        f.vm.retrySingle("a")
        advanceUntilIdle()
        val candidate = f.preview().items.first { it.songId == "a" }
        assertEquals("新专辑", candidate.matchedAlbum)
        assertEquals(setOf("album"), candidate.checkedFields)
        assertEquals(setOf("text"), candidate.failedRequests)
    }

    @Test
    fun `部分请求失败但选中字段应用成功后清理重试入口`() = workflow { f ->
        f.repo.songs["a"] = f.repo.songs.getValue("a").copy(album = null)
        f.searchText = { TextMetaHit(title = it.title, artist = it.artist, album = "新专辑", source = OnlineTextSource.KW) }
        f.search = { if (it.songId == "a") error("模拟封面网络失败") else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "album")
        f.vm.confirmWriteback()
        advanceUntilIdle()
        assertFalse(f.queue.contains("a"))
        assertTrue(f.vm.throttledIds.value.isEmpty())
    }

    @Test
    fun `停止匹配保留已完成结果且不移出队列`() = workflow { f ->
        f.search = { if (it.songId == "b") awaitCancellation() else "https://example.com/${it.songId}.jpg" }
        f.vm.startMatching()
        runCurrent()
        assertTrue(f.vm.pageState.value is ScrapePageState.Matching)
        f.vm.stopMatching()
        advanceUntilIdle()
        assertEquals(listOf("a"), f.preview().items.map { it.songId })
        assertEquals(listOf("a", "b", "c"), f.queue.load().map { it.songId })
        assertTrue(f.writes.isEmpty())
    }

    @Test
    fun `曲库读取失败不会当成歌曲已删除`() = workflow { f ->
        f.repo.readError = "b"
        f.vm.startMatching()
        advanceUntilIdle()
        assertEquals(listOf("a"), f.preview().items.map { it.songId })
        assertTrue(f.queue.contains("b"))
        assertNotNull(f.vm.errorMessage.value)
    }

    @Test
    fun `应用只处理勾选歌曲并保留结果歌名`() = workflow { f ->
        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "cover")
        f.vm.confirmWriteback()
        advanceUntilIdle()
        val result = f.vm.pageState.value as ScrapePageState.Result
        assertEquals(listOf("a"), f.writes.map { it.first })
        assertEquals("https://example.com/a.jpg", f.repo.songs.getValue("a").coverUri)

        assertEquals("歌曲 a", result.titles["a"])
        assertEquals(listOf("b", "c"), f.vm.queueSongIds.value)
        assertFalse("a" in f.vm.queueTitles.value)
    }

    @Test
    fun `文件失败留在队列且曲库保持原值可重试`() = workflow { f ->
        f.writeResult = { FileWriteResult(false, message = "模拟文件写入失败") }

        f.vm.startMatching()
        advanceUntilIdle()
        f.vm.toggleField("a", "cover")
        f.vm.confirmWriteback()
        advanceUntilIdle()
        val result = f.vm.pageState.value as ScrapePageState.Result
        assertEquals(WritebackStatus.FILE_FAILED, result.results.single().status)
        assertTrue(f.queue.contains("a"))
        // 文件未保存时不改曲库：封面地址也不落库，重试沿用同一份变更
        assertEquals(null, f.repo.songs.getValue("a").coverUri)

        f.vm.reviewWritebackFailure("a")
        assertEquals(setOf("cover"), f.preview().items.first().checkedFields)
        f.writeResult = { FileWriteResult(true) }
        f.vm.confirmWriteback()
        advanceUntilIdle()
        assertEquals(2, f.writes.size)
        assertEquals(f.writes.first().second, f.writes.last().second)
        assertFalse(f.queue.contains("a"))
    }

    @Test
    fun `连续核对包含未匹配和网络失败歌曲`() = workflow { f ->
        f.search = {
            when (it.songId) {
                "b" -> null
                "c" -> error("模拟网络错误")
                else -> "https://example.com/a.jpg"
            }
        }
        f.vm.startMatching()
        advanceUntilIdle()
        assertEquals("a", f.vm.startReviewQueue())
        assertEquals(listOf("a", "b", "c"), f.vm.pendingReviewQueue.value)
    }

    @Test
    fun `批量选择不会选中空候选或原值相同的字段`() {
        val candidate = PreviewCandidate(
            songId = "a", songTitle = "歌曲", currentArtist = "歌手",
            matchedTitle = "歌曲", matchedArtist = null, matchedAlbum = "新专辑",
            confidence = null, coverUrl = null,
        )
        assertEquals(setOf("album"), listOf(candidate).setAllChecked(true).single().checkedFields)
        assertTrue(listOf(candidate).setAllFields("cover", true).single().checkedFields.isEmpty())
        assertTrue(listOf(candidate).toggleField("a", "title").single().checkedFields.isEmpty())
        assertEquals(setOf("album"), listOf(candidate).toggleChecked("a").single().checkedFields)
    }
}

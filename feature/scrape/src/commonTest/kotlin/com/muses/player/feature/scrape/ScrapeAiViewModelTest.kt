package com.muses.player.feature.scrape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.ai.AiScrapeChoice
import com.muses.player.core.ai.AiScrapeDecision
import com.muses.player.core.ai.AiScrapeInput
import com.muses.player.core.ai.AiScrapeMatcher
import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.EditCloudMetaSearch
import com.muses.player.core.scrape.editmeta.LyricsHit
import com.muses.player.core.scrape.editmeta.LyricsSearchPort
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 模拟候选和 AI 请求，只验证选择与写回边界，不访问实际曲库或外部服务。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrapeAiViewModelTest {
    private class MemoryStore : DataStore<Preferences> {
        var failUpdates = false
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            check(!failUpdates) { "写回记录保存失败" }
            val updated = transform(data.value).toPreferences()
            data.value = updated
            return updated
        }
    }

    private class MemorySongs : SongRepository {
        var song = Song("song", "source", "/fake/song.mp3", "曲名", "歌手", "旧专辑")
        var updates = 0
        override fun observeSongs() = MutableStateFlow(listOf(song))
        override suspend fun getSong(id: String) = song.takeIf { it.id == id }
        override suspend fun upsert(song: Song) { this.song = song; updates++ }
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("本测试不扫描")
        override suspend fun deleteSourceSongs(sourceId: String): Unit = error("本测试不删除歌曲")
    }

    private class Fixture {
        val repo = MemorySongs()
        var config = AiRecommendConfig("https://example.com/v1", "fake-model", "fake-key")
        var behavior: suspend (AiScrapeInput) -> AiScrapeDecision = { decision(1, 1) }
        var requests = 0
        val writes = mutableListOf<ScrapeChanges>()
        val journalStore = MemoryStore()
        lateinit var vm: ScrapeReviewViewModel

        fun create() {
            val search = EditCloudMetaSearch(
                textProviders = listOf(OnlineTextSource.KW, OnlineTextSource.WY).map { source ->
                    object : TextMetaProvider {
                        override val id = source
                        override suspend fun search(query: OnlineTextQuery) = TextMetaHit(query.title, "歌手", "新专辑-${source.wire}", source)
                    }
                },
                coverProviders = listOf(object : CoverProvider {
                    override val id = OnlineCoverSource.ITUNES
                    override suspend fun searchCoverUrl(query: OnlineCoverQuery) = "https://example.com/cover.jpg"
                }),
                lyricsPorts = listOf("wy", "tx").map { source ->
                    object : LyricsSearchPort {
                        override val id = source
                        override suspend fun searchLyrics(query: EditCloudMetaQuery) = LyricsHit("[00:01.00]歌词-$source", "lrc")
                    }
                },
            )
            vm = ScrapeReviewViewModel(
                "song", null, search, repo,
                WritebackOrchestrator(repo, RollbackJournalStore(journalStore), AudioTagFileWriter { _, changes, _ ->
                    writes.add(changes)
                    FileWriteResult(true)
                }),
                ScrapeQueueStore(MemoryStore(), existingSongIds = { setOf("song") }),
                AiScrapeMatcher { input, _ -> requests++; behavior(input) },
                readAiConfig = { config },
            )
        }

        fun review() = vm.state.value as ScrapeReviewState.Review
    }

    private fun workflow(block: suspend TestScope.(Fixture) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        try {
            fixture.create()
            advanceUntilIdle()
            assertEquals(2, fixture.review().text.items.size)
            assertEquals(2, fixture.review().lyrics.items.size)
            block(fixture)
        } finally {
            fixture.vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `AI 只选择候选直到用户手动应用才写回`() = workflow { f ->
        val originalCover = f.review().selectedCoverIndex
        val expectedAlbum = f.review().text.items[1].album
        val expectedLyrics = f.review().lyrics.items[1].text
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(1, f.review().selectedTextIndex)
        assertEquals(1, f.review().selectedLyricsIndex)
        assertEquals(originalCover, f.review().selectedCoverIndex)
        assertTrue(f.vm.aiState.value is ScrapeAiState.Ready)
        assertTrue(f.writes.isEmpty())
        assertEquals(0, f.repo.updates)
        assertTrue("album" in f.review().checkedFields)
        assertTrue("lyrics" in f.review().checkedFields)
        assertFalse("title" in f.review().checkedFields)
        f.vm.apply()
        advanceUntilIdle()
        assertEquals(1, f.writes.size)
        assertEquals(expectedAlbum, f.writes.single().album)
        assertEquals(expectedLyrics, f.writes.single().lyrics)
    }

    @Test fun `写回记录异常仍保留候选和人工编辑`() = workflow { f ->
        f.vm.updateEditTitle("手动标题")
        f.vm.selectLyrics(1)
        val review = f.review()
        f.journalStore.failUpdates = true
        f.vm.apply()
        advanceUntilIdle()
        assertEquals(review, f.review())
        assertTrue(f.writes.isEmpty())
        assertEquals(0, f.repo.updates)
    }

    @Test
    fun `无法判断的维度保留人工覆写和勾选`() = workflow { f ->
        f.vm.updateEditTitle("手动标题")
        val original = f.review()
        f.behavior = { decision(null, 1) }
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(original.selectedTextIndex, f.review().selectedTextIndex)
        assertEquals("手动标题", f.review().editTitle)
        assertTrue("title" in f.review().checkedFields)
        assertEquals(1, f.review().selectedLyricsIndex)
        assertEquals(original.selectedCoverIndex, f.review().selectedCoverIndex)
    }

    @Test
    fun `有效 AI 推荐也保留原有手动编辑及其未勾选状态`() = workflow { f ->
        f.vm.updateEditTitle("手动标题")
        f.vm.toggleField("title")
        f.vm.updateEditArtist("手动歌手")
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(1, f.review().selectedTextIndex)
        assertEquals("手动标题", f.review().editTitle)
        assertEquals("手动歌手", f.review().editArtist)
        assertFalse("title" in f.review().checkedFields)
        assertTrue("artist" in f.review().checkedFields)
        assertTrue("album" in f.review().checkedFields)
    }

    @Test
    fun `人工选择后迟到的 AI 结果不得覆盖`() = workflow { f ->
        val response = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { withContext(NonCancellable) { response.await() } }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.selectTextCandidate(1)
        response.complete(decision(0, 1))
        advanceUntilIdle()
        assertEquals(1, f.review().selectedTextIndex)
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
        assertTrue(f.writes.isEmpty())
    }

    @Test
    fun `重新搜索后旧 AI 结果不污染新候选`() = workflow { f ->
        val response = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { withContext(NonCancellable) { response.await() } }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.updateKeywordTitle("另一首歌曲")
        f.vm.search()
        runCurrent()
        response.complete(decision(1, 1))
        advanceUntilIdle()
        assertEquals("另一首歌曲", f.review().keyword.title)
        assertEquals(0, f.review().selectedTextIndex)
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
    }

    @Test
    fun `两个 AI 请求逆序返回只采纳最新结果`() = workflow { f ->
        val oldResponse = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { withContext(NonCancellable) { oldResponse.await() } }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.cancelAiMatching()
        f.behavior = { decision(1, 1) }
        f.vm.matchWithAi()
        runCurrent()
        assertEquals(1, f.review().selectedTextIndex)
        oldResponse.complete(decision(0, 0))
        advanceUntilIdle()
        assertEquals(1, f.review().selectedTextIndex)
        assertEquals(1, (f.vm.aiState.value as ScrapeAiState.Ready).decision.text.index)
    }

    @Test
    fun `修改字段勾选或覆写会取消 AI 选择`() = workflow { f ->
        val response = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { withContext(NonCancellable) { response.await() } }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.toggleField("cover")
        f.vm.updateEditTitle("手动标题")
        val manual = f.review()
        response.complete(decision(1, 1))
        advanceUntilIdle()
        assertEquals(manual, f.review())
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
    }

    @Test
    fun `配置缺失和请求失败不改变原选择`() = workflow { f ->
        val original = f.review()
        f.config = AiRecommendConfig()
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(0, f.requests)
        assertEquals(original, f.review())
        assertTrue(f.vm.aiState.value is ScrapeAiState.Failed)
        f.config = AiRecommendConfig("https://example.com/v1", "fake-model", "fake-key")
        f.behavior = { error("模拟服务失败") }
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(original, f.review())
        assertTrue(f.vm.aiState.value is ScrapeAiState.Failed)
        assertTrue(f.writes.isEmpty())
    }

    @Test
    fun `AI 分析进行中不能触发写回`() = workflow { f ->
        val response = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { response.await() }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.apply()
        assertTrue(f.writes.isEmpty())
        assertTrue(f.vm.state.value is ScrapeReviewState.Review)
        f.vm.cancelAiMatching()
        advanceUntilIdle()
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
    }

    @Test
    fun `越界推荐在选择层再次拒绝`() = workflow { f ->
        val original = f.review()
        assertEquals(original, original.withAiSelection(decision(-1, 99)))
    }

    companion object {
        private fun decision(text: Int?, lyrics: Int?) = AiScrapeDecision(
            AiScrapeChoice(text, "模拟歌曲信息判断"), AiScrapeChoice(lyrics, "模拟歌词判断"),
        )
    }
}

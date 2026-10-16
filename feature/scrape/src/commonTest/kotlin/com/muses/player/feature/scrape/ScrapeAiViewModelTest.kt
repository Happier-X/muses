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
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.editmeta.EditCloudMetaQuery
import com.muses.player.core.scrape.editmeta.CloudPlatformId
import com.muses.player.core.scrape.editmeta.CloudLyricsPlatformId
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
        var failUpdates = false
        override fun observeSongs() = MutableStateFlow(listOf(song))
        override suspend fun getSong(id: String) = song.takeIf { it.id == id }
        override suspend fun upsert(song: Song) {
            check(!failUpdates) { "模拟曲库更新失败" }
            this.song = song; updates++
        }
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("本测试不扫描")
        override suspend fun deleteSourceSongs(sourceId: String): Unit = error("本测试不删除歌曲")
    }

    private class Fixture {
        val repo = MemorySongs()
        var config = AiRecommendConfig("https://example.com/v1", "fake-model", "fake-key")
        var behavior: suspend (AiScrapeInput) -> AiScrapeDecision = { decision(1, 1) }
        var requests = 0
        var fileRevision: String? = "初始文件版本"
        val writes = mutableListOf<ScrapeChanges>()
        var writeBehavior: suspend () -> FileWriteResult = { FileWriteResult(true) }
        val journalStore = MemoryStore()
        val textCalls = mutableMapOf<OnlineTextSource, Int>()
        var textBehavior: suspend (OnlineTextSource, OnlineTextQuery) -> TextMetaHit? = { source, query ->
            TextMetaHit(query.title, "歌手", "新专辑-${source.wire}", source)
        }
        lateinit var vm: ScrapeReviewViewModel

        fun create(draftStore: ScrapeReviewDraftStore? = null) {
            val search = EditCloudMetaSearch(
                textProviders = listOf(OnlineTextSource.KW, OnlineTextSource.WY).map { source ->
                    object : TextMetaProvider {
                        override val id = source
                        override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
                            textCalls[source] = (textCalls[source] ?: 0) + 1
                            return textBehavior(source, query)
                        }
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
                WritebackOrchestrator(repo, RollbackJournalStore(journalStore), object : AudioTagFileWriter {
                    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
                        writes.add(changes)
                        return writeBehavior()
                    }
                    override suspend fun writeReviewed(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?, expectedRevision: String): FileWriteResult {
                        assertEquals(fileRevision, expectedRevision)
                        return write(song, changes, coverBytes)
                    }
                }, readReviewFileRevision = { fileRevision }),
                ScrapeQueueStore(MemoryStore(), existingSongIds = { setOf("song") }),
                AiScrapeMatcher { input, _ -> requests++; behavior(input) },
                readAiConfig = { config },
                draftStore = draftStore,
                readDraftFileRevision = { fileRevision },
            )
        }

        fun review() = vm.state.value as ScrapeReviewState.Review
    }

    @Test fun `来源诊断使用当前输入但不改变审核选择或写回`() = workflow { f ->
        f.vm.selectLyrics(1)
        f.vm.updateEditTitle("人工标题")
        f.vm.updateKeywordTitle("另一个查询")
        f.vm.updateMetadataSource(CloudPlatformId.WY)
        val before = f.review()
        f.vm.startSourceDiagnostics()
        advanceUntilIdle()
        assertEquals(before, f.review())
        assertEquals("另一个查询", f.vm.diagnostics.value.query?.title)
        assertFalse(f.vm.diagnostics.value.running)
        assertTrue(f.vm.diagnostics.value.rows.isNotEmpty())
        assertTrue(f.writes.isEmpty())
        assertEquals(0, f.repo.updates)
        assertEquals(0, f.requests)
    }

    @Test fun `停止诊断后旧结果不回写且等待来源不启动`() = workflow { f ->
        val blocker = CompletableDeferred<Unit>()
        val calls = f.textCalls.toMap()
        f.textBehavior = { source, query ->
            withContext(NonCancellable) { blocker.await() }
            TextMetaHit(query.title, source = source)
        }
        f.vm.startSourceDiagnostics()
        runCurrent()
        f.vm.stopSourceDiagnostics()
        blocker.complete(Unit)
        advanceUntilIdle()
        assertFalse(f.vm.diagnostics.value.running)
        assertTrue(f.vm.diagnostics.value.rows.isEmpty())
        assertEquals(calls[OnlineTextSource.WY], f.textCalls[OnlineTextSource.WY])
    }

    @Test fun `重建审核恢复完整候选手改与未提交查询且不访问网络`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel()
        f.create(store)
        runCurrent()
        advanceUntilIdle()
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        f.vm.updateEditTitle("人工标题")
        f.vm.selectLyrics(1)
        f.vm.toggleField("cover")
        f.vm.updateKeywordAlbum("尚未搜索的专辑")
        f.vm.updateMetadataSource(CloudPlatformId.ITUNES)
        val expected = f.review()
        // load 同样经过持久队列，作为保存完成屏障。
        store.load("song")
        val calls = f.textCalls.toMap()
        f.vm.viewModelScope.cancel()
        f.create(store)
        runCurrent()
        advanceUntilIdle()
        val restored = f.review()
        assertTrue(restored.restoredDraft)
        assertEquals(expected.text, restored.text)
        assertEquals(expected.lyrics, restored.lyrics)
        assertEquals(expected.textFieldIndices, restored.textFieldIndices)
        assertEquals(expected.checkedFields, restored.checkedFields)
        assertEquals("人工标题", restored.resolvedTitle())
        assertEquals(expected.selectedLyricsIndex, restored.selectedLyricsIndex)
        assertEquals("尚未搜索的专辑", f.vm.keyword.value.album)
        assertEquals(CloudPlatformId.ITUNES, f.vm.searchSources.value.metadata)
        assertEquals(expected.keyword, restored.keyword)
        assertEquals(expected.sources, restored.sources)
        assertEquals(calls, f.textCalls)
        assertEquals(0, f.requests)
        assertTrue(f.writes.isEmpty())
    }

    @Test fun `录音分组的原始索引选中正确候选且新证据可跨重建恢复`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel()
        f.textBehavior = { source, query -> TextMetaHit(query.title, "歌手", "专辑-${source.wire}", source,
            durationSec = 180.0, recordingId = "musicbrainz:录音", releaseId = "${source.wire}:发行", candidateId = "${source.wire}:候选", versionInfo = "Live at Wembley") }
        f.create(store); runCurrent(); advanceUntilIdle()
        val group = com.muses.player.core.scrape.text.groupTextCandidates(f.review().text.items).single()
        val index = group.releases.last().candidateIndices.single()
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, index)
        val expected = f.review().fieldHit(ReviewTextField.ALBUM)
        store.load("song")
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        assertEquals(expected, f.review().fieldHit(ReviewTextField.ALBUM))
        assertEquals(180.0, f.review().fieldHit(ReviewTextField.ALBUM)?.durationSec)
        assertEquals("musicbrainz:录音", f.review().fieldHit(ReviewTextField.ALBUM)?.recordingId)
        assertEquals("Live at Wembley", f.review().fieldHit(ReviewTextField.ALBUM)?.versionInfo)
        assertEquals(index, f.review().textFieldIndices[ReviewTextField.ALBUM])
    }

    @Test fun `本地信息变化后草稿保留人工值但取消全部勾选`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        f.vm.updateEditTitle("人工标题")
        store.load("song")
        f.vm.viewModelScope.cancel()
        f.repo.song = f.repo.song.copy(album = "外部修改")
        f.create(store); runCurrent(); advanceUntilIdle()
        assertTrue(f.review().draftBaselineChanged)
        assertEquals("人工标题", f.review().resolvedTitle())
        assertTrue(f.review().checkedFields.isEmpty())
    }

    @Test fun `曲库没变化但文件修改或版本未知时恢复草稿不能沿用勾选`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        for (revision in listOf("外部修改后的文件版本", null)) {
            f.vm.viewModelScope.cancel(); f.fileRevision = "初始文件版本"
            store.clear("song")
            f.create(store); runCurrent(); advanceUntilIdle()
            f.vm.updateEditTitle("人工标题")
            store.load("song")
            f.vm.viewModelScope.cancel(); f.fileRevision = revision
            f.create(store); runCurrent(); advanceUntilIdle()
            assertTrue(f.review().draftBaselineChanged)
            assertEquals("人工标题", f.review().resolvedTitle())
            assertTrue(f.review().checkedFields.isEmpty())
        }
    }

    @Test fun `写回失败保留草稿成功后清理且不会被旧保存复活`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        f.vm.updateEditTitle("人工标题")
        f.writeBehavior = { FileWriteResult(false, message = "模拟失败") }
        f.vm.apply(); advanceUntilIdle()
        assertEquals("人工标题", store.load("song")?.title)
        assertEquals(null, store.load("other-song"))
        f.writeBehavior = { FileWriteResult(true) }
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals(null, store.load("song"))
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
    fun `相同文本不写文件曲库或撤销记录且后续仍可应用实际修改`() = workflow { f ->
        f.review().checkedFields.toList().forEach(f.vm::toggleField)
        f.vm.updateEditTitle(f.repo.song.title)
        f.vm.updateEditArtist(f.repo.song.artist.orEmpty())
        f.vm.updateEditAlbum(f.repo.song.album.orEmpty())
        val review = f.review()
        val journals = RollbackJournalStore(f.journalStore)
        journals.write(RollbackJournal(1, "previous", emptyList()))
        val previous = f.journalStore.data.value
        f.vm.apply()
        assertEquals(review, f.review())
        advanceUntilIdle()
        assertEquals(review, f.review())
        assertTrue(f.writes.isEmpty())
        assertEquals(0, f.repo.updates)
        assertEquals(null, f.vm.lastWrittenSongId)
        assertEquals(previous, f.journalStore.data.value)

        f.vm.updateEditTitle("实际修改")
        f.vm.apply()
        advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals("实际修改", f.writes.single().title)
        assertEquals(null, f.writes.single().lyricsFormat)
        assertEquals(1, f.repo.updates)
    }

    @Test
    fun `空白候选不能被全选或单项勾选写回清空原值`() = workflow { f ->
        f.textBehavior = { source, query -> TextMetaHit(query.title, "", "  ", source) }
        f.vm.search()
        advanceUntilIdle()
        f.vm.updateEditTitle("手动标题")
        for (field in selectableFields(f.review())) {
            if (field !in f.review().checkedFields) f.vm.toggleField(field)
        }
        f.vm.toggleField("artist")
        f.vm.toggleField("album")
        assertFalse("artist" in f.review().checkedFields)
        assertFalse("album" in f.review().checkedFields)
        val originalArtist = f.repo.song.artist
        val originalAlbum = f.repo.song.album
        f.vm.apply()
        advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals("手动标题", f.writes.single().title)
        assertEquals(null, f.writes.single().artist)
        assertEquals(null, f.writes.single().album)
        assertEquals(originalArtist, f.repo.song.artist)
        assertEquals(originalAlbum, f.repo.song.album)
    }

    @Test
    fun `文件保存后曲库失败仍保留审核且可重试`() = workflow { f ->
        f.vm.updateEditTitle("手动标题")
        val review = f.review()
        f.repo.failUpdates = true
        f.vm.apply()
        advanceUntilIdle()
        assertEquals(review, f.review())
        assertEquals(null, f.vm.lastWrittenSongId)
        assertEquals(0, f.repo.updates)
        assertEquals(1, f.writes.size)
        assertEquals("song", RollbackJournalStore(f.journalStore).read()?.entries?.single()?.songId)
        f.repo.failUpdates = false
        f.vm.apply()
        advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals(1, f.repo.updates)
        assertEquals("手动标题", f.repo.song.title)
        assertEquals(f.writes[0], f.writes[1])
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

    @Test fun `仅重试失败来源保留期间的编辑勾选和来源选择`() = workflow { f ->
        f.textBehavior = { source, query ->
            if (source == OnlineTextSource.KW) error("暂不可用")
            TextMetaHit(query.title, "歌手", "已有专辑", source)
        }
        f.vm.search()
        advanceUntilIdle()
        val original = f.review()
        val calls = f.textCalls.toMap()
        val response = CompletableDeferred<Unit>()
        f.textBehavior = { source, query ->
            response.await()
            assertEquals(original.keyword.title, query.title)
            TextMetaHit("恢复候选", "恢复歌手", "恢复专辑", source)
        }
        f.vm.updateKeywordTitle("尚未提交的草稿")
        f.vm.updateMetadataSource(CloudPlatformId.WY)
        f.vm.retryFailedSources()
        runCurrent()
        assertTrue(f.review().retryingFailures)
        f.vm.retryFailedSources() // 连点不重复发送。
        f.vm.selectTextFieldCandidate(ReviewTextField.ARTIST, 0)
        f.vm.updateEditTitle("重试期间的手改")
        f.vm.toggleField("lyrics")
        val edited = f.review()
        f.vm.apply()
        f.vm.matchWithAi()
        assertTrue(f.writes.isEmpty())
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
        response.complete(Unit)
        advanceUntilIdle()
        val recovered = f.review()
        assertFalse(recovered.retryingFailures)
        assertEquals(edited.editTitle, recovered.editTitle)
        assertEquals(edited.checkedFields, recovered.checkedFields)
        assertEquals(edited.textFieldIndices, recovered.textFieldIndices)
        assertEquals(original.text.items, recovered.text.items.take(original.text.items.size))
        assertEquals("恢复候选", recovered.text.items.last().title)
        assertTrue(recovered.text.failedSources.isEmpty())
        assertEquals(calls[OnlineTextSource.WY], f.textCalls[OnlineTextSource.WY])
        assertEquals(calls.getValue(OnlineTextSource.KW) + 1, f.textCalls[OnlineTextSource.KW])
        assertEquals("尚未提交的草稿", f.vm.keyword.value.title)
        assertEquals(original.sources, recovered.sources)
        assertEquals(CloudPlatformId.WY, f.vm.searchSources.value.metadata)
    }

    @Test fun `来源选择仅在重新搜索后生效且元信息与歌词各自过滤`() = workflow { f ->
        val original = f.review()
        val calls = f.textCalls.toMap()
        f.vm.updateMetadataSource(CloudPlatformId.WY)
        f.vm.updateLyricsSource(CloudLyricsPlatformId.TX)
        assertEquals(original, f.review())
        assertEquals(calls, f.textCalls)
        f.vm.search()
        advanceUntilIdle()
        val filtered = f.review()
        assertEquals(listOf(OnlineTextSource.WY), filtered.text.items.map { it.source })
        assertEquals(listOf("tx"), filtered.lyrics.items.map { it.source })
        assertTrue(filtered.cover.items.isEmpty()) // 夹具封面只有 iTunes。
        assertEquals(ScrapeSearchSources(CloudPlatformId.WY, CloudLyricsPlatformId.TX), filtered.sources)
        assertEquals(calls.getValue(OnlineTextSource.KW), f.textCalls[OnlineTextSource.KW])
        assertEquals(calls.getValue(OnlineTextSource.WY) + 1, f.textCalls[OnlineTextSource.WY])
        f.vm.updateMetadataSource(CloudPlatformId.ITUNES)
        f.vm.search()
        advanceUntilIdle()
        assertTrue(f.review().text.items.isEmpty())
        assertEquals(OnlineCoverSource.ITUNES, f.review().cover.items.single().source)
        assertEquals(listOf("tx"), f.review().lyrics.items.map { it.source })
    }

    @Test fun `查询中的来源快照不被后续草稿修改`() = workflow { f ->
        val response = CompletableDeferred<Unit>()
        f.textBehavior = { source, query ->
            response.await()
            TextMetaHit(query.title, "歌手", "新专辑", source)
        }
        f.vm.updateMetadataSource(CloudPlatformId.WY)
        f.vm.search()
        runCurrent()
        f.vm.updateMetadataSource(CloudPlatformId.KW)
        response.complete(Unit)
        advanceUntilIdle()
        assertEquals(CloudPlatformId.WY, f.review().sources.metadata)
        assertEquals(listOf(OnlineTextSource.WY), f.review().text.items.map { it.source })
        assertEquals(CloudPlatformId.KW, f.vm.searchSources.value.metadata)
    }

    @Test fun `版本疑点在初选整组切换与AI选择时均不预勾选但允许人工应用`() = workflow { f ->
        f.textBehavior = { source, query -> TextMetaHit("${query.title} (Live)", "歌手", "现场专辑", source) }
        f.vm.search()
        advanceUntilIdle()
        fun assertTextUnchecked() {
            assertTrue(f.review().checkedFields.intersect(setOf("title", "artist", "album")).isEmpty())
            assertTrue("cover" in f.review().checkedFields)
            assertTrue("lyrics" in f.review().checkedFields)
        }
        assertTextUnchecked()
        f.vm.selectTextCandidate(1)
        assertTextUnchecked()
        f.behavior = { decision(0, null) }
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertTextUnchecked()
        assertTrue(f.writes.isEmpty())
        // 核对后仍可明确选择来源、手改或手动勾选，实际写回遵守这些操作。
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        f.vm.updateEditArtist("人工确认的歌手")
        f.vm.toggleField("title")
        f.vm.apply()
        advanceUntilIdle()
        assertEquals("曲名 (Live)", f.writes.single().title)
        assertEquals("人工确认的歌手", f.writes.single().artist)
        assertEquals("现场专辑", f.writes.single().album)
    }

    @Test fun `重新搜索后迟到的重试结果不能覆盖新候选`() = workflow { f ->
        f.textBehavior = { source, query ->
            if (source == OnlineTextSource.KW) error("暂不可用")
            TextMetaHit(query.title, source = source)
        }
        f.vm.search()
        advanceUntilIdle()
        val response = CompletableDeferred<Unit>()
        f.textBehavior = { source, query ->
            if (query.title == "曲名") withContext(NonCancellable) { response.await() }
            TextMetaHit(query.title, source = source)
        }
        f.vm.retryFailedSources()
        runCurrent()
        f.vm.updateKeywordTitle("新的搜索词")
        f.vm.search()
        runCurrent()
        assertEquals("新的搜索词", f.review().keyword.title)
        response.complete(Unit)
        advanceUntilIdle()
        assertEquals("新的搜索词", f.review().keyword.title)
        assertTrue(f.review().text.items.all { it.title == "新的搜索词" })
        assertFalse(f.review().retryingFailures)
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

    @Test fun `应用立即进入写回态且连点与重搜均不会重复保存`() = workflow { f ->
        val response = CompletableDeferred<Unit>()
        val searchCalls = f.textCalls.toMap()
        f.writeBehavior = { response.await(); FileWriteResult(true) }
        f.vm.apply()
        assertEquals(ScrapeReviewState.Writing, f.vm.state.value)
        f.vm.apply() // 协程尚未启动时的第二次点击。
        runCurrent()
        assertEquals(1, f.writes.size)
        f.vm.updateKeywordTitle("写回期间修改的搜索草稿")
        f.vm.search()
        runCurrent()
        assertEquals(ScrapeReviewState.Writing, f.vm.state.value)
        assertEquals(searchCalls, f.textCalls)
        f.vm.apply() // 文件保存尚未结束时的再次点击。
        assertEquals(1, f.writes.size)
        assertEquals(0, f.repo.updates)
        response.complete(Unit)
        advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals(1, f.writes.size)
        assertEquals(1, f.repo.updates)
    }

    @Test fun `审核期间文件变化停止保存且重搜后才使用新基准`() = workflow { f ->
        f.vm.updateEditTitle("人工标题")
        val before = f.review()
        f.fileRevision = "外部改动后的文件"
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.writes.isEmpty())
        assertEquals(0, f.repo.updates)
        assertEquals(before.text, f.review().text)
        assertEquals("人工标题", f.review().editTitle)
        assertTrue(f.review().checkedFields.isEmpty())
        assertTrue(f.review().draftBaselineChanged)
        f.vm.updateEditTitle("再次勾选仍不能绕过")
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.writes.isEmpty())
        f.vm.search(); advanceUntilIdle()
        f.vm.updateEditTitle("重新审核标题")
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals(1, f.writes.size)
    }

    @Test fun `重搜等待期间保存输入仍保留旧候选的审核基准`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        val oldHits = f.review().text.items
        val gate = CompletableDeferred<Unit>()
        f.fileRevision = "新版本"
        f.textBehavior = { source, query -> gate.await(); TextMetaHit(query.title, "歌手", "新查询", source) }
        f.vm.search(); runCurrent()
        f.vm.updateKeywordTitle("尚未提交的输入")
        val draft = requireNotNull(store.load("song"))
        assertEquals("初始文件版本", draft.fileRevision)
        assertEquals(oldHits, draft.restore(f.repo.song, null, "新版本").text.items)
        gate.complete(Unit); advanceUntilIdle()
        assertEquals("新版本", store.load("song")?.fileRevision)
    }

    @Test fun `本地审核基准未知不能退回普通写入`() = workflow { f ->
        f.fileRevision = null
        f.vm.search(); advanceUntilIdle()
        f.vm.updateEditTitle("人工标题")
        f.fileRevision = "稍后恢复可读"
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.writes.isEmpty())
        assertTrue(f.review().draftBaselineChanged)
    }

    @Test fun `恢复过期草稿不会静默改写原始文件基准`() = workflow { f ->
        val store = ScrapeReviewDraftStore(MemoryStore(), backgroundScope)
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        f.vm.updateEditTitle("保留人工标题")
        store.load("song")
        f.vm.viewModelScope.cancel(); f.fileRevision = "新的文件版本"
        f.create(store); runCurrent(); advanceUntilIdle()
        assertTrue(f.review().draftBaselineChanged)
        f.vm.updateEditTitle("重新勾选旧候选")
        assertEquals("初始文件版本", store.load("song")?.fileRevision)
        f.vm.apply(); advanceUntilIdle()
        assertTrue(f.writes.isEmpty())
        f.vm.viewModelScope.cancel(); f.create(store); runCurrent(); advanceUntilIdle()
        assertTrue(f.review().draftBaselineChanged)
    }

    @Test fun `文件保存失败恢复原审核且可以重新应用`() = workflow { f ->
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        f.vm.updateEditTitle("手改标题")
        f.vm.selectLyrics(1)
        f.vm.updateMetadataSource(CloudPlatformId.WY) // 未提交的来源草稿不改变审核选择。
        val review = f.review()
        f.writeBehavior = { FileWriteResult(ok = false, code = "write_failed", message = "保存失败") }
        f.vm.apply()
        advanceUntilIdle()
        assertEquals(review, f.review())
        assertEquals(0, f.repo.updates)
        assertEquals(1, f.writes.size)
        f.writeBehavior = { FileWriteResult(true) }
        f.vm.apply()
        advanceUntilIdle()
        assertTrue(f.vm.state.value is ScrapeReviewState.Success)
        assertEquals(2, f.writes.size)
        assertEquals(f.writes.first(), f.writes.last())
        assertEquals(review.resolvedTitle(), f.writes.last().title)
        assertEquals(review.resolvedAlbum(), f.writes.last().album)
        assertEquals(review.selectedLyrics?.text, f.writes.last().lyrics)
        assertEquals(1, f.repo.updates)
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

    @Test
    fun `组合来源实际写回保留手改且不提交未勾选字段`() = workflow { f ->
        val expectedAlbum = f.review().text.items[1].album
        f.vm.updateEditTitle("手动标题")
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        f.vm.toggleField("cover")
        f.vm.toggleField("lyrics")
        assertEquals("手动标题", f.review().resolvedTitle())
        f.vm.apply()
        advanceUntilIdle()
        assertEquals("手动标题", f.writes.single().title)
        assertEquals(expectedAlbum, f.writes.single().album)
        assertEquals(null, f.writes.single().artist)
        assertEquals(null, f.writes.single().coverRemoteUrl)
        assertEquals(null, f.writes.single().lyrics)
    }

    @Test
    fun `单字段手选后迟到的AI结果不能覆盖且再次匹配保留来源`() = workflow { f ->
        val response = CompletableDeferred<AiScrapeDecision>()
        f.behavior = { withContext(NonCancellable) { response.await() } }
        f.vm.matchWithAi()
        runCurrent()
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        val manual = f.review()
        response.complete(decision(0, 0))
        advanceUntilIdle()
        assertEquals(manual, f.review())
        assertEquals(ScrapeAiState.Idle, f.vm.aiState.value)
        f.behavior = { decision(0, 0) }
        f.vm.matchWithAi()
        advanceUntilIdle()
        assertEquals(manual.resolvedAlbum(), f.review().resolvedAlbum())
        assertEquals(1, f.review().textFieldIndices[ReviewTextField.ALBUM])
    }

    @Test
    fun `明确整组采用可清除字段组合和手改`() = workflow { f ->
        f.vm.selectTextFieldCandidate(ReviewTextField.ALBUM, 1)
        f.vm.updateEditTitle("手动标题")
        f.vm.selectTextCandidate(0)
        assertTrue(f.review().textFieldIndices.isEmpty())
        assertEquals(null, f.review().editTitle)
        assertEquals(f.review().text.items[0].album, f.review().resolvedAlbum())
    }

    companion object {
        private fun decision(text: Int?, lyrics: Int?) = AiScrapeDecision(
            AiScrapeChoice(text, "模拟歌曲信息判断"), AiScrapeChoice(lyrics, "模拟歌词判断"),
        )
    }
}

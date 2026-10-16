package com.muses.player.feature.scrape

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.RollbackEntry
import com.muses.player.core.model.scrape.RollbackJournal
import com.muses.player.core.model.scrape.RollbackSongSnapshot
import com.muses.player.core.scrape.cover.CoverMatcher
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.scrape.text.TextMetaMatcher
import com.muses.player.core.scrape.writeback.AudioTagFileWriter
import com.muses.player.core.scrape.writeback.RollbackJournalStore
import com.muses.player.core.scrape.writeback.WritebackOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 撤销记录跨重启保留：队列页必须能看到并恢复，未确认条目跳过但保留记录。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScrapePendingUndoTest {
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
        override fun observeSongs() = MutableStateFlow(songs.values.toList())
        override suspend fun getSong(id: String) = songs[id]
        override suspend fun upsert(song: Song) { songs[song.id] = song; updates.add(song.id) }
        override suspend fun rebuildDerivedIndexes() = Unit
        override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("本测试不扫描")
        override suspend fun deleteSourceSongs(sourceId: String): Unit = error("本测试不删除歌曲")
    }

    private fun entry(songId: String, fileSaved: Boolean) = RollbackEntry(
        songId = songId,
        songBefore = RollbackSongSnapshot(
            title = "旧$songId", artist = null, album = null, coverUri = null,
            lyrics = null, lyricsFormat = null, lyricsSource = null, metaSources = null,
        ),
        createdAt = "2026-10-15T00:00:00.000Z",
        changedFields = setOf("title"),
        fileSaved = fileSaved,
    )

    @Test
    fun `重启后可见待恢复记录并只恢复已确认条目`() = runTest {
        val repo = MemorySongs()
        val journals = RollbackJournalStore(MemoryStore())
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = ScrapeViewModel(
            queueStore = ScrapeQueueStore(MemoryStore(), existingSongIds = { repo.songs.keys.toSet() }),
            textMetaMatcher = TextMetaMatcher(emptyList()),
            coverMatcher = CoverMatcher(emptyList()),
            writebackOrchestrator = WritebackOrchestrator(
                repo, journals, AudioTagFileWriter { _, _, _ -> FileWriteResult(true) }, readReviewFileRevision = { "文件版本" },
            ),
            songRepository = repo,
        )
        try {
            advanceUntilIdle()
            assertNull(vm.pendingUndo.value)

            journals.write(RollbackJournal(1, "j-1", listOf(entry("甲", true), entry("乙", false))))
            vm.reloadQueue()
            advanceUntilIdle()
            assertEquals(ScrapeUndoInfo("j-1", 2, 1), vm.pendingUndo.value)
            assertEquals(listOf("甲"), vm.rollbackHistory.value.single().entries.map { it.songId })
            vm.previewHistory("j-1", "甲")
            advanceUntilIdle()
            assertEquals("旧甲", vm.historyPreview.value?.entry?.songBefore?.title)
            assertTrue(repo.updates.isEmpty())
            vm.closeHistoryPreview()
            assertNull(vm.historyPreview.value)

            vm.undoPendingJournal()
            advanceUntilIdle()
            assertEquals(listOf("甲"), repo.updates)
            assertEquals(listOf("乙"), journals.read()?.entries?.map { it.songId })
            assertEquals(ScrapeUndoInfo("j-1", 1, 1), vm.pendingUndo.value)
            assertTrue(vm.errorMessage.value?.contains("未能恢复") == true)

            journals.write(RollbackJournal(1, "j-1", listOf(entry("乙", true))))
            vm.reloadQueue()
            advanceUntilIdle()
            vm.undoPendingJournal()
            advanceUntilIdle()
            assertNull(journals.read())
            assertNull(vm.pendingUndo.value)
            assertEquals(listOf("甲", "乙"), repo.updates)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `放弃撤销记录会清空入口与记录且不改动曲库`() = runTest {
        val repo = MemorySongs()
        val journals = RollbackJournalStore(MemoryStore())
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = ScrapeViewModel(
            queueStore = ScrapeQueueStore(MemoryStore(), existingSongIds = { repo.songs.keys.toSet() }),
            textMetaMatcher = TextMetaMatcher(emptyList()),
            coverMatcher = CoverMatcher(emptyList()),
            writebackOrchestrator = WritebackOrchestrator(
                repo, journals, AudioTagFileWriter { _, _, _ -> FileWriteResult(true) },
            ),
            songRepository = repo,
        )
        try {
            advanceUntilIdle()
            journals.write(RollbackJournal(1, "j-2", listOf(entry("甲", false))))
            vm.reloadQueue()
            advanceUntilIdle()
            assertEquals(ScrapeUndoInfo("j-2", 1, 1), vm.pendingUndo.value)

            vm.discardPendingUndo()
            advanceUntilIdle()
            assertNull(journals.read())
            assertNull(vm.pendingUndo.value)
            assertTrue(repo.updates.isEmpty())
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}

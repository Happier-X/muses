package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.model.playback.QueueItem
import com.muses.player.core.model.playback.toSavedOnlineSong
import com.muses.player.core.model.playback.toRecentPlayEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackOnlineSnapshotTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }

    private fun song(id: String = "在线曲", platform: String = "wy") = Song(
        id, "脚本", OnlineTrackRef("脚本", platform, """{"songmid":"123"}""").encode(),
        "嗜好", "颜人中", "专辑", durationMs = 180000, coverUri = "https://example.com/cover.jpg",
        sourceType = SourceType.ONLINE,
    )

    @Test fun `在线快照在内存会话清空后仍恢复全部队列与进度`() = runTest {
        val store = MemoryStore()
        val songs = listOf(song(), song("未播放曲", "kw"))
        val snapshot = PlaybackStateRepository.PlaybackSnapshot(
            items = listOf(QueueItem("本地曲"), QueueItem(songs[0].id), QueueItem(songs[1].id), QueueItem(songs[0].id)),
            originalOrder = listOf(QueueItem(songs[1].id), QueueItem("本地曲"), QueueItem(songs[0].id)),
            shuffleOrder = listOf(QueueItem(songs[0].id), QueueItem("本地曲"), QueueItem(songs[1].id)),
            currentIndex = 1, currentSongId = songs[0].id, positionMs = 65432,
            onlineSongs = songs.mapNotNull { it.toSavedOnlineSong() },
        )
        PlaybackStateRepository(store).writeSnapshot(snapshot)
        OnlineTrackSession.clear()
        try {
            val restored = PlaybackStateRepository(store).readSnapshot()!!
            assertEquals(snapshot, restored)
            OnlineTrackSession.remember(restored.restoreOnlineSongs())
            assertEquals(songs.map { it.path }, songs.map { OnlineTrackSession.find(it.id)!!.path })
            assertEquals(180000L, OnlineTrackSession.find(songs[0].id)!!.durationMs)
            assertEquals(2, OnlineTrackSession.size())
        } finally { OnlineTrackSession.clear() }
    }

    @Test fun `旧快照从历史补救而新快照优先使用实际平台引用`() = runTest {
        val old = song()
        val resolved = song(platform = "kw")
        val snapshot = PlaybackStateRepository.PlaybackSnapshot(items = listOf(QueueItem(old.id)))
        assertEquals(old.path, snapshot.restoreOnlineSongs(listOf(old.toRecentPlayEntry(1))).single().path)
        assertEquals(resolved.path, snapshot.copy(onlineSongs = listOf(resolved.toSavedOnlineSong()!!))
            .restoreOnlineSongs(listOf(old.toRecentPlayEntry(1), song("无关曲").toRecentPlayEntry(1))).single().path)
    }

    @Test fun `删除队列歌曲同步清理其在线快照且普通直链不持久化`() = runTest {
        val store = MemoryStore()
        val repo = PlaybackStateRepository(store)
        val songs = listOf(song(), song("保留曲"))
        repo.writeSnapshot(PlaybackStateRepository.PlaybackSnapshot(
            items = songs.map { QueueItem(it.id) }, onlineSongs = songs.mapNotNull { it.toSavedOnlineSong() },
        ))
        repo.removeSongs(setOf(songs[0].id))
        assertEquals(listOf("保留曲"), repo.readSnapshot()!!.onlineSongs.map { it.id })
        assertNull(song().copy(path = "https://example.com/expired.mp3").toSavedOnlineSong())
        assertNull(song().copy(sourceType = SourceType.LOCAL).toSavedOnlineSong())
    }

    @Test fun `旧版字段和损坏的在线项不影响本地队列解码`() = runTest {
        val store = MemoryStore()
        store.data.value = emptyPreferences().toMutablePreferences().apply {
            this[stringPreferencesKey("playback_snapshot")] = """{"version":"1","items":[{"songId":"本地曲"}],"positionMs":123,"onlineSongs":[{"错误":"字段"}]}"""
        }.toPreferences()
        val restored = PlaybackStateRepository(store).readSnapshot()!!
        assertEquals(listOf(QueueItem("本地曲")), restored.items)
        assertEquals(123L, restored.positionMs)
        assertTrue(restored.onlineSongs.isEmpty())
    }
}

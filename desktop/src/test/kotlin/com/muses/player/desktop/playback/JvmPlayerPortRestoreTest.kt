package com.muses.player.desktop.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef
import com.muses.player.core.model.online.OnlineTrackSession
import com.muses.player.core.model.online.OnlineTrackResolver
import com.muses.player.core.model.online.OnlinePlayableUrl
import com.muses.player.core.model.playback.QueueItem
import com.muses.player.core.model.playback.toSavedOnlineSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JvmPlayerPortRestoreTest {
    private class MemoryStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
            transform(data.value).also { data.value = it }
    }

    @Test fun `查曲库挂起时立即发布所选歌曲和缓冲状态`() = runTest {
        val store = MemoryStore()
        val lookupStarted = CompletableDeferred<String>()
        val port = JvmPlayerPort(
            songLookup = { id -> lookupStarted.complete(id); awaitCancellation() },
            playbackStateRepository = PlaybackStateRepository(store),
            recentPlaysRepository = RecentPlaysRepository(store),
            scope = backgroundScope,
            factoryProvider = { error("等待查曲库时不应启动解码器") },
            errorLog = { _, _, _ -> },
        )
        try {
            port.restoreJob()!!.join()
            port.enqueue(listOf("在线歌曲"), 0)
            // 异步查询尚未执行，点选反馈已经发布。
            assertEquals("在线歌曲", port.currentSongId.value)
            assertEquals(JvmPlaybackStates.STATE_BUFFERING, port.playbackState.value)
            assertFalse(port.isPlaying.value)
            assertEquals("在线歌曲", lookupStarted.await())
            assertEquals("在线歌曲", port.currentSongId.value)
            port.enqueue(listOf("另一首在线歌曲"), 0)
            assertEquals("另一首在线歌曲", port.currentSongId.value)
            assertEquals(0L, port.positionMs.value)
        } finally { port.release() }
    }

    @Test fun `批量曲库为空仍恢复在线队列当前曲进度且不自动播放`() = runTest {
        val song = Song("在线曲", "脚本", OnlineTrackRef("脚本", "wy", """{"id":"123"}""").encode(),
            "嗜好", "颜人中", sourceType = SourceType.ONLINE)
        for (currentId in listOf(song.id, "已删除曲")) {
            OnlineTrackSession.clear()
            val store = MemoryStore()
            val repo = PlaybackStateRepository(store)
            repo.writeSnapshot(PlaybackStateRepository.PlaybackSnapshot(
                items = listOf(QueueItem("已删除曲"), QueueItem(song.id)),
                currentSongId = currentId, positionMs = 65432,
                onlineSongs = listOf(song.toSavedOnlineSong()!!),
            ))
            val requested = CompletableDeferred<OnlineTrackRef>()
            val port = JvmPlayerPort(
                songLookup = { id -> OnlineTrackSession.find(id)?.let {
                    JvmPlayerPort.SongRef(it.id, it.sourceId, it.path, it.title, it.artist, it.album, it.coverUri, it.sourceType)
                } }, songsExist = { emptySet() }, songsLookup = { emptyMap() },
                playbackStateRepository = repo, recentPlaysRepository = RecentPlaysRepository(store),
                scope = backgroundScope,
                factoryProvider = { error("此测试仅恢复状态，不启动解码器") },
                errorLog = { _, _, _ -> },
                onlineResolver = object : OnlineTrackResolver {
                    override suspend fun resolve(ref: OnlineTrackRef): OnlinePlayableUrl {
                        requested.complete(ref)
                        awaitCancellation()
                    }
                },
            )
            try {
                port.restoreJob()!!.join()
                assertEquals(listOf(song.id), port.activeOrderIds())
                assertEquals(song.id, port.currentSongId.value)
                assertEquals(if (currentId == song.id) 65432L else 0L, port.positionMs.value)
                assertFalse(port.isPlaying.value)
                assertEquals(song.path, OnlineTrackSession.find(song.id)!!.path)
                port.play()
                assertEquals(OnlineTrackRef.parse(song.path), requested.await())
                assertEquals(if (currentId == song.id) 65432L else 0L, port.positionMs.value)
            } finally { port.release(); OnlineTrackSession.clear() }
        }
    }
}

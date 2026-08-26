package com.muses.player.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 播放列表仓库层单测（阶段 2 check should-fix 补漏）：
 * 覆盖 addSongsToPlaylist 去重（库内已存在跳过 + 批内 distinct）、updatedAt touch、songIds 顺序。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistRepositoryTest {

    private lateinit var db: MusesDatabase
    private lateinit var repository: RoomPlaylistRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MusesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPlaylistRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedSong(id: String) {
        db.songDao().upsert(
            SongEntity(
                id = id,
                sourceId = "src",
                sourceType = "LOCAL",
                path = "/music/$id.flac",
                title = "song-$id",
            ),
        )
    }

    @Test
    fun `addSongsToPlaylist deduplicates within batch and against existing rows`() = runTest {
        seedSong("s1")
        seedSong("s2")
        seedSong("s3")
        val playlistId = repository.createPlaylist("test")

        repository.addSongsToPlaylist(playlistId, listOf("s1", "s2", "s1"))
        // 批内 distinct + 全部新 → 3 条中 s1/s2 各一次，共 2 条
        assertEquals(listOf("s1", "s2"), repository.observePlaylistSongIds(playlistId).first())

        // 库内已存在的 s2 跳过，仅追加 s3
        repository.addSongsToPlaylist(playlistId, listOf("s2", "s3", "s3"))
        assertEquals(listOf("s1", "s2", "s3"), repository.observePlaylistSongIds(playlistId).first())
    }

    @Test
    fun `mutating operations touch updatedAt`() = runTest {
        val playlistId = repository.createPlaylist("touch-me")
        seedSong("s1")
        val created = repository.observePlaylists().first().single { it.id == playlistId }.updatedAt

        Thread.sleep(10)
        repository.renamePlaylist(playlistId, "renamed")
        val afterRename = repository.observePlaylists().first().single { it.id == playlistId }
        assertTrue("rename should touch updatedAt", afterRename.updatedAt >= created + 10 - 5)

        Thread.sleep(10)
        repository.addSongsToPlaylist(playlistId, listOf("s1"))
        val afterAdd = repository.observePlaylists().first().single { it.id == playlistId }
        assertTrue("addSongs should touch updatedAt", afterAdd.updatedAt >= afterRename.updatedAt)
        assertEquals("renamed", afterAdd.name)
    }

    @Test
    fun `observePlaylistSongIds returns songs ordered by position`() = runTest {
        seedSong("a")
        seedSong("b")
        seedSong("c")
        val playlistId = repository.createPlaylist("ordered")

        repository.addSongsToPlaylist(playlistId, listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), repository.observePlaylistSongIds(playlistId).first())

        // 把 position 0 的 c 移到末尾
        repository.moveSong(playlistId, fromPosition = 0, toPosition = 2)
        assertEquals(listOf("a", "b", "c"), repository.observePlaylistSongIds(playlistId).first())

        // 移除中间的 b 后顺序保持且连续
        repository.removeSongFromPlaylist(playlistId, "b")
        assertEquals(listOf("a", "c"), repository.observePlaylistSongIds(playlistId).first())
        val detail = repository.observePlaylist(playlistId).first()
        assertNotNull(detail)
        assertEquals(listOf("a", "c"), detail!!.songs.map { it.id })
    }

    @Test
    fun `deletePlaylist removes it and cascades song rows`() = runTest {
        seedSong("s1")
        val keepId = repository.createPlaylist("keep")
        val dropId = repository.createPlaylist("drop")
        repository.addSongsToPlaylist(dropId, listOf("s1"))

        repository.deletePlaylist(dropId)

        assertEquals(listOf("keep"), repository.observePlaylists().first().map { it.name })
        assertTrue(repository.observePlaylistSongIds(dropId).first().isEmpty())
        // keep 列表不受影响
        assertTrue(repository.observePlaylistSongIds(keepId).first().isEmpty())
    }
}

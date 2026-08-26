package com.muses.player.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.PlaylistEntity
import com.muses.player.core.data.db.PlaylistSongEntity
import com.muses.player.core.data.db.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 播放列表 DAO 单测（内存 Room）：
 * CRUD、追加 position、删除后紧凑重排、reorder 事务正确性、播放列表/歌曲级联删除。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistDaoTest {

    private lateinit var db: MusesDatabase
    private lateinit var dao: PlaylistDao
    private lateinit var songDao: SongDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MusesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.playlistDao()
        songDao = db.songDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedSong(id: String) {
        songDao.upsert(
            SongEntity(
                id = id,
                sourceId = "src",
                sourceType = "LOCAL",
                path = "/music/$id.flac",
                title = "song-$id",
            ),
        )
    }

    private suspend fun seedPlaylist(
        id: String = "pl-1",
        name: String = "我的列表",
        updatedAt: Long = 1L,
    ): PlaylistEntity =
        PlaylistEntity(id = id, name = name, createdAt = 1L, updatedAt = updatedAt).also { dao.insert(it) }

    private fun row(playlistId: String, songId: String, position: Int) =
        PlaylistSongEntity(playlistId = playlistId, songId = songId, position = position)

    // ---- CRUD ----

    @Test
    fun insert_and_observePlaylists() = runTest {
        seedPlaylist(id = "p1", name = "A", updatedAt = 10L)
        seedPlaylist(id = "p2", name = "B", updatedAt = 20L)

        val playlists = dao.observePlaylists().first()
        assertEquals(listOf("p2", "p1"), playlists.map { it.id }) // updatedAt DESC
    }

    @Test
    fun rename_updatesNameAndUpdatedAt() = runTest {
        seedPlaylist(name = "旧名")
        dao.rename("pl-1", "新名", updatedAt = 99L)

        val renamed = dao.getById("pl-1")
        assertNotNull(renamed)
        assertEquals("新名", renamed!!.name)
        assertEquals(99L, renamed.updatedAt)
    }

    @Test
    fun deleteById_removesPlaylist() = runTest {
        seedPlaylist()
        dao.deleteById("pl-1")
        assertNull(dao.getById("pl-1"))
    }

    // ---- 追加 / 删除重排 ----

    @Test
    fun appendSongs_assignsSequentialPositionsFromMax() = runTest {
        seedPlaylist()
        listOf("s1", "s2", "s3").forEach { seedSong(it) }
        dao.appendSongs(listOf(row("pl-1", "s1", 0), row("pl-1", "s2", 1)))

        assertEquals(listOf("s1", "s2"), dao.getSongIds("pl-1"))
        assertEquals(1, dao.maxPosition("pl-1"))

        // 追加从 max+1 续位（仓库层语义）
        dao.appendSongs(listOf(row("pl-1", "s3", (dao.maxPosition("pl-1") ?: -1) + 1)))
        assertEquals(listOf("s1", "s2", "s3"), dao.getSongIds("pl-1"))
    }

    @Test
    fun removeSongAndCompact_keepsPositionsContiguous() = runTest {
        seedPlaylist()
        listOf("s1", "s2", "s3", "s4").forEach { seedSong(it) }
        dao.appendSongs(
            listOf(
                row("pl-1", "s1", 0),
                row("pl-1", "s2", 1),
                row("pl-1", "s3", 2),
                row("pl-1", "s4", 3),
            ),
        )

        dao.removeSongAndCompact("pl-1", "s2")

        assertEquals(listOf("s1", "s3", "s4"), dao.getSongIds("pl-1"))
        // position 连续 0..n-1：通过 maxPosition 验证
        assertEquals(2, dao.maxPosition("pl-1"))
        // 移除不存在的 songId 是安全 no-op
        dao.removeSongAndCompact("pl-1", "missing")
        assertEquals(listOf("s1", "s3", "s4"), dao.getSongIds("pl-1"))
    }

    // ---- reorder 事务正确性 ----

    @Test
    fun moveSong_movesWithinBounds_singleTransaction() = runTest {
        seedPlaylist()
        listOf("s1", "s2", "s3", "s4", "s5").forEach { seedSong(it) }
        dao.appendSongs(
            listOf("s1", "s2", "s3", "s4", "s5").mapIndexed { i, s -> row("pl-1", s, i) },
        )

        dao.moveSong("pl-1", fromPosition = 0, toPosition = 3)
        assertEquals(listOf("s2", "s3", "s4", "s1", "s5"), dao.getSongIds("pl-1"))

        dao.moveSong("pl-1", fromPosition = 4, toPosition = 0)
        assertEquals(listOf("s5", "s2", "s3", "s4", "s1"), dao.getSongIds("pl-1"))

        // 越界与原地移动均为 no-op
        dao.moveSong("pl-1", fromPosition = -1, toPosition = 0)
        dao.moveSong("pl-1", fromPosition = 0, toPosition = 99)
        dao.moveSong("pl-1", fromPosition = 2, toPosition = 2)
        assertEquals(listOf("s5", "s2", "s3", "s4", "s1"), dao.getSongIds("pl-1"))
    }

    // ---- 级联删除 ----

    @Test
    fun deletePlaylist_cascadesToPlaylistSongs() = runTest {
        seedPlaylist(id = "pl-a")
        seedPlaylist(id = "pl-b")
        listOf("s1", "s2").forEach { seedSong(it) }
        dao.appendSongs(
            listOf(row("pl-a", "s1", 0), row("pl-a", "s2", 1), row("pl-b", "s1", 0)),
        )

        dao.deleteById("pl-a")

        assertEquals(emptyList<String>(), dao.getSongIds("pl-a"))
        assertEquals(listOf("s1"), dao.getSongIds("pl-b")) // 其他列表不受影响
    }

    @Test
    fun deleteSong_cascadesPlaylistRows() = runTest {
        seedPlaylist()
        seedSong("s1")
        seedSong("s2")
        dao.appendSongs(listOf(row("pl-1", "s1", 0), row("pl-1", "s2", 1)))

        songDao.deleteById("s1")

        assertEquals(listOf("s2"), dao.getSongIds("pl-1"))
    }
}

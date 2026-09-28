package com.muses.player.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongAlbumCrossRef
import com.muses.player.core.data.db.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongDaoTest {

    private lateinit var database: MusesDatabase
    private lateinit var songDao: SongDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        songDao = database.songDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun song(id: String, title: String, sourceId: String = "s1") = SongEntity(
        id = id,
        sourceId = sourceId,
        sourceType = "LOCAL",
        path = "/music/$id.mp3",
        title = title,
    )

    @Test
    fun insert_and_getById() = runTest {
        songDao.insertAll(listOf(song("a", "晴天"), song("b", "七里香")))
        assertEquals("晴天", songDao.getById("a")?.title)
        assertNull(songDao.getById("missing"))
    }

    @Test
    fun upsert_updates_existing_row() = runTest {
        songDao.upsert(song("a", "旧标题"))
        songDao.upsert(song("a", "新标题"))
        assertEquals(1, songDao.count())
        assertEquals("新标题", songDao.getById("a")?.title)
    }

    @Test
    fun observeAll_orders_by_title() = runTest {
        songDao.insertAll(listOf(song("b", "Banana"), song("a", "Apple")))
        val titles = songDao.observeAll().first().map { it.title }
        assertEquals(listOf("Apple", "Banana"), titles)
    }

    @Test
    fun searchByTitle_matches_substring_case_insensitive() = runTest {
        songDao.insertAll(
            listOf(
                song("a", "Love Story"),
                song("b", "love myself"),
                song("c", "Hate Story"),
            ),
        )
        val hits = songDao.searchByTitle("love")
        assertEquals(setOf("Love Story", "love myself"), hits.map { it.title }.toSet())
    }

    @Test
    fun markAllMissing_hides_songs_without_deleting() = runTest {
        songDao.insertAll(
            listOf(
                song("keep", "保留曲"),
                song("gone", "丢失曲"),
                song("other", "他源歌曲", sourceId = "s2"),
            ),
        )
        songDao.markAllMissing("s1")
        // 行仍在（未硬删），但列表/搜索不再返回
        assertEquals(3, songDao.count())
        assertEquals(listOf("gone", "keep"), songDao.getBySource("s1").map { it.id }.sorted())
        assertEquals(listOf("other"), songDao.observeAll().first().map { it.id })
        assertTrue(songDao.searchByTitle("丢失").isEmpty())
        // 他音源不受影响
        assertEquals(listOf("other"), songDao.getBySource("s2").map { it.id })
    }

    @Test
    fun insert_resets_missing_flag_on_rescan() = runTest {
        songDao.insertAll(listOf(song("a", "曲")))
        songDao.markAllMissing("s1")
        assertTrue(songDao.observeAll().first().isEmpty())
        // 重扫命中同 id 重新 upsert → 「丢失」标记复位
        songDao.insertAll(listOf(song("a", "曲")))
        assertEquals(listOf("a"), songDao.observeAll().first().map { it.id })
    }

    @Test
    fun crossRefs_cascade_delete_with_songs() = runTest {
        songDao.insertAll(listOf(song("a", "t")))
        database.albumDao().insertAll(
            listOf(com.muses.player.core.data.db.AlbumEntity(id = "al1", title = "Album")),
        )
        songDao.insertSongAlbumRefs(listOf(SongAlbumCrossRef(songId = "a", albumId = "al1")))

        songDao.deleteBySource("s1")
        val refs = database.openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM song_album_cross_ref").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(0, refs)
    }

    @Test
    fun count_returns_zero_for_empty_library() = runTest {
        assertTrue(songDao.count() == 0)
    }
}

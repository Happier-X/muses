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
    fun replaceSourceSongs_keeps_other_source_and_removes_missing() = runTest {
        songDao.insertAll(
            listOf(
                song("keep-old", "保留旧曲"),
                song("drop", "将被删除"),
                song("other", "他源歌曲", sourceId = "s2"),
            ),
        )
        songDao.replaceSourceSongs("s1", listOf(song("keep-old", "保留旧曲"), song("new", "新增")))
        val s1 = songDao.getBySource("s1").map { it.id }.toSet()
        assertEquals(setOf("keep-old", "new"), s1)
        // 他音源不受影响
        assertEquals(1, songDao.getBySource("s2").size)
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

package com.muses.player.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.MetaFieldSource
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
 * 扫描入库合并回归：重扫不再整行覆盖（富化字段保留）+ 未扫到只标记「丢失」不硬删 +
 * 空结果/锐减保护。规格见 [RoomSongRepository.replaceSourceSongs]。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SongRepositoryScanMergeTest {

    private lateinit var database: MusesDatabase
    private lateinit var repository: RoomSongRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomSongRepository(database.songDao(), database.albumDao(), database.artistDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** 扫描器产出的「发现层」歌（只有文件名，无富化字段） */
    private fun song(id: String, title: String = id, sourceId: String = "s1") = Song(
        id = id,
        sourceId = sourceId,
        path = "/music/$id.mp3",
        title = title,
        sourceType = SourceType.LOCAL,
    )

    @Test
    fun 重扫保留已刮削与已懒扫描的富化字段() = runTest {
        repository.replaceSourceSongs("s1", listOf(song("a", title = "文件名")))
        // 模拟刮削/懒扫描写入富化层
        database.songDao().upsert(
            SongEntity(
                id = "a",
                sourceId = "s1",
                sourceType = "LOCAL",
                path = "/music/a.mp3",
                title = "刮削标题",
                artist = "歌手",
                coverUri = "file:///c.jpg",
                lyrics = "[00:01]词",
                metaTitle = MetaFieldSource.SCRAPE.wire,
                tagsVersion = 1,
            ),
        )

        val result = repository.replaceSourceSongs("s1", listOf(song("a", title = "文件名")))

        assertEquals(0, result.added)
        val stored = database.songDao().getById("a")!!
        assertEquals("刮削标题", stored.title)
        assertEquals("歌手", stored.artist)
        assertEquals("file:///c.jpg", stored.coverUri)
        assertEquals("[00:01]词", stored.lyrics)
        assertEquals(1, stored.tagsVersion)
        assertTrue(!stored.missing)
    }

    @Test
    fun 未扫到的歌标记丢失而不是删除() = runTest {
        repository.replaceSourceSongs("s1", listOf(song("a"), song("b")))

        val result = repository.replaceSourceSongs("s1", listOf(song("a"), song("c")))

        assertEquals(1, result.added)
        assertEquals(1, result.missing)
        // 列表只剩 a/c；b 行仍在库里且被标记为丢失
        assertEquals(setOf("a", "c"), repository.observeSongs().first().map { it.id }.toSet())
        val gone = database.songDao().getById("b")
        assertNotNull(gone)
        assertTrue(gone!!.missing)
    }

    @Test
    fun 空结果不清库() = runTest {
        repository.replaceSourceSongs("s1", listOf(song("a"), song("b")))

        val result = repository.replaceSourceSongs("s1", emptyList())

        assertTrue(result.skipped)
        assertEquals(setOf("a", "b"), repository.observeSongs().first().map { it.id }.toSet())
    }

    @Test
    fun 锐减保护拦下异常扫描() = runTest {
        repository.replaceSourceSongs("s1", (1..10).map { song("s$it") })

        // 只剩 3 首（< 10 的一半）：疑似音源异常，拒绝落库
        val result = repository.replaceSourceSongs("s1", listOf(song("s1"), song("s2"), song("s3")))

        assertTrue(result.skipped)
        assertEquals(10, repository.observeSongs().first().size)
    }

    @Test
    fun 小幅减少正常落库并标记丢失() = runTest {
        repository.replaceSourceSongs("s1", (1..10).map { song("s$it") })

        val result = repository.replaceSourceSongs("s1", (1..6).map { song("s$it") })

        assertTrue(!result.skipped)
        assertEquals(4, result.missing)
        assertEquals(6, repository.observeSongs().first().size)
    }
}

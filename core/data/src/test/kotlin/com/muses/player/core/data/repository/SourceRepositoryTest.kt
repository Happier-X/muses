package com.muses.player.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.dao.SourceDao
import com.muses.player.core.data.db.MusesDatabase
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SourceRepositoryTest {

    private lateinit var database: MusesDatabase
    private lateinit var sourceDao: SourceDao
    private lateinit var repository: RoomSourceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sourceDao = database.sourceDao()
        repository = RoomSourceRepository(sourceDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun testSource(
        id: String = "src-1",
        name: String = "本地音乐",
        type: SourceType = SourceType.LOCAL,
        url: String? = null,
        path: String? = "/storage/emulated/0/Music",
        createdAt: Long = 1000L,
        updatedAt: Long = 2000L,
    ) = Source(
        id = id,
        name = name,
        type = type,
        url = url,
        path = path,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun upsert_and_observeSources() = runTest {
        val source = testSource()
        repository.upsert(source)

        val sources = repository.observeSources().first()
        assertEquals(1, sources.size)
        assertEquals("本地音乐", sources[0].name)
        assertEquals(SourceType.LOCAL, sources[0].type)
    }

    @Test
    fun upsert_updates_existing_source() = runTest {
        repository.upsert(testSource(name = "旧名称"))
        repository.upsert(testSource(name = "新名称"))

        val sources = repository.observeSources().first()
        assertEquals(1, sources.size)
        assertEquals("新名称", sources[0].name)
    }

    @Test
    fun getSource_returns_existing() = runTest {
        repository.upsert(testSource())
        val result = repository.getSource("src-1")
        assertNotNull(result)
        assertEquals("src-1", result?.id)
    }

    @Test
    fun getSource_returns_null_for_missing() = runTest {
        assertNull(repository.getSource("missing"))
    }

    @Test
    fun deleteById_removes_source() = runTest {
        repository.upsert(testSource())
        repository.deleteById("src-1")

        val sources = repository.observeSources().first()
        assertEquals(0, sources.size)
        assertNull(repository.getSource("src-1"))
    }

    @Test
    fun multiple_sources_ordered_by_createdAt() = runTest {
        repository.upsert(testSource(id = "src-b", name = "WebDAV", type = SourceType.WEBDAV, url = "https://dav.example.com", path = null))
        repository.upsert(testSource(id = "src-a", name = "本地", createdAt = 500L))

        val sources = repository.observeSources().first()
        assertEquals(2, sources.size)
        // 按 createdAt 升序
        assertEquals("本地", sources[0].name)
        assertEquals("WebDAV", sources[1].name)
    }

    @Test
    fun webdav_source_stores_url_not_path() = runTest {
        val source = Source(
            id = "webdav-1",
            name = "NAS",
            type = SourceType.WEBDAV,
            url = "https://nas.local/dav/music/",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        repository.upsert(source)

        val result = repository.getSource("webdav-1")
        assertNotNull(result)
        assertEquals("https://nas.local/dav/music/", result?.url)
        assertNull(result?.path)
        assertEquals(SourceType.WEBDAV, result?.type)
    }

    @Test
    fun local_source_stores_path_not_url() = runTest {
        val source = Source(
            id = "local-1",
            name = "本地音乐",
            type = SourceType.LOCAL,
            path = "/storage/emulated/0/Music",
            createdAt = 1000L,
            updatedAt = 1000L,
        )
        repository.upsert(source)

        val result = repository.getSource("local-1")
        assertNotNull(result)
        assertEquals("/storage/emulated/0/Music", result?.path)
        assertNull(result?.url)
        assertEquals(SourceType.LOCAL, result?.type)
    }
}

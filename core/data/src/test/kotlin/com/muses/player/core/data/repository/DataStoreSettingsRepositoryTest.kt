package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var repository: DataStoreSettingsRepository
    private lateinit var dataStoreFile: File

    @Before
    fun setUp() {
        dataStoreFile = File(tmpFolder.root, "test_settings.preferences_pb")
    }

    private fun TestScope.createRepository() = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { dataStoreFile },
        ),
    )

    @Test
    fun 默认值_未扫描时时间戳为0() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        assertEquals(0L, repository.lastScanTimestamp.first())
    }

    @Test
    fun 更新扫描时间戳后可读取() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        val now = System.currentTimeMillis()
        repository.updateLastScanTimestamp(now)
        assertEquals(now, repository.lastScanTimestamp.first())
    }

    @Test
    fun 封面强调色默认开启且切换后持久化() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        assertEquals(true, repository.coverContentColorEnabled.first())
        repository.setCoverContentColorEnabled(false)
        assertEquals(false, repository.coverContentColorEnabled.first())
        repository.setCoverContentColorEnabled(true)
        assertEquals(true, repository.coverContentColorEnabled.first())
    }

    @Test
    fun 歌词翻译默认开启且切换后持久化() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        assertEquals(true, repository.lyricTranslationEnabled.first())
        repository.setLyricTranslationEnabled(false)
        assertEquals(false, repository.lyricTranslationEnabled.first())
        repository.setLyricTranslationEnabled(true)
        assertEquals(true, repository.lyricTranslationEnabled.first())
    }

    @Test
    fun 歌词注音默认开启且切换后持久化() = runTest(StandardTestDispatcher()) {
        repository = createRepository()
        assertEquals(true, repository.lyricRomanizationEnabled.first())
        repository.setLyricRomanizationEnabled(false)
        assertEquals(false, repository.lyricRomanizationEnabled.first())
        repository.setLyricRomanizationEnabled(true)
        assertEquals(true, repository.lyricRomanizationEnabled.first())
    }
}

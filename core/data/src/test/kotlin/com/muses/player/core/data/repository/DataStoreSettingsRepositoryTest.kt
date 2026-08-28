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
}

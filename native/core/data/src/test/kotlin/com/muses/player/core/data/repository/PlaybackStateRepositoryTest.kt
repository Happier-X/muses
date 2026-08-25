package com.muses.player.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.model.playback.QueueItem
import com.muses.player.core.model.playback.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 规格 = src/features/player/queue.ts + session.ts 持久化语义 */
class PlaybackStateRepositoryTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newRepo(): PlaybackStateRepository {
        val file = File(tmp.root, "pb_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return PlaybackStateRepository(dataStore)
    }

    @Test
    fun `空存储读取快照为null`() = runTest {
        assertNull(newRepo().readSnapshot())
    }

    @Test
    fun `快照roundtrip含三序列与进度`() = runTest {
        val repo = newRepo()
        val snapshot = PlaybackStateRepository.PlaybackSnapshot(
            items = listOf(QueueItem("a"), QueueItem("b")),
            originalOrder = listOf(QueueItem("b"), QueueItem("a")),
            shuffleOrder = listOf(QueueItem("b")),
            currentIndex = 1,
            positionMs = 65432L,
            currentSongId = "b",
        )
        repo.writeSnapshot(snapshot)
        assertEquals(snapshot, repo.readSnapshot())
    }

    @Test
    fun `position负数容错归零`() = runTest {
        val decoded = decodeForTest("""{"version":"1","items":[],"originalOrder":[],"currentIndex":"0","positionMs":-500,"currentSongId":"x"}""")
        assertEquals(0L, decoded?.positionMs)
    }

    @Test
    fun `坏数据宽松回退`() = runTest {
        assertNull(decodeForTest("not-json{"))
        // 结构不符（缺 items 数组）→ 与 Web 一致回退默认空表，不抛错
        val decoded = decodeForTest("""{"foo":[]}""")
        assertTrue(decoded == null || decoded.items.isEmpty())
    }

    @Test
    fun `配置默认值对齐queue_ts_defaultConfig`() = runTest {
        val config = newRepo().readConfig()
        assertEquals(RepeatMode.ALL, config.repeatMode)
        assertEquals(false, config.shuffleEnabled)
        // 缺键时 loudness 默认 true；仅显式 false 关闭
        assertEquals(true, config.loudnessNormalizeEnabled)
    }

    @Test
    fun `配置显式false关闭loudness且roundtrip`() = runTest {
        val repo = newRepo()
        repo.writeConfig(
            com.muses.player.core.model.playback.PlayerConfig(
                repeatMode = RepeatMode.ONE,
                shuffleEnabled = true,
                loudnessNormalizeEnabled = false,
            ),
        )
        val read = repo.readConfig()
        assertEquals(RepeatMode.ONE, read.repeatMode)
        assertTrue(read.shuffleEnabled)
        assertEquals(false, read.loudnessNormalizeEnabled)
    }

    // 反射规避 private decode 的桥接：直接走 write+read 验证容错语义
    private suspend fun decodeForTest(raw: String): PlaybackStateRepository.PlaybackSnapshot? {
        val repo = newRepo()
        // 写入原始坏/好数据再走标准读取路径（DataStore 值即 JSON 字符串）
        val file = File(tmp.root, "raw_${System.nanoTime()}.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        ds.edit { prefs ->
            prefs[androidx.datastore.preferences.core.stringPreferencesKey("playback_snapshot")] = raw
        }
        return PlaybackStateRepository(ds).readSnapshot()
    }

    // 抑制未用导入告警的引用点
    @Suppress("unused")
    private val jsonArrayRef: JsonArray? = null
}

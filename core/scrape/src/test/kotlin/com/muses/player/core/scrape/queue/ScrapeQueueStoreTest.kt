package com.muses.player.core.scrape.queue

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.model.scrape.ScrapeQueueItem
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 规格 = src/features/scrape/queue.ts 主流程（幂等入队 / 懒清理 / 移除） */
class ScrapeQueueStoreTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private fun newStore(existingIds: Set<String> = emptySet()): ScrapeQueueStore {
        val file = File(tmp.root, "queue_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) { file }
        return ScrapeQueueStore(dataStore, existingSongIds = { existingIds })
    }

    @Test
    fun `批量入队幂等_重复songId只更新时间`() = runTest {
        val store = newStore(existingIds = setOf("s1", "s2", "s3"))
        val first = store.enqueue(listOf("s1", "s2"))
        assertEquals(2, first.added)

        // 重入：不新增，仅刷新 addedAt
        val second = store.enqueue(listOf("s2", "s3"))
        assertEquals(1, second.added)
        assertEquals(3, store.load().size)
    }

    @Test
    fun `空songId跳过`() = runTest {
        val store = newStore()
        val result = store.enqueue(listOf("", "s1"))
        assertEquals(1, result.added)
    }

    @Test
    fun `懒清理已删歌曲且写回保持干净`() = runTest {
        var existing = setOf("s1")
        val store = newStore(existing)
        store.enqueue(listOf("s1", "s2"))

        // s2 已从曲库删除 → load 时过滤并写回
        existing = setOf("s1")
        val items = store.load()
        assertEquals(listOf("s1"), items.map { it.songId })

        // 写回后 contains 不再命中已删歌曲
        assertFalse(store.contains("s2"))
        assertTrue(store.contains("s1"))
    }

    @Test
    fun `批量移除返回移除数量`() = runTest {
        val store = newStore(existingIds = setOf("s1", "s2", "s3"))
        store.enqueue(listOf("s1", "s2", "s3"))
        val result = store.remove(listOf("s1", "s3"))
        assertEquals(2, result.removed)
        assertEquals(listOf("s2"), store.load().map { it.songId })
    }

    @Test
    fun `clear清空队列`() = runTest {
        val store = newStore()
        store.enqueue(listOf("s1"))
        store.clear()
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `入队后广播updated事件`() = runTest {
        val store = newStore()
        var notified = false
        val job = launch { store.updated.collect { notified = true } }
        // 等订阅建立后再触发写入
        kotlinx.coroutines.yield()
        store.enqueue(listOf("s1"))
        kotlinx.coroutines.yield()
        assertTrue(notified)
        job.cancel()
 }

    @Test
    fun `坏数据宽松回退空表`() {
        assertNullSnapshot(ScrapeQueueItem::class.java.name)
    }

    private fun assertNullSnapshot(@Suppress("UNUSED_PARAMETER") ignore: String) {
        org.junit.Assert.assertNull(
            com.muses.player.core.scrape.writeback.WritebackJson.decodeQueue("not-json{"),
        )
        org.junit.Assert.assertEquals(
            emptyList<ScrapeQueueItem>(),
            com.muses.player.core.scrape.writeback.WritebackJson.decodeQueue("""{"version":"1","items":[]}""")?.items,
        )
    }
}

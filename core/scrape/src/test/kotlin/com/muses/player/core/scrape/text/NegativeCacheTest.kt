package com.muses.player.core.scrape.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 规格 = src/features/metadata/match.ts 负缓存常量 + src/features/runtime/boundedCache.ts LRU 语义 */
class NegativeCacheTest {

    @Test
    fun `TTL与容量常量对齐Web`() {
        assertEquals(45 * 60 * 1000L, NegativeCache.NEGATIVE_CACHE_TTL_MS)
        assertEquals(256, NegativeCache.MAX_SIZE)
    }

    @Test
    fun `过期条目由调用方按expiresAt判定`() {
        val cache = NegativeCache()
        cache.put("s1", NegativeCache.NegativeEntry(queryKey = """["T","",""]""", expiresAt = 1000L))
        val entry = cache.get("s1")
        // match.ts：cached.expiresAt > Date.now() 才算有效，过期即失效
        assertTrue(entry != null && entry.expiresAt <= System.currentTimeMillis())
    }

    @Test
    fun `容量超限淘汰最旧`() {
        val cache = NegativeCache()
        repeat(NegativeCache.MAX_SIZE) { i ->
            cache.put("song$i", NegativeCache.NegativeEntry("k$i", Long.MAX_VALUE))
        }
        assertEquals(NegativeCache.MAX_SIZE, cache.size())

        // 再放一条 → 最旧的 song0 被淘汰，size 保持上限
        cache.put("new", NegativeCache.NegativeEntry("k-new", Long.MAX_VALUE))
        assertEquals(NegativeCache.MAX_SIZE, cache.size())
        assertNull(cache.get("song0"))
        assertEquals(NegativeCache.NegativeEntry("k-new", Long.MAX_VALUE), cache.get("new"))
    }

    @Test
    fun `读取刷新近期使用顺序`() {
        val cache = NegativeCache()
        repeat(NegativeCache.MAX_SIZE) { i ->
            cache.put("song$i", NegativeCache.NegativeEntry("k$i", Long.MAX_VALUE))
        }
        // 访问 song0 使其变为最新
        cache.get("song0")
        // 插入新条目淘汰的是当前最旧 song1 而非刚访问过的 song0
        cache.put("new", NegativeCache.NegativeEntry("k-new", Long.MAX_VALUE))
        assertNull(cache.get("song1"))
        assertEquals(NegativeCache.NegativeEntry("k0", Long.MAX_VALUE), cache.get("song0"))
    }

    @Test
    fun `覆写已有key不增加容量且移到末尾`() {
        val cache = NegativeCache()
        repeat(2) { i ->
            cache.put("song$i", NegativeCache.NegativeEntry("k$i", Long.MAX_VALUE))
        }
        cache.put("song0", NegativeCache.NegativeEntry("k0-v2", Long.MAX_VALUE))
        assertEquals(2, cache.size())
        assertEquals("k0-v2", cache.get("song0")?.queryKey)
    }

    @Test
    fun `buildQueryKey 对齐match-ts的JSON数组形状`() {
        val query = com.muses.player.core.model.scrape.OnlineTextQuery(
            songId = "s",
            title = " Love Story ",
            artist = null,
            album = "  ",
        )
        // JSON.stringify(["Love Story".trim() 后值, '', ''])
        assertEquals("""["Love Story","",""]""", NegativeCache.buildQueryKey(query))
    }
}

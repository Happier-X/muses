package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextQuery
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 歌曲级负缓存（规格书 = src/features/metadata/match.ts）：
 * - TTL 45 分钟、容量上限 256，超限淘汰最旧条目；
 * - key = songId；value 含 queryKey（title/artist/album JSON 数组）+ 过期时间。
 *
 * 有界 Map 采用插入序 LRU（对齐 src/features/runtime/boundedCache.ts 的
 * getBoundedCache / setBoundedCache：命中/覆写时移到末尾）。
 */
class NegativeCache {

    data class NegativeEntry(
        val queryKey: String,
        val expiresAt: Long,
    )

    private val map = LinkedHashMap<String, NegativeEntry>()

    /** match.ts NEGATIVE_CACHE_TTL_MS = 45 * 60 * 1000 */
    fun ttlMs(): Long = NEGATIVE_CACHE_TTL_MS

    fun get(songId: String): NegativeEntry? = synchronized(map) {
        // 对齐 boundedCache.ts getBoundedCache：读取刷新近期使用顺序
        val value = map[songId]
        if (value == null) {
            null
        } else {
            map.remove(songId)
            map[songId] = value
            value
        }
    }

    fun put(songId: String, entry: NegativeEntry): Unit = synchronized(map) {
        // 对齐 boundedCache.ts setBoundedCache：先删后插移到末尾，超限淘汰最旧
        map.remove(songId)
        map[songId] = entry
        while (map.size > MAX_SIZE) {
            val oldestKey = map.keys.firstOrNull() ?: break
            map.remove(oldestKey)
        }
    }

    fun size(): Int = synchronized(map) { map.size }

    /** match.ts resetOnlineTextMetaCache */
    fun clear() = synchronized(map) { map.clear() }

    companion object {
        const val NEGATIVE_CACHE_TTL_MS: Long = 45 * 60 * 1000L
        const val MAX_SIZE: Int = 256

        private val json = Json
        private val stringListSerializer = ListSerializer(String.serializer())

        /**
         * match.ts buildQueryKey：
         * JSON.stringify([title.trim(), artist?.trim() || '', album?.trim() || ''])
         */
        fun buildQueryKey(query: OnlineTextQuery): String =
            json.encodeToString(
                stringListSerializer,
                listOf(
                    query.title.trim(),
                    query.artist?.trim().orEmpty(),
                    query.album?.trim().orEmpty(),
                ),
            )
    }
}

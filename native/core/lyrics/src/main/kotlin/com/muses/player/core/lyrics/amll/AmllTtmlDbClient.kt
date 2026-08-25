package com.muses.player.core.lyrics.amll

import com.muses.player.core.model.lyrics.AmllFailReason
import com.muses.player.core.model.lyrics.AmllMatchQuery
import com.muses.player.core.model.lyrics.AmllMatchResult
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.provider.enc
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * AMLL TTML 聚合库查询（规格书 = src/features/lyrics/amllTtmlDb.ts matchAmllTtmlLyrics）。
 * - 索引内存缓存；TTML 按 songId 缓存（容量 256）；负缓存短时（TTL 5min）
 * - 失败不抛到 UI 层，统一返回 Fail(reason)
 */
open class AmllTtmlDbClient(
    private val http: LyricsHttp,
    private val indexRepository: AmllIndexRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {

    companion object {
        /** amllTtmlDb.ts NEGATIVE_CACHE_TTL_MS = 5 分钟 */
        const val NEGATIVE_CACHE_TTL_MS: Long = 5 * 60_000L

        /** amllTtmlDb.ts SONG_CACHE_MAX_SIZE */
        const val SONG_CACHE_MAX_SIZE: Int = 256
    }

    private data class TtmlCacheEntry(val queryKey: String, val ttml: String, val rawLyricFile: String, val score: Int)
    private data class NegativeEntry(val queryKey: String, val reason: AmllFailReason, val expiresAt: Long)

    private val ttmlBySongId = LinkedHashMap<String, TtmlCacheEntry>()
    private val negativeBySongId = LinkedHashMap<String, NegativeEntry>()

    @Suppress("SameParameterValue")
    private fun <V> boundedPut(map: LinkedHashMap<String, V>, key: String, value: V, maxSize: Int) {
        map[key] = value
        while (map.size > maxSize) {
            map.remove(map.keys.first())
        }
    }

    private fun createQueryKey(query: AmllMatchQuery): String =
        // JSON.stringify([title.trim(), artist?.trim() || '', album?.trim() || '']) 对齐
        Json.encodeToString(
            ListSerializer(String.serializer()),
            listOf(
                query.title.trim(),
                query.artist?.trim().orEmpty(),
                query.album?.trim().orEmpty(),
            ),
        )

    private fun getNegative(songId: String, queryKey: String): NegativeEntry? = synchronized(negativeBySongId) {
        val entry = negativeBySongId[songId] ?: return null
        if (entry.queryKey != queryKey || nowMs() > entry.expiresAt) {
            negativeBySongId.remove(songId)
            return null
        }
        return entry
    }

    private fun isSafeRawLyricFile(rawLyricFile: String): Boolean =
        rawLyricFile.isNotEmpty() && !rawLyricFile.contains('/') && !rawLyricFile.contains('\\') &&
            rawLyricFile != "." && rawLyricFile != ".."

    private sealed interface TtmlFetch {
        data class Ok(val text: String) : TtmlFetch
        data class Err(val reason: AmllFailReason) : TtmlFetch
    }

    /** fetchTtml：parse/network 二分；TTML 形状校验对齐 /<(?:[a-z][\w.-]*:)?tt(?:\s|>)/i */
    private suspend fun fetchTtml(rawLyricFile: String): TtmlFetch {
        if (!isSafeRawLyricFile(rawLyricFile)) return TtmlFetch.Err(AmllFailReason.PARSE)
        val url = "$AMLL_TTML_BASE_URL${enc(rawLyricFile)}"
        return try {
            val text = http.getText(url, emptyMap(), AMLL_TTML_TIMEOUT_SEC)
            val trimmed = text.trim()
            val shapeOk = Regex("<(?:[a-z][\\w.-]*:)?tt(?:\\s|>)", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)
            if (trimmed.isEmpty() || !shapeOk) {
                TtmlFetch.Err(AmllFailReason.PARSE)
            } else {
                TtmlFetch.Ok(text)
            }
        } catch (_: Exception) {
            TtmlFetch.Err(AmllFailReason.NETWORK)
        }
    }

    /**
     * 从 amll-ttml-db 为歌曲匹配 TTML 歌词（matchAmllTtmlLyrics 主流程）。
     */
    open suspend fun match(query: AmllMatchQuery): AmllMatchResult {
        val songId = query.songId.trim()
        if (songId.isEmpty() || query.title.isBlank()) {
            return AmllMatchResult.Fail(AmllFailReason.NO_MATCH)
        }

        val queryKey = createQueryKey(query)

        // TTML 正缓存：queryKey 一致才命中，否则删除旧条目
        synchronized(ttmlBySongId) {
            ttmlBySongId[songId]?.let { cached ->
                if (cached.queryKey == queryKey) {
                    return AmllMatchResult.Ok(
                        ttml = cached.ttml,
                        rawLyricFile = cached.rawLyricFile,
                        score = cached.score,
                    )
                }
                ttmlBySongId.remove(songId)
            }
        }

        // 负缓存短路
        getNegative(songId, queryKey)?.let { negative ->
            return AmllMatchResult.Fail(negative.reason)
        }

        fun setNegative(reason: AmllFailReason) {
            synchronized(negativeBySongId) {
                boundedPut(
                    negativeBySongId,
                    songId,
                    NegativeEntry(queryKey, reason, nowMs() + NEGATIVE_CACHE_TTL_MS),
                    SONG_CACHE_MAX_SIZE,
                )
            }
        }

        // 索引加载（单飞）
        val index = try {
            indexRepository.ensureIndex()
        } catch (_: Exception) {
            setNegative(AmllFailReason.NETWORK)
            return AmllMatchResult.Fail(AmllFailReason.NETWORK)
        }

        val searchIndex = indexRepository.currentSearchIndex() ?: createSearchIndex(index)
        val candidates = searchIndex.selectCandidates(query.title)
        val best = findBestMatch(query, candidates)

        if (best == null) {
            setNegative(AmllFailReason.NO_MATCH)
            return AmllMatchResult.Fail(AmllFailReason.NO_MATCH)
        }

        return when (val fetch = fetchTtml(best.entry.rawLyricFile)) {
            is TtmlFetch.Err -> {
                setNegative(fetch.reason)
                AmllMatchResult.Fail(fetch.reason)
            }
            is TtmlFetch.Ok -> {
                val hit = TtmlCacheEntry(queryKey, fetch.text, best.entry.rawLyricFile, best.score)
                synchronized(ttmlBySongId) {
                    boundedPut(ttmlBySongId, songId, hit, SONG_CACHE_MAX_SIZE)
                }
                synchronized(negativeBySongId) { negativeBySongId.remove(songId) }
                AmllMatchResult.Ok(
                    ttml = hit.ttml,
                    rawLyricFile = hit.rawLyricFile,
                    score = hit.score,
                    confidence = best.confidence,
                )
            }
        }
    }

    /** 测试辅助：清空运行时缓存 */
    fun resetCacheForTests() {
        synchronized(ttmlBySongId) { ttmlBySongId.clear() }
        synchronized(negativeBySongId) { negativeBySongId.clear() }
    }

    /** 测试辅助：TTML 缓存是否命中 */
    fun hasTtmlCacheForTests(songId: String): Boolean =
        synchronized(ttmlBySongId) { ttmlBySongId.containsKey(songId) }

    /** 测试辅助：候选数量（走 selectCandidates） */
    fun candidateCountForTests(title: String): Int =
        indexRepository.currentSearchIndex()?.selectCandidates(title)?.size ?: -1
}

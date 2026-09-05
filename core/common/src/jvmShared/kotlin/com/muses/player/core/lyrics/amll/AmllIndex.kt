package com.muses.player.core.lyrics.amll

import com.muses.player.core.model.lyrics.AmllIndexEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * AMLL TTML DB 索引与搜索索引（规格书 = src/features/lyrics/amllTtmlDb.ts）：
 * - jsonl 行宽松解析（metadata 为 [key, values[]] 对）
 * - exactTitles / titleTrigrams（短标题 short: 前缀桶）候选索引
 * - 进程内单飞加载（对齐 indexPromise 语义），失败不缓存可重试
 */

// X2 迁移注：原 internal，留守 :core:lyrics 的 LyricsModule.kt 跨模块引用，放宽为 public（语义不变）
const val AMLL_INDEX_URL =
    "https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/metadata/raw-lyrics-index.jsonl"

internal const val AMLL_TTML_BASE_URL =
    "https://cdn.jsdelivr.net/gh/amll-dev/amll-ttml-db@main/raw-lyrics/"

const val AMLL_INDEX_TIMEOUT_SEC = 20L
internal const val AMLL_TTML_TIMEOUT_SEC = 12L

/** 解析单行索引（parseIndexLine）；结构不符/缺关键字段 → null */
fun parseIndexLine(line: String): AmllIndexEntry? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val raw = Json.parseToJsonElement(trimmed) as? kotlinx.serialization.json.JsonObject ?: return null
        val rawLyricFile = (raw["rawLyricFile"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.trim().orEmpty()
        fun readValues(key: String): List<String> {
            val metadata = raw["metadata"] as? JsonArray ?: return emptyList()
            for (item in metadata) {
                val pair = item as? JsonArray ?: continue
                if (pair.size < 2) continue
                val metaKey = (pair[0] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content ?: continue
                if (metaKey != key) continue
                val values = pair[1] as? JsonArray ?: continue
                return values.mapNotNull { v ->
                    (v as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
                }
            }
            return emptyList()
        }
        val musicName = readValues("musicName").firstOrNull()?.trim().orEmpty()
        if (rawLyricFile.isEmpty() || musicName.isEmpty()) return null
        AmllIndexEntry(
            musicName = musicName,
            artists = readValues("artists"),
            album = readValues("album").firstOrNull(),
            durationSec = null, // Web 索引行可选携带 duration；当前解析器未读该字段，保持 null 语义
            rawLyricFile = rawLyricFile,
        )
    } catch (_: Exception) {
        null
    }
}

/** 解析整段 jsonl 文本（parseIndexJsonl） */
fun parseIndexJsonl(text: String): List<AmllIndexEntry> =
    text.split(Regex("\\r?\\n")).mapNotNull(::parseIndexLine)

/** 搜索索引（AmllSearchIndex） */
class AmllSearchIndex internal constructor(
    val entries: List<AmllIndexEntry>,
    private val exactTitles: Map<String, List<Int>>,
    private val titleTrigrams: Map<String, List<Int>>,
) {
    /** selectCandidates：exact + trigram + short 桶并集展开为候选条目 */
    fun selectCandidates(title: String): List<AmllIndexEntry> {
        val normalizedTitle = normalizeLyricsText(title)
        val candidateIndexes = LinkedHashSet<Int>(exactTitles[normalizedTitle].orEmpty())
        if (normalizedTitle.length >= 3) {
            for (offset in 0..normalizedTitle.length - 3) {
                titleTrigrams[normalizedTitle.substring(offset, offset + 3)]?.let { candidateIndexes.addAll(it) }
            }
            for ((key, indexes) in titleTrigrams) {
                if (key.startsWith("short:") && normalizedTitle.contains(key.removePrefix("short:"))) {
                    candidateIndexes.addAll(indexes)
                }
            }
        } else if (normalizedTitle.isNotEmpty()) {
            titleTrigrams["short:$normalizedTitle"]?.let { candidateIndexes.addAll(it) }
        }
        return candidateIndexes.mapNotNull { entries.getOrNull(it) }
    }
}

/** 创建搜索索引（createSearchIndex） */
fun createSearchIndex(entries: List<AmllIndexEntry>): AmllSearchIndex {
    val exactTitles = mutableMapOf<String, MutableList<Int>>()
    val titleTrigrams = mutableMapOf<String, MutableList<Int>>()
    entries.forEachIndexed { index, entry ->
        val title = normalizeLyricsText(entry.musicName)
        if (title.isEmpty()) return@forEachIndexed
        exactTitles.getOrPut(title) { mutableListOf() }.add(index)
        if (title.length < 3) {
            titleTrigrams.getOrPut("short:$title") { mutableListOf() }.add(index)
        } else {
            for (offset in 0..title.length - 3) {
                titleTrigrams.getOrPut(title.substring(offset, offset + 3)) { mutableListOf() }.add(index)
            }
        }
    }
    return AmllSearchIndex(entries, exactTitles, titleTrigrams)
}

/**
 * 索引仓库：进程内单例缓存 + 单飞并发加载（ensureIndex 语义）。
 * 加载失败不缓存，允许下次重试；测试可通过 [injectIndex] 跳过网络。
 */
class AmllIndexRepository(private val loadFromNetwork: suspend () -> String) {

    private val mutex = Mutex()
    private var indexCache: List<AmllIndexEntry>? = null
    private var searchIndexCache: AmllSearchIndex? = null

    suspend fun ensureIndex(): List<AmllIndexEntry> {
        indexCache?.let { return it }
        searchIndexCache?.let { _ -> return indexCache!! }
        return mutex.withLock {
            indexCache ?: run {
                val entries = parseIndexJsonl(loadFromNetwork())
                indexCache = entries
                searchIndexCache = createSearchIndex(entries)
                entries
            }
        }
    }

    fun currentSearchIndex(): AmllSearchIndex? = searchIndexCache

    /** 测试辅助：注入内存索引（跳过网络） */
    fun injectIndex(entries: List<AmllIndexEntry>?) {
        indexCache = entries
        searchIndexCache = entries?.let { createSearchIndex(it) }
    }
}

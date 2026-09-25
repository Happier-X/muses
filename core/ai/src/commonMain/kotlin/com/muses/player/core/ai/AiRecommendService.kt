package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.OnlineSearchService
import com.muses.player.core.search.PlatformSearchOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * 「猜你喜欢」编排：曲库画像 → LLM 出**歌名+歌手** → 平台搜索精确匹配 → 可播曲目。
 *
 * 为什么让 AI 出歌名而不是「只出关键词」：
 * 用户选择的口径是「贴口味」优先——LLM 直接点名具体作品更接近「猜你喜欢」的体感；
 * 代价是会产生幻觉（编造/记错歌名），故**匹配不上的直接丢弃**，
 * 由 [AiRecommendResult.suggested] 与 [AiRecommendResult.matched] 如实暴露失真比例。
 *
 * 匹配为什么必须精确：推荐结果最终要交给洛雪脚本换直链播放，
 * 只有平台真实存在的曲目（带 `musicInfoJson`）才播得出来；
 * 模糊匹配会把「名字相近的另一首歌」当成推荐命中，属于欺骗性结果。
 */
class AiRecommendService(
    private val chat: AiChatClient,
    private val matcher: AiSuggestionMatcher,
) {

    /**
     * @param count 期望推荐的歌曲数（AI 被要求按此数量输出；实际以匹配成功数为准）
     * @throws AiException 未配置完成 / 调用失败 / 未返回可用建议
     */
    suspend fun recommend(
        profile: LibraryProfile,
        config: AiRecommendConfig,
        count: Int = DEFAULT_COUNT,
    ): AiRecommendResult {
        if (!config.isUsable) {
            throw AiException("AI 推荐尚未配置完成：请先填写服务地址、模型与 API Key")
        }
        if (profile.isEmpty) {
            return AiRecommendResult(emptyList(), suggested = 0, unmatched = emptyList())
        }

        val tracks = mutableListOf<AiRecommendedTrack>()
        val unmatched = mutableListOf<AiSongSuggestion>()
        val attempted = mutableSetOf<String>()
        val attemptedNames = mutableListOf<String>()
        var suggested = 0
        repeat(5) {
            if (tracks.size >= count) return AiRecommendResult(tracks.take(count), suggested, unmatched)
            val content = try {
                chat.complete(config, SYSTEM_PROMPT, buildUserPrompt(profile, count) +
                    if (attemptedNames.isEmpty()) "" else "\n本次已尝试的歌曲不要重复：${attemptedNames.takeLast(60).joinToString("；")}。")
            } catch (error: Exception) {
                if (tracks.isNotEmpty()) return AiRecommendResult(tracks, suggested, unmatched)
                throw error
            }
            val suggestions = parseSuggestions(content).deduplicate()
                .filter { suggestion ->
                    val key = "${suggestion.name.normalizeForMatch()}|${suggestion.artist.normalizeForMatch()}"
                    if (key.substringBefore('|').isEmpty() || !attempted.add(key)) return@filter false
                    attemptedNames += "${suggestion.name} - ${suggestion.artist.orEmpty()}"
                    !profile.containsSong(suggestion.name, suggestion.artist)
                }
                .take(count)
            if (suggestions.isEmpty()) {
                if (tracks.isEmpty()) throw AiException("AI 未返回曲库之外的可用歌曲建议")
                return AiRecommendResult(tracks, suggested, unmatched)
            }
            val batch = matcher.match(suggestions)
            suggested += batch.suggested
            unmatched += batch.unmatched
            batch.tracks.forEach { track ->
                if (!profile.containsSong(track.result.name, track.result.artist) &&
                    tracks.none { existing -> existing.result.name.normalizeForMatch() == track.result.name.normalizeForMatch() &&
                        existing.result.artist.normalizeForMatch() == track.result.artist.normalizeForMatch() }
                ) tracks += track
            }
        }
        return AiRecommendResult(tracks.take(count), suggested, unmatched)
    }

    companion object {
        const val DEFAULT_COUNT = 20

        /** 系统提示词：把「必须真实存在」与「严格 JSON」写成硬约束（幻觉是主要失真来源） */
        internal val SYSTEM_PROMPT: String = """
            你是一位资深音乐推荐助手，熟悉华语与欧美流行、摇滚、民谣、说唱、电子、古典等各流派。

            你的任务：根据用户曲库画像，推荐用户很可能喜欢、但曲库里还没有的歌曲。

            硬性要求：
            1. 只输出 JSON 数组，不要任何解释文字，不要 markdown 代码块标记。
            2. 每个元素固定三个字段：name（歌名，必填）、artist（歌手，必填）、reason（推荐理由，12 字以内）。
            3. 推荐歌曲必须真实存在且有正式发行，歌名与歌手名要写准确——系统会去音乐平台核对，核对不到的直接丢弃。
            4. 不要推荐画像「抽样曲目」里已出现过的歌。
            5. 优先同歌手的其它作品、同流派/同年代/相似听感的歌，可少量跨界拓展，但不要离题太远。
        """.trimIndent()

        internal fun buildUserPrompt(profile: LibraryProfile, count: Int): String = buildString {
            appendLine(profile.toPromptText())
            appendLine()
            appendLine("【任务】请推荐 $count 首歌曲，严格按上述 JSON 数组格式输出（只输出数组本身）。")
        }.trimEnd()
    }
}

/** 无歌手标签按歌名保守排除；有歌手时兼容合唱、分隔符等平台写法。 */
fun LibraryProfile.containsSong(name: String, artist: String?): Boolean {
    val artists = ownedSongs[name.normalizeForMatch()] ?: return false
    val target = artist.normalizeForMatch()
    return target.isEmpty() || artists.any { it.isEmpty() || it.contains(target) || target.contains(it) }
}

/** 从已生成的结果中剔除后来进入曲库的歌曲。 */
fun AiRecommendResult.excludingOwnedSongs(profile: LibraryProfile): AiRecommendResult =
    copy(tracks = tracks.filterNot { profile.containsSong(it.result.name, it.result.artist) })

/**
 * 建议 → 平台真实曲目 的匹配器。
 *
 * 并行度为 [parallelism]（内部每首还会跨 5 个平台并行搜，但 [OnlineSearchService] 自带并发限流），
 * 取 4 是因为：20 首串行会等 30s+，而过高并发对逆向接口不友好。
 */
class AiSuggestionMatcher(
    private val searchService: OnlineSearchService,
) {

    suspend fun match(suggestions: List<AiSongSuggestion>, parallelism: Int = 4): AiRecommendResult =
        coroutineScope {
            val semaphore = Semaphore(parallelism.coerceAtLeast(1))
            val pairs = suggestions
                .map { suggestion ->
                    async { semaphore.withPermit { suggestion to findBestMatch(suggestion) } }
                }
                .map { it.await() }

            val tracks = pairs
                .mapNotNull { (suggestion, result) ->
                    result?.let { AiRecommendedTrack(suggestion, it) }
                }
                // 同一平台同一曲目只留一条（AI 可能换个说法重复推荐）
                .distinctBy { it.result.platform to it.result.songId }

            AiRecommendResult(
                tracks = tracks,
                suggested = suggestions.size,
                unmatched = pairs.filter { it.second == null }.map { it.first },
            )
        }

    /** 该建议在各平台的搜索结果里找**歌名精确**命中的曲目；找不到返回 null（丢弃） */
    private suspend fun findBestMatch(suggestion: AiSongSuggestion): OnlineSearchResult? {
        val targetName = suggestion.name.normalizeForMatch()
        if (targetName.isEmpty()) return null

        val keyword = listOfNotNull(
            suggestion.name.trim().takeIf { it.isNotEmpty() },
            suggestion.artist?.trim()?.takeIf { it.isNotEmpty() },
        ).joinToString(" ")
        if (keyword.isEmpty()) return null

        val outcomes = runCatching {
            searchService.searchAll(keyword, page = 1, pageSize = PAGE_SIZE)
        }.getOrDefault(emptyList())
        val candidates = outcomes
            .filterIsInstance<PlatformSearchOutcome.Success>()
            .flatMap { it.page.results }
        if (candidates.isEmpty()) return null

        val sameName = candidates.filter { it.name.normalizeForMatch() == targetName }
        if (sameName.isEmpty()) return null

        val artist = suggestion.artist.normalizeForMatch()
        if (artist.isEmpty()) return sameName.first()

        // 歌手命中优先；各平台歌手字段写法差异大（合唱拼接、罗马音、别名），
        // 故「歌名精确」仍是硬门槛，歌手只做择优而非否决
        return sameName.firstOrNull { candidate ->
            val candidateArtist = candidate.artist.normalizeForMatch()
            candidateArtist.isNotEmpty() &&
                (candidateArtist.contains(artist) || artist.contains(candidateArtist))
        } ?: sameName.first()
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}

// ── 解析与归一化（internal：供单测直接覆盖）──

/**
 * 从 LLM 回复中提取歌曲建议。
 *
 * 容错链（实测各家模型都不完全听话）：
 * 1. 去掉 markdown 代码块围栏；
 * 2. 截取首个 `[` 到末个 `]`（模型常在数组外多说一句）；
 * 3. 元素既支持对象 `{name,artist,reason}`，也支持纯字符串 `"歌名 - 歌手"`。
 */
internal fun parseSuggestions(content: String): List<AiSongSuggestion> {
    val cleaned = content
        .replace("```json", "", ignoreCase = true)
        .replace("```", "")
        .trim()
    val start = cleaned.indexOf('[')
    val end = cleaned.lastIndexOf(']')
    val jsonText = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned

    val array = runCatching { AiJson.parseToJsonElement(jsonText) as? JsonArray }.getOrNull()
        ?: return emptyList()

    return array.mapNotNull { element ->
        when (element) {
            is JsonObject -> {
                val name = (element["name"] as? JsonPrimitive)?.contentOrNull?.trim()
                    ?: (element["title"] as? JsonPrimitive)?.contentOrNull?.trim()
                    ?: (element["song"] as? JsonPrimitive)?.contentOrNull?.trim()
                    ?: return@mapNotNull null
                if (name.isEmpty()) return@mapNotNull null
                val artist = ((element["artist"] as? JsonPrimitive)?.contentOrNull
                    ?: (element["singer"] as? JsonPrimitive)?.contentOrNull)?.trim()?.takeIf { it.isNotEmpty() }
                val reason = (element["reason"] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                AiSongSuggestion(name = name, artist = artist, reason = reason)
            }
            is JsonPrimitive -> {
                // "歌名 - 歌手" / "歌名-歌手" 形态
                val raw = element.contentOrNull?.trim().orEmpty()
                if (raw.isEmpty()) return@mapNotNull null
                val idx = raw.lastIndexOf(" - ").takeIf { it > 0 } ?: raw.lastIndexOf('-').takeIf { it > 0 }
                if (idx == null) {
                    AiSongSuggestion(name = raw, artist = null)
                } else {
                    AiSongSuggestion(
                        name = raw.substring(0, idx).trim(),
                        artist = raw.substring(idx + 1).trimStart('-', ' ').trim().takeIf { it.isNotEmpty() },
                    )
                }
            }
            else -> null
        }
    }.filter { it.name.isNotBlank() }
}

/** 归一化：用于「歌名精确匹配」判定（大小写/空格/标点/括号版本标记均不影响判定） */
internal fun String?.normalizeForMatch(): String =
    BRACKET_REGEX.replace(this.orEmpty(), "")
        .lowercase()
        .filter { it.isLetterOrDigit() }

/** 去掉 (…) （…） […] 内的版本/说明标记：`歌名 (Live)` / `歌名（重制版）` */
private val BRACKET_REGEX = Regex("[（(\\[][^）)\\]]*[）)\\]]")

/** 按「歌名+歌手」去重（AI 重复推荐同一首歌的不同写法时只留一条） */
internal fun List<AiSongSuggestion>.deduplicate(): List<AiSongSuggestion> =
    distinctBy { "${it.name.normalizeForMatch()}|${it.artist.normalizeForMatch()}" }

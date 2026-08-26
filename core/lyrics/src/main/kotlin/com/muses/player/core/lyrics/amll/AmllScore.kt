package com.muses.player.core.lyrics.amll

import com.muses.player.core.model.scrape.MatchConfidence
import com.muses.player.core.model.lyrics.AmllIndexEntry

/**
 * AMLL 匹配评分（规格书 = src/features/lyrics/normalize.ts + score.ts，逐值对齐）。
 */

/** 规范化歌名/歌手/专辑文本（normalize.ts normalizeText） */
fun normalizeLyricsText(input: String?): String {
    if (input.isNullOrEmpty()) return ""
    return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC)
        .lowercase()
        .replace(Regex("[「」『』【】\\[\\]()（）{}<>《》\"\"'']"), " ")
        .replace(Regex("\\b(live|remix|remaster(?:ed)?|ver\\.?|version|acoustic|instrumental|off\\s*vocal|karaoke|edit|mix)\\b", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[-–—_:|/\\\\·•~,，.。!！?？+]+"), " ")
        .replace(Regex("[\u3000\\s]+"), " ")
        .trim()
}

/** 歌手字符串拆 token，支持 /、&、feat. 等（normalize.ts splitArtistTokens） */
fun splitArtistTokens(artist: String?): List<String> {
    if (artist.isNullOrBlank()) return emptyList()
    // 先按原始分隔符拆分，再分别 normalize；否则 normalizeText 会把 / 等符号抹成空格
    return artist.split(Regex("\\s*(?:/|&|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bx\\b|\\band\\b|、|；|;)\\s*", RegexOption.IGNORE_CASE))
        .map { normalizeLyricsText(it) }
        .filter { it.isNotEmpty() }
}

/** 打分权重（score.ts SCORE_WEIGHTS） */
object ScoreWeights {
    const val TITLE_EXACT = 100
    const val TITLE_CONTAINS = 60
    const val ARTIST_HIT = 25
    const val ALBUM_EXACT = 15
    const val ALBUM_CONTAINS = 8

    /** 时长容忍阈值（秒）（child4 R4-1） */
    const val DURATION_TOLERANCE_SEC = 5.0

    /** 最低采纳分：歌名至少达到「包含」级才进入打分 */
    const val MIN_ACCEPT_SCORE = TITLE_CONTAINS
}

enum class TitleMatchLevel { EXACT, CONTAINS, NONE }

data class TitleMatch(val level: TitleMatchLevel, val score: Int)

fun scoreTitle(queryTitle: String, candidateTitle: String): TitleMatch {
    val q = normalizeLyricsText(queryTitle)
    val c = normalizeLyricsText(candidateTitle)
    if (q.isEmpty() || c.isEmpty()) return TitleMatch(TitleMatchLevel.NONE, 0)
    if (q == c) return TitleMatch(TitleMatchLevel.EXACT, ScoreWeights.TITLE_EXACT)
    if (q.contains(c) || c.contains(q)) return TitleMatch(TitleMatchLevel.CONTAINS, ScoreWeights.TITLE_CONTAINS)
    return TitleMatch(TitleMatchLevel.NONE, 0)
}

fun scoreArtists(queryArtist: String?, candidateArtists: List<String>): Int {
    val queryTokens = splitArtistTokens(queryArtist)
    if (queryTokens.isEmpty() || candidateArtists.isEmpty()) {
        // 无歌手信息不重罚
        return 0
    }
    val candidateTokens = candidateArtists.flatMap { splitArtistTokens(it) }
    if (candidateTokens.isEmpty()) return 0

    var hits = 0
    for (q in queryTokens) {
        if (candidateTokens.any { c -> c == q || c.contains(q) || q.contains(c) }) hits += 1
    }
    return if (hits > 0) ScoreWeights.ARTIST_HIT * minOf(hits, 2) else 0
}

fun scoreAlbum(queryAlbum: String?, candidateAlbum: String?): Int {
    val q = normalizeLyricsText(queryAlbum)
    val c = normalizeLyricsText(candidateAlbum)
    if (q.isEmpty() || c.isEmpty()) return 0
    if (q == c) return ScoreWeights.ALBUM_EXACT
    if (q.contains(c) || c.contains(q)) return ScoreWeights.ALBUM_CONTAINS
    return 0
}

/**
 * 判定单条匹配置信度（child4 R4-1，score.ts classifyMatch）：
 * - 时长偏差超阈值直接 low
 * - title exact 且（无歌手信息 或 artist 命中）→ high
 * - title contains 且 artist 命中 → high；其余 low
 */
fun classifyMatch(
    query: com.muses.player.core.model.lyrics.AmllMatchQuery,
    entry: AmllIndexEntry,
    titleMatch: TitleMatch,
    artistScore: Int,
): MatchConfidence {
    val qDuration = query.durationSec
    val eDuration = entry.durationSec
    if (qDuration != null && eDuration != null) {
        if (Math.abs(qDuration - eDuration) > ScoreWeights.DURATION_TOLERANCE_SEC) {
            return MatchConfidence.LOW
        }
    }
    val queryArtists = splitArtistTokens(query.artist)
    val candidateArtists = entry.artists.flatMap { splitArtistTokens(it) }
    val artistProvided = queryArtists.isNotEmpty() && candidateArtists.isNotEmpty()
    val artistHit = artistProvided && artistScore > 0
    if (titleMatch.level == TitleMatchLevel.EXACT) {
        // exact 无歌手信息可采纳；有歌手信息需命中
        return if (artistProvided && !artistHit) MatchConfidence.LOW else MatchConfidence.HIGH
    }
    if (titleMatch.level == TitleMatchLevel.CONTAINS) {
        // contains 必须 artist 命中才 high
        return if (artistHit) MatchConfidence.HIGH else MatchConfidence.LOW
    }
    return MatchConfidence.LOW
}

/** 对单条索引计算总分；歌名未达包含级直接 0（score.ts scoreEntry） */
fun scoreEntry(
    query: com.muses.player.core.model.lyrics.AmllMatchQuery,
    entry: AmllIndexEntry,
): Int {
    val title = scoreTitle(query.title, entry.musicName)
    if (title.level == TitleMatchLevel.NONE) return 0

    val queryArtists = splitArtistTokens(query.artist)
    val candidateArtists = entry.artists.flatMap { splitArtistTokens(it) }
    val artistScore = scoreArtists(query.artist, entry.artists)
    if (queryArtists.isNotEmpty() && candidateArtists.isNotEmpty() && artistScore == 0) {
        return 0
    }

    val albumScore = scoreAlbum(query.album, entry.album)
    if (title.level == TitleMatchLevel.CONTAINS && artistScore == 0 && albumScore == 0) {
        return 0
    }
    return title.score + artistScore + albumScore
}

/** 最佳匹配结果（score.ts BestMatch） */
data class BestMatch(
    val entry: AmllIndexEntry,
    val score: Int,
    val confidence: MatchConfidence,
)

data class FindBestMatchOptions(
    /** 最低总分，默认 MIN_ACCEPT_SCORE（向后兼容） */
    val minScore: Int? = null,
    /** 最低置信度门槛：high（默认）仅采纳高置信；low 放宽供刮削候选场景 */
    val minConfidence: MatchConfidence = MatchConfidence.HIGH,
)

/**
 * 在索引中选取最高分且不低于阈值的一条；无则 null（score.ts findBestMatch）。
 * 默认 minConfidence=HIGH：自动写库路径仅采纳高置信。
 */
fun findBestMatch(
    query: com.muses.player.core.model.lyrics.AmllMatchQuery,
    index: List<AmllIndexEntry>,
    options: FindBestMatchOptions = FindBestMatchOptions(),
): BestMatch? {
    val minScore = options.minScore ?: ScoreWeights.MIN_ACCEPT_SCORE
    var best: BestMatch? = null

    for (entry in index) {
        val score = scoreEntry(query, entry)
        if (score < minScore) continue
        val titleMatch = scoreTitle(query.title, entry.musicName)
        val artistScore = scoreArtists(query.artist, entry.artists)
        val confidence = classifyMatch(query, entry, titleMatch, artistScore)
        if (options.minConfidence == MatchConfidence.HIGH && confidence != MatchConfidence.HIGH) {
            continue
        }
        if (best == null || score > best.score) {
            best = BestMatch(entry, score, confidence)
        }
    }
    return best
}

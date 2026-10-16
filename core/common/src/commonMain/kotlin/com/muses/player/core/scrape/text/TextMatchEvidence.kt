package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.TextMetaHit

/** 审核候选的规则依据；只参与人工审核排序，不改变播放时的自动补缺规则。 */
data class TextMatchEvidence(
    /** 仅身份匹配分，范围 0–100；不把专辑与字段齐全程度算进身份分。 */
    val score: Int,
    val reasons: List<String>,
    val warnings: List<String>,
    /** 标题主体、歌手或版本存在明确疑点时，审核页应等待人工勾选。 */
    val requiresReview: Boolean = false,
    /** 未知专辑时为 null；与查询专辑一致为 100，相近为 50，不同为 0。 */
    val releaseScore: Int? = null,
    /** 标题、歌手、专辑三项有值的数量，不代表录音匹配。 */
    val completeness: Int = 0,
) {
    val identityScore: Int get() = score
}

private enum class RecordingVersion(val label: String, val pattern: Regex) {
    LIVE("现场版", Regex("(?<![a-z])live(?![a-z])|现场(?:版)?|演唱会", RegexOption.IGNORE_CASE)),
    REMIX("混音版", Regex("(?<![a-z])remix(?![a-z])|混音(?:版)?", RegexOption.IGNORE_CASE)),
    REMASTER("重制版", Regex("(?<![a-z])remaster(?:ed)?(?![a-z])|重制(?:版)?|重新母带", RegexOption.IGNORE_CASE)),
    ACOUSTIC("不插电版", Regex("(?<![a-z])(?:acoustic|unplugged)(?![a-z])|不插电(?:版)?", RegexOption.IGNORE_CASE)),
    INSTRUMENTAL("伴奏版", Regex("(?<![a-z])(?:instrumental|karaoke|off\\s*vocal)(?![a-z])|伴奏(?:版)?|纯音乐(?:版)?", RegexOption.IGNORE_CASE)),
    DEMO("样带版", Regex("(?<![a-z])demo(?![a-z])|样带(?:版)?", RegexOption.IGNORE_CASE)),
}

private val VERSION_BRACKETS = Regex("""\(([^()]*)\)|\[([^\[\]]*)\]|【([^【】]*)】""")
private val VERSION_SUFFIX = Regex("""\s[-–—:]\s*(.+)$""")
private val CHINESE_VERSION_SUFFIX = Regex("""(?:现场版|演唱会版|混音版|重制版|不插电版|伴奏版|纯音乐版|样带版)$""")
private val VERSION_YEAR = Regex("""\b\d{4}\b""")
private val VERSION_LABEL = Regex("""\b(?:version|ver)\b\.?|版本|版""", RegexOption.IGNORE_CASE)
private val LIVE_LOCATION = Regex("""^\s*(?:at|from|in)\b.+$""", RegexOption.IGNORE_CASE)
private val EVIDENCE_PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")

/** 只读取括号、分隔符后缀或明确的中文“版”后缀，避免将 Live Forever、Deliver 等歌名当作版本。 */
private data class RecordingTitleProfile(val base: String, val versions: Set<RecordingVersion>, val details: Map<String, Set<String>>)
private fun titleProfile(title: String?): RecordingTitleProfile {
    val versions = linkedSetOf<RecordingVersion>()
    val details = mutableMapOf<String, MutableSet<String>>()
    fun readAnnotation(value: String): Boolean {
        val annotation = value.trim('(', ')', '[', ']', '【', '】').trim()
        val found = RecordingVersion.entries.filter { it.pattern.containsMatchIn(annotation) }
        if (found.isEmpty()) return false
        var remaining = annotation
        found.forEach { remaining = remaining.replace(it.pattern, " ") }
        remaining = remaining.replace(VERSION_YEAR, " ").replace(VERSION_LABEL, " ").trim()
        // 只接受完整版本说明；现场地点允许 Live at/from/in …，普通短语不作为版本。
        val location = RecordingVersion.LIVE in found && LIVE_LOCATION.matches(remaining)
        if (!location && remaining.replace(EVIDENCE_PUNCTUATION, "").isNotEmpty()) return false
        VERSION_YEAR.findAll(annotation).forEach { details.getOrPut("版本年份") { linkedSetOf() }.add(it.value) }
        if (location) details.getOrPut("现场地点") { linkedSetOf() }.add(candidateIdentity(remaining.replace(Regex("^\\s*(?:at|from|in)\\s+", RegexOption.IGNORE_CASE), "")))
        versions.addAll(found)
        return true
    }
    var base = VERSION_BRACKETS.replace(normalizeNfkc(title.orEmpty())) { match ->
        if (readAnnotation(match.value)) " " else match.value
    }.trim()
    VERSION_SUFFIX.find(base)?.let { match ->
        if (readAnnotation(match.groupValues[1])) base = base.take(match.range.first).trim()
    }
    CHINESE_VERSION_SUFFIX.find(base)?.let { match ->
        readAnnotation(match.value)
        base = base.take(match.range.first).trim()
    }
    return RecordingTitleProfile(base, versions, details)
}

fun evaluateTextMatch(hit: TextMetaHit, query: OnlineTextQuery): TextMatchEvidence {
    val reasons = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    var requiresReview = false
    var identityScore = 0
    fun compare(label: String, local: String?, candidate: String?, identityField: Boolean = false, weight: Int = 0): Int? {
        fun normalize(value: String?) = normalizeNfkc(value.orEmpty()).lowercase()
            .replace(EVIDENCE_PUNCTUATION, " ").trim()
        val left = normalize(local)
        val right = normalize(candidate)
        if (left.isEmpty() || right.isEmpty()) return null
        return when {
            left == right -> { reasons += "${label}一致"; identityScore += weight; 100 }
            left.contains(right) || right.contains(left) -> {
                reasons += "${label}相近"
                identityScore += weight / 2
                if (identityField) { warnings += "${label}仅相近，请核对"; requiresReview = true }
                50
            }
            else -> {
                warnings += "${label}不同"
                if (identityField) requiresReview = true
                0
            }
        }
    }
    val (queryTitle, expected, expectedDetails) = titleProfile(query.title)
    val hitProfile = titleProfile(hit.title)
    val extraProfile = titleProfile("候选 (${hit.versionInfo.orEmpty()})")
    val hitTitle = hitProfile.base
    val actual = hitProfile.versions + extraProfile.versions
    val actualDetails = (hitProfile.details.keys + extraProfile.details.keys).associateWith {
        hitProfile.details[it].orEmpty() + extraProfile.details[it].orEmpty()
    }
    if (!hit.versionInfo.isNullOrBlank() && extraProfile.versions.isEmpty()) {
        warnings += "候选有额外录音说明，请核对：${hit.versionInfo.take(120)}"
        requiresReview = true
    }
    compare("标题主体", queryTitle, hitTitle, identityField = true, weight = 50)
    compare("歌手", query.artist, hit.artist, identityField = true, weight = 30)
    val releaseScore = compare("专辑", query.album, hit.album)

    fun Set<RecordingVersion>.labels() = joinToString("、") { it.label }
    val versionAdjustment = when {
        expected.isEmpty() && actual.isEmpty() -> 0
        expected == actual -> {
            reasons += "版本标记一致（${actual.labels()}）"
            4
        }
        expected.isEmpty() -> {
            warnings += "候选为${actual.labels()}，搜索词未标明版本"
            -4
        }
        actual.isEmpty() -> {
            warnings += "搜索词为${expected.labels()}，候选未标明版本"
            -4
        }
        else -> {
            warnings += "版本标记不同：搜索词为${expected.labels()}，候选为${actual.labels()}"
            -8
        }
    }
    if (!hit.title.isNullOrBlank() && query.title.isNotBlank()) identityScore += 10 + versionAdjustment.coerceAtMost(0)
    for (key in expectedDetails.keys + actualDetails.keys) {
        if (expectedDetails[key] != actualDetails[key]) {
            val missing = expectedDetails[key] == null || actualDetails[key] == null
            warnings += if (missing) "${key}信息不完整，请核对" else "${key}不同"
            identityScore -= if (missing) 3 else 5
            requiresReview = true
        }
    }
    val expectedDuration = validTextDuration(query.durationSec)
    val actualDuration = validTextDuration(hit.durationSec)
    if (expectedDuration != null && actualDuration != null) {
        val delta = kotlin.math.abs(expectedDuration - actualDuration)
        if (delta <= durationTolerance(expectedDuration, actualDuration)) {
            reasons += "时长接近"
            identityScore += 10
        } else {
            warnings += "时长不同（相差${kotlin.math.round(delta).toInt()}秒）"
            requiresReview = true
        }
    }
    return TextMatchEvidence(identityScore.coerceIn(0, 100), reasons, warnings,
        requiresReview = requiresReview || versionAdjustment < 0,
        releaseScore = releaseScore,
        completeness = listOf(hit.title, hit.artist, hit.album).count { !it.isNullOrBlank() })
}

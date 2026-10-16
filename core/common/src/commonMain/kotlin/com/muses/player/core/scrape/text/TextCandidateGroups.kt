package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.TextMetaHit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 分组只保存原列表索引，不删除候选或改变人工、AI 与草稿的选择位置。 */
data class TextReleaseGroup(val candidateIndices: List<Int>)
data class TextRecordingGroup(val candidateIndices: List<Int>, val releases: List<TextReleaseGroup>)

internal fun candidateIdentity(value: String?): String = normalizeNfkc(value.orEmpty()).lowercase()
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

fun validTextDuration(value: Double?): Double? = value?.takeIf { it.isFinite() && it > 0 }

/** 允许编码和片头片尾带来的小差异，长曲目最多容许 5 秒，避免误合并剪辑版本。 */
internal fun durationTolerance(left: Double, right: Double) = max(3.0, min(5.0, min(left, right) * 0.02))

private fun validId(value: String?): String? = value?.trim()?.takeIf {
    it.substringBefore(':').isNotBlank() && ':' in it && it.substringAfter(':').isNotBlank()
}
private fun idConflict(left: String?, right: String?): Boolean {
    val a = validId(left) ?: return false
    val b = validId(right) ?: return false
    return a.substringBefore(':') == b.substringBefore(':') && a != b
}

private fun knownTextConflict(left: String?, right: String?): Boolean {
    val a = candidateIdentity(left)
    val b = candidateIdentity(right)
    return a.isNotEmpty() && b.isNotEmpty() && a != b
}

private fun recordingTitle(hit: TextMetaHit) = listOfNotNull(hit.title?.takeIf(String::isNotBlank), hit.versionInfo?.takeIf(String::isNotBlank)).joinToString(" ")

private fun sameRecording(left: TextMetaHit, right: TextMetaHit): Boolean {
    if (idConflict(left.recordingId, right.recordingId)) return false
    // 保留完整版本、年份与现场地点；不把相近标题、歌手或组合歌手当作相同。
    if (knownTextConflict(recordingTitle(left), recordingTitle(right)) || knownTextConflict(left.artist, right.artist)) return false
    val a = validTextDuration(left.durationSec)
    val b = validTextDuration(right.durationSec)
    if (a != null && b != null && abs(a - b) > durationTolerance(a, b)) return false
    val sameId = validId(left.recordingId)?.let { it == validId(right.recordingId) } == true
    if (sameId) return true
    return candidateIdentity(left.title).isNotEmpty() && candidateIdentity(right.title).isNotEmpty() &&
        candidateIdentity(left.artist).isNotEmpty() && candidateIdentity(right.artist).isNotEmpty() && a != null && b != null
}

private fun sameRelease(left: TextMetaHit, right: TextMetaHit): Boolean {
    if (idConflict(left.releaseId, right.releaseId) || knownTextConflict(left.album, right.album)) return false
    val sameId = validId(left.releaseId)?.let { it == validId(right.releaseId) } == true
    if (sameId) return true
    return candidateIdentity(left.album).isNotEmpty() && candidateIdentity(right.album).isNotEmpty()
}

/** 每次入组与所有成员比较，避免相近时长或缺失字段形成传递误合并。 */
fun groupTextCandidates(hits: List<TextMetaHit>): List<TextRecordingGroup> {
    val recordings = mutableListOf<MutableList<Int>>()
    hits.indices.forEach { index ->
        val group = recordings.firstOrNull { indices -> indices.all { sameRecording(hits[it], hits[index]) } }
        if (group == null) recordings += mutableListOf(index) else group += index
    }
    return recordings.map { recording ->
        val releases = mutableListOf<MutableList<Int>>()
        recording.forEach { index ->
            val release = releases.firstOrNull { indices -> indices.all { sameRelease(hits[it], hits[index]) } }
            if (release == null) releases += mutableListOf(index) else release += index
        }
        TextRecordingGroup(recording.toList(), releases.map { TextReleaseGroup(it.toList()) })
    }
}

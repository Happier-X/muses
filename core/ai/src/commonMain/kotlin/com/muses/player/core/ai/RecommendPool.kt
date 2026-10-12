package com.muses.player.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RecommendFeedback(val name: String, val artist: String?, val liked: Boolean)

/** 持续推荐池：反馈跨平台按作品排除，保留尚未处理的歌曲与顺序。 */
data class RecommendPool(
    val result: AiRecommendResult = AiRecommendResult(emptyList(), 0, emptyList()),
    val feedback: List<RecommendFeedback> = emptyList(),
) {
    fun respond(name: String, artist: String?, liked: Boolean): RecommendPool = copy(
        feedback = feedback.filterNot { same(it.name, it.artist, name, artist) } + RecommendFeedback(name, artist, liked),
        result = result.copy(tracks = result.tracks.filterNot { same(it.result.name, it.result.artist, name, artist) }),
    )

    fun merge(batch: AiRecommendResult, profile: LibraryProfile): RecommendPool = copy(result = result.copy(
        tracks = (result.tracks + batch.tracks)
            .filterNot { track -> profile.containsSong(track.result.name, track.result.artist) ||
                feedback.any { same(it.name, it.artist, track.result.name, track.result.artist) } }
            .distinctBy { it.result.name.normalizeForMatch() to it.result.artist.normalizeForMatch() }
            .take(AiRecommendService.DEFAULT_COUNT),
        suggested = result.suggested + batch.suggested,
        unmatched = batch.unmatched,
    ))

    fun profile(base: LibraryProfile): LibraryProfile {
        val excluded = feedback.map { it.name to it.artist } + result.tracks.map { it.result.name to it.result.artist }
        val owned = base.ownedSongs.toMutableMap()
        excluded.forEach { (name, artist) ->
            val key = name.normalizeForMatch()
            owned[key] = owned[key].orEmpty() + artist.normalizeForMatch()
        }
        val weights = base.artistWeights.toMutableMap()
        feedback.filter { it.liked }.forEach { item ->
            val key = item.artist.normalizeForMatch()
            if (key.isNotEmpty()) weights[key] = (weights[key] ?: 0.0) + 3.0
        }
        return base.copy(ownedSongs = owned, artistWeights = weights,
            recommendationExclusions = (feedback.takeLast(80).map { it.name to it.artist } +
                result.tracks.map { it.result.name to it.result.artist }).map { "${it.first} - ${it.second.orEmpty()}" },
            sampleTracks = (feedback.filter { it.liked }.takeLast(20).map { "${it.name} - ${it.artist.orEmpty()}" } + base.sampleTracks).take(60))
    }

    companion object {
        private fun same(a: String, b: String?, c: String, d: String?): Boolean {
            if (a.normalizeForMatch() != c.normalizeForMatch()) return false
            val x = b.normalizeForMatch(); val y = d.normalizeForMatch()
            return x.isEmpty() || y.isEmpty() || x.contains(y) || y.contains(x)
        }
    }
}

@Serializable
private data class StoredPool(val version: Int = 1, val snapshot: String, val feedback: List<RecommendFeedback>)

object RecommendPoolSnapshot {
    fun encode(pool: RecommendPool): String = Json.encodeToString(StoredPool(
        snapshot = DailyRecommendSnapshot.encode("persistent", pool.result), feedback = pool.feedback))

    fun decode(raw: String): RecommendPool {
        val stored = runCatching { Json.decodeFromString<StoredPool>(raw) }.getOrNull()
        return if (stored != null && stored.version == 1) RecommendPool(
            DailyRecommendSnapshot.decodePersistent(stored.snapshot) ?: RecommendPool().result, stored.feedback)
        else RecommendPool(DailyRecommendSnapshot.decodePersistent(raw) ?: RecommendPool().result)
    }
}

package com.muses.player.core.ai

import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.data.repository.PlayStats
import com.muses.player.core.model.playback.RecentPlayEntry
import kotlin.math.exp
import kotlin.math.ln

/** 仅用于本机排序和去重，禁止加入外部 AI 提示词。 */
internal data class LocalListeningTaste(
    val artistWeights: Map<String, Double>,
    val recentSongs: Map<String, Set<String>>,
)

internal fun buildLocalListeningTaste(
    songs: List<SongEntity>,
    recent: List<RecentPlayEntry>,
    stats: PlayStats,
    nowMs: Long,
): LocalListeningTaste {
    val library = songs.associateBy { it.id }
    val history = recent.filter { it.playedAt <= nowMs + 300_000 }
        .sortedByDescending { it.playedAt }.distinctBy { it.songId }.associateBy { it.songId }
    data class WeightedSong(val title: String, val artist: String, val weight: Double, val recent: Boolean)
    val weighted = (history.keys + stats.songs.keys).mapNotNull { id ->
        val row = library[id]
        val entry = history[id]
        val stat = stats.songs[id]
        val title = (row?.title ?: entry?.title ?: stat?.title)?.trim().orEmpty()
        // 副标题只有专辑时不能猜成歌手；优先使用曲库或在线历史的结构化字段。
        val artist = (row?.artist ?: entry?.onlineTrack?.artist
            ?: entry?.subtitle?.takeIf { entry.onlineTrack == null && it.contains(" - ") }?.substringBefore(" - ")
            ?: stat?.subtitle?.takeIf { it.contains(" - ") }?.substringBefore(" - "))?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank() || artist.isUnknownArtist()) return@mapNotNull null
        val daysAgo = entry?.let { (nowMs - it.playedAt).coerceAtLeast(0) / 86_400_000.0 }
        val count = stat?.playCount?.coerceIn(0, 25) ?: 0
        // 对累计次数取对数并封顶；没有近期记录的旧热门降低权重。
        val frequency = ln(1.0 + count) * if (daysAgo != null && daysAgo <= 90) 1.0 else 0.35
        // 最近记录也可能来自恢复队列，故只作为弱信号，不当作完播。
        val recency = if (daysAgo == null) 0.0 else exp(-daysAgo / 14.0) * if (count > 0) 2.0 else 0.5
        val weight = frequency + recency
        if (weight <= 0) return@mapNotNull null
        WeightedSong(title, artist.normalizeForMatch(), weight, daysAgo != null && daysAgo <= 7)
    }.sortedWith(compareByDescending<WeightedSong> { it.weight }.thenBy { it.title })
    val unique = weighted.distinctBy { it.title.normalizeForMatch() to it.artist }
    val weights = unique.groupBy { it.artist }
        .mapValues { (_, group) -> group.take(3).sumOf { it.weight } }
    val recentSongs = weighted.filter { it.recent }.groupBy { it.title.normalizeForMatch() }
        .mapValues { (_, group) -> group.map { it.artist }.toSet() }
    return LocalListeningTaste(weights, recentSongs)
}

/** 七成熟悉歌手、三成新歌手的目标比例；候选不足时补足，但每位歌手仍有上限。 */
internal fun balanceRecommendations(
    tracks: List<AiRecommendedTrack>,
    profile: LibraryProfile,
    count: Int,
): List<AiRecommendedTrack> {
    if (count <= 0) return emptyList()
    fun artist(track: AiRecommendedTrack) = track.result.artist.normalizeForMatch()
    fun known(track: AiRecommendedTrack) = artist(track) in profile.knownArtists
    val familiar = tracks.filter { known(it) }.sortedByDescending { profile.artistWeights[artist(it)] ?: 0.0 }
    val discovery = tracks.filterNot { known(it) }
    val selected = mutableListOf<AiRecommendedTrack>()
    val artistCounts = mutableMapOf<String, Int>()
    val artistCap = maxOf(1, (count * 0.15).toInt())
    fun select(pool: List<AiRecommendedTrack>, limit: Int) {
        var added = 0
        for (track in pool) {
            if (added >= limit || selected.size >= count) break
            if (track in selected) continue
            val key = artist(track).ifEmpty { "未知:${track.result.platform}:${track.result.songId}" }
            val used = artistCounts[key] ?: 0
            if (used >= artistCap) continue
            artistCounts[key] = used + 1
            selected += track
            added++
        }
    }
    val discoveryTarget = (count * 0.3).toInt()
    select(familiar, count - discoveryTarget)
    select(discovery, discoveryTarget)
    select(familiar + discovery, count - selected.size)
    val familiarSelected = selected.filter { known(it) }.toMutableList()
    val newSelected = selected.filterNot { known(it) }.toMutableList()
    return buildList {
        while (familiarSelected.isNotEmpty() || newSelected.isNotEmpty()) {
            repeat(2) { if (familiarSelected.isNotEmpty()) add(familiarSelected.removeAt(0)) }
            if (newSelected.isNotEmpty()) add(newSelected.removeAt(0))
        }
    }
}

package com.muses.player.core.ai

import com.muses.player.core.search.OnlineSearchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 以设备本地日期为界，重启应用后仍使用当天的推荐。 */
expect fun localRecommendDay(): String

@Serializable
private data class StoredSuggestion(val name: String, val artist: String?, val reason: String?)

@Serializable
private data class StoredTrack(
    val suggestion: StoredSuggestion,
    val platform: String,
    val songId: String,
    val name: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val coverUrl: String?,
    val musicInfoJson: String,
) {
    fun toTrack(): AiRecommendedTrack = AiRecommendedTrack(
        AiSongSuggestion(suggestion.name, suggestion.artist, suggestion.reason),
        OnlineSearchResult(platform, songId, name, artist, album, durationMs, coverUrl, musicInfoJson),
    )
}

@Serializable
private data class StoredDay(
    val day: String,
    val tracks: List<StoredTrack>,
    val suggested: Int,
    val unmatched: List<StoredSuggestion>,
)

object DailyRecommendSnapshot {
    fun encode(day: String, result: AiRecommendResult): String = Json.encodeToString(
        StoredDay(
            day = day,
            tracks = result.tracks.map { track ->
                val song = track.result
                StoredTrack(
                    StoredSuggestion(track.suggestion.name, track.suggestion.artist, track.suggestion.reason),
                    song.platform, song.songId, song.name, song.artist, song.album,
                    song.durationMs, song.coverUrl, song.musicInfoJson,
                )
            },
            suggested = result.suggested,
            unmatched = result.unmatched.map { StoredSuggestion(it.name, it.artist, it.reason) },
        ),
    )

    fun decode(snapshot: String, day: String, profile: LibraryProfile): AiRecommendResult? {
        val stored = runCatching { Json.decodeFromString<StoredDay>(snapshot) }.getOrNull() ?: return null
        if (stored.day != day || stored.tracks.isEmpty()) return null
        val tracks = stored.tracks.map { it.toTrack() }
            .filterNot { profile.containsSong(it.result.name, it.result.artist) }
        if (tracks.isEmpty()) return null
        return AiRecommendResult(
            tracks = tracks,
            suggested = stored.suggested,
            unmatched = stored.unmatched.map { AiSongSuggestion(it.name, it.artist, it.reason) },
        )
    }
}

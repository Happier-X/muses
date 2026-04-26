package com.example.muses.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Represents an audio track from either local storage or a WebDAV server.
 */
@Immutable
data class AudioTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val source: TrackSource,
    /** Size in bytes (0 if unknown). */
    val sizeBytes: Long = 0L,
    /** Number of times this track has been played. */
    val playCount: Int = 0,
    /** Timestamp (epoch ms) of when this track was last played. */
    val lastPlayedAt: Long = 0L,
    /** Album art stored as a file URI in the app's internal cache, or null if not extracted yet. */
    val albumArtUri: Uri? = null
)

enum class TrackSource {
    LOCAL,
    WEBDAV
}

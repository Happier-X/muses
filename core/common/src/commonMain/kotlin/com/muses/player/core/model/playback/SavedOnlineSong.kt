package com.muses.player.core.model.playback

import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef
import kotlinx.serialization.Serializable

/** 保存可重新解析的在线歌曲引用，不保存临时播放直链或歌词全文。 */
@Serializable
data class SavedOnlineSong(
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: String? = null,
    val durationMs: Long = 0,
) {
    fun toSong(): Song? {
        if (OnlineTrackRef.parse(path) == null) return null
        return Song(id, sourceId, path, title, artist, album,
            durationMs = durationMs, durationSec = durationMs / 1000,
            coverUri = coverUri, sourceType = SourceType.ONLINE)
    }
}

fun Song.toSavedOnlineSong(): SavedOnlineSong? =
    if (sourceType == SourceType.ONLINE && OnlineTrackRef.parse(path) != null) {
        SavedOnlineSong(id, sourceId, path, title, artist, album, coverUri,
            durationMs.takeIf { it > 0 } ?: durationSec * 1000)
    } else null

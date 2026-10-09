package com.muses.player.core.media.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

internal object PlaybackMetadataUpdates {
    fun artwork(item: MediaItem?, songId: String, coverUri: String): MediaItem? {
        if (item == null || item.mediaId != songId || coverUri.isBlank()) return null
        val uri = Uri.parse(coverUri.trim())
        if (item.mediaMetadata.artworkUri == uri && item.mediaMetadata.artworkData == null) return null
        val metadata = item.mediaMetadata.buildUpon()
            .setArtworkData(null, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .setArtworkUri(uri)
            .build()
        return item.buildUpon().setMediaMetadata(metadata).build()
    }

    /** 通知歌词只替换文字，封面及其它媒体字段继续保留。 */
    fun notificationText(base: MediaMetadata, title: CharSequence, artist: CharSequence?): MediaMetadata =
        base.buildUpon().setTitle(title).setArtist(artist).build()
}

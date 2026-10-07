package com.muses.player.core.media.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef

/** 请求元数据随媒体会话传递，且不会被通知歌词的展示元数据替换清掉。 */
internal object PlaybackSourceMetadata {
    private const val SOURCE_TYPE = "muses.playback.sourceType"

    fun requestMetadata(sourceType: SourceType): MediaItem.RequestMetadata =
        MediaItem.RequestMetadata.Builder()
            .setExtras(Bundle().apply { putString(SOURCE_TYPE, sourceType.name) })
            .build()

    fun sourceType(item: MediaItem?): SourceType? {
        val name = item?.requestMetadata?.extras?.getString(SOURCE_TYPE)
        SourceType.entries.firstOrNull { it.name == name }?.let { return it }
        // 兼容没有音源元数据的旧队列；普通 HTTP 地址不能据此认定为 WebDAV。
        val uri = item?.localConfiguration?.uri?.toString() ?: return null
        return if (OnlineTrackRef.parse(uri) != null) SourceType.ONLINE else null
    }
}

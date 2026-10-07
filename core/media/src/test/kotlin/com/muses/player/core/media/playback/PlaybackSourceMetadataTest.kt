package com.muses.player.core.media.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.online.OnlineTrackRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class PlaybackSourceMetadataTest {
    @Test
    fun `媒体会话序列化和歌词替换保留音源类型`() {
        for (type in SourceType.entries) {
            val item = MediaItem.Builder()
                .setUri("https://example.com/music.mp3")
                .setRequestMetadata(PlaybackSourceMetadata.requestMetadata(type))
                .build()
            val transferred = MediaItem.fromBundle(item.toBundle())
            val updated = transferred.buildUpon()
                .setMediaMetadata(MediaMetadata.Builder().setTitle("歌词").build())
                .build()
            assertEquals(type, PlaybackSourceMetadata.sourceType(updated))
        }
    }

    @Test
    fun `未知HTTP地址不能判为WebDAV`() {
        assertNull(PlaybackSourceMetadata.sourceType(MediaItem.fromUri("https://example.com/music.mp3")))
        assertNull(PlaybackSourceMetadata.sourceType(null))
    }

    @Test
    fun `旧在线队列可通过稳定URI识别`() {
        val item = MediaItem.fromUri(OnlineTrackRef("wy", "{}", "script").encode())
        assertEquals(SourceType.ONLINE, PlaybackSourceMetadata.sourceType(item))
    }
}

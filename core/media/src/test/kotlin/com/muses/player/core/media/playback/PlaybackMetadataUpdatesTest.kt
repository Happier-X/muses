package com.muses.player.core.media.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.muses.player.core.model.SourceType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class PlaybackMetadataUpdatesTest {
    private fun item() = MediaItem.Builder()
        .setMediaId("lx-song")
        .setUri("https://example.com/audio.mp3")
        .setRequestMetadata(PlaybackSourceMetadata.requestMetadata(SourceType.ONLINE))
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle("歌曲")
            .setArtist("歌手")
            .setAlbumTitle("专辑")
            .setArtworkData(byteArrayOf(1, 2), MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build())
        .build()

    @Test
    fun `脚本封面写入媒体项且保留播放地址和歌曲信息`() {
        val original = item()
        val updated = PlaybackMetadataUpdates.artwork(original, "lx-song", "https://example.com/lx.jpg")!!
        assertEquals(Uri.parse("https://example.com/lx.jpg"), updated.mediaMetadata.artworkUri)
        assertNull(updated.mediaMetadata.artworkData)
        assertEquals(original.localConfiguration, updated.localConfiguration)
        assertEquals(original.requestMetadata, updated.requestMetadata)
        assertEquals("歌曲", updated.mediaMetadata.title)
        assertEquals("专辑", updated.mediaMetadata.albumTitle)
        assertNull(PlaybackMetadataUpdates.artwork(updated, "lx-song", "https://example.com/lx.jpg"))
    }

    @Test
    fun `切歌后旧封面与空封面不会覆盖当前曲目`() {
        assertNull(PlaybackMetadataUpdates.artwork(item(), "old-song", "https://example.com/old.jpg"))
        assertNull(PlaybackMetadataUpdates.artwork(item(), "lx-song", " "))
        assertNull(PlaybackMetadataUpdates.artwork(null, "lx-song", "https://example.com/lx.jpg"))
    }

    @Test
    fun `通知歌词更新及恢复保留远程封面和其它字段`() {
        val original = PlaybackMetadataUpdates.artwork(item(), "lx-song", "https://example.com/lx.jpg")!!.mediaMetadata
        val lyric = PlaybackMetadataUpdates.notificationText(original, "当前歌词", "歌曲 - 歌手")
        val restored = PlaybackMetadataUpdates.notificationText(lyric, "歌曲", "歌手")
        assertEquals(original.artworkUri, lyric.artworkUri)
        assertEquals(original, restored)
    }

    @Test
    fun `通知歌词更新保留内嵌封面`() {
        val original = item().mediaMetadata
        val lyric = PlaybackMetadataUpdates.notificationText(original, "当前歌词", "歌曲 - 歌手")
        assertArrayEquals(original.artworkData, lyric.artworkData)
    }
}

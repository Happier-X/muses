package com.muses.player.core.media.scanner

import com.muses.player.core.data.db.SongTags
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.data.mapper.toEntity
import com.muses.player.core.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryAudioQualityTest {
    @Test fun `已读标签的歌曲仍以文件最新标签为准`() {
        val song = Song("1", "source", "/歌.flac", "已刮削歌名", artist = "已刮削歌手", tagsVersion = SongTags.TAGS_VERSION)
        val result = PlaybackLazyScan.merge(song, PlaybackLazyScan.FileTags(title = "文件旧标题", artist = "文件旧歌手", audioQuality = "SQ"))!!
        assertEquals(song.copy(title = "文件旧标题", artist = "文件旧歌手", audioQuality = "SQ"), result)
        assertEquals("SQ", result.toEntity().toDomain().audioQuality)
        assertNull(PlaybackLazyScan.merge(result, PlaybackLazyScan.FileTags(title = result.title, artist = result.artist, audioQuality = "SQ")))
    }
}

package com.muses.player.core.scrape.writeback

import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ReviewFileRevisionTest {
    @Test fun `审核文件版本识别同大小同时间戳的外部改动`() = runTest {
        val file = File.createTempFile("muses-review-", ".mp3")
        try {
            file.writeText("初始内容")
            val song = Song("song", "source", file.absolutePath, "曲名")
            val before = readLocalAudioRevision(song)
            assertNotNull(before)
            assertEquals(before, readLocalAudioRevision(song.copy(path = file.toURI().toString())))
            val timestamp = file.lastModified()
            file.writeText("外部修改")
            assertTrue(file.setLastModified(timestamp))
            assertNotEquals(before, readLocalAudioRevision(song))
            assertNull(readLocalAudioRevision(song.copy(sourceType = SourceType.WEBDAV)))
            assertNull(readLocalAudioRevision(song.copy(path = "content://music/123")))
        } finally { file.delete() }
    }
}

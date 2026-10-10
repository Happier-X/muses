package com.muses.player.core.scrape.writeback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExternalStorageTreePathTest {
    private val root = "/storage/emulated/0"

    @Test fun `目录授权覆盖中文文件与嵌套目录`() {
        assertEquals("primary:Music/我的音乐/雨过后的风景.mp3",
            externalStorageDocumentId("$root/Music/我的音乐/雨过后的风景.mp3", "primary:Music", root))
        assertEquals("primary:Music/我的音乐/雨过后的风景.mp3",
            externalStorageDocumentId("$root/Music/我的音乐/雨过后的风景.mp3", "primary:Music/我的音乐", root))
    }

    @Test fun `相似目录与越界路径不能复用授权`() {
        assertNull(externalStorageDocumentId("$root/Music2/歌曲.mp3", "primary:Music", root))
        assertNull(externalStorageDocumentId("$root/Music/../其他/歌曲.mp3", "primary:Music", root))
        assertNull(externalStorageDocumentId("$root/其他/歌曲.mp3", "primary:Music", root))
        assertNull(externalStorageDocumentId("$root/Music/歌曲.mp3", "raw:/storage/emulated/0/Music", root))
    }

    @Test fun `支持外置存储卷与根目录授权`() {
        assertEquals("1234-ABCD:Music/歌曲.flac",
            externalStorageDocumentId("/storage/1234-ABCD/Music/歌曲.flac", "1234-ABCD:Music", root))
        assertEquals("primary:Music/歌曲.flac",
            externalStorageDocumentId("$root/Music/歌曲.flac", "primary:", root))
        assertNull(externalStorageDocumentId("/storage/5678-ABCD/Music/歌曲.flac", "1234-ABCD:Music", root))
    }
}

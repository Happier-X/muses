package com.muses.player.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SongIdentityTest {
    private fun song(id: String, artist: String?, title: String = "测试歌曲") =
        Song(id, "test", "/$id", title, artist)

    @Test fun `合作歌手的全半角分隔符与排列顺序不影响喜欢匹配`() {
        val original = song("local", "歌手甲／歌手乙")
        listOf("歌手甲、歌手乙", "歌手甲/歌手乙", "歌手甲，歌手乙", "歌手乙 & 歌手甲",
            "歌手甲＋歌手乙", "歌手甲 | 歌手乙", "歌手甲 feat. 歌手乙")
            .forEach { assertTrue(sameSong(original, song("online", it)), it) }
        assertFalse(sameSong(original, song("online", "歌手甲")))
        assertFalse(sameSong(original, song("online", "歌手甲/歌手丙")))
    }

    @Test fun `明确双语括注可以匹配任一语言但不猜测未声明别名`() {
        val bilingual = song("local", "测试歌手（Example Artist）")
        assertTrue(sameSong(bilingual, song("online", "测试歌手")))
        assertTrue(sameSong(bilingual, song("online", "EXAMPLE ARTIST")))
        assertTrue(sameSong(song("local", "Example Artist (测试歌手)"), song("online", "测试歌手")))
        assertFalse(sameSong(song("local", "测试歌手"), song("online", "Example Artist")))
        assertFalse(sameSong(bilingual, song("online", "另一歌手")))
    }

    @Test fun `标题版本未知歌手与乐队名括注仍严格区分`() {
        val original = song("local", "测试歌手")
        assertFalse(sameSong(original, song("online", "测试歌手", "测试歌曲 (Live)")))
        assertFalse(sameSong(original, song("online", "测试歌手", "测试歌曲 Remix")))
        assertFalse(sameSong(song("local", null), song("online", null)))
        assertFalse(sameSong(song("local", "Band (Project)"), song("online", "Band")))
        assertTrue(sameSong(song("local", "A.B."), song("online", "AB")))
    }

    @Test fun `别名交集匹配不受比较方向和歌手顺序影响`() {
        val left = song("local", "Example Artist (测试歌手) / Example Artist")
        val right = song("online", "Example Artist / 测试歌手")
        assertTrue(sameSong(left, right))
        assertTrue(sameSong(right, left))
        val reordered = right.copy(artist = "测试歌手、Example Artist")
        assertTrue(sameSong(left, reordered))
        assertTrue(sameSong(reordered, left))
        val different = right.copy(artist = "测试歌手 / 另一歌手")
        assertFalse(sameSong(left, different))
        assertFalse(sameSong(different, left))
    }
}

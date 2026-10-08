package com.muses.player.core.lyrics

import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricSyllable
import com.muses.player.core.lyrics.model.LyricTimingKind
import com.muses.player.core.model.lyrics.DesktopLyricsWord
import com.muses.player.core.lyrics.model.LyricsDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopLyricsStateTest {
    private val timedLine = LyricLine(1000, text = "你好！", syllables = listOf(
        LyricSyllable("你", 1000, 2000), LyricSyllable("好！", 2500, 3500)))
    private fun timedSnapshot(line: LyricLine = timedLine) = snapshot.copy(document = LyricsDocument(listOf(line)))

    @Test fun 字内进度与间隙按真实时间计算() {
        val words = desktopLyricsText(timedSnapshot(), 1500, true, true).words
        assertEquals(0f, words[0].progressAt(1000), 0.001f)
        assertEquals(0.5f, words[0].progressAt(1500), 0.001f)
        assertEquals(1f, words[0].progressAt(2200), 0.001f)
        assertEquals(0f, words[1].progressAt(2200), 0.001f)
        assertEquals(1f, words[1].progressAt(3500), 0.001f)
    }

    @Test fun 暂停与回退使用传入进度() {
        val paused = desktopLyricsText(timedSnapshot(), 1600, true, false)
        assertEquals(false, paused.isPlaying)
        assertEquals(1600L, paused.positionMs)
        val rewound = desktopLyricsText(timedSnapshot(), 1200, true, true)
        assertEquals(0.2f, rewound.words[0].progressAt(rewound.positionMs), 0.001f)
    }

    @Test fun 首尾空格裁剪保留内部空格标点() {
        val line = timedLine.copy(text = "你 好！", syllables = listOf(
            LyricSyllable("  你 ", 1000, 2000), LyricSyllable("好！ ", 2000, 3000)))
        assertEquals(listOf(DesktopLyricsWord(0, 2, 1000, 2000), DesktopLyricsWord(2, 4, 2000, 3000)),
            desktopLyricsText(timedSnapshot(line), 1500, true).words)
    }

    @Test fun 普通歌词伪逐字和不匹配文本不启用效果() {
        assertEquals(emptyList<DesktopLyricsWord>(), desktopLyricsText(snapshot, 1500, true).words)
        assertEquals(emptyList<DesktopLyricsWord>(), desktopLyricsText(timedSnapshot(
            timedLine.copy(timingKind = LyricTimingKind.LineSynchronized)), 1500, true).words)
        assertEquals(emptyList<DesktopLyricsWord>(), desktopLyricsText(timedSnapshot(
            timedLine.copy(text = "别的歌词")), 1500, true).words)
    }

    @Test fun 普通歌词与无歌词也保留真实播放状态() {
        for (current in listOf(snapshot, snapshot.copy(document = null))) {
            assertEquals(true, desktopLyricsText(current, 1500, true, true).isPlaying)
            assertEquals(false, desktopLyricsText(current, 1500, true, false).isPlaying)
            assertEquals(emptyList<DesktopLyricsWord>(), desktopLyricsText(current, 1500, true, true).words)
        }
    }

    @Test fun 页面空文档与行歌词不遮住服务逐字歌词() {
        val service = timedSnapshot()
        for (page in listOf(snapshot, snapshot.copy(document = null))) {
            val selected = desktopLyricsSnapshot(snapshot.songId!!, page, service)
            assertEquals(service, selected)
            assertEquals(2, desktopLyricsText(selected, 1500, true, true).words.size)
        }
        assertEquals(service, desktopLyricsSnapshot(snapshot.songId!!, service, snapshot))
        assertEquals(snapshot, desktopLyricsSnapshot(snapshot.songId!!, snapshot, service.copy(songId = "另一首")))
    }

    @Test fun 零时长即时完成负时长丢弃() {
        val line = timedLine.copy(syllables = listOf(LyricSyllable("你", 1000, 1000), LyricSyllable("好！", 2000, 1500)))
        val words = desktopLyricsText(timedSnapshot(line), 1000, true).words
        assertEquals(1, words.size)
        assertEquals(0f, words[0].progressAt(999), 0.001f)
        assertEquals(1f, words[0].progressAt(1000), 0.001f)
    }

    @Test fun 跨行使用新的字符范围且切歌清除逐字状态() {
        val next = LyricLine(4000, text = "下一句", syllables = listOf(LyricSyllable("下一句", 4000, 6000)))
        val current = snapshot.copy(document = LyricsDocument(listOf(timedLine, next)))
        val text = desktopLyricsText(current, 4500, true, true)
        assertEquals("下一句", text.primary)
        assertEquals(listOf(DesktopLyricsWord(0, 3, 4000, 6000)), text.words)
        assertEquals(0.25f, text.words.single().progressAt(text.positionMs), 0.001f)
        assertEquals(emptyList<DesktopLyricsWord>(), desktopLyricsText(
            DesktopLyricsSnapshot("新歌", "新歌名"), 4500, true, true).words)
    }
    private val snapshot = DesktopLyricsSnapshot("歌曲", "歌名", "歌手", LyricsDocument(listOf(
        LyricLine(1000, text = "第一句", translation = "第一句译文"),
        LyricLine(3000, text = "第二句"),
    )))

    @Test fun 前奏和无歌词时显示歌曲信息() {
        assertEquals(DesktopLyricsText("歌名", "歌手"), desktopLyricsText(snapshot, 999, true))
        assertEquals(DesktopLyricsText("歌名", "歌手"), desktopLyricsText(snapshot.copy(document = null), 5000, true))
    }

    @Test fun 歌词按时间切换并支持回退进度() {
        assertEquals("第一句", desktopLyricsText(snapshot, 1000, true).primary)
        assertEquals("第二句", desktopLyricsText(snapshot, 3000, true).primary)
        assertEquals("第一句", desktopLyricsText(snapshot, 2000, true).primary)
    }

    @Test fun 译文遵循开关且不补充其他内容() {
        assertEquals("第一句译文", desktopLyricsText(snapshot, 1000, true).secondary)
        assertEquals(null, desktopLyricsText(snapshot, 1000, false).secondary)
        assertEquals(null, desktopLyricsText(snapshot, 3000, true).secondary)
    }

    @Test fun 切歌立即清掉旧歌词() {
        val previous = DesktopLyricsState.snapshot.value
        try {
            DesktopLyricsState.publish(snapshot)
            DesktopLyricsState.publish(DesktopLyricsSnapshot("新歌曲", "新歌名", "新歌手"))
            assertEquals(DesktopLyricsText("新歌名", "新歌手"),
                desktopLyricsText(DesktopLyricsState.snapshot.value, 3000, true))
        } finally { DesktopLyricsState.publish(previous) }
    }
}

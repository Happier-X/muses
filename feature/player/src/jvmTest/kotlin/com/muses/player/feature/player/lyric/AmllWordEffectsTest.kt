package com.muses.player.feature.player.lyric

import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricSyllable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmllWordEffectsTest {
    @Test
    fun 注音独立时间按字符数分配主字宽度并保留停顿() {
        val line = LyricLine(1000, text = "你好", syllables = listOf(
            LyricSyllable("你好", 1000, 3000, listOf(
                LyricSyllable("ni", 1000, 1500), LyricSyllable("hao", 2500, 3000),
            )),
        ))
        val words = AmllWordEffects.words(line)
        assertEquals(1, words.size, "带注音词不可拆成两个汉字")
        assertEquals(5, words.single().emphasis?.characterCount)
        val mask = AmllWordEffects.MaskTimeline(words, listOf(100f))
        assertEquals(40f, mask.front(0, 1500, 0f), .001f)
        assertEquals(40f, mask.front(0, 2400, 0f), .001f)
        assertEquals(70f, mask.front(0, 2750, 0f), .001f)
        assertEquals(100f, mask.front(0, 3000, 0f), .001f)
        assertEquals(0f, mask.front(0, 1000, 0f), .001f)
    }

    private fun line(vararg parts: Triple<String, Long, Long>) = LyricLine(
        timeMs = parts.first().second,
        text = parts.joinToString("") { it.first },
        syllables = parts.map { LyricSyllable(it.first, it.second, it.third) },
    )

    @Test
    fun 英文多音节共用强调分组且不改写音节时间() {
        val words = AmllWordEffects.words(line(Triple("su", 1000, 1600), Triple("gar", 1800, 2400), Triple(" sweet", 2400, 2800)))
        assertEquals(3, words.size)
        assertEquals(words[0].emphasis, words[1].emphasis)
        assertNotNull(words[0].emphasis)
        assertEquals(2, words[1].characterOffset)
        assertEquals(1800.0, words[1].startMs)
        assertNull(words[2].emphasis)
    }

    @Test
    fun 中文按字分时且短字不会被整句时长误判为长音() {
        val words = AmllWordEffects.words(line(Triple("我爱你", 0, 1500)))
        assertEquals(listOf(0.0, 500.0, 1000.0), words.map { it.startMs })
        assertTrue(words.all { it.emphasis == null })
        assertFalse(AmllWordEffects.shouldEmphasize("爱", 999.0))
        assertTrue(AmllWordEffects.shouldEmphasize("爱", 1000.0))
        assertFalse(AmllWordEffects.shouldEmphasize("a", 2000.0))
        assertFalse(AmllWordEffects.shouldEmphasize("beautiful", 2000.0))
    }

    @Test
    fun 重音字素不拆散组合音标和表情() {
        val text = "e\u0301👨‍👩‍👧‍👦🇨🇳"
        val ranges = AmllWordEffects.graphemes(text)
        assertEquals(listOf("e\u0301", "👨‍👩‍👧‍👦", "🇨🇳"), ranges.map { text.substring(it.start, it.end) })
    }

    @Test
    fun 重复单词按顺序匹配且不把坏数据套到下一字() {
        val repeated = AmllWordEffects.words(line(Triple("la ", 0, 500), Triple("la", 500, 1000)))
        assertEquals(listOf(0, 3), repeated.map { it.range.start })
        val broken = LyricLine(0, text = "正确", syllables = listOf(LyricSyllable("错误", 0, 1000)))
        assertTrue(AmllWordEffects.words(broken).isEmpty())
    }

    @Test
    fun 普通词只有缓出上浮且和声幅度加倍() {
        val word = AmllWordEffects.words(line(Triple("hey", 1000, 1400))).single()
        val before = AmllWordEffects.visual(word, 0, 999, false, false)
        val end = AmllWordEffects.visual(word, 0, 2000, false, false)
        val background = AmllWordEffects.visual(word, 0, 2000, true, false)
        assertEquals(0f, before.parentLiftEm)
        assertEquals(.05f, end.parentLiftEm)
        assertEquals(.1f, background.parentLiftEm)
        assertEquals(1f, end.scale)
        assertEquals(0f, end.glowAlpha)
        assertEquals(0f, end.translateXEm)
    }

    @Test
    fun 两秒句尾长音峰值与上游公式一致() {
        val word = AmllWordEffects.words(line(Triple("love", 1000, 3000))).single()
        val emphasis = assertNotNull(word.emphasis)
        assertEquals(2400.0, emphasis.durationMs)
        assertEquals(.96f, emphasis.amount, .00001f)
        assertEquals(2f / 9f, emphasis.blur, .00001f)
        val peak = AmllWordEffects.visual(word, 0, 2200, false, false)
        assertEquals(1.096f, peak.scale, .00001f)
        assertEquals(2f / 9f, peak.glowAlpha, .00001f)
        assertEquals(1f / 15f, peak.glowRadiusEm, .00001f)
        val rightPeak = AmllWordEffects.visual(word, 3, 2920, false, false)
        assertEquals(peak.scale, rightPeak.scale, .00001f)
        assertTrue(peak.translateXEm < 0f)
        assertTrue(rightPeak.translateXEm > 0f)
    }

    @Test
    fun 强调浮动提前四百毫秒且结束没有随机抖动() {
        val word = AmllWordEffects.words(line(Triple("love", 1000, 3000))).single()
        val anticipation = AmllWordEffects.visual(word, 0, 800, false, false)
        assertTrue(anticipation.translateYEm < 0f)
        assertEquals(0f, anticipation.glowAlpha)
        val end = AmllWordEffects.visual(word, 0, 10000, false, false)
        assertEquals(1f, end.scale)
        assertEquals(0f, end.glowAlpha)
        assertEquals(0f, end.translateXEm, .00001f)
        assertEquals(0f, end.translateYEm, .00001f)
        assertEquals(AmllWordEffects.Visual(0f), AmllWordEffects.visual(word, 0, 2200, false, true))
    }

    @Test
    fun 遮罩按词宽线性推进并在词间停顿冻结() {
        val words = AmllWordEffects.words(line(Triple("wi ", 0, 1000), Triple("de", 2000, 3000)))
        val mask = AmllWordEffects.MaskTimeline(words, listOf(100f, 200f))
        assertEquals(-40f, mask.front(0, 0, 20f))
        assertEquals(25f, mask.front(0, 500, 20f))
        assertEquals(90f, mask.front(0, 1000, 20f))
        assertEquals(mask.front(0, 1000, 20f), mask.front(0, 1999, 20f))
        assertEquals(-10f, mask.front(1, 2000, 20f))
        assertEquals(200f, mask.front(1, 3000, 20f))
        // 向后跳转由绝对播放时间求值，不保留上一次已经唱完的状态。
        assertEquals(25f, mask.front(0, 500, 20f))
    }

    @Test
    fun 单词首尾羽化补偿和零时长不会产生非有限值() {
        val words = AmllWordEffects.words(line(Triple("ok", 1000, 1000)))
        val mask = AmllWordEffects.MaskTimeline(words, listOf(40f))
        assertEquals(-40f, mask.front(0, 999, 20f))
        assertEquals(40f, mask.front(0, 1000, 20f))
    }
}

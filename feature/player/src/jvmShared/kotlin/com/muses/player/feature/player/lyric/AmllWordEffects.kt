package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.CubicBezierEasing
import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricSyllable
import java.util.regex.Pattern
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** AMLL 86200dead 的分词、遮罩和强调规则；来源与许可见 docs/歌词滚动移植.md。 */
internal object AmllWordEffects {
    val easeOut = CubicBezierEasing(0f, 0f, .58f, 1f)
    private val emphasizeIn = CubicBezierEasing(.2f, .4f, .58f, 1f)
    private val emphasizeOut = CubicBezierEasing(.3f, 0f, .58f, 1f)
    private val graphemePattern = Pattern.compile("\\X")

    data class TextRange(val start: Int, val end: Int)
    data class Emphasis(
        val startMs: Double,
        val durationMs: Double,
        val characterCount: Int,
        val amount: Float,
        val blur: Float,
    )
    data class Word(
        val range: TextRange,
        val startMs: Double,
        val endMs: Double,
        val characters: List<TextRange>,
        val emphasis: Emphasis? = null,
        val characterOffset: Int = 0,
        val ruby: List<LyricSyllable> = emptyList(),
    )
    data class Visual(
        val parentLiftEm: Float,
        val scale: Float = 1f,
        val translateXEm: Float = 0f,
        val translateYEm: Float = 0f,
        val glowAlpha: Float = 0f,
        val glowRadiusEm: Float = 0f,
    )

    fun graphemes(text: String, from: Int = 0, until: Int = text.length): List<TextRange> {
        val matcher = graphemePattern.matcher(text).region(from, until)
        return buildList { while (matcher.find()) add(TextRange(matcher.start(), matcher.end())) }
    }

    // 对齐上游 is-cjk.ts 的整词判断，不把混有拉丁字母的词误判为 CJK。
    fun isCjk(text: String): Boolean = text.isNotEmpty() && text.codePoints().allMatch {
        it in 0x0800..0x9FFC || it in 0x3400..0x4DBF || it in 0x20000..0x323AF ||
            it in 0xFA0E..0xFA29
    }

    fun shouldEmphasize(text: String, durationMs: Double): Boolean =
        durationMs >= 1000.0 && (isCjk(text) || text.trim().length in 2..7)

    fun words(line: LyricLine): List<Word> {
        val atoms = mutableListOf<Word>()
        var searchFrom = 0
        for (syllable in line.syllables) {
            if (syllable.text.isEmpty()) continue
            val located = line.text.indexOf(syllable.text, searchFrom)
            // 不匹配的来源片段不能套到下一个字上，否则全行的高亮都会错位。
            if (located < 0) continue
            searchFrom = located + syllable.text.length
            val ruby = syllable.ruby.filter { it.text.isNotBlank() }
            if (ruby.isNotEmpty()) {
                val start = located + syllable.text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
                val end = located + syllable.text.trimEnd().length
                if (start < end) atoms += Word(TextRange(start, end), syllable.startTimeMs.toDouble(),
                    syllable.endTimeMs.toDouble(), graphemes(line.text, start, end), ruby = ruby)
                continue
            }
            val nonSpaceLength = syllable.text.count { !it.isWhitespace() }.coerceAtLeast(1)
            val perUnit = max(0L, syllable.endTimeMs - syllable.startTimeMs).toDouble() / nonSpaceLength
            var consumed = 0
            var local = 0
            while (local < syllable.text.length) {
                if (syllable.text[local].isWhitespace()) { local++; continue }
                var end = local + 1
                while (end < syllable.text.length && !syllable.text[end].isWhitespace()) end++
                val part = syllable.text.substring(local, end)
                val ranges = if (isCjk(part)) graphemes(line.text, located + local, located + end)
                else listOf(TextRange(located + local, located + end))
                for (range in ranges) {
                    val startMs = syllable.startTimeMs + consumed * perUnit
                    consumed += range.end - range.start
                    atoms += Word(range, startMs, syllable.startTimeMs + consumed * perUnit,
                        graphemes(line.text, range.start, range.end))
                }
                local = end
            }
        }
        val result = mutableListOf<Word>()
        var index = 0
        while (index < atoms.size) {
            val first = index
            val firstText = line.text.substring(atoms[index].range.start, atoms[index].range.end)
            index++
            if (!isCjk(firstText) && atoms[first].ruby.isEmpty()) {
                while (index < atoms.size) {
                    val previous = atoms[index - 1]
                    val next = atoms[index]
                    if (next.ruby.isNotEmpty() || previous.range.end != next.range.start ||
                        isCjk(line.text.substring(next.range.start, next.range.end))) break
                    index++
                }
            }
            val chunk = atoms.subList(first, index)
            val text = line.text.substring(chunk.first().range.start, chunk.last().range.end)
            val start = chunk.minOf { it.startMs }
            val end = chunk.maxOf { it.endMs }
            val emphasized = chunk.any { shouldEmphasize(line.text.substring(it.range.start, it.range.end), it.endMs - it.startMs) } ||
                (!isCjk(text) && shouldEmphasize(text, end - start))
            val count = chunk.sumOf { word -> word.ruby.sumOf { it.text.length }.takeIf { it > 0 } ?: word.characters.size }
            val emphasis = if (emphasized) emphasis(start, end - start, count, index == atoms.size) else null
            var characterOffset = 0
            for (word in chunk) {
                result += word.copy(emphasis = emphasis, characterOffset = characterOffset)
                characterOffset += word.characters.size
            }
        }
        return result
    }

    fun emphasis(startMs: Double, durationMs: Double, characterCount: Int, lastWord: Boolean): Emphasis {
        var duration = max(1000.0, durationMs)
        fun strength(divisor: Double): Double {
            val ratio = duration / divisor
            return if (ratio > 1) sqrt(ratio) else ratio.pow(3)
        }
        var amount = strength(2000.0) * .6
        var blur = strength(3000.0) * .5
        if (lastWord) { amount *= 1.6; blur *= 1.5; duration *= 1.2 }
        return Emphasis(startMs, duration, characterCount.coerceAtLeast(1),
            min(1.2, amount).toFloat(), min(.8, blur).toFloat())
    }

    /** 上游关键帧首个 offset 为 1/32；0 处由浏览器补入无变换状态。 */
    private fun sampled(progress: Double, sample: (Float) -> Float): Float {
        val frame = progress.coerceIn(0.0, 1.0) * 32
        val left = floor(frame).toInt()
        val a = if (left == 0) 0f else sample(left / 32f)
        val b = if (left >= 32) a else sample((left + 1) / 32f)
        return a + (b - a) * (frame - left).toFloat()
    }

    fun visual(word: Word, character: Int, timeMs: Long, background: Boolean, reduceMotion: Boolean): Visual {
        if (reduceMotion) return Visual(0f)
        val ordinaryProgress = ((timeMs - word.startMs) / max(1000.0, word.endMs - word.startMs)).coerceIn(0.0, 1.0)
        val multiplier = if (background) 2f else 1f
        val parentLift = .05f * multiplier * easeOut.transform(ordinaryProgress.toFloat())
        val emphasis = word.emphasis ?: return Visual(parentLift)
        val index = word.characterOffset + character
        val delay = emphasis.durationMs / 2.5 / emphasis.characterCount * index
        val elapsed = timeMs - emphasis.startMs - delay
        val envelope = sampled(elapsed / emphasis.durationMs) {
            if (it < .5f) emphasizeIn.transform(it * 2f)
            else 1f - emphasizeOut.transform((it - .5f) * 2f)
        }
        val float = sampled((elapsed + 400) / (emphasis.durationMs * 1.4)) { sin(it * PI).toFloat() }
        val scale = 1f + envelope * .1f * emphasis.amount
        return Visual(
            parentLiftEm = parentLift,
            scale = scale,
            translateXEm = -envelope * .03f * emphasis.amount * (emphasis.characterCount / 2f - index) * scale,
            translateYEm = -envelope * .025f * emphasis.amount * scale - float * .05f * multiplier,
            glowAlpha = envelope * emphasis.blur,
            glowRadiusEm = min(.3f, emphasis.blur * .3f),
        )
    }

    /** 沿整行真实词宽推进：词间停顿冻结，首词和尾词补偿渐变带宽。 */
    class MaskTimeline(words: List<Word>, widths: List<Float>) {
        private val words = words
        private val starts = DoubleArray(words.size) { words[it].startMs }
        private val ends = DoubleArray(words.size) { words[it].endMs }
        private val widths = widths.toFloatArray()
        private val prefix = FloatArray(words.size + 1).also { out ->
            widths.forEachIndexed { i, width -> out[i + 1] = out[i] + width }
        }

        fun front(wordIndex: Int, timeMs: Long, fadeWidth: Float): Float {
            var movement = 0f
            for (i in starts.indices) {
                val ruby = words[i].ruby
                if (ruby.isNotEmpty()) {
                    val count = ruby.sumOf { it.text.length }
                    var character = 0
                    ruby.forEach { segment ->
                        val start = max(starts[i], segment.startTimeMs.toDouble()).coerceAtMost(ends[i])
                        val end = min(ends[i], max(start, segment.endTimeMs.toDouble()))
                        val duration = (end - start) / segment.text.length
                        repeat(segment.text.length) {
                            val charStart = start + it * duration
                            val progress = if (duration <= 0) {
                                if (timeMs >= charStart) 1f else 0f
                            } else ((timeMs - charStart) / duration).coerceIn(0.0, 1.0).toFloat()
                            val extension = (if (i == 0 && character == 0) fadeWidth * 1.5f else 0f) +
                                (if (i == starts.lastIndex && character == count - 1) fadeWidth * .5f else 0f)
                            movement += (widths[i] / count + extension) * progress
                            character++
                        }
                    }
                    continue
                }
                val progress = if (ends[i] <= starts[i]) {
                    if (timeMs >= starts[i]) 1f else 0f
                } else ((timeMs - starts[i]) / (ends[i] - starts[i])).coerceIn(0.0, 1.0).toFloat()
                val extension = (if (i == 0) fadeWidth * 1.5f else 0f) +
                    (if (i == starts.lastIndex) fadeWidth * .5f else 0f)
                movement += (widths[i] + extension) * progress
            }
            // 返回渐变亮端位置，暗端位于 front + fadeWidth。
            return movement - prefix[wordIndex] - fadeWidth * 2f
        }
    }
}

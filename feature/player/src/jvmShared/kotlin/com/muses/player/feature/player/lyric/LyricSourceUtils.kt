package com.muses.player.feature.player.lyric

import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.BreakIterator
import java.util.Locale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.muses.player.core.lyrics.model.LyricHighlightStrategy
import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.processor.LyricTimelineProcessor
import com.muses.player.core.lyrics.aligner.LyricRomanizationAligner
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.model.withPseudoTiming
import com.muses.player.feature.player.lyric.SettingsRuntime
import com.muses.player.feature.player.lyric.AppVisibility
import com.muses.player.feature.player.lyric.LocalFontFamily
import com.muses.player.feature.player.lyric.LanTingProFontFamily
import com.muses.player.feature.player.lyric.LyricsRenderingQuality
import com.muses.player.feature.player.lyric.LyricAnnotationDisplayMode
import com.muses.player.feature.player.lyric.LyricsGroupingMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


internal fun sourceLyricInterludes(lines: List<LyricLine>): List<LyricInterlude> = buildList {
    lines.forEachIndexed { index, line ->
        val start = if (index == 0) {
            0L
        } else {
            val previous = lines[index - 1]
            max(
                previous.timeMs + (previous.durationMs ?: 0L),
                previous.syllables.maxOfOrNull { it.endTimeMs } ?: previous.timeMs,
            )
        }
        val countdownEnd = (line.timeMs - 250L).coerceAtLeast(start)
        if (countdownEnd - start >= 4_000L) {
            add(LyricInterlude(start, countdownEnd, line.timeMs, index))
        }
    }
}

internal fun sourceGlyphTimings(
    line: LyricLine,
    liftMode: LyricsGroupingMode,
    longToneDetectionMode: LyricsGroupingMode,
    longToneThresholdMs: Int,
): List<GlyphTiming?> {
    val result = MutableList<GlyphTiming?>(line.text.length) { null }
    var searchFrom = 0
    for (syllable in line.syllables) {
        if (syllable.text.isEmpty()) continue
        val located = line.text.indexOf(syllable.text, startIndex = searchFrom)
        val startIndex = if (located >= 0) located else searchFrom.coerceAtMost(line.text.length)
        val count = syllable.text.length.coerceAtMost(line.text.length - startIndex)
        if (count <= 0) continue
        searchFrom = startIndex + count
        val syllableDuration = max(syllable.endTimeMs - syllable.startTimeMs, 0L).toFloat()
        val characterDuration = syllableDuration / count.toFloat()
        for (local in 0 until count) {
            val offset = startIndex + local
            val start = syllable.startTimeMs + characterDuration * local
            val end = if (local == count - 1) max(syllable.endTimeMs.toFloat(), start) else start + characterDuration
            result[offset] = GlyphTiming(
                textOffset = offset,
                start = start,
                end = end,
                liftStart = start,
                liftEnd = end + UpstreamLyrics.LIFT_CONTINUATION_MS,
                syllableStart = syllable.startTimeMs.toFloat(),
                syllableEnd = syllable.endTimeMs.toFloat(),
                characterIndex = local,
                characterCount = count,
                wordStart = start,
                wordEnd = end,
                wordCharacterIndex = 0,
                wordCharacterCount = 1,
                usesWordTimingForLongTone = false,
                longToneStart = syllable.startTimeMs.toFloat(),
                longToneDuration = syllableDuration,
                longToneCharacterIndex = local,
                longToneCharacterCount = count,
                isLongTone = false,
                expansionAmount = 0f,
                glowAmount = 0f,
            )
        }
    }

    // Match TimedLyricTextBuilder: highlights preserve individual character
    // timings, while a word block supplies the optional shared lift clock.
    sourceWordBlocks(line.text).forEach { block ->
        val timedOffsets = (block.start until block.end).filter { offset ->
            result.getOrNull(offset) != null && !line.text[offset].isWhitespace()
        }
        val wordStart = timedOffsets.minOfOrNull { result[it]!!.start } ?: return@forEach
        val wordEnd = timedOffsets.maxOfOrNull { result[it]!!.end } ?: return@forEach
        val positions = timedOffsets.withIndex().associate { it.value to it.index }
        val usesWordTimingForLongTone = timedOffsets.size > 1 && timedOffsets.all { offset ->
            line.text[offset].isAsciiLatinLetter()
        }
        for (offset in block.start until block.end) {
            result[offset] = result.getOrNull(offset)?.copy(
                wordStart = wordStart,
                wordEnd = wordEnd,
                wordCharacterIndex = positions[offset] ?: max(timedOffsets.size - 1, 0),
                wordCharacterCount = max(timedOffsets.size, 1),
                usesWordTimingForLongTone = usesWordTimingForLongTone,
            )
        }
    }

    // AMLL 对齐（LyricLineBase.shouldEmphasize）：CJK 词仅看时长 ≥1000ms；
    // 非 CJK 还要求去空格后长度 2..7。旧逻辑只看 toneDuration ≥ 阈值，
    // 中文短字（如 200ms 的“爱”）也会被误判为长词带辉光。
    fun isCjk(text: String): Boolean = text.any { ch ->
        ch.code in 0x4E00..0x9FFF || ch.code in 0x3400..0x4DBF ||
            ch.code in 0x3040..0x30FF || ch.code in 0xAC00..0xD7AF
    }
    return result.map { timing ->
        timing?.let {
            val liftStart = if (liftMode == LyricsGroupingMode.Word) it.wordStart else it.start
            val liftEnd = if (liftMode == LyricsGroupingMode.Word) it.wordEnd else it.end
            val usesWordGroup = it.usesWordTimingForLongTone ||
                longToneDetectionMode == LyricsGroupingMode.Word
            val toneStart = if (usesWordGroup) it.wordStart else it.syllableStart
            val toneEnd = if (usesWordGroup) it.wordEnd else it.syllableEnd
            val toneDuration = max(toneEnd - toneStart, 0f)
            val toneIndex = if (usesWordGroup) it.wordCharacterIndex else it.characterIndex
            val toneCount = if (usesWordGroup) it.wordCharacterCount else it.characterCount
            val wordStartOffset = (it.textOffset - it.wordCharacterIndex).coerceIn(0, line.text.length)
            val wordEndOffset = (wordStartOffset + it.wordCharacterCount).coerceIn(0, line.text.length)
            val wordText = line.text.substring(wordStartOffset, wordEndOffset)
            val trimmedLen = wordText.trim().length
            val emphasizeByAmll = toneDuration >= 1000f && (isCjk(wordText) || (trimmedLen in 2..7))
            val longTone = !line.text[it.textOffset].isWhitespace() &&
                emphasizeByAmll && toneDuration >= longToneThresholdMs
            val emphasisProgress = sourceSmootherStep(
                (toneDuration - longToneThresholdMs) / (2800f - longToneThresholdMs),
            )
            it.copy(
                liftStart = liftStart,
                liftEnd = liftEnd + UpstreamLyrics.LIFT_CONTINUATION_MS,
                longToneStart = toneStart,
                longToneDuration = toneDuration,
                longToneCharacterIndex = toneIndex,
                longToneCharacterCount = toneCount,
                isLongTone = longTone,
                expansionAmount = if (longTone) .7f + .3f * emphasisProgress else 0f,
                glowAmount = if (longTone) .32f + .38f * emphasisProgress else 0f,
            )
        }
    }
}

internal fun sourceGlyphVisual(
    timing: GlyphTiming,
    playbackTimeMs: Long,
    density: Float,
    reduceMotion: Boolean,
    fontScale: Float,
): GlyphVisual {
    val duration = max(timing.end - timing.start, 0f)
    val raw = when {
        playbackTimeMs < timing.start -> 0f
        playbackTimeMs >= timing.end -> 1f
        duration <= 0f -> 1f
        else -> ((playbackTimeMs - timing.start) / duration).coerceIn(0f, 1f)
    }
    val reveal = sourceHighlightRevealProgress(
        playbackTimeMs.toFloat(),
        timing.start,
        timing.end,
        raw,
        timing.isLongTone,
    )
    val lift = if (playbackTimeMs <= timing.liftStart) 0f else sourceSmootherStep(
        (playbackTimeMs - timing.liftStart) / max(timing.liftEnd - timing.liftStart, 1f),
    )
    val risePx = if (reduceMotion) 0f else
        min(max(UpstreamLyrics.FONT_SIZE_SP * fontScale * .1f, 1.5f), 6f) * density
    val envelope = if (timing.isLongTone) sourceLongToneEnvelope(
        playbackTimeMs.toFloat(),
        timing.longToneStart,
        timing.longToneDuration,
        characterIndex = timing.longToneCharacterIndex,
        characterCount = timing.longToneCharacterCount,
    ) else 0f
    val longToneScale = if (reduceMotion) 1f else
        1f + (UpstreamLyrics.LONG_TONE_MAX_SCALE - 1f) * envelope * timing.expansionAmount *
            SettingsRuntime.lyricLongToneStrength
    // AMLL 对齐：普通字无缩放、无弹跳，只有长词（emphasize）才有 swell/glow。
    // 旧的 1.0→1.2→1.0 bounce（20% 放大）是误读，AMLL 普通词只有 float 上浮。
    val scale = longToneScale
    // AMLL 对齐：辉光仅属于强调词（shouldEmphasize：词长 ≥1000ms）。
    // 普通字 glow 恒为 0，否则整行都有辉光。
    val glow = if (reduceMotion || !SettingsRuntime.lyricGlowEnabled || !timing.isLongTone) {
        0f
    } else {
        envelope * timing.glowAmount
    }
    val shakeAmplitude = if (reduceMotion || !timing.isLongTone || raw <= 0f || raw >= 1f) 0f else {
        min(.58f * density, UpstreamLyrics.FONT_SIZE_SP * fontScale * .013f * density) *
            envelope * timing.expansionAmount * SettingsRuntime.lyricLongToneStrength
    }
    val shakePhase = (playbackTimeMs - timing.longToneStart).coerceAtLeast(0f)
    val shakeX = sin(shakePhase * .016f + timing.longToneCharacterIndex * 1.73f) * shakeAmplitude
    val shakeY = sin(shakePhase * .021f + timing.longToneCharacterIndex * .91f) * shakeAmplitude * .42f
    return GlyphVisual(
        reveal = reveal,
        liftPx = risePx * lift,
        scale = scale,
        glow = glow,
        shakeXPx = shakeX,
        shakeYPx = shakeY,
    )
}

internal data class SourceTextBlock(val start: Int, val end: Int)

internal fun sourceWordBlocks(text: String): List<SourceTextBlock> {
    if (text.isEmpty()) return emptyList()
    val tokens = mutableListOf<SourceTextBlock>()
    var phraseStart = -1
    for (index in 0..text.length) {
        val isWhitespace = index == text.length || text[index].isWhitespace()
        if (!isWhitespace && phraseStart < 0) phraseStart = index
        if (isWhitespace && phraseStart >= 0) {
            val phraseEnd = index
            val iterator = BreakIterator.getWordInstance(Locale.ROOT).apply {
                setText(text.substring(phraseStart, phraseEnd))
            }
            var tokenStart = iterator.first()
            var tokenEnd = iterator.next()
            var foundWord = false
            while (tokenEnd != BreakIterator.DONE) {
                val token = text.substring(phraseStart + tokenStart, phraseStart + tokenEnd)
                if (token.any { it.isLetterOrDigit() }) {
                    tokens += SourceTextBlock(phraseStart + tokenStart, phraseStart + tokenEnd)
                    foundWord = true
                }
                tokenStart = tokenEnd
                tokenEnd = iterator.next()
            }
            if (!foundWord) tokens += SourceTextBlock(phraseStart, phraseEnd)
            phraseStart = -1
        }
    }
    if (tokens.isEmpty()) return listOf(SourceTextBlock(0, text.length))
    val sorted = tokens.sortedBy { it.start }
    return buildList {
        sorted.forEachIndexed { index, token ->
            val start = if (index == 0) 0 else sorted[index - 1].end
            val end = if (index + 1 < sorted.size) sorted[index + 1].start else text.length
            if (start < end) add(SourceTextBlock(start, end))
        }
    }
}

internal fun Char.isAsciiLatinLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

internal fun sourceTimingEffectsStrength(
    line: LyricLine,
    playbackTimeMs: Long,
    focusProgress: Float,
): Float {
    val focus = focusProgress.coerceIn(0f, 1f)
    if (focus <= 0f || line.syllables.isEmpty()) return 0f
    val firstSyllableStartMs = line.syllables.minOf { it.startTimeMs }
    val activation = sourceSmootherStep(
        (playbackTimeMs - firstSyllableStartMs).toFloat() /
            UpstreamLyrics.FOCUS_COLOR_DURATION_MS.toFloat(),
    )
    return min(focus, activation)
}

internal fun sourceTimedAnnotatedString(line: LyricLine, playbackTimeMs: Long) =
    buildAnnotatedString {
        line.syllables.forEach { syllable ->
            val characters = syllable.text.toCharArray()
            if (characters.isEmpty()) return@forEach
            val syllableDuration = max(syllable.endTimeMs - syllable.startTimeMs, 0L).toFloat()
            val characterDuration = syllableDuration / characters.size.toFloat()
            characters.forEachIndexed { index, character ->
                val start = syllable.startTimeMs + characterDuration * index
                val end = if (index == characters.lastIndex) {
                    max(syllable.endTimeMs.toFloat(), start)
                } else {
                    start + characterDuration
                }
                val duration = max(end - start, 0f)
                val rawProgress = when {
                    playbackTimeMs < start -> 0f
                    playbackTimeMs >= end -> 1f
                    duration <= 0f -> 1f
                    else -> ((playbackTimeMs - start) / duration).coerceIn(0f, 1f)
                }
                val isLongTone = syllableDuration >= UpstreamLyrics.LONG_TONE_THRESHOLD_MS && !character.isWhitespace()
                val revealProgress = sourceHighlightRevealProgress(
                    playbackTimeMs.toFloat(), start, end, rawProgress, isLongTone,
                )
                val liftEnd = end + UpstreamLyrics.LIFT_CONTINUATION_MS
                val liftProgress = if (playbackTimeMs <= start) 0f else {
                    sourceSmootherStep((playbackTimeMs - start) / max(liftEnd - start, 1f))
                }
                val playedRise = min(max(UpstreamLyrics.FONT_SIZE_SP * 0.1f, 1.5f), 6f)
                val longEnvelope = if (isLongTone) {
                    sourceLongToneEnvelope(
                        playbackTimeMs.toFloat(),
                        syllable.startTimeMs.toFloat(),
                        syllableDuration,
                        index,
                        characters.size,
                    )
                } else 0f
                val expansionAmount = if (isLongTone) {
                    0.7f + 0.3f * sourceSmootherStep(
                        (syllableDuration - UpstreamLyrics.LONG_TONE_THRESHOLD_MS) /
                            (2800f - UpstreamLyrics.LONG_TONE_THRESHOLD_MS),
                    )
                } else 0f
                val glyphScale = 1f +
                    (UpstreamLyrics.LONG_TONE_MAX_SCALE - 1f) * longEnvelope * expansionAmount *
                        SettingsRuntime.lyricLongToneStrength
                val glowAmount = if (isLongTone) {
                    0.32f + 0.38f * sourceSmootherStep(
                        (syllableDuration - UpstreamLyrics.LONG_TONE_THRESHOLD_MS) /
                            (2800f - UpstreamLyrics.LONG_TONE_THRESHOLD_MS),
                    )
                } else 0f
                val glowStrength = if (SettingsRuntime.lyricGlowEnabled) {
                    longEnvelope * glowAmount * SettingsRuntime.lyricGlowStrength
                } else 0f
                val opacity = SettingsRuntime.lyricInactiveOpacity +
                    (1f - SettingsRuntime.lyricInactiveOpacity) * revealProgress

                val startOffset = length
                append(character)
                addStyle(
                    SpanStyle(
                        color = Color.White.copy(alpha = opacity.coerceIn(0f, 1f)),
                        fontSize = (UpstreamLyrics.FONT_SIZE_SP * glyphScale).sp,
                        fontWeight = SettingsRuntime.lyricFontWeight.composeWeight,
                        baselineShift = BaselineShift(
                            (playedRise / UpstreamLyrics.FONT_SIZE_SP) * liftProgress,
                        ),
                        shadow = if (glowStrength > 0f) {
                            Shadow(
                                color = Color.White.copy(alpha = (glowStrength * 0.7425f).coerceIn(0f, 1f)),
                                blurRadius = UpstreamLyrics.FONT_SIZE_SP * 0.3f,
                            )
                        } else null,
                    ),
                    startOffset,
                    length,
                )
            }
        }
    }

internal fun sourceHighlightRevealProgress(
    playbackTimeMs: Float,
    startMs: Float,
    endMs: Float,
    rawProgress: Float,
    isLongTone: Boolean,
): Float {
    val regular = sourceSmootherStep(rawProgress)
    val duration = endMs - startMs
    if (!isLongTone || duration <= 460f) return regular
    val elapsed = max(playbackTimeMs - startMs, 0f)
    val attack = sourceSmootherStep(elapsed / 300f)
    val releaseStart = endMs - 160f
    val release = sourceSmootherStep((playbackTimeMs - releaseStart) / 160f)
    return (0.82f * attack + 0.08f * rawProgress + 0.10f * release).coerceIn(0f, 1f)
}

internal fun sourceLongToneEnvelope(
    playbackTimeMs: Float,
    groupStartMs: Float,
    durationMs: Float,
    characterIndex: Int,
    characterCount: Int,
): Float {
    val stagger = if (characterCount > 1) {
        durationMs * 0.55f * characterIndex.toFloat() / (characterCount - 1).toFloat()
    } else 0f
    val animationDuration = max(durationMs, 1000f)
    val progress = ((playbackTimeMs - groupStartMs - stagger) / animationDuration).coerceIn(0f, 1f)
    return if (progress <= 0.5f) {
        sourceSmootherStep(progress / 0.5f)
    } else {
        1f - sourceSmootherStep((progress - 0.5f) / 0.5f)
    }
}

internal fun sourceConcurrentLyricIndexes(
    lines: List<LyricLine>,
    highlightedIndex: Int,
    toleranceMs: Long = 280L,
): IntRange {
    if (highlightedIndex !in lines.indices) return IntRange.EMPTY
    val activation = sourceLineActivationTimeMs(lines[highlightedIndex])
    var first = highlightedIndex
    var last = highlightedIndex
    while (first > 0 && activation - sourceLineActivationTimeMs(lines[first - 1]) <= toleranceMs) first--
    while (last < lines.lastIndex && sourceLineActivationTimeMs(lines[last + 1]) - activation <= toleranceMs) last++
    return first..last
}

internal fun sourceFocusAnimationDurationMs(index: Int, lines: List<LyricLine>): Int {
    if (index !in lines.indices) return 360
    val available = if (index + 1 < lines.size) {
        sourceLineActivationTimeMs(lines[index + 1]) - sourceLineActivationTimeMs(lines[index])
    } else null
    if (available == null || available <= 0L) return 400
    // 三轮：区间映射回调，避免快歌级联过长导致重叠，保留柔感
    return (available * 0.44f).coerceIn(100f, 460f).roundToInt()
}

internal fun sourceRemainingFocusDurationMs(
    index: Int,
    playbackTimeMs: Long,
    lines: List<LyricLine>,
): Float? {
    if (index !in lines.indices || index + 1 >= lines.size) return null
    return max((sourceLineActivationTimeMs(lines[index + 1]) - playbackTimeMs).toFloat(), 0f)
}

internal fun sourceLineActivationTimeMs(line: LyricLine): Long =
    line.syllables.minOfOrNull { it.startTimeMs } ?: line.timeMs

internal data class SourceCascadeLineTiming(val delayMs: Float, val durationMs: Float)

internal fun sourceCascadeLineTimings(
    maximumLineOrder: Int,
    animationDurationMs: Float,
): List<SourceCascadeLineTiming> {
    val catchUpCompletionTime = animationDurationMs * SettingsRuntime.lyricCascadeCatchUpRatio
    val minimumCatchUpDuration = min(240f, animationDurationMs * 0.50f)
    return (0..maximumLineOrder.coerceAtLeast(0)).map { order ->
        if (order == 0) {
            SourceCascadeLineTiming(0f, animationDurationMs)
        } else {
            val accumulatedIncrease = order.toFloat() * (order - 1).toFloat() / 2f
            val delay = SettingsRuntime.lyricCascadeFollowingDelayMs +
                order * SettingsRuntime.lyricCascadeDelayMs +
                accumulatedIncrease * SettingsRuntime.lyricCascadeDelayIncreaseMs
            SourceCascadeLineTiming(
                delayMs = delay,
                durationMs = max(catchUpCompletionTime - delay, minimumCatchUpDuration),
            )
        }
    }
}

internal fun sourceCascadeBounce(chaseOrder: Int, maximumChaseOrder: Int): Float {
    if (!SettingsRuntime.lyricCascadeBounceEnabled) return 0f
    val count = max(maximumChaseOrder + 1, 1)
    val position = chaseOrder.coerceIn(0, maximumChaseOrder) + 1
    val normalized = position.toFloat() / count.toFloat()
    val bounceScale = 1f - (1f - normalized) * SettingsRuntime.lyricCascadeBounceGradient
    return SettingsRuntime.lyricCascadeBounce * bounceScale
}

internal fun sourceDistanceBlurRadius(
    distancePx: Float,
    lyricStridePx: Float,
    intensity: Float,
    focusProgress: Float,
): Float {
    val lineDistance = distancePx / lyricStridePx
    val blurProgress = max(lineDistance - 1.35f, 0f)
    val baseRadius = min(blurProgress * 3.1f, 10f)
    return baseRadius * intensity * (1f - focusProgress.coerceIn(0f, 1f))
}

internal fun sourceFocusBlurRadius(
    intensity: Float,
    preceding: Boolean,
    following: Boolean,
): Float = ((if (preceding) 2.4f else 0f) + (if (following) 0.7f else 0f)) * intensity

internal fun sourceDistanceOpacity(
    distancePx: Float,
    lyricStridePx: Float,
    dimAmount: Float,
    focusProgress: Float,
): Float {
    val lineDistance = distancePx / lyricStridePx
    val base = when {
        lineDistance <= 1f -> 1f - lineDistance * 0.44f
        lineDistance <= 2f -> 0.56f - (lineDistance - 1f) * 0.22f
        else -> max(0.12f, 0.34f - (lineDistance - 2f) * 0.07f)
    }
    val distanceOpacity = 1f - (1f - base) * dimAmount
    return distanceOpacity + (1f - distanceOpacity) * focusProgress.coerceIn(0f, 1f)
}

internal fun sourceEmphasis(focusProgress: Float, dimAmount: Float): Float {
    val unfocused = 1f - (1f - 0.52f) * dimAmount
    return unfocused + (1f - unfocused) * focusProgress.coerceIn(0f, 1f)
}

internal fun sourceSmootherStep(value: Float): Float {
    val p = value.coerceIn(0f, 1f)
    return p * p * p * (p * (p * 6f - 15f) + 10f)
}

internal object SourceSmoothStepEasing : Easing {
    override fun transform(fraction: Float): Float {
        val p = fraction.coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }
}

internal class SourceSpringEasing(private val bounce: Float) : Easing {
    override fun transform(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        if (bounce <= 0.0001f) return SourceSmoothStepEasing.transform(t)
        // 三轮：找回逐级可见性，omega 回调至 7.0 保留柔感但不抹平级联
        val damping = (1f - bounce.coerceIn(0f, 0.95f) * 0.56f).coerceIn(0.24f, 0.95f)
        val omega = 7.0f
        val damped = omega * sqrt(max(1f - damping * damping, 0.0001f))
        val envelope = exp((-damping * omega * t).toDouble()).toFloat()
        val raw = 1f - envelope * (
            kotlin.math.cos((damped * t).toDouble()).toFloat() +
                (damping / sqrt(max(1f - damping * damping, 0.0001f))) *
                sin((damped * t).toDouble()).toFloat()
            )
        val endEnvelope = exp((-damping * omega).toDouble()).toFloat()
        val endRaw = 1f - endEnvelope * (
            kotlin.math.cos(damped.toDouble()).toFloat() +
                (damping / sqrt(max(1f - damping * damping, 0.0001f))) *
                sin(damped.toDouble()).toFloat()
            )
        return if (abs(endRaw) > 0.0001f) raw / endRaw else raw
    }
}

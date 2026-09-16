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


/** Three staggered instrumental bars, matching the report's 750ms wave entry. */
@Composable
internal fun LyricInstrumentalWave(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instrumental-wave")
    val phase0 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = CubicBezierEasing(.4f, 0f, 1f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "instrumental-wave-0",
    )
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, delayMillis = 50, easing = CubicBezierEasing(.4f, 0f, 1f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "instrumental-wave-1",
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, delayMillis = 100, easing = CubicBezierEasing(.4f, 0f, 1f, 1f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "instrumental-wave-2",
    )
    val phases = listOf(phase0, phase1, phase2)
    Canvas(modifier.fillMaxWidth().height(46.dp)) {
        val barWidth = 7.dp.toPx()
        val gap = 9.dp.toPx()
        val totalWidth = barWidth * 3f + gap * 2f
        val startX = (size.width - totalWidth) / 2f
        phases.forEachIndexed { index, phase ->
            val barHeight = 20.dp.toPx() + index * 7.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = phase.coerceIn(0f, 1f)),
                topLeft = Offset(
                    startX + index * (barWidth + gap),
                    (size.height - barHeight) / 2f,
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
internal fun UpstreamLyricLine(
    line: LyricLine,
    playbackTimeProvider: () -> Long,
    supportsTimedLyrics: Boolean,
    fontScale: Float,
    reduceMotion: Boolean,
    focusProgress: Float,
    timingEffectsStrength: Float,
    timedUnplayedAlpha: Float,
    visualScale: Float,
    focusScaleReservePadding: Dp,
    visualOffsetPx: Float,
    rowAlpha: Float,
    distanceBlurDp: Float,
    focusBlurDp: Float,
    renderingQuality: LyricsRenderingQuality,
    showTranslation: Boolean,
    showRomanization: Boolean,
    reserveTranslation: Boolean,
    reserveRomanization: Boolean,
    tapSeekEnabled: Boolean,
    onMeasured: (Int) -> Unit,
    onClick: () -> Unit,
) {
    val flipped = line.agent?.alignment == com.muses.player.core.lyrics.model.LyricAgentAlignment.Flipped
    val lineAlignment = if (flipped) Alignment.End else Alignment.Start
    val lineTextAlign = if (flipped) TextAlign.End else TextAlign.Start
    val requestedBlur = max(max(distanceBlurDp, focusBlurDp), 0f)
    val effectiveBlur = when (renderingQuality) {
        LyricsRenderingQuality.Low -> 0f
        LyricsRenderingQuality.Balanced -> requestedBlur * .55f
        LyricsRenderingQuality.High -> requestedBlur
    }
    val accompanimentBefore = line.accompaniment.filter { it.timeMs < line.timeMs }
    val accompanimentAfter = line.accompaniment.filterNot { it.timeMs < line.timeMs }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .graphicsLayer {
                translationY = visualOffsetPx
                scaleX = visualScale
                scaleY = visualScale
                alpha = rowAlpha
                transformOrigin = TransformOrigin(if (flipped) 1f else 0f, 0f)
            }
            .clickable(
                enabled = tapSeekEnabled,
                onClick = { if (tapSeekEnabled) onClick() },
            )
            // The scale origin sits on the flipped-aware start edge, so the
            // reserved gutter goes to the opposite side where the text grows.
            .padding(
                start = if (flipped) 8.dp + focusScaleReservePadding else 8.dp,
                end = if (flipped) 8.dp else 8.dp + focusScaleReservePadding,
            ),
        horizontalAlignment = lineAlignment,
    ) {
        accompanimentBefore.forEach { vocal ->
            TimedAccompaniment(vocal, playbackTimeProvider, fontScale, reduceMotion, focusProgress, renderingQuality, effectiveBlur)
        }
        if (showRomanization) {
            RubyLyricText(
                line = line,
                playbackTimeProvider = playbackTimeProvider,
                // Only the current/transitioning line subscribes to the frame
                // clock. Static ruby rows otherwise invalidated every unit at
                // 60 Hz even though their visual result did not change.
                supportsTimedLyrics = supportsTimedLyrics,
                fontScale = fontScale,
                renderingQuality = renderingQuality,
                softBlurDp = effectiveBlur,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            GlyphLyricText(
                line = line,
                playbackTimeProvider = playbackTimeProvider,
                supportsTimedLyrics = supportsTimedLyrics,
                fontScale = fontScale,
                //  iOS sends pseudo-timed LRC through this same
                // renderer. Only genuine long tones expand; ordinary glyphs
                // merely reveal and lift according to the selected mode.
                reduceMotion = reduceMotion,
                timingEffectsStrength = timingEffectsStrength,
                unplayedAlpha = timedUnplayedAlpha,
                renderingQuality = renderingQuality,
                softBlurDp = effectiveBlur,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        accompanimentAfter.forEach { vocal ->
            TimedAccompaniment(vocal, playbackTimeProvider, fontScale, reduceMotion, focusProgress, renderingQuality, effectiveBlur)
        }

        val romanSize = max(UpstreamLyrics.FONT_SIZE_SP * fontScale * SettingsRuntime.lyricRomanizationFontScale, 13f)
        if (!showRomanization && reserveRomanization) {
            val romanHeight = with(LocalDensity.current) { (romanSize * 1.2f).sp.toDp() }
            Spacer(Modifier.height(romanHeight + UpstreamLyrics.ANNOTATION_SPACING_DP.dp))
        }

        val translationSize = max(UpstreamLyrics.FONT_SIZE_SP * fontScale * SettingsRuntime.lyricTranslationFontScale, 13f)
        if (showTranslation) {
            Text(
                text = line.translation.orEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = UpstreamLyrics.ANNOTATION_SPACING_DP.dp),
                color = Color.White.copy(alpha = SettingsRuntime.lyricTranslationOpacity),
                textAlign = lineTextAlign,
                fontSize = translationSize.sp,
                lineHeight = translationSize.sp * 1.2f,
                fontWeight = SettingsRuntime.lyricFontWeight.composeWeight,
                style = TextStyle(
                    shadow = if (effectiveBlur > .05f) {
                        Shadow(
                            color = Color.White.copy(alpha = .48f),
                            offset = Offset.Zero,
                            blurRadius = effectiveBlur,
                        )
                    } else null,
                ),
            )
        } else if (reserveTranslation) {
            val translationHeight = with(LocalDensity.current) { (translationSize * 1.2f).sp.toDp() }
            Spacer(Modifier.height(translationHeight + UpstreamLyrics.ANNOTATION_SPACING_DP.dp))
        }
    }
}

@Composable
internal fun TimedAccompaniment(
    vocal: com.muses.player.core.lyrics.model.LyricAccompaniment,
    playbackTimeProvider: () -> Long,
    fontScale: Float,
    reduceMotion: Boolean,
    focusProgress: Float,
    renderingQuality: LyricsRenderingQuality,
    softBlurDp: Float,
) {
    val end = vocal.durationMs?.let { vocal.timeMs + it }
        ?: vocal.syllables.maxOfOrNull { it.endTimeMs }
        ?: vocal.timeMs
    val visible = playbackTimeProvider() in (vocal.timeMs - 600L)..(end + 600L)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 } + expandVertically(tween(600)),
        exit = fadeOut(tween(600)) + slideOutVertically(tween(600)) { it / 2 } + shrinkVertically(tween(600)),
    ) {
        val vocalLine = LyricLine(vocal.timeMs, vocal.durationMs, vocal.text, vocal.syllables, agent = vocal.agent)
        GlyphLyricText(
            line = vocalLine,
            playbackTimeProvider = playbackTimeProvider,
            supportsTimedLyrics = vocal.syllables.isNotEmpty(),
            fontScale = fontScale * .68f,
            reduceMotion = reduceMotion,
            timingEffectsStrength = focusProgress,
            renderingQuality = renderingQuality,
            softBlurDp = softBlurDp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { alpha = .72f },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RubyLyricText(
    line: LyricLine,
    playbackTimeProvider: () -> Long,
    supportsTimedLyrics: Boolean,
    fontScale: Float,
    renderingQuality: LyricsRenderingQuality,
    softBlurDp: Float = 0f,
    modifier: Modifier = Modifier,
) {
    if (SettingsRuntime.lyricWordByWordEnabled) {
        Column(modifier = modifier) {
            GlyphLyricText(
                line = line,
                playbackTimeProvider = playbackTimeProvider,
                supportsTimedLyrics = supportsTimedLyrics,
                fontScale = fontScale,
                reduceMotion = false,
                timingEffectsStrength = 1f,
                renderingQuality = renderingQuality,
                softBlurDp = softBlurDp,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!line.romanization.isNullOrBlank()) {
                Text(
                    text = line.romanization.orEmpty(),
                    color = Color.White.copy(alpha = SettingsRuntime.lyricRomanizationOpacity),
                    modifier = Modifier.fillMaxWidth().padding(top = UpstreamLyrics.ANNOTATION_SPACING_DP.dp),
                    fontSize = max(
                        UpstreamLyrics.FONT_SIZE_SP * fontScale * SettingsRuntime.lyricRomanizationFontScale,
                        13f,
                    ).sp,
                    lineHeight = (
                        max(
                            UpstreamLyrics.FONT_SIZE_SP * fontScale * SettingsRuntime.lyricRomanizationFontScale,
                            13f,
                        ) * 1.2f
                    ).sp,
                    fontWeight = SettingsRuntime.lyricFontWeight.composeWeight,
                    textAlign = if (line.agent?.alignment == com.muses.player.core.lyrics.model.LyricAgentAlignment.Flipped) TextAlign.End else TextAlign.Start,
                )
            }
        }
        return
    }

    val flipped = line.agent?.alignment == com.muses.player.core.lyrics.model.LyricAgentAlignment.Flipped
    val units = remember(line) { LyricRomanizationAligner.units(line) }
    if (units.isEmpty()) {
        GlyphLyricText(
            line = line,
            playbackTimeProvider = playbackTimeProvider,
            supportsTimedLyrics = supportsTimedLyrics,
            fontScale = fontScale,
            reduceMotion = true,
            timingEffectsStrength = 1f,
            renderingQuality = renderingQuality,
            softBlurDp = softBlurDp,
            modifier = modifier,
        )
        return
    }

    val primarySize = UpstreamLyrics.FONT_SIZE_SP * fontScale
    val rubySize = max(primarySize * SettingsRuntime.lyricRomanizationFontScale, 13f)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            2.dp,
            alignment = if (flipped) Alignment.End else Alignment.Start,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        units.forEach { unit ->
            val start = unit.originalSyllables.minOfOrNull { it.startTimeMs } ?: 0L
            val end = unit.originalSyllables.maxOfOrNull { it.endTimeMs }?.coerceAtLeast(start + 1L) ?: 1L
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = unit.originalText,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (!supportsTimedLyrics || unit.originalSyllables.isEmpty()) {
                            1f
                        } else {
                            val reveal = ((playbackTimeProvider() - start).toFloat() / (end - start).toFloat())
                                .coerceIn(0f, 1f)
                            0.3f + reveal * 0.7f
                        }
                    },
                    fontSize = primarySize.sp,
                    lineHeight = (UpstreamLyrics.LINE_HEIGHT_SP * fontScale).sp,
                    fontWeight = SettingsRuntime.lyricFontWeight.composeWeight,
                    maxLines = 1,
                    style = TextStyle(
                        shadow = if (softBlurDp > .05f) {
                            Shadow(Color.White.copy(alpha = .48f), Offset.Zero, softBlurDp)
                        } else null,
                    ),
                )
                val rubyText = unit.romanizationText
                if (!rubyText.isNullOrBlank()) {
                    val originalUnits = unit.originalText.codePointCount(0, unit.originalText.length).coerceAtLeast(1)
                    val rubyUnits = rubyText.codePointCount(0, rubyText.length).coerceAtLeast(1)
                    val rubyCompression = (originalUnits * 1.85f / rubyUnits).coerceIn(.68f, 1f)
                    Text(
                        text = rubyText,
                        color = Color.White.copy(alpha = SettingsRuntime.lyricRomanizationOpacity),
                        fontSize = (rubySize * rubyCompression).sp,
                        lineHeight = (rubySize * 1.2f).sp,
                        fontWeight = SettingsRuntime.lyricFontWeight.composeWeight,
                        maxLines = 1,
                        style = TextStyle(
                            shadow = if (softBlurDp > .05f) {
                                Shadow(Color.White.copy(alpha = .42f), Offset.Zero, softBlurDp)
                            } else null,
                        ),
                    )
                }
            }
        }
    }
}

internal data class GlyphVisual(
    val reveal: Float,
    val liftPx: Float,
    val scale: Float,
    val glow: Float,
    val shakeXPx: Float,
    val shakeYPx: Float,
)

internal data class GlyphTiming(
    val textOffset: Int,
    val start: Float,
    val end: Float,
    val liftStart: Float,
    val liftEnd: Float,
    val syllableStart: Float,
    val syllableEnd: Float,
    val characterIndex: Int,
    val characterCount: Int,
    val wordStart: Float,
    val wordEnd: Float,
    val wordCharacterIndex: Int,
    val wordCharacterCount: Int,
    val usesWordTimingForLongTone: Boolean,
    val longToneStart: Float,
    val longToneDuration: Float,
    val longToneCharacterIndex: Int,
    val longToneCharacterCount: Int,
    val isLongTone: Boolean,
    val expansionAmount: Float,
    val glowAmount: Float,
)

internal data class DrawableGlyph(
    val textOffset: Int,
    val text: String,
    val bounds: Rect,
    val baseline: Float,
)

internal val InactiveGlyphVisual = GlyphVisual(0f, 0f, 1f, 0f, 0f, 0f)

internal data class LyricInterlude(
    val startTimeMs: Long,
    val countdownEndTimeMs: Long,
    val followingLyricTimeMs: Long,
    val followingLyricIndex: Int,
)

@Composable
internal fun LyricInterludeCountdown(
    interlude: LyricInterlude,
    playbackTimeProvider: () -> Long,
    reduceMotion: Boolean,
    visualScale: Float,
    visualOffsetPx: Float,
    rowAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .fillMaxWidth(0.28f)
            .height(48.dp)
            .graphicsLayer {
                translationY = visualOffsetPx
                scaleX = visualScale
                scaleY = visualScale
                alpha = rowAlpha
                transformOrigin = TransformOrigin(0f, 0f)
            },
    ) {
        val now = playbackTimeProvider()
        if (now < interlude.startTimeMs) return@Canvas
        val duration = (interlude.countdownEndTimeMs - interlude.startTimeMs).coerceAtLeast(1L)
        val elapsed = (now - interlude.startTimeMs).coerceIn(0L, duration)
        val remaining = interlude.countdownEndTimeMs - now
        if (remaining <= 0L) return@Canvas
        val durationFloat = duration.toFloat()
        val elapsedFloat = elapsed.toFloat()
        val baseRadius = 5.dp.toPx()
        val gap = 18.dp.toPx()
        val centerY = size.height / 2f
        repeat(3) { index ->
            val segmentStart = durationFloat * index / 3f
            val progress = ((elapsedFloat - segmentStart) / (durationFloat / 3f)).coerceIn(0.25f, 1f)
            val fadeOut = (remaining / 375f).coerceIn(0f, 1f)
            val breathe = if (reduceMotion) 1f else 1f +
                sin(elapsed.toDouble() / 1_500.0 * 2.0 * Math.PI).toFloat() * 0.05f
            drawCircle(
                color = Color.White.copy(alpha = progress * fadeOut),
                radius = baseRadius * breathe * if (index == 2) (1f + progress * 0.18f) else 1f,
                center = Offset(baseRadius + index * gap, centerY),
            )
        }
    }
}

@Composable
internal fun GlyphLyricText(
    line: LyricLine,
    playbackTimeProvider: () -> Long,
    supportsTimedLyrics: Boolean,
    fontScale: Float,
    reduceMotion: Boolean,
    timingEffectsStrength: Float,
    unplayedAlpha: Float = SettingsRuntime.lyricInactiveOpacity,
    renderingQuality: LyricsRenderingQuality,
    softBlurDp: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val flipped = line.agent?.alignment == com.muses.player.core.lyrics.model.LyricAgentAlignment.Flipped
    val density = LocalDensity.current
    val lyricWeight = SettingsRuntime.lyricFontWeight.composeWeight
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val style = TextStyle(
            color = Color.White,
            fontFamily = LocalFontFamily.current,
            fontSize = (UpstreamLyrics.FONT_SIZE_SP * fontScale).sp,
            lineHeight = (UpstreamLyrics.LINE_HEIGHT_SP * fontScale).sp,
            fontWeight = lyricWeight,
            textAlign = if (flipped) TextAlign.End else TextAlign.Start,
        )
        val layout = remember(line.text, widthPx, style) {
            textMeasurer.measure(
                text = AnnotatedString(line.text),
                style = style,
                constraints = Constraints(minWidth = widthPx, maxWidth = widthPx),
                softWrap = true,
            )
        }
        val height = with(density) { layout.size.height.toDp() }
        val drawableGlyphs = remember(layout, line.text) {
            buildList {
                var offset = 0
                while (offset < line.text.length) {
                    val character = line.text[offset]
                    val codeUnitCount = if (
                        Character.isHighSurrogate(character) &&
                        offset + 1 < line.text.length &&
                        Character.isLowSurrogate(line.text[offset + 1])
                    ) 2 else 1
                    if (character != '\n' && character != '\r' && !Character.isLowSurrogate(character)) {
                        val bounds = runCatching { layout.getBoundingBox(offset) }.getOrNull()?.takeIf { box ->
                            box.width.isFinite() && box.height.isFinite() && box.width > 0f && box.height > 0f
                        }
                        if (bounds != null) {
                            val lineIndex = layout.getLineForOffset(offset)
                            val baseline = layout.getLineBaseline(lineIndex)
                            add(
                                DrawableGlyph(
                                    textOffset = offset,
                                    text = line.text.substring(offset, offset + codeUnitCount),
                                    bounds = bounds,
                                    baseline = baseline,
                                ),
                            )
                        }
                    }
                    offset += codeUnitCount
                }
            }
        }
        val liftMode = SettingsRuntime.lyricLiftMode
        val longToneDetectionMode = SettingsRuntime.lyricLongToneDetectionMode
        val longToneThresholdMs = SettingsRuntime.lyricLongToneThresholdMs
        val glyphTimings = remember(line, liftMode, longToneDetectionMode, longToneThresholdMs) {
            sourceGlyphTimings(
                line = line,
                liftMode = liftMode,
                longToneDetectionMode = longToneDetectionMode,
                longToneThresholdMs = longToneThresholdMs,
            )
        }

        // U21 跨平台 glyph 绘制：以整行同款 TextStyle 逐字预排版，drawText 按基线对齐绘制。
        // 光晕/柔焦用 TextStyle.Shadow（compose 双端一致），替代 android 的
        // nativeCanvas.drawText + BlurMaskFilter；透明字色只留模糊白轮廓（发光/失焦柔焦语义）。
        val glyphLayoutCache = remember(style) { mutableMapOf<String, TextLayoutResult>() }
        fun glyphLayoutFor(text: String, shadowBlurPx: Float?): TextLayoutResult =
            glyphLayoutCache.getOrPut(if (shadowBlurPx == null) text else "$text\u0001$shadowBlurPx") {
                val effectiveStyle = if (shadowBlurPx == null) {
                    style
                } else {
                    style.copy(shadow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = shadowBlurPx))
                }
                textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = effectiveStyle,
                    softWrap = false,
                )
            }

        val animatesTiming = supportsTimedLyrics &&
            timingEffectsStrength > 0.001f &&
            line.text.isNotEmpty()
        if (!animatesTiming) {
            // 柔焦（距离/焦点模糊）整行走 Shadow 白轮廓：一次 drawText 代替逐字 maskFilter，
            // 视觉等价于按行模糊白字轮廓，字形位置仍来自 Compose 排版
            val softBlurPx = with(density) { softBlurDp.dp.toPx() }
            val blurredLayout = if (softBlurPx > .05f) {
                remember(layout, softBlurPx) {
                    textMeasurer.measure(
                        text = AnnotatedString(line.text),
                        style = style.copy(
                            shadow = Shadow(color = Color.White, offset = Offset.Zero, blurRadius = softBlurPx),
                        ),
                        constraints = Constraints(minWidth = widthPx, maxWidth = widthPx),
                        softWrap = true,
                    )
                }
            } else {
                null
            }
            Canvas(Modifier.fillMaxWidth().height(height)) {
                val blurred = blurredLayout
                if (blurred == null) {
                    drawText(layout, color = Color.White)
                } else {
                    drawText(blurred, color = Color.Transparent)
                }
            }
            return@BoxWithConstraints
        }

        Canvas(Modifier.fillMaxWidth().height(height)) {
            val playbackTimeMs = playbackTimeProvider()
            val effectsStrength = sourceTimingEffectsStrength(
                line = line,
                playbackTimeMs = playbackTimeMs,
                focusProgress = timingEffectsStrength,
            )
            if (effectsStrength <= 0.0001f) {
                drawText(
                    layout,
                    color = Color.White.copy(alpha = unplayedAlpha),
                )
                return@Canvas
            }

            // The unrevealed layer must remain at the ordinary unplayed lyric
            // opacity. Interpolating it from fully white made the whole next
            // line flash before the first syllable started revealing.
            val unplayedAlpha = unplayedAlpha.coerceIn(0f, 1f)

            if ((renderingQuality != LyricsRenderingQuality.High || reduceMotion) &&
                (!SettingsRuntime.lyricWordByWordEnabled || reduceMotion)
            ) {
                // Low/Balanced render the complete shaped row twice and reveal
                // it with one row mask. This preserves ligatures and reduces a
                // CJK line from dozens of native drawText/clip calls to two.
                drawText(layout, color = Color.White.copy(alpha = unplayedAlpha))
                val first = line.syllables.minOfOrNull { it.startTimeMs } ?: line.timeMs
                val last = line.syllables.maxOfOrNull { it.endTimeMs }
                    ?: (line.timeMs + (line.durationMs ?: 2_000L))
                val rowReveal = ((playbackTimeMs - first).toFloat() / (last - first).coerceAtLeast(1L))
                    .coerceIn(0f, 1f)
                if (rowReveal > 0f) {
                    if (isRtl) {
                        clipRect(left = size.width * (1f - rowReveal)) {
                            drawText(layout, color = Color.White)
                        }
                    } else {
                        clipRect(right = size.width * rowReveal) {
                            drawText(layout, color = Color.White)
                        }
                    }
                }
                return@Canvas
            }

            for (glyph in drawableGlyphs) {
                val bounds = glyph.bounds
                val fx = glyphTimings.getOrNull(glyph.textOffset)?.let { timing ->
                    sourceGlyphVisual(
                        timing = timing,
                        playbackTimeMs = playbackTimeMs,
                        density = density.density,
                        reduceMotion = reduceMotion,
                        fontScale = fontScale,
                    )
                } ?: InactiveGlyphVisual

                withTransform({
                    translate(
                        left = fx.shakeXPx * effectsStrength,
                        top = -fx.liftPx * effectsStrength + fx.shakeYPx * effectsStrength,
                    )
                    val presentationScale = 1f + (fx.scale - 1f) * effectsStrength
                    scale(
                        scaleX = presentationScale,
                        scaleY = presentationScale,
                        pivot = bounds.center,
                    )
                }) {
                    fun drawGlyph(alpha: Float, shadowBlurPx: Float? = null) {
                        // The measured position and baseline come from the
                        // complete Compose layout, but only this glyph is
                        // rasterized. Both layers share this transformed
                        // coordinate space, matching iOS's runContext.
                        // U21：单字 drawText（基线对齐）替代 nativeCanvas.drawText；
                        // shadowBlurPx 非空时走 Shadow 白轮廓（透明字色只留光晕）。
                        val result = glyphLayoutFor(glyph.text, shadowBlurPx)
                        drawText(
                            textLayoutResult = result,
                            color = if (shadowBlurPx == null) {
                                Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
                            } else {
                                Color.Transparent
                            },
                            topLeft = Offset(glyph.bounds.left, glyph.baseline - result.firstBaseline),
                        )
                    }

                    // Draw the unplayed layer after applying the glyph's lift
                    // and expansion. Keeping it at the original line position
                    // left a gray duplicate below every lifted white glyph.
                    drawGlyph(unplayedAlpha)

                    val reveal = fx.reveal.coerceIn(0f, 1f)
                    val glow = fx.glow * effectsStrength * SettingsRuntime.lyricGlowStrength
                    if (
                        reveal > 0f &&
                        glow > 0.001f &&
                        renderingQuality != LyricsRenderingQuality.Low &&
                        !reduceMotion
                    ) {
                        val glowRadius = with(density) {
                            (style.fontSize.toPx() * if (renderingQuality == LyricsRenderingQuality.High) .30f else .18f)
                                .coerceAtLeast(3.dp.toPx())
                        }
                        val revealFront = if (isRtl) {
                            bounds.right - bounds.width * reveal
                        } else {
                            bounds.left + bounds.width * reveal
                        }
                        // U22：Shadow 在 TextLayoutResult 中烘焙，clipRect 无法约束其模糊溢出，
                        // 导致整行辉光。将 shadowBlurPx 缩至 glowRadius 的 12%（≤1.5dp），
                        // 使溢出不可见，同时保留光晕语义。
                        val clampedGlowBlur = glowRadius * 0.12f
                        clipRect(
                            left = if (isRtl) revealFront - glowRadius else bounds.left - glowRadius,
                            top = bounds.top - glowRadius,
                            right = if (isRtl) bounds.right + glowRadius else revealFront + glowRadius,
                            bottom = bounds.bottom + glowRadius,
                        ) {
                            drawGlyph(glow.coerceIn(0f, 1f) * .648f, clampedGlowBlur)
                        }
                    }

                    clipRect(
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom,
                    ) {
                        if (reveal <= 0f) return@clipRect

                        // AMLL 对齐：fadeWidth = word.height * wordFadeWidth(0.5)。
                        // 旧值 0.7*字宽在 CJK 大字上羽化带过宽，看起来整字发虚。
                        val feather = max(
                            bounds.height * 0.5f,
                            1.5f * density.density,
                        )
                        val front = if (isRtl) {
                            bounds.right + feather - (bounds.width + feather) * reveal
                        } else {
                            bounds.left - feather + (bounds.width + feather) * reveal
                        }
                        val solidLeft = if (isRtl) max(front, bounds.left) else bounds.left
                        val solidRight = if (isRtl) bounds.right else min(front, bounds.right)
                        fun drawRevealed(alpha: Float) {
                            if (solidRight > solidLeft) {
                                clipRect(
                                    left = solidLeft,
                                    top = bounds.top,
                                    right = solidRight,
                                    bottom = bounds.bottom,
                                ) {
                                    drawGlyph(alpha)
                                }
                            }

                            val stopCount = when (renderingQuality) {
                                LyricsRenderingQuality.Low -> 1
                                LyricsRenderingQuality.Balanced -> 2
                                LyricsRenderingQuality.High -> 3
                            }
                            for (step in 0 until stopCount) {
                                val a = step.toFloat() / stopCount.toFloat()
                                val b = (step + 1).toFloat() / stopCount.toFloat()
                                val mid = (a + b) * .5f
                                val remaining = 1f - mid
                                val baseMask = remaining *
                                    (1f - SettingsRuntime.lyricHighlightGradientReduction * mid)
                                val maskAlpha = baseMask +
                                    (1f - baseMask) * glow.coerceIn(0f, 1f) * .14f
                                val left = if (isRtl) {
                                    max(front - feather * b, bounds.left)
                                } else {
                                    max(front + feather * a, bounds.left)
                                }
                                val right = if (isRtl) {
                                    min(front - feather * a, bounds.right)
                                } else {
                                    min(front + feather * b, bounds.right)
                                }
                                if (right > left) {
                                    clipRect(
                                        left = left,
                                        top = bounds.top,
                                        right = right,
                                        bottom = bounds.bottom,
                                    ) {
                                        drawGlyph(alpha * maskAlpha.coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }

                        drawRevealed(1f)
                    }
                }
            }
        }
    }
}

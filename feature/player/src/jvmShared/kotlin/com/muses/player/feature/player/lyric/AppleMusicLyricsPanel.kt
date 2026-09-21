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
import com.muses.player.core.data.store.platformMonotonicMs
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppleMusicLyricsPanel(
    state: PlaybackUiState,
    modifier: Modifier = Modifier,
    isInterfaceHidden: Boolean = false,
    onInterfaceInteraction: () -> Unit = {},
    onInterfaceVisibilityChange: (Boolean) -> Unit = {},
    active: Boolean = true,
    externalDocument: com.muses.player.core.lyrics.model.LyricsDocument? = null,
) {
    val haptics = LocalHapticFeedback.current
    val activeState = rememberUpdatedState(active)
    // Keep four future lines composed below the viewport. Their independent
    // cascade animations can then run before clipping reveals them.
    val listState = rememberLazyListState(
        cacheWindow = LazyLayoutCacheWindow(
            ahead = 720.dp,
            behind = 240.dp,
        ),
    )
    val density = LocalDensity.current
    val mediaId = state.mediaId

    val automaticLyricSelectionEnabled = SettingsRuntime.automaticLyricSelectionEnabled
    var lyrics by remember(mediaId, automaticLyricSelectionEnabled) { mutableStateOf<LyricsDocument?>(null) }
    var isLoading by remember(mediaId) { mutableStateOf(false) }
    var errorMessage by remember(mediaId) { mutableStateOf<String?>(null) }

    var anchorPositionMs by remember(mediaId) { mutableLongStateOf(state.positionMs) }
    var anchorRealtimeMs by remember(mediaId) { mutableLongStateOf(platformMonotonicMs()) }
    val renderedPositionState = remember(mediaId) { mutableLongStateOf(state.positionMs) }
    var holdTrackAtStart by remember(mediaId) { mutableStateOf(false) }
    var initializedMediaId by remember { mutableStateOf(mediaId) }
    var hasInitializedTrack by remember { mutableStateOf(false) }

    // A Bluetooth/headset skip can deliver the first UI state for the new item
    // before its position callback arrives.  Do not let the previous track's
    // progress keep the new lyric document focused in the middle of the list.
    // A newly composed panel, however, is re-entering the current track and must
    // retain its reported position.
    LaunchedEffect(mediaId) {
        val initialization = lyricsPanelPlaybackInitialization(
            isFirstComposition = !hasInitializedTrack,
            mediaIdChanged = hasInitializedTrack && initializedMediaId != mediaId,
            reportedPositionMs = state.positionMs,
        )
        initializedMediaId = mediaId
        hasInitializedTrack = true
        anchorPositionMs = initialization.positionMs
        anchorRealtimeMs = platformMonotonicMs()
        renderedPositionState.longValue = initialization.positionMs
        holdTrackAtStart = initialization.holdAtTrackStart
        if (initialization.resetListToStart) listState.scrollToItem(0)
    }

    LaunchedEffect(state.positionMs, state.isPlaying, mediaId) {
        if (holdTrackAtStart) return@LaunchedEffect
        anchorPositionMs = state.positionMs
        anchorRealtimeMs = platformMonotonicMs()
        renderedPositionState.longValue = state.positionMs
    }

    LaunchedEffect(mediaId, state.title, state.artist, state.album, state.durationMs, automaticLyricSelectionEnabled) {
        if (mediaId.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        errorMessage = null
        runCatching { ProviderLyricsLoader.load(state) }
            .onSuccess { lyrics = it }
            .onFailure { errorMessage = it.message ?: "歌词加载失败" }
        // A newly selected item always enters from its lyric start anchor.
        // Subsequent seeks keep using the normal position callback above.
        holdTrackAtStart = false
        isLoading = false
    }

    val document = externalDocument ?: lyrics
    val pseudoTimingEnabled = SettingsRuntime.lyricPseudoTimingEnabled
    val renderedDocument = remember(document, pseudoTimingEnabled) {
        if (pseudoTimingEnabled) document?.withPseudoTiming() else document
    }
    val lines = renderedDocument?.lines.orEmpty()
    val hasSyllableSync = remember(renderedDocument) { lines.any { it.syllables.isNotEmpty() } }
    val bilibiliOffsetMs = rememberBilibiliLyricOffset(mediaId)
    val lyricAdvanceMs = effectiveBilibiliLyricAdvance(SettingsRuntime.lyricAdvanceMs, bilibiliOffsetMs)
    val isBilibiliLyrics = remember(mediaId) { isBilibiliMediaId(mediaId) }
    val usesWordByWordPresentation = hasSyllableSync && SettingsRuntime.lyricWordByWordEnabled
    val timedAdvanceMs = if (isBilibiliLyrics || SettingsRuntime.lyricAdvanceAppliesToWordByWord) {
        lyricAdvanceMs
    } else 0L
    val lineAdvanceMs = if (usesWordByWordPresentation) timedAdvanceMs else lyricAdvanceMs
    var highlightedIndex by remember(document) {
        mutableIntStateOf(
            renderedDocument?.highlightedIndex(
                initialLyricsHighlightPositionMs(renderedPositionState.longValue, lineAdvanceMs),
                LyricHighlightStrategy.FirstSyllableOrLineStart,
            ) ?: -1,
        )
    }
    var colorHighlightedIndex by remember(document) { mutableIntStateOf(highlightedIndex) }
    var activeTimedLineIndexes by remember(document) { mutableStateOf(emptySet<Int>()) }
    val playbackTimeProvider = remember(mediaId, timedAdvanceMs) {
        { renderedPositionState.longValue + timedAdvanceMs }
    }

    // Update the line index only when it actually changes. The per-frame time is
    // read later from Canvas, so the 60 Hz clock invalidates drawing rather than
    // recomposing and relaying out the complete lyric list.
    val renderingQuality = SettingsRuntime.lyricRenderingQuality
    val refreshRate = when (renderingQuality) {
        LyricsRenderingQuality.Low -> min(SettingsRuntime.lyricRefreshRate, 30)
        LyricsRenderingQuality.Balanced -> min(SettingsRuntime.lyricRefreshRate, 60)
        // Native glyph drawing is CPU-bound; more than one update per display
        // frame adds work without producing a visible state. 60 Hz remains the
        // ceiling even when a migrated preference contains 90/120 Hz.
        LyricsRenderingQuality.High -> min(SettingsRuntime.lyricRefreshRate, 60)
    }
    val interludes = remember(lines) { sourceLyricInterludes(lines) }
    val interludeByLyricIndex = remember(interludes) { interludes.associateBy { it.followingLyricIndex } }
    var activeInterludeIndex by remember(document) { mutableIntStateOf(-1) }

    // High-frequency clock only for Canvas word animation. Line focus changes
    // are computed in a separate event-driven loop so compositional work only
    // happens at lyric boundaries.
    LaunchedEffect(state.isPlaying, mediaId, document, refreshRate, AppVisibility.isForeground) {
        var lastFrameNanos = 0L
        val minimumFrameNanos = 1_000_000_000L / refreshRate.coerceIn(30, 120)
        while (true) {
            if (!activeState.value || !AppVisibility.isForeground || holdTrackAtStart) {
                delay(200L)
                continue
            }
            if (state.isPlaying) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos != 0L && frameNanos - lastFrameNanos < minimumFrameNanos) continue
                lastFrameNanos = frameNanos
            }
            val position = if (state.isPlaying) {
                anchorPositionMs + (platformMonotonicMs() - anchorRealtimeMs)
            } else {
                anchorPositionMs
            }
            renderedPositionState.longValue = position
            val nextInterlude = interludes.indexOfFirst { position >= it.startTimeMs && position < it.followingLyricTimeMs }
            if (nextInterlude != activeInterludeIndex) activeInterludeIndex = nextInterlude
            if (!state.isPlaying) delay(200L)
        }
    }

    val lineIndexSeekSignal = remember(mediaId) { Channel<Unit>(Channel.CONFLATED) }
    var previousSeekPositionMs by remember(mediaId) { mutableLongStateOf(state.positionMs) }
    var previousSeekRealtimeMs by remember(mediaId) { mutableLongStateOf(platformMonotonicMs()) }
    LaunchedEffect(state.positionMs) {
        if (state.positionMs != previousSeekPositionMs) {
            val expected = previousSeekPositionMs + if (state.isPlaying) {
                platformMonotonicMs() - previousSeekRealtimeMs
            } else {
                0L
            }
            if (kotlin.math.abs(state.positionMs - expected) > 200L) {
                lineIndexSeekSignal.trySend(Unit)
            }
            previousSeekPositionMs = state.positionMs
            previousSeekRealtimeMs = platformMonotonicMs()
        }
    }

    LaunchedEffect(state.isPlaying, mediaId, document, hasSyllableSync, lineAdvanceMs, AppVisibility.isForeground) {
        while (true) {
            if (!activeState.value || !AppVisibility.isForeground || holdTrackAtStart || renderedDocument == null) {
                delay(200L)
                continue
            }
            val position = if (state.isPlaying) {
                anchorPositionMs + (platformMonotonicMs() - anchorRealtimeMs)
            } else {
                anchorPositionMs
            }
            val effectivePosition = position + lineAdvanceMs
            val activeInterlude = if (
                SettingsRuntime.lyricInterludeCountdownEnabled
            ) interludes.getOrNull(activeInterludeIndex) else null
            val nextIndex = if (
                SettingsRuntime.lyricInterludeCountdownEnabled &&
                activeInterlude != null &&
                position >= activeInterlude.startTimeMs &&
                position < activeInterlude.followingLyricTimeMs
            ) {
                activeInterlude.followingLyricIndex
            } else {
                renderedDocument.highlightedIndex(effectivePosition, LyricHighlightStrategy.FirstSyllableOrLineStart) ?: -1
            }
            if (nextIndex != highlightedIndex) highlightedIndex = nextIndex
            val nextColorIndex = renderedDocument.highlightedIndex(
                effectivePosition + SettingsRuntime.lyricFocusColorLeadMs,
                LyricHighlightStrategy.FirstSyllableOrLineStart,
            ) ?: -1
            if (nextColorIndex != colorHighlightedIndex) colorHighlightedIndex = nextColorIndex
            val timedPosition = position + timedAdvanceMs
            val nextTimedLines = LyricTimelineProcessor.activeTimedLineIndexes(renderedDocument, timedPosition)
            if (nextTimedLines != activeTimedLineIndexes) activeTimedLineIndexes = nextTimedLines

            val lineWait = LyricTimelineProcessor.nextEventTimeMs(renderedDocument, effectivePosition)
                ?.minus(effectivePosition)
            val timedWait = LyricTimelineProcessor.nextEventTimeMs(renderedDocument, timedPosition)
                ?.minus(timedPosition)
            val waitMs = listOfNotNull(lineWait, timedWait).minOrNull()?.coerceIn(16L, 500L) ?: 250L
            withTimeoutOrNull(waitMs) { lineIndexSeekSignal.receive() }
            if (!state.isPlaying) delay(200L)
        }
    }

    val focusProgress = remember(document) {
        List(lines.size) { Animatable(0f) }
    }
    val scaleProgress = remember(document) {
        List(lines.size) { Animatable(0f) }
    }
    val cascadeLineProgress = remember(document) {
        List(lines.size) { Animatable(1f) }
    }
    val cascadeScrollProgress = remember(document) { Animatable(1f) }
    var cascadeDistancePx by remember(document) { mutableStateOf(0f) }

    /**
     * 本次 cascade 中**实际滚出去**的像素（浮点记账）。
     *
     * 行的虚拟偏移必须用它做「滚动补偿」，不能用「应滚量 × 动画进度」：后者与 LazyColumn
     * 实际落位（layoutInfo.offset，整数且滞后一帧）存在残差，残差随两条曲线收敛节奏变号，
     * 收尾时视觉位置会在 388.4～389.4 之间来回摆（实测，就是「高亮行抖一下」）。
     * 用实际滚动量则与列表位置严格互补，视觉位置只由该行自己的 spring 驱动。
     */
    var appliedScrollPx by remember(document) { mutableStateOf(0f) }

    /**
     * 本次 cascade 开始时各行的**布局基线**（layoutInfo.offset，浮点化）。
     *
     * 用来补偿 LazyColumn 的整数落位：行在列表里的位置是 `round(base - applied)`，
     * 取整残差会留在「itemY + translationY」里（实测收尾时 ± 0.5px 来回摆）。
     * 我们在渲染时把残差加回 translationY，使行视觉位置变成纯浮点（`base - applied + off`）。
     */
    var cascadeBaseOffsets by remember(document) { mutableStateOf<Map<Int, Float>>(emptyMap()) }
    var cascadeInitialOffsets by remember(document) {
        mutableStateOf<Map<Int, Float>>(emptyMap())
    }
    var cascadeDestinationOffsets by remember(document) {
        mutableStateOf<Map<Int, Float>>(emptyMap())
    }
    val rowHeightsPx = remember(
        document,
        SettingsRuntime.lyricFontScale,
        SettingsRuntime.lyricSpacingScale,
        SettingsRuntime.showLyricTranslation,
        SettingsRuntime.showLyricRomanization,
    ) { mutableStateMapOf<Int, Int>() }
    var viewportHeightPx by remember(document) { mutableIntStateOf(0) }
    var viewportWidthPx by remember(document) { mutableIntStateOf(0) }
    var visualFocusIndex by remember(document) { mutableIntStateOf(-1) }
    var isBrowsingLyrics by remember(document) { mutableStateOf(false) }
    var playbackFocusGeneration by remember(document) { mutableIntStateOf(0) }
    var browseGeneration by remember(document) { mutableIntStateOf(0) }
    var scrollHideDistancePx by remember(document) { mutableStateOf(0f) }
    val latestInterfaceHidden = rememberUpdatedState(isInterfaceHidden)
    val latestVisibilityCallback = rememberUpdatedState(onInterfaceVisibilityChange)
    val latestInteractionCallback = rememberUpdatedState(onInterfaceInteraction)
    var lastCanScrollForward by remember(document) { mutableStateOf(true) }
    var lastCanScrollBackward by remember(document) { mutableStateOf(true) }
    var initialLyricsPositioned by remember(renderedDocument) { mutableStateOf(lines.isEmpty()) }
    LaunchedEffect(document) {
        snapshotFlow { listState.canScrollForward to listState.canScrollBackward }
            .collect { (canForward, canBackward) ->
                if (SettingsRuntime.hapticFeedbackEnabled) {
                    if (lastCanScrollForward && !canForward) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (lastCanScrollBackward && !canBackward) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                lastCanScrollForward = canForward
                lastCanScrollBackward = canBackward
            }
    }

    val lyricFontScale = SettingsRuntime.lyricFontScale
    val lyricSpacingScale = SettingsRuntime.lyricSpacingScale
    val lineSpacingPx = with(density) { (UpstreamLyrics.LINE_SPACING_DP * lyricSpacingScale).dp.toPx() }
    val primaryHeightPx = with(density) { (UpstreamLyrics.LINE_HEIGHT_SP * lyricFontScale).sp.toPx() }
    val annotationFontPx = with(density) {
        max(UpstreamLyrics.FONT_SIZE_SP * lyricFontScale * SettingsRuntime.lyricRomanizationFontScale, 13f).sp.toPx()
    }
    val annotationSpacingPx = with(density) { UpstreamLyrics.ANNOTATION_SPACING_DP.dp.toPx() }

    fun estimatedHeight(index: Int): Float {
        rowHeightsPx[index]?.let { return it.toFloat() }
        val line = lines.getOrNull(index)
        var height = primaryHeightPx
        if (SettingsRuntime.showLyricRomanization && !line?.romanization.isNullOrBlank()) {
            height += annotationFontPx * 1.2f + annotationSpacingPx
        }
        if (SettingsRuntime.showLyricTranslation && !line?.translation.isNullOrBlank()) {
            height += annotationFontPx * 1.2f + annotationSpacingPx
        }
        return height
    }

    fun settledMovementOffset(index: Int, focusIndex: Int): Float {
        if (focusIndex !in lines.indices || index <= focusIndex) return 0f
        return max(
            estimatedHeight(focusIndex) * (SettingsRuntime.lyricFocusScale - 1f),
            0f,
        )
    }

    fun currentMovementOffset(index: Int): Float {
        val initial = cascadeInitialOffsets[index]
            ?: return settledMovementOffset(index, visualFocusIndex)
        val destination = cascadeDestinationOffsets[index] ?: 0f
        val lineProgress = cascadeLineProgress[index].value
        return initial + appliedScrollPx -
            (cascadeDistancePx + initial - destination) * lineProgress
    }

    /**
     * LazyColumn 取整残差的补偿量：`frac(base - applied)`。
     *
     * 行实际布局位置 = `round(base - applied)`（已实测与预测完全一致），
     * 而我们要的视觉位置是 `base - applied + off`；
     * 二者相减就是这里返回的残差，加到 translationY 上即可得到纯浮点的行位置。
     */
    fun roundingCorrection(index: Int): Float {
        val base = cascadeBaseOffsets[index] ?: return 0f
        val currentBase = base - appliedScrollPx
        return currentBase - kotlin.math.round(currentBase)
    }

    suspend fun clearCascadePresentation(focusIndex: Int) {
        visualFocusIndex = focusIndex
        cascadeInitialOffsets = emptyMap()
        cascadeDestinationOffsets = emptyMap()
        cascadeBaseOffsets = emptyMap()
        cascadeDistancePx = 0f
        cascadeScrollProgress.snapTo(1f)
    }

    suspend fun handOffFocusColor(targetIndexes: Set<Int>) = coroutineScope {
        focusProgress.forEachIndexed { index, anim ->
            val target = if (index in targetIndexes) 1f else 0f
            if (abs(anim.value - target) > 0.0001f) {
                launch {
                    if (SettingsRuntime.lyricReduceMotion) anim.snapTo(target)
                    else anim.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = UpstreamLyrics.FOCUS_COLOR_DURATION_MS,
                            easing = SourceSmoothStepEasing,
                        ),
                    )
                }
            }
        }
    }

    suspend fun handOffFocusScale(previousIndex: Int, nextIndex: Int) = coroutineScope {
        if (SettingsRuntime.lyricReduceMotion) {
            scaleProgress.forEachIndexed { index, anim -> anim.snapTo(if (index == nextIndex) 1f else 0f) }
            return@coroutineScope
        }
        if (previousIndex in scaleProgress.indices && previousIndex != nextIndex) {
            launch {
                scaleProgress[previousIndex].animateTo(
                    0f,
                    if (SettingsRuntime.lyricScaleBounceEnabled) {
                        spring(dampingRatio = 0.82f, stiffness = 380f)
                    } else tween(durationMillis = SettingsRuntime.lyricScaleBounceDurationMs, easing = SourceSmoothStepEasing),
                )
            }
        }
        if (nextIndex in scaleProgress.indices) {
            launch {
                scaleProgress[nextIndex].animateTo(
                    1f,
                    if (SettingsRuntime.lyricScaleBounceEnabled) {
                        spring(dampingRatio = 0.80f, stiffness = 320f)
                    } else tween(durationMillis = SettingsRuntime.lyricScaleBounceDurationMs, easing = SourceSmoothStepEasing),
                )
            }
        }
    }

    LaunchedEffect(colorHighlightedIndex, activeTimedLineIndexes, activeInterludeIndex, document) {
        if (activeInterludeIndex >= 0) {
            handOffFocusColor(emptySet())
        } else {
            val targets = activeTimedLineIndexes.ifEmpty {
                colorHighlightedIndex.takeIf(lines.indices::contains)?.let(::setOf).orEmpty()
            }
            handOffFocusColor(targets)
        }
    }

    val scrollHideThresholdPx = with(density) { SettingsRuntime.lyricScrollHideThresholdDp.dp.toPx() }
    val lyricInteractionConnection = remember(document, scrollHideThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val offsetDelta = -available.y
                if (kotlin.math.abs(offsetDelta) < 0.01f) return Offset.Zero

                isBrowsingLyrics = true
                browseGeneration += 1

                if (offsetDelta < 0f) {
                    scrollHideDistancePx = 0f
                    if (latestInterfaceHidden.value) {
                        latestVisibilityCallback.value.invoke(true)
                    } else {
                        latestInteractionCallback.value.invoke()
                    }
                } else if (!latestInterfaceHidden.value) {
                    latestInteractionCallback.value.invoke()
                    scrollHideDistancePx += offsetDelta
                    if (scrollHideDistancePx >= scrollHideThresholdPx) {
                        scrollHideDistancePx = 0f
                        latestVisibilityCallback.value.invoke(false)
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(browseGeneration, document) {
        if (browseGeneration <= 0) return@LaunchedEffect
        delay(SettingsRuntime.lyricFollowDelayMs.toLong())
        isBrowsingLyrics = false
        playbackFocusGeneration += 1
    }

    LaunchedEffect(isBrowsingLyrics, document) {
        if (isBrowsingLyrics) clearCascadePresentation(visualFocusIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewportHeightPx = it.height
                viewportWidthPx = it.width
            },
    ) {
        val focusPosition = SettingsRuntime.lyricFocusPosition
        val topPaddingPx = viewportHeightPx * focusPosition
        val bottomPaddingPx = max(
            viewportHeightPx * (1f - focusPosition),
            with(density) { 40.dp.toPx() },
        )

        fun focusItemScrollOffset(index: Int): Int {
            if (index !in lines.indices || viewportHeightPx <= 0) return 0
            val viewportAnchor = viewportHeightPx * focusPosition
            // 聚焦行视觉高度含 lyricFocusScale 放大（与 settledMovementOffset 同口径）：
            // 首屏/跳转用未放大高度算目标会偏下，长句换行时偏差更大
            val focusHeight = estimatedHeight(index) * SettingsRuntime.lyricFocusScale
            val desiredItemTop = viewportAnchor - focusHeight * focusPosition
            return -desiredItemTop.roundToInt()
        }

        val playbackFocusIndex = activeTimedLineIndexes.minOrNull() ?: highlightedIndex

        LaunchedEffect(
            playbackFocusIndex,
            playbackFocusGeneration,
            viewportHeightPx,
            isBrowsingLyrics,
            document,
        ) {
            val sourceIndex = highlightedIndex
            val nextIndex = playbackFocusIndex
            if (viewportHeightPx <= 0 || isBrowsingLyrics) {
                if (isBrowsingLyrics && sourceIndex in lines.indices) {
                    visualFocusIndex = sourceIndex
                }
                return@LaunchedEffect
            }

            if (nextIndex !in lines.indices) {
                listState.scrollToItem(0)
                initialLyricsPositioned = true
                return@LaunchedEffect
            }

            val previousIndex = visualFocusIndex

            if (previousIndex !in lines.indices) {
                // 首屏定位：等一帧让可见行真实高度经 onMeasured 回写进 rowHeightsPx，
                // 再用实测高度算目标。否则首屏用估算高度（长句换行/放大行偏小）定位偏下，
                // 后续每次回写都漂移目标导致乱跳。面板此时透明（initialLyricsPositioned=false），晚一帧无感知
                androidx.compose.runtime.withFrameNanos { }
                val firstTargetOffset = focusItemScrollOffset(nextIndex)
                listState.scrollToItem(nextIndex + 1, firstTargetOffset)
                focusProgress.forEachIndexed { index, anim ->
                    val targets = activeTimedLineIndexes.ifEmpty {
                        colorHighlightedIndex.takeIf(lines.indices::contains)?.let(::setOf).orEmpty()
                    }
                    anim.snapTo(if (index in targets) 1f else 0f)
                }
                scaleProgress.forEachIndexed { index, anim ->
                    anim.snapTo(if (index == nextIndex) 1f else 0f)
                }
                clearCascadePresentation(nextIndex)
                initialLyricsPositioned = true
                return@LaunchedEffect
            }

            if (previousIndex == nextIndex) return@LaunchedEffect

            // 非首屏跳转：此时 rowHeightsPx 已有实测值，直接现算目标（首屏分支内已单独处理）
            val targetOffset = focusItemScrollOffset(nextIndex)

            if (!SettingsRuntime.lyricAutoFollowEnabled) {
                handOffFocusScale(previousIndex, nextIndex)
                visualFocusIndex = nextIndex
                return@LaunchedEffect
            }

            if (SettingsRuntime.lyricReduceMotion) {
                clearCascadePresentation(nextIndex)
                handOffFocusScale(previousIndex, nextIndex)
                listState.scrollToItem(nextIndex + 1, targetOffset)
                return@LaunchedEffect
            }

            val baseDurationMs = sourceFocusAnimationDurationMs(nextIndex, lines)
            val skippedLineCount = (nextIndex - previousIndex).coerceAtLeast(1)
            val isAdjacentForward = skippedLineCount == 1
            val effectivePosition = renderedPositionState.longValue + lyricAdvanceMs
            val remainingMs = sourceRemainingFocusDurationMs(nextIndex, effectivePosition, lines)

            val fullCascadeMs = max(baseDurationMs.toFloat(), SettingsRuntime.lyricCascadeDurationMs)
            val availableMs = remainingMs?.coerceAtLeast(0f)
            val cascadeDurationMs = if (availableMs == null) {
                fullCascadeMs
            } else {
                if (availableMs < SettingsRuntime.lyricSnapThresholdMs) 0f
                else min(fullCascadeMs, availableMs)
            }

            val desiredTop = -targetOffset.toFloat()
            val targetItem = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == nextIndex + 1 }
            if (cascadeDurationMs <= 0f || targetItem == null) {
                clearCascadePresentation(nextIndex)
                coroutineScope {
                    launch { handOffFocusScale(previousIndex, nextIndex) }
                    launch {
                        if (cascadeDurationMs <= 0f) {
                            listState.scrollToItem(nextIndex + 1, targetOffset)
                        } else {
                            listState.animateScrollToItem(
                                nextIndex + 1,
                                targetOffset,
                            )
                        }
                    }
                }
                return@LaunchedEffect
            }

            if (!isAdjacentForward) {
                // Several lines can finish at the same timestamp. Advance one
                // row at a time so intermediate lyrics are visibly promoted
                // with the same cadence as an ordinary adjacent transition.
                val singleStepDurationMs = fullCascadeMs.coerceAtLeast(1f)
                clearCascadePresentation(nextIndex)
                handOffFocusScale(previousIndex, nextIndex)
                for (stepIndex in (previousIndex + 1)..nextIndex) {
                    val stepOffset = focusItemScrollOffset(stepIndex)
                    val stepItem = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.index == stepIndex + 1 }
                    if (stepItem == null) {
                        listState.scrollToItem(stepIndex + 1, stepOffset)
                        continue
                    }
                    val stepDesiredTop = -stepOffset.toFloat()
                    val stepDistance = stepItem.offset - stepDesiredTop
                    listState.scroll {
                        var previousProgress = 0f
                        Animatable(0f).animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = singleStepDurationMs.roundToInt(),
                                easing = SourceSmoothStepEasing,
                            ),
                        ) {
                            scrollBy((value - previousProgress) * stepDistance)
                            previousProgress = value
                        }
                    }
                    visualFocusIndex = stepIndex
                }
                return@LaunchedEffect
            }

            val movementDistance = targetItem.offset - desiredTop
            if (abs(movementDistance) <= 0.5f) {
                clearCascadePresentation(nextIndex)
                handOffFocusScale(previousIndex, nextIndex)
                return@LaunchedEffect
            }

            val visibleLineIndexes = listState.layoutInfo.visibleItemsInfo
                .mapNotNull { (it.index - 1).takeIf(lines.indices::contains) }
            val firstVisible = visibleLineIndexes.minOrNull() ?: max(nextIndex - 1, 0)
            val lastVisible = visibleLineIndexes.maxOrNull() ?: nextIndex
            val firstMoving = min(firstVisible, max(nextIndex - 1, 0))
            // The cache window keeps these rows composed even though the
            // viewport clips them. Each retains its own delayed spring.
            val lastMoving = min(lastVisible + 4, lines.lastIndex)
            val movingIndexes = firstMoving..lastMoving
            val carriedOffsets = movingIndexes.associateWith(::currentMovementOffset)
            val destinations = movingIndexes.associateWith { settledMovementOffset(it, nextIndex) }

            cascadeDistancePx = movementDistance
            appliedScrollPx = 0f
            cascadeBaseOffsets = movingIndexes.associateWith { index ->
                // 可见行才有真实布局位置；缓存外的行不会参与绘制，给 0 即可
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == index + 1 }
                    ?.offset?.toFloat()
                    ?: 0f
            }
            cascadeInitialOffsets = carriedOffsets
            cascadeDestinationOffsets = destinations
            cascadeScrollProgress.snapTo(0f)
            movingIndexes.forEach { cascadeLineProgress[it].snapTo(0f) }
            visualFocusIndex = nextIndex

            val firstChasing = max(nextIndex - 1, 0)
            val maximumChaseOrder = max(lastMoving - firstChasing, 0)
            val lineTimings = sourceCascadeLineTimings(
                maximumLineOrder = maximumChaseOrder,
                animationDurationMs = cascadeDurationMs,
            )
            val slowestDuration = lineTimings.first().durationMs

            coroutineScope {
                launch { handOffFocusScale(previousIndex, nextIndex) }
                launch {
                    var previousScrollProgress = 0f
                    listState.scroll {
                        cascadeScrollProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = cascadeDurationMs.roundToInt(),
                                easing = SourceSmoothStepEasing,
                            ),
                        ) {
                            val delta = (value - previousScrollProgress) * movementDistance
                            scrollBy(delta)
                            // 记账：虚拟偏移的滚动补偿项必须与此严格一致
                            appliedScrollPx += delta
                            previousScrollProgress = value
                        }
                    }
                }

                movingIndexes.forEach { index ->
                    val movementOrder = max(index - nextIndex, 0)
                    val chaseOrder = (index - firstChasing).coerceAtLeast(0)
                    val movementTiming = lineTimings[min(movementOrder, lineTimings.lastIndex)]
                    val chaseTiming = lineTimings[min(chaseOrder, lineTimings.lastIndex)]
                    val duration = slowestDuration +
                        (chaseTiming.durationMs - slowestDuration) *
                        SettingsRuntime.lyricCascadeChaseSpeedGradient
                    val bounce = sourceCascadeBounce(chaseOrder, maximumChaseOrder)
                    // 收回弹：提高阻尼、提高刚度，回到 AMLL 过阻尼柔感
                    val lineStiffness = (260f - bounce * 180f).coerceIn(140f, 260f)
                    val lineDamping = (0.88f - bounce * 0.08f).coerceIn(0.82f, 0.92f)
                    launch {
                        if (movementTiming.delayMs > 0f) delay(movementTiming.delayMs.toLong())
                        cascadeLineProgress[index].animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = lineDamping, stiffness = lineStiffness),
                        )
                    }
                }

            }
            clearCascadePresentation(nextIndex)
        }

        when {
            isLoading && document == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = Color.White.copy(alpha = 0.9f)),
                )
            }
            errorMessage != null && document == null -> {
                Text(
                    text = errorMessage.orEmpty(),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = Color.White.copy(alpha = 0.52f),
                    style = MiuixTheme.textStyles.body1,
                )
            }
            document != null && lines.isEmpty() -> {
                Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.42f),
                    style = MiuixTheme.textStyles.title4,
                )
            }
            document == null -> {
                Text(
                    text = "暂无歌词",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.42f),
                    style = MiuixTheme.textStyles.title4,
                )
            }
            else -> {
                // The focused row scales from its start (or end, when flipped)
                // edge by lyricFocusScale, so a full-width line visually grows
                // past the 8dp gutter and gets clipped by clipToBounds().
                // Reserve a static gutter E on the growing side: the far text
                // edge sits at P - 8 - E before scaling and lands exactly on
                // the panel edge after, s * (P - 8 - E) <= P, which solves to
                // E >= (s - 1) * P / s - 8. Sized from the panel width at the
                // maximum scale so the wrap points never shift while the
                // scale animates.
                val focusScale = SettingsRuntime.lyricFocusScale
                val focusScaleReservePadding = with(density) {
                    val panelWidthDp = viewportWidthPx.toDp().value
                    (((focusScale - 1f) * panelWidthDp / focusScale) - 8f)
                        .coerceAtLeast(0f)
                        .dp
                }
                val focusAnchorY = viewportHeightPx * focusPosition
                val annotationHeightPx = annotationFontPx * 1.2f * 2f + annotationSpacingPx * 2f
                val lyricStridePx = max(primaryHeightPx + annotationHeightPx + lineSpacingPx, 1f)
                val layoutOverscanPx = (lyricStridePx * 4f).roundToInt()
                // Observing layoutInfo here during automatic playback scrolls
                // invalidated and recomposed every visible lyric item per
                // frame. Exact coordinates are only required while the user is
                // browsing; playback mode has a stable source-derived stride.
                val browsingVisibleItemsByIndex by remember(listState) {
                    derivedStateOf { listState.layoutInfo.visibleItemsInfo.associateBy { it.index } }
                }
                val visibleItemsByIndex = browsingVisibleItemsByIndex

                Layout(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .graphicsLayer { alpha = if (initialLyricsPositioned) 1f else 0f },
                    content = {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().nestedScroll(lyricInteractionConnection),
                        ) {
                    item(key = "lyrics-top-padding") {
                        Spacer(Modifier.height(with(density) { topPaddingPx.toDp() }))
                    }
                    itemsIndexed(
                        items = lines,
                        key = { index, line -> "${line.timeMs}:$index" },
                    ) { index, line ->
                        val interlude = interludeByLyricIndex[index]
                        // Reserve the interlude's space as soon as the document
                        // is laid out.  Inserting it only after playback reaches
                        // the gap makes the countdown pop in and fight the lyric
                        // scroll animation.
                        val showsInterlude = SettingsRuntime.lyricInterludeCountdownEnabled &&
                            interlude != null
                        val interludeHeightPx = if (showsInterlude) with(density) { 56.dp.toPx() } else 0f
                        val height = estimatedHeight(index) + interludeHeightPx
                        val visualOffset = currentMovementOffset(index) + roundingCorrection(index)
                        val frameMinY = visibleItemsByIndex[index + 1]?.offset?.toFloat()
                            ?: focusAnchorY + (index - visualFocusIndex) * lyricStridePx
                        val visualMidY = frameMinY + visualOffset + height * 0.5f
                        val distance = if (isBrowsingLyrics || visualFocusIndex !in lines.indices) {
                            abs(visualMidY - focusAnchorY)
                        } else {
                            abs(index - visualFocusIndex) * lyricStridePx
                        }
                        val isActiveLine = index in activeTimedLineIndexes
                        val fp = focusProgress[index].value.coerceIn(0f, 1f)
                        // Some LRC/YRC files deliberately provide two distinct
                        // lines at virtually the same time.  Keep both readable
                        // instead of promoting only the last binary-search hit.
                        val effectiveFocus = fp
                        val distanceBlur = sourceDistanceBlurRadius(
                            distancePx = distance,
                            lyricStridePx = lyricStridePx,
                            intensity = UpstreamLyrics.BLUR_INTENSITY *
                                (if (isInterfaceHidden) SettingsRuntime.lyricHiddenInterfaceBlurScale else SettingsRuntime.lyricDistanceBlurScale) *
                                SettingsRuntime.lyricBlurStrength,
                            focusProgress = effectiveFocus,
                        )
                        val preceding = index == visualFocusIndex - 1
                        val following = index == visualFocusIndex + 1
                        val focusBlur = sourceFocusBlurRadius(
                            UpstreamLyrics.BLUR_INTENSITY * SettingsRuntime.lyricBlurStrength,
                            preceding,
                            following,
                        ) * (1f - effectiveFocus)
                        val distanceOpacity = sourceDistanceOpacity(
                            distance,
                            lyricStridePx,
                            SettingsRuntime.lyricDimAmount,
                            effectiveFocus,
                        )
                        val emphasis = sourceEmphasis(effectiveFocus, SettingsRuntime.lyricDimAmount)
                        val rowAlpha = (distanceOpacity * emphasis).coerceIn(0f, 1f)
                        val followingLineAlpha = sourceDistanceOpacity(
                            lyricStridePx,
                            lyricStridePx,
                            SettingsRuntime.lyricDimAmount,
                            0f,
                        ) * sourceEmphasis(0f, SettingsRuntime.lyricDimAmount)
                        val timedUnplayedAlpha = (followingLineAlpha / rowAlpha.coerceAtLeast(.001f))
                            .coerceIn(0f, 1f)
                        val scale = 1f + (SettingsRuntime.lyricFocusScale - 1f) *
                            max(scaleProgress[index].value, fp)

                        if (showsInterlude) {
                            LyricInterludeCountdown(
                                interlude = checkNotNull(interlude),
                                playbackTimeProvider = { renderedPositionState.longValue },
                                reduceMotion = SettingsRuntime.lyricReduceMotion,
                                visualScale = scale,
                                visualOffsetPx = visualOffset,
                                rowAlpha = rowAlpha,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        UpstreamLyricLine(
                            line = line,
                            playbackTimeProvider = playbackTimeProvider,
                            // Only the focused line (plus its short colour hand-off)
                            // needs the playback clock. Subscribing every visible
                            // syllable row made the complete LazyColumn redraw at
                            // the lyric refresh rate on low-end devices.
                            supportsTimedLyrics = line.syllables.isNotEmpty() &&
                                SettingsRuntime.lyricWordByWordEnabled &&
                                (effectiveFocus > 0.001f || isActiveLine),
                            fontScale = lyricFontScale,
                            reduceMotion = SettingsRuntime.lyricReduceMotion,
                            focusProgress = effectiveFocus,
                            timingEffectsStrength = if (isActiveLine) 1f else effectiveFocus,
                            timedUnplayedAlpha = timedUnplayedAlpha,
                            visualScale = scale,
                            focusScaleReservePadding = focusScaleReservePadding,
                            visualOffsetPx = visualOffset,
                            rowAlpha = rowAlpha,
                            distanceBlurDp = distanceBlur,
                            focusBlurDp = focusBlur,
                            renderingQuality = renderingQuality,
                            showTranslation = SettingsRuntime.showLyricTranslation &&
                                !line.translation.isNullOrBlank(),
                            showRomanization = SettingsRuntime.showLyricRomanization &&
                                !line.romanization.isNullOrBlank() &&
                                (SettingsRuntime.lyricRomanizationDisplayMode == LyricAnnotationDisplayMode.AllLines || isActiveLine),
                            reserveTranslation = SettingsRuntime.showLyricTranslation && !line.translation.isNullOrBlank(),
                            reserveRomanization = SettingsRuntime.showLyricRomanization && !line.romanization.isNullOrBlank(),
                            onMeasured = { measured ->
                                if (measured > 0 && rowHeightsPx[index] != measured) rowHeightsPx[index] = measured
                            },
                            tapSeekEnabled = SettingsRuntime.lyricTapSeekEnabled,
                            onClick = { onInterfaceInteraction(); state.seekTo(line.timeMs) },
                        )

                        if (index != lines.lastIndex) {
                            Spacer(Modifier.height((UpstreamLyrics.LINE_SPACING_DP * lyricSpacingScale).dp))
                        }
                    }
                    item(key = "lyrics-bottom-padding") {
                        Spacer(Modifier.height(with(density) { (bottomPaddingPx + layoutOverscanPx).toDp() }))
                    }
                        }
                    },
                ) { measurables, constraints ->
                    val expandedHeight = (constraints.maxHeight.toLong() + layoutOverscanPx)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt()
                    val list = measurables.single().measure(
                        constraints.copy(minHeight = expandedHeight, maxHeight = expandedHeight),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        list.placeRelative(0, 0)
                    }
                }
            }
        }

        //  iOS reserves only the former bottom-controls region as the
        // hidden-controls tap target. Covering the complete viewport here made
        // every lyric stop receiving scroll input as soon as transport chrome
        // retracted, so the lines the user was browsing appeared to vanish or
        // become unreachable.
        if (isInterfaceHidden) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(NowPlayingControlsHeight.dp)
                    .zIndex(100f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onInterfaceInteraction,
                    ),
            )
        }
    }
}

/*
 * Vendored third-party source - muses
 *
 * Origin : Accompanist lyrics-ui 1.0.19 (AMLL official Compose implementation)
 * Repo   : https://github.com/6xingyv/accompanist-lyrics-ui
 * License: Apache License 2.0  (https://www.apache.org/licenses/LICENSE-2.0)
 * Author : The Accompanist / MochaRealm Authors
 *
 * The package name is intentionally kept as `com.mocharealm.accompanist.*` so the
 * code can be diffed against upstream. Local modifications:
 *  - expect/actual (Kotlin Multiplatform) collapsed into plain Kotlin for this
 *    single-target Android module: Char.isCjk / isArabic / isDevanagari now live
 *    directly in utils/String.kt
 *  - com.mocharealm.gaze.capsule.ContinuousRoundedRectangle replaced with
 *    androidx.compose.foundation.shape.RoundedCornerShape, dropping the
 *    gaze-capsule dependency (only used to clip a line item)
 */

package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

internal data class FocusState(
    val firstIndex: Int,
    val allIndices: List<Int>,
    val activeInterludeIndex: Int?,
    val activeIntro: Boolean
)

/**
 * A comprehensive lyrics view that supports Karaoke and Synced lyrics with advanced rendering.
 *
 * This composable handles:
 * - Scrolling and auto-scrolling to the current line
 * - Rendering karaoke lines with syllable-level timing and animations
 * - Rendering synced lines
 * - Displaying breathing dots during instrumental interludes
 * - Determining active and accompaniment lines
 *
 * @param listState The scroll state for the lazy list.
 * @param lyrics The lyrics data to display.
 * @param currentPosition A lambda returning the current playback position in milliseconds.
 * @param onLineClicked Callback when a line is clicked (seek to position).
 * @param onLinePressed Callback when a line is long-pressed (share/menu).
 * @param modifier The modifier to apply to the layout.
 * @param normalLineTextStyle The style for normal text lines.
 * @param accompanimentLineTextStyle The style for accompaniment/background vocals lines.
 * @param textColor The primary text color.
 * @param breathingDotsDefaults Styling defaults for the breathing dots.
 * @param blendMode The blend mode used for rendering text (e.g., [BlendMode.Plus] for glowing effects).
 * @param useBlurEffect Whether to apply blur effect to non-active lines.
 * @param offset The vertical padding/offset at the start and end of the list.
 * @param showDebugRectangles Debug flag to draw bounding boxes around glyphs.
 */
// 歌词行纵向滚动的弹簧物理参数（对齐 Web 版 AMLL getPosYSpringPolicy）：
// m=1、stiffness=220、damping=2.2*sqrt(stiffness) → 阻尼比 ≈ 1.1（轻微过阻尼），
// 过阻尼阶跃响应 = 起始最快、越接近目标越慢的减速曲线
private const val SPRING_STIFFNESS = 220f
private const val SPRING_DAMPING = 32.63f // 2.2 * sqrt(220f)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    textColor: Color = Color.White,
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    phoneticTextStyle: TextStyle = normalLineTextStyle.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
    ),
//    TODO: expose it
//    verticalFadeBrush: Brush = Brush.verticalGradient(
//        0f to Color.White.copy(0f),
//        0.05f to Color.White,
//        0.6f to Color.White,
//        1f to Color.White.copy(0f)
//    ),
    blendMode: BlendMode = BlendMode.Plus,
    useBlurEffect: Boolean = true,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    offset: Dp = 32.dp,
    keepAliveZone: Dp = 100.dp,
    blurDelta: Float = 3f,
    showDebugRectangles: Boolean = false
) {
    val density = LocalDensity.current
    val stableNormalTextStyle = remember(normalLineTextStyle) { normalLineTextStyle }
    val stableAccompanimentTextStyle =
        remember(accompanimentLineTextStyle) { accompanimentLineTextStyle }
    val stablePhoneticTextStyle = remember(phoneticTextStyle) { phoneticTextStyle }
    val stableOffset = remember(offset) { offset }
    val stableOffsetPx =
        remember(stableOffset) { with(density) { stableOffset.toPx().fastRoundToInt() } }
    val keepAliveZonePx = with(density) { keepAliveZone.toPx() }
    val stableBlendMode = remember(blendMode) { blendMode }

    val textMeasurer = rememberTextMeasurer()
    val layoutCache = remember { mutableStateMapOf<Int, List<SyllableLayout>>() }

    LaunchedEffect(
        lyrics,
        stableNormalTextStyle,
        stableAccompanimentTextStyle,
        stablePhoneticTextStyle
    ) {
        layoutCache.clear()
        withContext(Dispatchers.Default) {
            val normalStyle = stableNormalTextStyle.copy(textDirection = TextDirection.Content)
            val accompanimentStyle =
                stableAccompanimentTextStyle.copy(textDirection = TextDirection.Content)
            val phoneticStyle = stablePhoneticTextStyle.copy(textDirection = TextDirection.Content)

            val normalSpaceWidth = textMeasurer.measure(" ", normalStyle).size.width.toFloat()
            val accompanimentSpaceWidth =
                textMeasurer.measure(" ", accompanimentStyle).size.width.toFloat()

            lyrics.lines.forEachIndexed { index, line ->
                if (!isActive) return@forEachIndexed
                if (line is KaraokeLine) {
                    val style =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentStyle else normalStyle
                    val spaceWidth =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentSpaceWidth else normalSpaceWidth

                    val processedSyllables = if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }

                    val layout = measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = style,
                        phoneticStyle = phoneticStyle,
                        isAccompanimentLine = line is KaraokeLine.AccompanimentKaraokeLine,
                        spaceWidth = spaceWidth
                    )

                    withContext(Dispatchers.Main) {
                        layoutCache[index] = layout
                    }
                }
            }
        }
    }

    val currentTimeMs: () -> Int = currentPosition

    val timeProvider = remember { currentPosition }

    val accompanimentToMainMap = remember(lyrics.lines) {
        val map = mutableMapOf<Int, Int>()
        val mainLinesIndices = lyrics.lines.indices.filter { index ->
            val line = lyrics.lines[index]
            line !is KaraokeLine || line !is KaraokeLine.AccompanimentKaraokeLine
        }
        if (mainLinesIndices.isNotEmpty()) {
            lyrics.lines.forEachIndexed { index, line ->
                if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                    // Find the main line that is closest in time (either the one just before or just after)
                    val beforeIdx = mainLinesIndices.findLast { it <= index }
                    val afterIdx = mainLinesIndices.find { it >= index }

                    val anchorIndex = when {
                        beforeIdx != null && afterIdx != null -> {
                            val distBefore =
                                (line.start - lyrics.lines[beforeIdx].start).absoluteValue
                            val distAfter =
                                (lyrics.lines[afterIdx].start - line.start).absoluteValue
                            if (distBefore <= distAfter) beforeIdx else afterIdx
                        }

                        beforeIdx != null -> beforeIdx
                        afterIdx != null -> afterIdx
                        else -> mainLinesIndices.first()
                    }
                    map[index] = anchorIndex
                }
            }
        }
        map
    }
    val effectiveEndTimes = remember(lyrics.lines) {
        IntArray(lyrics.lines.size) { index ->
            val line = lyrics.lines[index]
            var maxEnd = line.end

            if (line is KaraokeLine.MainKaraokeLine) {
                line.accompanimentLines?.forEach { acc ->
                    if (acc.end > maxEnd) maxEnd = acc.end
                }
            }
            maxEnd
        }
    }

    val firstLine = lyrics.lines.firstOrNull()

    val haveDotsIntro by remember(firstLine) {
        derivedStateOf {
            if (firstLine == null) false
            else (firstLine.start > 5000)
        }
    }

    val lyricsFocusState by remember(lyrics, effectiveEndTimes, accompanimentToMainMap, haveDotsIntro) {
        derivedStateOf {
            val time = currentTimeMs()
            val activeIndex = lyrics.lines.indices.find { idx ->
                time >= lyrics.lines[idx].start && time < effectiveEndTimes[idx]
            }

            val first = if (activeIndex != null) {
                activeIndex
            } else {
                val nextIdx = lyrics.lines.indexOfFirst { it.start > time }
                if (nextIdx != -1) nextIdx else lyrics.lines.lastIndex
            }

            val base = lyrics.lines.indices.filter { index ->
                time >= lyrics.lines[index].start && time < effectiveEndTimes[index]
            }
            val result = base.toMutableSet()
            base.forEach { index ->
                val line = lyrics.lines.getOrNull(index)
                if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                    accompanimentToMainMap[index]?.let { result.add(it) }
                }
            }

            val activeInterludeIndex = lyrics.lines.indices.find { index ->
                val line = lyrics.lines[index]
                val previousLine = lyrics.lines.getOrNull(index - 1)
                previousLine != null && (line.start - previousLine.end > 5000) && time in previousLine.end..line.start
            }
            val activeIntro = haveDotsIntro && time in 0 until (firstLine?.start ?: 0)

            FocusState(first, result.toList().sorted(), activeInterludeIndex, activeIntro)
        }
    }

    val scrollInCode = remember { mutableStateOf(false) }

    val isManualScrolling by remember {
        derivedStateOf {
            listState.isScrollInProgress && !scrollInCode.value
        }
    }

    // 手动滚动期间暂停自动跟随（对齐 Web 版 ScrollInteractionEngine 的交互语义）
    val followPaused = remember { mutableStateOf(false) }
    // 每次手动滚动开始递增，驱动"松手 5 秒后恢复自动对齐"的倒计时重启
    val resumeTick = remember { mutableStateOf(0) }

    // 对齐逻辑：把当前行滚动到居中锚点。
    // - 当前行不可见：animateScrollToItem(index, 0) 整体滚动动画（LazyList 内置弹簧，
    //   与 Web 版 posY spring 语义一致，且是整体滚动而非逐行弹簧，不会"掉下来"）；
    // - 进入视口后：弹簧驱动 scrollBy 精确到位。
    suspend fun alignToCurrentLine(firstIndex: Int) {
        try {
            scrollInCode.value = true
            var targetItem = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == firstIndex }
            if (targetItem == null) {
                listState.animateScrollToItem(firstIndex, 0)
                targetItem = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == firstIndex }
            }
            val targetOffset = targetItem
                ?.offset
                ?.minus(listState.layoutInfo.viewportStartOffset + stableOffsetPx + keepAliveZonePx)
            if (targetOffset != null && kotlin.math.abs(targetOffset) > 0.5f) {
                // 弹簧物理驱动滚动：半隐式欧拉数值积分，参数完全对齐 Web 版
                // posY Spring（m=1、k=220、c=2.2*sqrt(k)）——过阻尼阶跃响应：
                // 起始速度最快、越接近目标越慢，即"越往上滚越慢"的减速曲线。
                var remaining = targetOffset.toFloat()
                var velocity = 0f
                var prevFrameMs = -1L
                var guard = 0
                while (kotlin.math.abs(remaining) > 0.5f && guard++ < 240) {
                    val frameMs = withFrameMillis { it }
                    if (prevFrameMs < 0L) prevFrameMs = frameMs
                    val dtSec =
                        ((frameMs - prevFrameMs) / 1000f).coerceIn(0.001f, 0.032f)
                    prevFrameMs = frameMs
                    // remaining 视为位移误差（目标 0），弹簧把它拉向 0：
                    // v' = -k*remaining - c*v；v 是 remaining 的变化率（衰减为负）
                    val a = -SPRING_STIFFNESS * remaining - SPRING_DAMPING * velocity
                    velocity += a * dtSec
                    val dx = velocity * dtSec
                    // 列表滚动方向与 remaining 衰减相反：remaining>0（行在目标下方）
                    // 需要列表向下滚（scrollBy 正）让行上移
                    listState.scrollBy(-dx)
                    remaining += dx
                }
                if (kotlin.math.abs(remaining) > 0.5f) {
                    listState.scrollBy(remaining)
                }
            }
        } catch (_: Exception) {
        } finally {
            scrollInCode.value = false
        }
    }

    // 换行时跟随当前行（手动滚动或跟随暂停期间不干预）
    LaunchedEffect(
        layoutCache,
        stableOffsetPx,
    ) {
        androidx.compose.runtime.snapshotFlow { lyricsFocusState.firstIndex }
            .collect { firstIndex ->
                if (!scrollInCode.value && !isManualScrolling && !followPaused.value) {
                    alignToCurrentLine(firstIndex)
                }
            }
    }

    // 手动滚动：开始时暂停跟随；物理静止（含惯性结束）后启动
    // 5 秒倒计时（对齐 Web 版 AUTO_ALIGN_RESUME_DELAY_MS）恢复自动对齐。
    LaunchedEffect(layoutCache, stableOffsetPx) {
        var prevScrolling = false
        androidx.compose.runtime.snapshotFlow { isManualScrolling }
            .collect { scrolling ->
                if (scrolling) {
                    followPaused.value = true
                } else if (prevScrolling) {
                    resumeTick.value++
                }
                prevScrolling = scrolling
            }
    }
    LaunchedEffect(layoutCache, stableOffsetPx, resumeTick.value) {
        if (followPaused.value) {
            delay(5000)
            if (followPaused.value && !isManualScrolling) {
                followPaused.value = false
                alignToCurrentLine(lyricsFocusState.firstIndex)
            }
        }
    }

    // 切歌/换歌词时恢复自动跟随
    LaunchedEffect(lyrics) { followPaused.value = false }
    Crossfade(lyrics) { lyrics ->
            Box(modifier = modifier.clipToBounds()) {
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                val topFade = 20.dp.toPx() / size.height
                                val bottomFade = 100.dp.toPx() / size.height
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        topFade to Color.Black,
                                        1f - bottomFade to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .layout { measurable, constraints ->
                            val extraHeightPx = (keepAliveZone * 2).roundToPx()

                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxHeight = constraints.maxHeight + extraHeightPx
                                )
                            )

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -(keepAliveZone.roundToPx()))
                            }
                        },
                    contentPadding = PaddingValues(vertical = stableOffset + keepAliveZone)
                ) {
                    itemsIndexed(
                        items = lyrics.lines,
                        key = { index, line -> "${line.start}-${line.end}-$index" }
                    ) { index, line ->
                        val isCurrentFocusLine = index in lyricsFocusState.allIndices
                        val isLineRtl =
                            when (line) {
                                is KaraokeLine -> {
                                    remember(line.syllables) { line.syllables.any { it.content.isRtl() } }
                                }

                                else -> false
                            }
                        val isLineRightAligned = when (line) {
                            is KaraokeLine -> {
                                remember { line.alignment == KaraokeAlignment.End }
                            }

                            else -> false
                        }
                        val isVisualRightAligned = remember(isLineRightAligned, isLineRtl) {
                            if (isLineRightAligned) !isLineRtl
                            else isLineRtl
                        }

                        val distanceWeightState = remember(useBlurEffect, lyricsFocusState) {
                            derivedStateOf {
                                val start = lyricsFocusState.allIndices.firstOrNull() ?: lyricsFocusState.firstIndex
                                val end = lyricsFocusState.allIndices.lastOrNull() ?: lyricsFocusState.firstIndex
                                maxOf(0, start - index, index - end)
                            }
                        }

                        // 波浪级联（对齐 Web 版 PlaybackTick 的 stagger 阶梯动画）：
                        // 换行时每行按「距当前行的行数」错峰延迟（40ms/行），
                        // 弹簧快速归位，形成从上到下依次跟进的波浪移动。
                        val waveOffset = remember { Animatable(0f) }
                        LaunchedEffect(lyricsFocusState.firstIndex) {
                            val dist = kotlin.math.abs(index - lyricsFocusState.firstIndex)
                            if (dist == 0) return@LaunchedEffect
                            delay((dist * 40).coerceAtMost(200).toLong())
                            waveOffset.snapTo(18f)
                            waveOffset.animateTo(
                                0f,
                                spring(dampingRatio = 1.1f, stiffness = 220f)
                            )
                        }

                        // 行容器不做位置弹簧：滚动已由弹簧物理整体驱动（对齐 Web 版——
                        // 若行再叠一层弹簧会滞后于滚动，产生拖尾/掉落错位观感）
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = waveOffset.value },
                            horizontalAlignment = if (isVisualRightAligned) Alignment.End else Alignment.Start
                        ) {
                            val animDuration = 600

                            val previousLine = lyrics.lines.getOrNull(index - 1)

                            val showDotsInterlude = lyricsFocusState.activeInterludeIndex == index
                            val showDotsIntro = lyricsFocusState.activeIntro && index == 0

                            AnimatedVisibility(showDotsInterlude || showDotsIntro) {
                                KaraokeBreathingDots(
                                    alignment = when (val line = previousLine ?: firstLine) {
                                        is KaraokeLine -> line.alignment
                                        is SyncedLine -> if (line.content.isRtl()) KaraokeAlignment.End else KaraokeAlignment.Start
                                        else -> KaraokeAlignment.Start
                                    },
                                    startTimeMs = previousLine?.end ?: 0,
                                    endTimeMs = if (showDotsIntro) firstLine!!.start else line.start,
                                    currentTimeProvider = timeProvider,
                                    defaults = breathingDotsDefaults,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }


                            val blurRadiusState = animateFloatAsState(
                                targetValue = (
                                        if (!useBlurEffect) 0f
                                        else if (distanceWeightState.value > 0 && (!listState.isScrollInProgress || scrollInCode.value)) {
                                            distanceWeightState.value * blurDelta
                                        } else 0f),
                                animationSpec = tween(300),
                            )

                            when (line) {
                                is KaraokeLine -> {
                                    if (line is KaraokeLine.MainKaraokeLine) {
                                        LyricsLineItem(
                                            isFocused = isCurrentFocusLine,
                                            isRightAligned = isVisualRightAligned,
                                            onLineClicked = {
                                                // 点击行（seek）立即恢复自动跟随（对齐 Web 版 resetScroll）
                                                followPaused.value = false
                                                onLineClicked(line)
                                            },
                                            onLinePressed = {
                                                followPaused.value = false
                                                onLinePressed(line)
                                            },
                                            blurRadius = { blurRadiusState.value },
                                            blendMode = stableBlendMode,
                                        ) {
                                            KaraokeLineText(
                                                line = line,
                                                currentTimeProvider = timeProvider,
                                                normalLineTextStyle = stableNormalTextStyle,
                                                accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                                phoneticTextStyle = stablePhoneticTextStyle,
                                                activeColor = textColor,
                                                blendMode = stableBlendMode,
                                                showDebugRectangles = showDebugRectangles,
                                                showTranslation = showTranslation,
                                                showPhonetic = showPhonetic,
                                                precalculatedLayouts = layoutCache[index]
                                            )
                                        }
                                    }
                                }

                                is SyncedLine -> {
                                    val isLineRtl = remember(line.content) { line.content.isRtl() }
                                    LyricsLineItem(
                                        isFocused = isCurrentFocusLine,
                                        isRightAligned = isLineRtl,
                                        onLineClicked = {
                                            followPaused.value = false
                                            onLineClicked(line)
                                        },
                                        onLinePressed = {
                                            followPaused.value = false
                                            onLinePressed(line)
                                        },
                                        blurRadius = { blurRadiusState.value },
                                        blendMode = stableBlendMode,
                                    ) {
                                        SyncedLineText(
                                            line = line,
                                            isLineRtl = isLineRtl,
                                            textStyle = stableNormalTextStyle.copy(lineHeight = 1.2.em),
                                            textColor = textColor,
                                            showTranslation = showTranslation,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item("BottomSpacing") {
                        Spacer(
                            modifier = Modifier.fillMaxWidth().height(2000.dp)
                        )
                    }
                }
            }
        }
}
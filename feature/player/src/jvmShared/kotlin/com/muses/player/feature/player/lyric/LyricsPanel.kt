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

/*
 * 歌词字形、翻译和逐字高亮的渲染常量。
 * 滚动、行缩放和距离模糊已迁移到 AmllScrollPhysics，以 AMLL 源码为准；
 * 此处保留逐字渲染使用的参数，不再用旧级联曲线驱动滚动。
 */
internal object UpstreamLyrics {
    const val FONT_SIZE_SP = 24f
    const val LINE_HEIGHT_SP = 28.8f // fontSize * 1.2
    const val LINE_SPACING_DP = 28f
    const val CURRENT_LINE_SCALE = 1.05f
    const val FOCUS_POSITION = 0.5f
    const val BLUR_INTENSITY = 0.8f
    const val DIM_AMOUNT = 1f
    const val DISTANCE_BLUR_SCALE = 1.05f
    const val ROMANIZATION_FONT_SCALE = 0.65f
    const val TRANSLATION_FONT_SCALE = 0.65f
    const val ANNOTATION_OPACITY = 0.9f
    const val ANNOTATION_SPACING_DP = 4f
    const val FOCUS_COLOR_DURATION_MS = 180

    const val CASCADE_DELAY_MS = 17f
    const val CASCADE_DELAY_INCREASE_MS = 2.2f
    const val CASCADE_FOLLOWING_DELAY_MS = 27f
    const val CASCADE_CATCH_UP_RATIO = 0.84f
    const val CASCADE_CHASE_SPEED_GRADIENT = 0.60f
    const val CASCADE_DURATION_MS = 740f
    const val CASCADE_SNAP_THRESHOLD_MS = 150f
    const val CASCADE_BOUNCE = 0.16f
    const val CASCADE_BOUNCE_GRADIENT = 0.40f
    const val SCALE_BOUNCE = 0.30f
    const val SCALE_BOUNCE_DURATION_MS = 580

    const val GLOW_INTENSITY = 1f
    const val LONG_TONE_THRESHOLD_MS = 950f
    const val LONG_TONE_MAX_SCALE = 1.0625f
    const val GLOW_TAIL_MS = 550f
    const val HIGHLIGHT_GRADIENT_WIDTH = 0.7f
    const val HIGHLIGHT_GRADIENT_REDUCTION = 0.65f
    const val UNPLAYED_OPACITY = 0.3f
    const val UNPLAYED_BLUR_LEAD_MS = 2400f
    const val MIN_UNPLAYED_BLUR_FRACTION = 0.12f
    const val LIFT_CONTINUATION_MS = 320f
}

internal data class LyricsPanelPlaybackInitialization(
    val positionMs: Long,
    val holdAtTrackStart: Boolean,
    val resetListToStart: Boolean,
)

internal fun lyricsPanelPlaybackInitialization(
    isFirstComposition: Boolean,
    mediaIdChanged: Boolean,
    reportedPositionMs: Long,
): LyricsPanelPlaybackInitialization = if (!isFirstComposition && mediaIdChanged) {
    LyricsPanelPlaybackInitialization(
        positionMs = 0L,
        holdAtTrackStart = true,
        resetListToStart = true,
    )
} else {
    LyricsPanelPlaybackInitialization(
        positionMs = reportedPositionMs,
        holdAtTrackStart = false,
        resetListToStart = false,
    )
}

internal fun initialLyricsHighlightPositionMs(positionMs: Long, advanceMs: Long): Long =
    positionMs + advanceMs

@Composable
fun LyricsPanel(
    state: PlaybackUiState,
    modifier: Modifier = Modifier,
    isInterfaceHidden: Boolean = false,
    onInterfaceInteraction: () -> Unit = {},
    onInterfaceVisibilityChange: (Boolean) -> Unit = {},
    active: Boolean = true,
    externalDocument: com.muses.player.core.lyrics.model.LyricsDocument? = null,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        AppleMusicLyricsPanel(
            state,
            Modifier,
            isInterfaceHidden,
            onInterfaceInteraction,
            onInterfaceVisibilityChange,
            active,
            externalDocument = externalDocument,
        )
    }
}

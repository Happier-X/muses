package com.muses.player.core.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.delay
import kotlin.math.ceil

data class SongMarqueeState(val songId: String? = null, val isPlaying: Boolean = false) {
    fun matches(id: String?) = isPlaying && id != null && songId == id
}

val LocalSongMarqueeState = staticCompositionLocalOf { SongMarqueeState() }
val LocalTextMarqueeEnabled = staticCompositionLocalOf { true }

data class TimedTextMarquee(val durationMillis: Long, val key: String)

/** 在给定显示时间内滚到末尾并停住，不重复绕回开头。 */
@Composable
private fun Modifier.timedMarquee(durationMillis: Long, running: Boolean, onReadDurationChanged: ((Long) -> Unit)?): Modifier {
    val offset = remember { Animatable(0f) }
    var overflowPixels by remember { mutableStateOf(0) }
    val density = LocalDensity.current.density
    val reportDuration by rememberUpdatedState(onReadDurationChanged)
    LaunchedEffect(overflowPixels, density) {
        // 轮播优先以舒适速度阅读，短标题仍沿用五秒停留。
        reportDuration?.invoke((ceil(overflowPixels / density / 30f * 1_000).toLong() + 1_300).coerceAtLeast(5_000))
    }
    LaunchedEffect(overflowPixels, durationMillis, running) {
        if (!running) return@LaunchedEffect
        offset.snapTo(0f)
        if (overflowPixels == 0) return@LaunchedEffect
        val duration = durationMillis.coerceAtLeast(1)
        val lead = minOf(500L, duration / 8)
        val tail = minOf(800L, duration / 8)
        delay(lead)
        offset.animateTo(overflowPixels.toFloat(), tween(
            durationMillis = (duration - lead - tail).coerceIn(1, Int.MAX_VALUE.toLong()).toInt(),
            easing = LinearEasing,
        ))
    }
    return clipToBounds().layout { measurable, constraints ->
        val text = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val width = constraints.constrainWidth(text.width)
        val overflow = (text.width - width).coerceAtLeast(0)
        if (overflowPixels != overflow) overflowPixels = overflow
        layout(width, constraints.constrainHeight(text.height)) {
            text.placeRelativeWithLayer(0, 0) { translationX = -offset.value }
        }
    }
}

/** 只有超出宽度的文字才滚动；列表行可通过局部状态关闭滚动。 */
fun Modifier.autoMarquee(enabled: Boolean = true): Modifier =
    if (enabled) basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1_200, repeatDelayMillis = 1_200)
    else this

/** 保留 miuix 文字样式，将原省略文字改为单行横向滚动。 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = MiuixTheme.textStyles.main,
    marqueeEnabled: Boolean = LocalTextMarqueeEnabled.current,
    displayDurationMillis: Long? = null,
    marqueeRunning: Boolean = true,
    marqueeKey: Any? = text,
    onReadDurationChanged: ((Long) -> Unit)? = null,
) {
    val scrolling = marqueeEnabled && overflow == TextOverflow.Ellipsis
    key(text, marqueeKey) {
    val scrollingModifier = if (scrolling && displayDurationMillis != null) {
        modifier.timedMarquee(displayDurationMillis, marqueeRunning, onReadDurationChanged)
    } else modifier.autoMarquee(scrolling)
    Text(
        text = text, modifier = scrollingModifier, color = color,
        fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight, fontFamily = fontFamily,
        letterSpacing = letterSpacing, textDecoration = textDecoration, textAlign = textAlign,
        lineHeight = lineHeight, overflow = if (scrolling) TextOverflow.Clip else overflow,
        softWrap = if (scrolling) false else softWrap,
        maxLines = if (scrolling) 1 else maxLines, minLines = if (scrolling) 1 else minLines,
        onTextLayout = onTextLayout, style = style,
    )
    }
}

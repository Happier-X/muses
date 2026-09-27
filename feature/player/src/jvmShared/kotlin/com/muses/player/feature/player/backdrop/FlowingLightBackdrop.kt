package com.muses.player.feature.player.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FrameIntervalNanos = 50_000_000L
private const val FadeDurationNanos = 500_000_000L
private data class CoverArtwork(val pixels: CoverPixels, val contentColor: Color)

@Composable
fun FlowingLightBackdrop(
    coverUri: String?,
    hasLyric: Boolean,
    modifier: Modifier = Modifier,
    flowSpeed: Float = 2f,
    onContentColorChange: (Color) -> Unit = {},
) {
    @Suppress("UNUSED_PARAMETER") val lyricAvailable = hasLyric
    var cover by remember { mutableStateOf<CoverPixels?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var displayedCover by remember { mutableStateOf<CoverPixels?>(null) }
    var outgoing by remember { mutableStateOf<ImageBitmap?>(null) }
    var fade by remember { mutableStateOf(1f) }
    val onColorChange = rememberUpdatedState(onContentColorChange)
    val cache = remember { LinkedHashMap<String, CoverArtwork?>(24, 0.75f, true) }

    LaunchedEffect(coverUri) {
        if (coverUri.isNullOrBlank()) {
            cover = null
            frame = null
            displayedCover = null
            outgoing = null
            onColorChange.value(Color.White)
        } else {
            val artwork = if (cache.containsKey(coverUri)) cache[coverUri] else {
                val decoded = withContext(Dispatchers.IO) { decodeCoverPixels(coverUri) }
                withContext(Dispatchers.Default) {
                    decoded?.let { CoverArtwork(preprocessCoverPixels(it), coverContentColor(it)) }
                }.also {
                    cache[coverUri] = it
                    if (cache.size > 20) cache.remove(cache.keys.first())
                }
            }
            if (artwork != null) {
                cover = artwork.pixels
                onColorChange.value(artwork.contentColor)
            }
        }
    }

    LaunchedEffect(cover, viewport, flowSpeed) {
        val source = cover ?: return@LaunchedEffect
        if (viewport.width <= 0 || viewport.height <= 0) return@LaunchedEffect
        var lastFrame = 0L
        var fadeStart = 0L
        var firstFrame = true
        do {
            val now = withFrameNanos { it }
            if (lastFrame != 0L && now - lastFrame < FrameIntervalNanos) continue
            lastFrame = now
            val rendered = withContext(Dispatchers.Default) {
                renderFlowFrame(source, viewport.width, viewport.height, now / 1_000_000_000.0 * flowSpeed.coerceAtLeast(0f))
            }
            if (firstFrame) {
                outgoing = if (displayedCover !== source) frame else null
                displayedCover = source
                fadeStart = now
                fade = if (outgoing == null) 1f else 0f
                firstFrame = false
            }
            frame = coverPixelsToImageBitmap(rendered.pixels, rendered.width, rendered.height)
            if (outgoing != null) {
                fade = ((now - fadeStart).toFloat() / FadeDurationNanos).coerceIn(0f, 1f)
                if (fade >= 1f) outgoing = null
            }
        } while (flowSpeed > 0f)
    }

    Box(modifier = modifier.clipToBounds().onSizeChanged { viewport = it }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF05070D))
            val previous = outgoing
            val current = frame
            if (current == null && previous == null) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF171B2B), Color(0xFF0A0C14), Color(0xFF05070D))))
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x479478FF), Color.Transparent),
                        Offset(size.width * 0.5f, size.height * 0.18f),
                        size.width * 0.9f,
                    )
                )
            }
            val targetSize = IntSize(size.width.toInt(), size.height.toInt())
            if (previous != null) drawImage(
                previous, dstOffset = IntOffset.Zero, dstSize = targetSize,
                alpha = 1f - fade, filterQuality = FilterQuality.Medium,
            )
            if (current != null) drawImage(
                current, dstOffset = IntOffset.Zero, dstSize = targetSize,
                alpha = fade, filterQuality = FilterQuality.Medium,
            )
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05070D).copy(alpha = 0.45f),
                        Color(0xFF05070D).copy(alpha = 0.56f),
                        Color(0xFF05070D).copy(alpha = 0.88f),
                    )
                )
            )
        }
    }
}

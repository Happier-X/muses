package com.example.muses.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muses.R
import com.example.muses.ui.util.ParsedLyrics
import com.example.muses.ui.util.rememberAlbumArt
import com.example.muses.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LyricsPage(
    viewModel: PlayerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lyrics = state.lyrics
    val currentIndex = state.currentLyricIndex
    val albumArtUri = state.albumArtUri

    if (lyrics == null || lyrics.lines.isEmpty()) {
        NoLyricsPlaceholder()
        return
    }

    LyricsContent(
        lyrics = lyrics,
        currentIndex = currentIndex,
        albumArtUri = albumArtUri,
        onLyricClick = { index ->
            val timeMs = lyrics.lines[index].timeMs
            viewModel.seekTo(timeMs)
        }
    )
}

@Composable
private fun NoLyricsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_lyrics),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LyricsContent(
    lyrics: ParsedLyrics,
    currentIndex: Int,
    albumArtUri: android.net.Uri?,
    onLyricClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Auto-scroll is enabled by default, disabled on manual scroll, re-enabled after 3s
    var autoScrollEnabled by remember { mutableStateOf(true) }

    // Cancel auto-scroll on user drag, re-enable after 3 seconds
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect {
                autoScrollEnabled = false
                delay(3000)
                autoScrollEnabled = true
            }
    }

    // Auto-scroll: smooth scroll to center the current lyric line
    LaunchedEffect(currentIndex, autoScrollEnabled) {
        if (currentIndex < 0 || !autoScrollEnabled) return@LaunchedEffect
        listState.animateScrollToItem(
            index = currentIndex.coerceIn(0, lyrics.lines.lastIndex),
            scrollOffset = with(density) { -200.dp.toPx().toInt() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred album art background
        val bitmap = rememberAlbumArt(albumArtUri, targetSizePx = 200)
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Lyrics list — center-aligned
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item { Spacer(modifier = Modifier.height(200.dp)) }
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.timeMs}_$index" }
            ) { index, line ->
                LyricLineItem(
                    text = line.text,
                    isCurrent = index == currentIndex,
                    isPast = index < currentIndex,
                    onClick = { onLyricClick(index) }
                )
            }
            item { Spacer(modifier = Modifier.height(200.dp)) }
        }
    }
}

@Composable
private fun LyricLineItem(
    text: String,
    isCurrent: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isCurrent -> Color.White
            isPast -> Color.White.copy(alpha = 0.5f)
            else -> Color.White.copy(alpha = 0.3f)
        },
        animationSpec = tween(300),
        label = "lyricColor"
    )

    val fontSize by animateFloatAsState(
        targetValue = if (isCurrent) 24f else 18f,
        animationSpec = tween(300),
        label = "lyricSize"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Text(
        text = text,
        color = textColor,
        fontSize = fontSize.sp,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        lineHeight = (fontSize * 1.6f).sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
    )
}

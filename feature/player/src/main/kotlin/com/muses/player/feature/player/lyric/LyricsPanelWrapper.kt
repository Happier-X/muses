package com.muses.player.feature.player.lyric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.muses.player.core.lyrics.model.LyricsDocument

@Composable
fun LyricsPanel(
    document: LyricsDocument?,
    positionMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = PlaybackUiState(
        mediaId = null,
        title = "",
        artist = "",
        album = "",
        durationMs = 0L,
        positionMs = positionMs,
        isPlaying = isPlaying,
        artworkUrl = null,
        seekTo = onSeek,
    )
    LyricsPanel(
        state = state,
        modifier = modifier,
        active = true,
        externalDocument = document,
    )
}

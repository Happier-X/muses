package com.example.muses.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muses.R
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.TrackSource
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.util.formatDurationMs
import com.example.muses.ui.viewmodel.LibraryUiState
import com.example.muses.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
    hasPermission: Boolean = true,
    requestPermission: () -> Unit = {},
    onTrackClick: (AudioTrack) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadTracks()
        }
    }

    LibraryContent(
        modifier = modifier,
        uiState = uiState,
        hasPermission = hasPermission,
        requestPermission = requestPermission,
        viewModel = viewModel,
        onTrackClick = onTrackClick
    )
}

@Composable
fun LibraryContent(
    modifier: Modifier = Modifier,
    uiState: LibraryUiState,
    hasPermission: Boolean,
    requestPermission: () -> Unit,
    viewModel: LibraryViewModel,
    onTrackClick: (AudioTrack) -> Unit
) {
    when (val state = uiState) {
        is LibraryUiState.NeedsPermission -> {
            PermissionRequestContent(
                modifier = modifier,
                onRequestPermission = requestPermission
            )
        }
        is LibraryUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }
        is LibraryUiState.Empty -> {
            EmptyContent(modifier = modifier)
        }
        is LibraryUiState.Ready -> {
            TrackListContent(
                tracks = state.tracks,
                modifier = modifier,
                onTrackClick = onTrackClick
            )
        }
        is LibraryUiState.Error -> {
            if (!hasPermission) {
                PermissionRequestContent(
                    modifier = modifier,
                    onRequestPermission = requestPermission
                )
            } else {
                ErrorContent(
                    message = state.message,
                    modifier = modifier,
                    onRetry = { viewModel.loadTracks() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.local_music_loading),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.local_music_empty),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.local_music_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.local_music_loading)
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = stringResource(R.string.local_music_permission_denied),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

@Composable
private fun TrackListContent(
    tracks: List<AudioTrack>,
    modifier: Modifier = Modifier,
    onTrackClick: (AudioTrack) -> Unit = {}
) {
    LazyColumn(modifier = modifier) {
        items(
            items = tracks,
            key = { it.id }
        ) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

@Composable
fun TrackListItem(
    track: AudioTrack,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = {
            Text(
                text = track.title.ifBlank { stringResource(R.string.unknown_title) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val artistText = track.artist.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_artist)
            val albumText = track.album.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_album)
            Text(
                text = stringResource(R.string.track_artist_separator, artistText, albumText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            Text(
                text = formatDurationMs(track.durationMs),
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LibraryScreenPreview() {
    MusesTheme {
        LibraryScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionRequestContentPreview() {
    MusesTheme {
        PermissionRequestContent()
    }
}

@Preview(showBackground = true)
@Composable
fun TrackListItemPreview() {
    MusesTheme {
        TrackListItem(
            track = AudioTrack(
                id = "1",
                uri = android.net.Uri.EMPTY,
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera",
                durationMs = 354000L,
                source = TrackSource.LOCAL
            )
        )
    }
}

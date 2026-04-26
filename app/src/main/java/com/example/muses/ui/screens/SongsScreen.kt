package com.example.muses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muses.R
import com.example.muses.data.model.AudioTrack
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.viewmodel.SongsUiState
import com.example.muses.ui.viewmodel.SongsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = viewModel(),
    onTrackClick: (AudioTrack) -> Unit = {},
    navigationIcon: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_songs)) },
                navigationIcon = navigationIcon,
                actions = {
                    val state = uiState
                    if (state is SongsUiState.Ready) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.nav_songs)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        SongsContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            uiState = uiState,
            onTrackClick = onTrackClick
        )
    }
}

@Composable
fun SongsContent(
    modifier: Modifier = Modifier,
    uiState: SongsUiState,
    onTrackClick: (AudioTrack) -> Unit = {}
) {
    when (val state = uiState) {
        is SongsUiState.Idle, is SongsUiState.Empty -> {
            EmptySongsContent(modifier = modifier)
        }
        is SongsUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }
        is SongsUiState.Ready -> {
            if (state.tracks.isEmpty()) {
                EmptySongsContent(modifier = modifier)
            } else {
                TrackListContent(
                    tracks = state.tracks,
                    modifier = modifier,
                    onTrackClick = onTrackClick
                )
            }
        }
        is SongsUiState.Error -> {
            ErrorContent(
                message = state.message,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun EmptySongsContent(modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.songs_empty),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.songs_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
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
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongsScreenPreview() {
    MusesTheme {
        SongsScreen()
    }
}
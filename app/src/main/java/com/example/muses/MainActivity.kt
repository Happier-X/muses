package com.example.muses

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.muses.data.model.AudioTrack
import com.example.muses.ui.screens.AddMusicScreen
import com.example.muses.ui.screens.NowPlayingScreen
import com.example.muses.ui.screens.PlayerBar
import com.example.muses.ui.screens.QueueSheet
import com.example.muses.ui.screens.SongsScreen
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.viewmodel.PlayerViewModel
import com.example.muses.ui.viewmodel.SongsUiState
import com.example.muses.ui.viewmodel.SongsViewModel
import com.example.muses.ui.viewmodel.WebdavViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusesTheme {
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    val playerViewModel: PlayerViewModel = viewModel()
    val songsViewModel: SongsViewModel = viewModel()
    val webdavViewModel: WebdavViewModel = viewModel()

    playerViewModel.onMetadataEnriched = { track ->
        songsViewModel.updateTrack(track)
    }
    var selectedItem by remember { mutableIntStateOf(0) }
    var showQueue by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val addedDirPaths by webdavViewModel.addedDirectoryPaths.collectAsStateWithLifecycle()

    val menuIcon: @Composable () -> Unit = {
        IconButton(onClick = { scope.launch { drawerState.open() } }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu_open)
            )
        }
    }

    data class DrawerItem(
        val labelRes: Int,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val drawerItems = listOf(
        DrawerItem(R.string.nav_songs, Icons.Default.MusicNote),
        DrawerItem(R.string.nav_albums, Icons.Default.Album),
        DrawerItem(R.string.nav_artists, Icons.Outlined.Person),
        DrawerItem(R.string.nav_playlists, Icons.AutoMirrored.Filled.PlaylistPlay),
        DrawerItem(R.string.nav_add_music, Icons.Default.Add),
        DrawerItem(R.string.nav_settings, Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(240.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(4.dp))
                drawerItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.labelRes)) },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                when (selectedItem) {
                    0 -> SongsScreen(
                        viewModel = songsViewModel,
                        onTrackClick = { track ->
                            val tracks = (songsViewModel.uiState.value as? SongsUiState.Ready)?.tracks ?: return@SongsScreen
                            val index = tracks.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
                            playerViewModel.setPlaylist(tracks, index)
                        },
                        navigationIcon = menuIcon
                    )
                    4 -> AddMusicScreen(
                        onTrackClick = { track, tracks ->
                            val index = tracks.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
                            playerViewModel.setPlaylist(tracks, index)
                        },
                        onTracksAdded = { tracks ->
                            songsViewModel.addTracks(tracks)
                        },
                        onTracksRemoved = { trackIds ->
                            songsViewModel.removeTracks(trackIds)
                        },
                        onFolderSelected = { uri -> songsViewModel.addFolderFromTreeUri(uri) },
                        addedDirectoryPaths = addedDirPaths,
                        webdavViewModel = webdavViewModel,
                        navigationIcon = menuIcon
                    )
                    else -> PlaceholderScreen(navigationIcon = menuIcon)
                }

                // Transparent click overlay on PlayerBar to open NowPlaying screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showNowPlaying = true }
                ) {
                    PlayerBar(
                        viewModel = playerViewModel,
                        onQueueClick = { showQueue = true }
                    )
                }
            }

            // Now Playing full-screen overlay
            AnimatedVisibility(
                visible = showNowPlaying,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                NowPlayingScreen(
                    onDismiss = { showNowPlaying = false },
                    viewModel = playerViewModel
                )
            }
        }

        if (showQueue) {
            QueueSheet(
                onDismiss = { showQueue = false },
                onTrackClick = { index -> playerViewModel.seekToQueueItem(index) }
            )
        }
    }
}

private fun playTrack(track: AudioTrack, playerViewModel: PlayerViewModel) {
    val mediaMetadataBuilder = MediaMetadata.Builder()
        .setTitle(track.title)
    if (track.artist.isNotBlank()) {
        mediaMetadataBuilder.setArtist(track.artist)
    }
    if (track.album.isNotBlank()) {
        mediaMetadataBuilder.setAlbumTitle(track.album)
    }

    val mediaItem = MediaItem.Builder()
        .setMediaId(track.id)
        .setUri(track.uri)
        .setMediaMetadata(mediaMetadataBuilder.build())
        .build()

    playerViewModel.playTrack(mediaItem)
}

private fun playTracks(tracks: List<AudioTrack>, playerViewModel: PlayerViewModel) {
    if (tracks.isEmpty()) return
    val mediaItems = tracks.map { track ->
        val metadataBuilder = MediaMetadata.Builder().setTitle(track.title)
        if (track.artist.isNotBlank()) metadataBuilder.setArtist(track.artist)
        if (track.album.isNotBlank()) metadataBuilder.setAlbumTitle(track.album)
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
    playerViewModel.playTracks(mediaItems)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(navigationIcon: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Muses") },
                navigationIcon = navigationIcon
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainContentPreview() {
    MusesTheme {
        MainContent()
    }
}
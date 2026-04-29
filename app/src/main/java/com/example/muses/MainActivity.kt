package com.example.muses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    data class UiState(
        val selectedItem: Int = 0,
        val showQueue: Boolean = false,
        val showNowPlaying: Boolean = false,
        val drawerOpen: Boolean = false
    )
    val uiStateState = remember { mutableStateOf(UiState()) }
    val ui = uiStateState.value

    val addedDirPathsState = webdavViewModel.addedDirectoryPaths.collectAsStateWithLifecycle(initialValue = emptySet())
    val halfWidthPx = with(LocalDensity.current) {
        (LocalConfiguration.current.screenWidthDp / 2).dp.toPx()
    }

    fun setSelectedItem(v: Int) { uiStateState.value = uiStateState.value.copy(selectedItem = v) }
    fun setShowQueue(v: Boolean) { uiStateState.value = uiStateState.value.copy(showQueue = v) }
    fun setShowNowPlaying(v: Boolean) { uiStateState.value = uiStateState.value.copy(showNowPlaying = v) }
    fun setDrawerOpen(v: Boolean) { uiStateState.value = uiStateState.value.copy(drawerOpen = v) }

    val menuIcon: @Composable () -> Unit = {
        IconButton(onClick = { setDrawerOpen(!ui.drawerOpen) }) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩（底层，点击关闭）
        if (ui.drawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clickable { setDrawerOpen(false) }
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        // 抽屉（底层左侧，屏幕一半宽）
        val drawerDp = with(LocalDensity.current) { halfWidthPx.toDp() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerDp)
        ) {
            if (ui.drawerOpen) {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerDp),
                    drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                ) {
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
                            selected = ui.selectedItem == index,
                            onClick = {
                                setSelectedItem(index)
                                setDrawerOpen(false)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }

        // 内容区（顶层，抽屉打开时视觉右推）
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = if (ui.drawerOpen) halfWidthPx else 0f
                        }
                ) {
                    when (ui.selectedItem) {
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
                            addedDirectoryPaths = addedDirPathsState.value,
                            webdavViewModel = webdavViewModel,
                            navigationIcon = menuIcon
                        )
                        else -> PlaceholderScreen(navigationIcon = menuIcon)
                    }
                }
            }

            // PlayerBar（固定底部，始终可见）
            PlayerBar(
                viewModel = playerViewModel,
                onQueueClick = { setShowQueue(true) },
                onClick = { setShowNowPlaying(true) }
            )
        }

        // Now Playing 全屏页面
        AnimatedVisibility(
            visible = ui.showNowPlaying,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            NowPlayingScreen(
                onDismiss = { setShowNowPlaying(false) },
                viewModel = playerViewModel
            )
        }

        // 队列 sheet
        if (ui.showQueue) {
            QueueSheet(
                onDismiss = { setShowQueue(false) },
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

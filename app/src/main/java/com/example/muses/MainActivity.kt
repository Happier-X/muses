package com.example.muses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.muses.data.model.AudioTrack
import com.example.muses.ui.screens.LibraryScreen
import com.example.muses.ui.screens.PlayerBar
import com.example.muses.ui.screens.WebdavScreen
import com.example.muses.ui.theme.MusesTheme
import com.example.muses.ui.viewmodel.PlayerViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val playerViewModel: PlayerViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Determine which permission to request based on API level
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Check initial permission state
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    val tabs: List<Pair<Int, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
        R.string.tab_local to Icons.Default.LibraryMusic,
        R.string.tab_webdav to Icons.Default.Cloud
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index: Int, (labelRes: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(labelRes)
                            )
                        },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> LibraryScreen(
                        hasPermission = hasPermission,
                        requestPermission = { permissionLauncher.launch(permissionToRequest) },
                        onTrackClick = { track -> playTrack(track, playerViewModel) }
                    )
                    1 -> WebdavScreen(
                        onTrackClick = { track -> playTrack(track, playerViewModel) }
                    )
                }
            }

            PlayerBar(
                viewModel = playerViewModel
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainContentPreview() {
    MusesTheme {
        MainContent()
    }
}

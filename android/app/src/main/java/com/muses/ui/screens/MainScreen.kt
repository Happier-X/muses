package com.muses.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.data.model.SongDto
import com.muses.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    var songs by mutableStateOf<List<SongDto>>(emptyList())
    var isLoading by mutableStateOf(true)
    var currentSong by mutableStateOf<SongDto?>(null)
    var isPlaying by mutableStateOf(false)

    init { loadSongs() }

    fun loadSongs() {
        viewModelScope.launch {
            isLoading = true
            songs = musicRepository.getSongs()
            isLoading = false
        }
    }

    fun playSong(song: SongDto) {
        currentSong = song
        isPlaying = true
    }

    fun togglePlay() { isPlaying = !isPlaying }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            Column {
                viewModel.currentSong?.let { song ->
                    MiniPlayer(song = song, isPlaying = viewModel.isPlaying, onPlayPause = { viewModel.togglePlay() })
                }
                NavigationBar {
                    NavigationBarItem(icon = { Icon(Icons.Default.MusicNote, "Library") }, label = { Text("音乐库") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
                    NavigationBarItem(icon = { Icon(Icons.Default.List, "Playlists") }, label = { Text("播放列表") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
                    NavigationBarItem(icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("设置") }, selected = selectedTab == 2, onClick = { selectedTab = 2 })
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> LibraryTab(viewModel, Modifier.padding(padding))
            1 -> PlaylistsTab(Modifier.padding(padding))
            2 -> SettingsTab(onLogout, Modifier.padding(padding))
        }
    }
}

@Composable
fun MiniPlayer(song: SongDto, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium)
                Text(song.artist.name, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
            }
        }
    }
}

@Composable
fun LibraryTab(viewModel: LibraryViewModel, modifier: Modifier = Modifier) {
    if (viewModel.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(viewModel.songs) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist.name) },
                    leadingContent = { IconButton(onClick = { viewModel.playSong(song) }) { Icon(Icons.Default.PlayArrow, "Play") } }
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("播放列表功能开发中") }
}

@Composable
fun SettingsTab(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) { Button(onClick = onLogout) { Text("退出登录") } }
}

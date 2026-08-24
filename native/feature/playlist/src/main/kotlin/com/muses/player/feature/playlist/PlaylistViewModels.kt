package com.muses.player.feature.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.PlaylistRepository
import com.muses.player.core.model.Playlist
import com.muses.player.core.model.PlaylistWithSongs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    private val playlistId = MutableStateFlow<String?>(null)

    var renameVisible by mutableStateOf(false)
        private set

    val detail: StateFlow<PlaylistWithSongs?> = playlistId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observePlaylist(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 导航参数到达后绑定目标播放列表（幂等） */
    fun bind(id: String) {
        if (playlistId.value != id) playlistId.value = id
    }

    fun move(fromPosition: Int, toPosition: Int) {
        val id = playlistId.value ?: return
        viewModelScope.launch { repository.moveSong(id, fromPosition, toPosition) }
    }

    fun remove(songId: String) {
        val id = playlistId.value ?: return
        viewModelScope.launch { repository.removeSongFromPlaylist(id, songId) }
    }

    fun rename(name: String) {
        val id = playlistId.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch { repository.renamePlaylist(id, name.trim()) }
    }

    fun deletePlaylist() {
        val id = playlistId.value ?: return
        viewModelScope.launch { repository.deletePlaylist(id) }
    }

    fun showRename() {
        renameVisible = true
    }

    fun dismissRename() {
        renameVisible = false
    }
}

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTo(playlistId: String, songIds: List<String>) {
        viewModelScope.launch { repository.addSongsToPlaylist(playlistId, songIds) }
    }

    fun createAndAdd(name: String, songIds: List<String>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name.trim())
            repository.addSongsToPlaylist(id, songIds)
        }
    }
}

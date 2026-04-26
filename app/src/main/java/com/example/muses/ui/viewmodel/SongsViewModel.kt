package com.example.muses.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.repository.LocalMusicRepository
import com.example.muses.data.repository.TrackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SongsUiState {
    data object Idle : SongsUiState
    data object Loading : SongsUiState
    data class Ready(val tracks: List<AudioTrack>) : SongsUiState
    data object Empty : SongsUiState
    data class Error(val message: String) : SongsUiState
}

class SongsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SongsVM"
    }

    private val repository = LocalMusicRepository(application.contentResolver)

    private val _uiState = MutableStateFlow<SongsUiState>(SongsUiState.Idle)
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    init {
        val saved = TrackStore.loadTracks(application)
        if (saved.isNotEmpty()) {
            _tracks.value = saved
            _uiState.value = SongsUiState.Ready(saved)
        }
    }

    fun addTracks(tracks: List<AudioTrack>) {
        val existingIds = _tracks.value.map { it.id }.toSet()
        val newTracks = tracks.filter { it.id !in existingIds }
        if (newTracks.isNotEmpty()) {
            _tracks.update { it + newTracks }
            _uiState.value = SongsUiState.Ready(_tracks.value)
            TrackStore.saveTracks(getApplication(), _tracks.value)
        }
    }

    fun addFolderFromTreeUri(treeUri: Uri) {
        _uiState.value = SongsUiState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.loadTracksFromTreeUri(treeUri)
            }
            result.fold(
                onSuccess = { folderTracks ->
                    if (folderTracks.isEmpty()) {
                        _uiState.value = if (_tracks.value.isEmpty()) {
                            SongsUiState.Empty
                        } else {
                            SongsUiState.Ready(_tracks.value)
                        }
                    } else {
                        addTracks(folderTracks)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load folder", error)
                    _uiState.value = if (_tracks.value.isNotEmpty()) {
                        SongsUiState.Ready(_tracks.value)
                    } else {
                        SongsUiState.Error(error.localizedMessage ?: "Failed to load folder")
                    }
                }
            )
        }
    }

    fun refresh() {
        _uiState.value = if (_tracks.value.isEmpty()) {
            SongsUiState.Empty
        } else {
            SongsUiState.Ready(_tracks.value)
        }
    }
}
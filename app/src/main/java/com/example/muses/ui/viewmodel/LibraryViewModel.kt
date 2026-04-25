package com.example.muses.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.repository.LocalMusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Ready(val tracks: List<AudioTrack>) : LibraryUiState
    data object Empty : LibraryUiState
    data object NeedsPermission : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LibraryVM"
    }

    private val repository = LocalMusicRepository(application.contentResolver)

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.NeedsPermission)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun loadTracks() {
        _uiState.value = LibraryUiState.Loading
        viewModelScope.launch {
            repository.loadTracks().fold(
                onSuccess = { tracks ->
                    if (tracks.isEmpty()) {
                        _uiState.value = LibraryUiState.Empty
                    } else {
                        _uiState.value = LibraryUiState.Ready(tracks)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load tracks", error)
                    _uiState.value = LibraryUiState.Error(
                        error.localizedMessage ?: "Failed to load music library"
                    )
                }
            )
        }
    }

    fun refresh() {
        loadTracks()
    }
}
package com.example.muses.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.repository.LocalMusicRepository
import com.example.muses.data.repository.MetadataExtractor
import com.example.muses.data.repository.TrackStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    val uiState: StateFlow<SongsUiState> = _tracks.map { tracks ->
        if (tracks.isEmpty()) SongsUiState.Empty else SongsUiState.Ready(tracks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SongsUiState.Idle)

    init {
        val saved = TrackStore.loadTracks(application)
        if (saved.isNotEmpty()) {
            _tracks.value = saved
        }
    }

    fun addTracks(tracks: List<AudioTrack>) {
        val existingIds = _tracks.value.map { it.id }.toSet()
        val newTracks = tracks.filter { it.id !in existingIds }
        if (newTracks.isNotEmpty()) {
            val needsMetadata = newTracks.any { it.artist.isBlank() && it.album.isBlank() && it.durationMs == 0L }
            val enriched = if (needsMetadata) {
                viewModelScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        enrichTracksMetadata(newTracks)
                    }
                    _tracks.update { current ->
                        current.map { track ->
                            result.find { it.id == track.id } ?: track
                        }
                    }
                    TrackStore.saveTracks(getApplication(), _tracks.value)
                }
                newTracks
            } else {
                newTracks
            }
            _tracks.update { it + enriched }
            TrackStore.saveTracks(getApplication(), _tracks.value)
        }
    }

    fun removeTracks(trackIds: List<String>) {
        val idSet = trackIds.toSet()
        val remaining = _tracks.value.filter { it.id !in idSet }
        if (remaining.size < _tracks.value.size) {
            _tracks.value = remaining
            TrackStore.saveTracks(getApplication(), remaining)
        }
    }

    fun addFolderFromTreeUri(treeUri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.loadTracksFromTreeUri(treeUri)
            }
            result.fold(
                onSuccess = { folderTracks ->
                    if (folderTracks.isEmpty()) {
                        // no tracks found, do nothing
                    } else {
                        val enriched = withContext(Dispatchers.IO) {
                            enrichTracksMetadata(folderTracks)
                        }
                        addTracks(enriched)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load folder", error)
                }
            )
        }
    }

    private fun enrichTracksMetadata(tracks: List<AudioTrack>): List<AudioTrack> {
        val context = getApplication<Application>()
        val needsMetadata = tracks.any { it.artist.isBlank() && it.album.isBlank() && it.durationMs == 0L }
        if (!needsMetadata) return tracks
        return tracks.map { track ->
            if (track.artist.isNotBlank() || track.album.isNotBlank() || track.durationMs > 0L) {
                track
            } else {
                val metadata = MetadataExtractor.extractFromUri(context, track.uri)
                if (metadata != null) MetadataExtractor.applyMetadata(track, metadata) else track
            }
        }
    }

    fun updateTrack(track: AudioTrack) {
        val idx = _tracks.value.indexOfFirst { it.id == track.id }
        Log.d(TAG, "updateTrack: id=${track.id}, found=$idx, title=${track.title}, artist=${track.artist}")
        if (idx >= 0) {
            _tracks.update { tracks ->
                tracks.toMutableList().apply { this[idx] = track }
            }
            TrackStore.saveTracks(getApplication(), _tracks.value)
        }
    }

    fun getTrackById(id: String): AudioTrack? = _tracks.value.find { it.id == id }

    fun refresh() {
        // uiState is derived from _tracks, no manual update needed
    }
}
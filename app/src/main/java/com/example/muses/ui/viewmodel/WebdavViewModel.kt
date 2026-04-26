package com.example.muses.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.model.WebdavConfig
import com.example.muses.data.repository.WebdavConfigManager
import com.example.muses.data.repository.WebdavItem
import com.example.muses.data.repository.WebdavRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WebdavUiState {
    data object NotConfigured : WebdavUiState
    data object Connecting : WebdavUiState
    data class Browsing(
        val config: WebdavConfig,
        val currentPath: String,
        val items: List<WebdavItem>
    ) : WebdavUiState
    data class EmptyDirectory(
        val config: WebdavConfig,
        val currentPath: String
    ) : WebdavUiState
    data class AddingDirectory(
        val config: WebdavConfig,
        val currentPath: String,
        val items: List<WebdavItem>,
        val targetPath: String
    ) : WebdavUiState
    data class Error(
        val message: String,
        val config: WebdavConfig? = null
    ) : WebdavUiState
}

class WebdavViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WebdavVM"
    }

    private val repository = WebdavRepository()

    private val _uiState = MutableStateFlow<WebdavUiState>(WebdavUiState.NotConfigured)
    val uiState: StateFlow<WebdavUiState> = _uiState.asStateFlow()

    private val _addedTracks = MutableSharedFlow<List<AudioTrack>>()
    val addedTracks: SharedFlow<List<AudioTrack>> = _addedTracks.asSharedFlow()

    private val _removedTrackIds = MutableSharedFlow<List<String>>()
    val removedTrackIds: SharedFlow<List<String>> = _removedTrackIds.asSharedFlow()

    private val _addedDirectoryPaths = MutableStateFlow<Set<String>>(
        WebdavConfigManager.loadWebdavDirs(getApplication())
    )
    val addedDirectoryPaths: StateFlow<Set<String>> = _addedDirectoryPaths.asStateFlow()

    private var addDirectoryJob: Job? = null

    val lastConfig: WebdavConfig?
        get() = WebdavConfigManager.load(getApplication())

    init {
        val savedConfig = WebdavConfigManager.load(getApplication())
        if (savedConfig != null && savedConfig.isValid()) {
            connectAndBrowse(savedConfig)
        }
    }

    fun connect(config: WebdavConfig) {
        if (!config.isValid()) {
            _uiState.value = WebdavUiState.Error("Invalid server URL")
            return
        }
        WebdavConfigManager.save(getApplication(), config)
        connectAndBrowse(config)
    }

    fun browsePath(path: String) {
        val state = _uiState.value
        if (state !is WebdavUiState.Browsing && state !is WebdavUiState.EmptyDirectory) return
        val config = when (state) {
            is WebdavUiState.Browsing -> state.config
            is WebdavUiState.EmptyDirectory -> state.config
            else -> return
        }
        loadDirectory(config, path)
    }

    fun goToParent() {
        val state = _uiState.value
        val config = when (state) {
            is WebdavUiState.Browsing -> state.config to state.currentPath
            is WebdavUiState.EmptyDirectory -> state.config to state.currentPath
            else -> return
        }
        val (currentConfig, currentPath) = config
        val parent = repository.parentPath(currentPath)
        loadDirectory(currentConfig, parent)
    }

    fun toAudioTrack(item: WebdavItem): AudioTrack? {
        val config = (uiState.value as? WebdavUiState.Browsing)?.config
            ?: return null
        return repository.toAudioTrack(item, config)
    }

    fun addDirectory(path: String, recursive: Boolean) {
        val state = _uiState.value
        val browsingState = when (state) {
            is WebdavUiState.Browsing -> state
            is WebdavUiState.EmptyDirectory -> WebdavUiState.Browsing(state.config, state.currentPath, emptyList())
            else -> return
        }
        _uiState.value = WebdavUiState.AddingDirectory(
            config = browsingState.config,
            currentPath = browsingState.currentPath,
            items = browsingState.items,
            targetPath = path
        )
        addDirectoryJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.listAudioFiles(browsingState.config, path, recursive)
                }
                result.fold(
                    onSuccess = { items ->
                        val tracks = items.mapNotNull { item ->
                            repository.toAudioTrack(item, browsingState.config)
                        }
                        Log.i(TAG, "Found ${tracks.size} tracks from $path (recursive=$recursive)")
                        if (tracks.isNotEmpty()) {
                            _addedTracks.emit(tracks)
                            WebdavConfigManager.addWebdavDir(getApplication(), path)
                            _addedDirectoryPaths.update { it + path }
                        }
                        _uiState.value = WebdavUiState.Browsing(
                            config = browsingState.config,
                            currentPath = browsingState.currentPath,
                            items = browsingState.items
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to add directory $path", error)
                        _uiState.value = WebdavUiState.Browsing(
                            config = browsingState.config,
                            currentPath = browsingState.currentPath,
                            items = browsingState.items
                        )
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in addDirectory", e)
                _uiState.value = WebdavUiState.Browsing(
                    config = browsingState.config,
                    currentPath = browsingState.currentPath,
                    items = browsingState.items
                )
            }
        }
    }

    fun cancelAddDirectory() {
        addDirectoryJob?.cancel()
        addDirectoryJob = null
        val state = _uiState.value
        val browsingState = when (state) {
            is WebdavUiState.AddingDirectory -> WebdavUiState.Browsing(
                config = state.config,
                currentPath = state.currentPath,
                items = state.items
            )
            else -> return
        }
        _uiState.value = browsingState
    }

    fun removeDirectory(path: String) {
        WebdavConfigManager.removeWebdavDir(getApplication(), path)
        _addedDirectoryPaths.update { it - path }

        val currentConfig = (uiState.value as? WebdavUiState.Browsing)?.config

        if (currentConfig == null) {
            Log.w(TAG, "No browsing config when removing directory: $path")
        }

        viewModelScope.launch {
            val result = currentConfig?.let { config ->
                withContext(Dispatchers.IO) {
                    repository.listAudioFiles(config, path, recursive = true)
                }
            } ?: Result.failure(Exception("No browsing config"))

            result.fold(
                onSuccess = { items ->
                    val trackIds = items.mapNotNull { item ->
                        val track = repository.toAudioTrack(item, currentConfig!!)
                        track?.id
                    }
                    if (trackIds.isNotEmpty()) {
                        _removedTrackIds.emit(trackIds)
                    } else if (items.isEmpty()) {
                        Log.d(TAG, "No audio files found in removed directory: $path")
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to list files for removal: $path", error)
                    _removedTrackIds.emit(listOf("webdav:$path"))
                }
            )
        }
    }

    fun disconnect() {
        _uiState.value = WebdavUiState.NotConfigured
    }

    private fun connectAndBrowse(config: WebdavConfig) {
        loadDirectory(config, "")
    }

    private fun loadDirectory(config: WebdavConfig, path: String) {
        _uiState.value = WebdavUiState.Connecting
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.listDirectory(config, path)
            }
            result.fold(
                onSuccess = { items ->
                    _uiState.value = if (items.isEmpty()) {
                        WebdavUiState.EmptyDirectory(config, path)
                    } else {
                        WebdavUiState.Browsing(config, path, items)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to list directory: $path", error)
                    _uiState.value = WebdavUiState.Error(
                        message = error.localizedMessage ?: "Connection failed",
                        config = config
                    )
                }
            )
        }
    }
}
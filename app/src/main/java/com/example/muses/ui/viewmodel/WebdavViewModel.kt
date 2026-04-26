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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        viewModelScope.launch {
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
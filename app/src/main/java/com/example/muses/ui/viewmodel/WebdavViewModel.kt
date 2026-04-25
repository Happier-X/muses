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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WebdavUiState {
    /** Not yet configured — show config form. */
    data object NotConfigured : WebdavUiState
    /** Connecting to server. */
    data object Connecting : WebdavUiState
    /** Connected and browsing. */
    data class Browsing(
        val config: WebdavConfig,
        val currentPath: String,
        val items: List<WebdavItem>
    ) : WebdavUiState
    /** Directory is empty. */
    data class EmptyDirectory(
        val config: WebdavConfig,
        val currentPath: String
    ) : WebdavUiState
    /** An error occurred. */
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

    init {
        // Load saved config if available
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
        if (state !is WebdavUiState.Browsing) return
        val parent = repository.parentPath(state.currentPath)
        browsePath(parent)
    }

    fun toAudioTrack(item: WebdavItem): AudioTrack? {
        val config = (uiState.value as? WebdavUiState.Browsing)?.config
            ?: return null
        return repository.toAudioTrack(item, config)
    }

    fun disconnect() {
        WebdavConfigManager.clear(getApplication())
        _uiState.value = WebdavUiState.NotConfigured
    }

    private fun connectAndBrowse(config: WebdavConfig) {
        loadDirectory(config, "")
    }

    private fun loadDirectory(config: WebdavConfig, path: String) {
        _uiState.value = WebdavUiState.Connecting
        viewModelScope.launch {
            repository.listDirectory(config, path).fold(
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

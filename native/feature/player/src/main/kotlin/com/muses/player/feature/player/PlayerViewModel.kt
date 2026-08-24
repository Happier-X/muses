package com.muses.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.media.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 播放页 ViewModel：包装 PlayerConnection 的 Flow 并提供位置轮询 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerConnection: PlayerConnection,
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
    val currentMediaItem = playerConnection.currentMediaItem
    val duration: StateFlow<Long> = playerConnection.duration
    val repeatMode: StateFlow<Int> = playerConnection.repeatMode
    val shuffleModeEnabled: StateFlow<Boolean> = playerConnection.shuffleModeEnabled
    val queue: StateFlow<List<androidx.media3.common.MediaItem>> = playerConnection.queue

    // 位置轮询（约 500ms 一次）
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    // 是否正在拖拽进度条
    private val _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    init {
        startPositionPolling()
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                if (!_isSeeking.value) {
                    _position.value = playerConnection.currentPosition()
                }
                delay(500)
            }
        }
    }

    fun playPause() = playerConnection.playPause()

    fun skipToNext() = playerConnection.skipToNext()

    fun skipToPrevious() = playerConnection.skipToPrevious()

    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

    fun setRepeatMode(mode: Int) = playerConnection.setRepeatMode(mode)

    fun setShuffleModeEnabled(enabled: Boolean) = playerConnection.setShuffleModeEnabled(enabled)

    fun onSeekStart() {
        _isSeeking.value = true
    }

    fun onSeekEnd(positionMs: Long) {
        _isSeeking.value = false
        seekTo(positionMs)
        _position.value = positionMs
    }
}

/** 队列页 ViewModel */
@HiltViewModel
class QueueViewModel @Inject constructor(
    val playerConnection: PlayerConnection,
) : ViewModel() {
    val queue: StateFlow<List<androidx.media3.common.MediaItem>> = playerConnection.queue
    val currentMediaItem = playerConnection.currentMediaItem
    val isPlaying: StateFlow<Boolean> = playerConnection.isPlaying
}

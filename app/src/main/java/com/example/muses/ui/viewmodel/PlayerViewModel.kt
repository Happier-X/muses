package com.example.muses.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.muses.playback.MusicService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerState(
    val isPlaying: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isReady: Boolean = false,
    val hasTrack: Boolean = false,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerVM"
        private const val POSITION_POLL_MS = 250L
    }

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .also { future ->
                future.addListener(
                    {
                        if (future.isCancelled) {
                            Log.e(TAG, "MediaController connection cancelled")
                            return@addListener
                        }
                        try {
                            val controller = future.get()
                            mediaController = controller
                            controller.addListener(PlayerListener())
                            startPositionPolling()
                            syncState()
                            Log.i(TAG, "Connected to MusicService")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to get MediaController", e)
                        }
                    },
                    context.mainExecutor
                )
            }
    }

    fun playTrack(mediaItem: MediaItem) {
        val controller = mediaController ?: run {
            Log.w(TAG, "MediaController not ready, retrying connection")
            connectToService()
            return
        }
        _state.update { it.copy(errorMessage = null) }
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.playWhenReady = true
    }

    fun playTracks(mediaItems: List<MediaItem>) {
        val controller = mediaController ?: run {
            Log.w(TAG, "MediaController not ready, retrying connection")
            connectToService()
            return
        }
        _state.update { it.copy(errorMessage = null) }
        controller.setMediaItems(mediaItems)
        controller.prepare()
        controller.playWhenReady = true
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController ?: break
                if (controller.playbackState == Player.STATE_READY) {
                    val pos = controller.currentPosition
                    val dur = controller.duration
                    if (pos >= 0 && dur > 0) {
                        _state.update {
                            it.copy(positionMs = pos, durationMs = dur)
                        }
                    }
                }
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun syncState() {
        val controller = mediaController ?: return
        val isReady = controller.playbackState == Player.STATE_READY
        val currentItem = if (controller.mediaItemCount > 0) controller.currentMediaItem else null

        _state.update {
            it.copy(
                isPlaying = controller.isPlaying,
                title = currentItem?.mediaMetadata?.title?.toString(),
                artist = currentItem?.mediaMetadata?.artist?.toString(),
                isReady = isReady,
                hasTrack = controller.mediaItemCount > 0,
                durationMs = if (isReady && controller.duration > 0) controller.duration else it.durationMs,
                positionMs = if (isReady) controller.currentPosition else it.positionMs
            )
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        controllerFuture?.let {
            if (!it.isCancelled && !it.isDone) {
                it.cancel(true)
            }
        }
        mediaController?.release()
        super.onCleared()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncState()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncState()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            syncState()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.message}", error)
            _state.update {
                it.copy(
                    errorMessage = error.message ?: "Playback error",
                    isPlaying = false
                )
            }
        }
    }
}
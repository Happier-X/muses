package com.example.muses.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.muses.playback.MusicService
import com.example.muses.data.model.AudioTrack
import com.example.muses.data.repository.MetadataExtractor
import com.example.muses.data.repository.TrackStore
import com.example.muses.data.repository.WebdavConfigManager
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
    val album: String? = null,
    val albumArtUri: Uri? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isReady: Boolean = false,
    val hasTrack: Boolean = false,
    val errorMessage: String? = null,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerVM"
        private const val POSITION_POLL_MS = 250L
    }

    var onMetadataEnriched: ((AudioTrack) -> Unit)? = null
    private val enrichedIds = mutableSetOf<String>()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionJob: Job? = null

    /** 当前播放列表快照，用于构建 ExoPlayer 队列以支持自动切歌 */
    private var currentPlaylist: List<AudioTrack> = emptyList()

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

    /**
     * 设置播放列表并从指定曲目开始播放。
     * 将所有曲目加入 ExoPlayer 队列，实现自动切歌功能。
     */
    fun setPlaylist(tracks: List<AudioTrack>, startIndex: Int) {
        val controller = mediaController ?: run {
            Log.w(TAG, "MediaController not ready, retrying connection")
            connectToService()
            return
        }
        _state.update { it.copy(errorMessage = null) }
        currentPlaylist = tracks
        val mediaItems = tracks.map { track ->
            val metadataBuilder = MediaMetadata.Builder().setTitle(track.title)
            if (track.artist.isNotBlank()) metadataBuilder.setArtist(track.artist)
            if (track.album.isNotBlank()) metadataBuilder.setAlbumTitle(track.album)
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }
        controller.setMediaItems(mediaItems, startIndex, 0L)
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

    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
        }
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
                positionMs = if (isReady) controller.currentPosition else it.positionMs,
                shuffleModeEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        }
    }

    private fun persistTrackUpdate(trackId: String, update: (AudioTrack) -> AudioTrack) {
        viewModelScope.launch {
            val enriched = withContext(Dispatchers.IO) {
                val tracks = TrackStore.loadTracks(getApplication())
                var result: AudioTrack? = null
                val updated = tracks.map { track ->
                    if (track.id == trackId) {
                        val newTrack = update(track)
                        result = newTrack
                        newTrack
                    } else {
                        track
                    }
                }
                TrackStore.saveTracks(getApplication(), updated)
                result
            }
            if (enriched != null) {
                onMetadataEnriched?.invoke(enriched)
                    ?: Log.w(TAG, "onMetadataEnriched is null, track $trackId not propagated to SongsVM")
            }
        }
    }

    private fun enrichTrackMetadata(trackId: String, mediaItem: MediaItem) {
        val uri = mediaItem.requestMetadata.mediaUri
            ?: mediaItem.localConfiguration?.uri
        if (uri == null) {
            Log.w(TAG, "No URI found for track $trackId, skipping enrichment")
            return
        }
        val isWebdav = trackId.startsWith("webdav:")
        viewModelScope.launch {
            val metadata = withContext(Dispatchers.IO) {
                if (isWebdav) {
                    val authHeader = WebdavConfigManager.getBasicAuthHeader(getApplication())
                    MetadataExtractor.extractFromUrlWithTempFile(getApplication(), uri.toString(), authHeader)
                        ?: MetadataExtractor.extractFromUrl(getApplication(), uri.toString(), authHeader)
                } else {
                    MetadataExtractor.extractFromUri(getApplication(), uri)
                }
            }
            if (metadata != null) {
                Log.d(TAG, "enrichTrackMetadata: got metadata for $trackId: title=${metadata.title}, artist=${metadata.artist}")
                persistTrackUpdate(trackId) { existing ->
                    existing.copy(
                        title = metadata.title?.takeIf { it.isNotBlank() } ?: existing.title,
                        artist = metadata.artist?.takeIf { it.isNotBlank() } ?: existing.artist,
                        album = metadata.album?.takeIf { it.isNotBlank() } ?: existing.album,
                        durationMs = metadata.durationMs?.takeIf { it > 0 } ?: existing.durationMs,
                        albumArtUri = metadata.albumArtUri ?: existing.albumArtUri
                    )
                }
                _state.update {
                    it.copy(
                        title = metadata.title ?: it.title,
                        artist = metadata.artist?.takeIf { a -> a.isNotBlank() } ?: it.artist,
                        albumArtUri = metadata.albumArtUri ?: it.albumArtUri
                    )
                }
            } else {
                Log.w(TAG, "enrichTrackMetadata: MetadataExtractor returned null for $trackId")
            }
        }
    }

    private fun updatePlayMetadata(trackId: String) {
        val now = System.currentTimeMillis()
        persistTrackUpdate(trackId) { existing ->
            existing.copy(playCount = existing.playCount + 1, lastPlayedAt = now)
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
            Log.d(TAG, "onMediaItemTransition: mediaId=${mediaItem?.mediaId}, enrichedIds=$enrichedIds")
            mediaItem?.mediaId?.let { trackId ->
                updatePlayMetadata(trackId)
                if (trackId !in enrichedIds) {
                    enrichedIds.add(trackId)
                    enrichTrackMetadata(trackId, mediaItem)
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val trackId = mediaController?.currentMediaItem?.mediaId ?: return
            val title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
            val artist = mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() }
            val album = mediaMetadata.albumTitle?.toString()?.takeIf { it.isNotBlank() }

            if (title == null && artist == null && album == null) return

            Log.d(TAG, "onMediaMetadataChanged: trackId=$trackId, title=$title, artist=$artist, album=$album")

            _state.update {
                it.copy(
                    title = title ?: it.title,
                    artist = artist ?: it.artist,
                    album = album ?: it.album
                )
            }

            persistTrackUpdate(trackId) { existing ->
                existing.copy(
                    title = title ?: existing.title,
                    artist = artist ?: existing.artist,
                    album = album ?: existing.album
                )
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            syncState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleModeEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode) }
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

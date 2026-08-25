@file:Suppress("UnsafeOptInUsageError")

package com.muses.player.core.media.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 播放连接：经 MediaController 连接 PlaybackService，
 * 暴露播放状态 Flow 供 ViewModel 消费。
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentMediaItem.value = mediaItem
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = playbackState
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleModeEnabled.value = shuffleModeEnabled
        }
    }

    fun connect() {
        if (controller != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        // MediaController 所有方法必须在主线程调用：Future 回调也需投递主线程，
        // 否则 syncState 里的 isPlaying 等直接抛 IllegalStateException（MuMu 实测崩溃）
        controllerFuture?.addListener(
            { mainHandler.post { connectOnMainThread() } },
            java.util.concurrent.Executors.newSingleThreadExecutor(),
        )
    }

    private fun connectOnMainThread() {
        val future = controllerFuture ?: return // disconnect 已发生，丢弃迟到回调
        val connected = runCatching { future.get() }.getOrNull() ?: return
        controller = connected.also { player ->
            player.addListener(playerListener)
            syncState(player)
        }
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.cancel(true)
        controller = null
        controllerFuture = null
    }

    /**
     * 从歌曲列表中选择 songId 开始播放。
     * WebDAV 曲目需提前调用 [setWebDavAuthorization] 设置 header。
     */
    fun play(songId: String, songs: List<com.muses.player.core.model.Song>) {
        val player = controller ?: return
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.path)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
        }
        val index = mediaItems.indexOfFirst { it.mediaId == songId }
        if (index < 0) return

        player.setMediaItems(mediaItems, index, C.TIME_UNSET)
        player.prepare()
        player.playWhenReady = true
    }

    fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipToNext() {
        controller?.let {
            if (it.hasNextMediaItem()) it.seekToNext()
        }
    }

    fun skipToPrevious() {
        controller?.let {
            if (it.currentPosition > 3000) {
                it.seekTo(0)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            }
        }
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun stop() {
        controller?.stop()
    }

    private fun syncState(player: MediaController) {
        _isPlaying.value = player.isPlaying
        _currentMediaItem.value = player.currentMediaItem
        _position.value = player.currentPosition
        _duration.value = if (player.duration > 0) player.duration else 0L
        _playbackState.value = player.playbackState
        _queue.value = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        _repeatMode.value = player.repeatMode
        _shuffleModeEnabled.value = player.shuffleModeEnabled
    }

    /**
     * 位置更新轮询（UI 层调用，约 500ms 一次）。
     * ExoPlayer 的 Player.Listener 不实时推送 position，
     * UI 需主动读取以驱动进度条。
     */
    fun currentPosition(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long {
        val d = controller?.duration ?: 0L
        return if (d > 0) d else 0L
    }
}

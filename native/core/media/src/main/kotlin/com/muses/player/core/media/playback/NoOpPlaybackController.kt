package com.muses.player.core.media.playback

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/** 阶段 0 占位实现；阶段 3 由 MediaController 驱动的实现替换 */
@Singleton
class NoOpPlaybackController @Inject constructor() : PlaybackController {

    override fun play(songId: String) {
        Log.d(TAG, "play($songId) — 阶段 3 接入真实播放")
    }

    override fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause() — 阶段 3 接入真实播放")
    }

    private companion object {
        const val TAG = "PlaybackController"
    }
}

package com.muses.player.core.media.playback

/**
 * 播放控制接口：UI/ViewModel 经此驱动 PlaybackService（Media3 MediaSessionService）。
 * 阶段 3 实现 PlayerConnection + 真实控制器；阶段 0 仅占位契约。
 */
interface PlaybackController {
    /** 按 songId 播放（阶段 3 起可用） */
    fun play(songId: String)

    /** 暂停/继续 */
    fun togglePlayPause()
}

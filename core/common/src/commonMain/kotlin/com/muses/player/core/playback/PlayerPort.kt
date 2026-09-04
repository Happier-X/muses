package com.muses.player.core.playback

import com.muses.player.core.model.playback.PlayerConfig
import com.muses.player.core.model.playback.RepeatMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放器端口最小接口（任务 09-04-kmp-p1-common，P1 只定接口、无实现）。
 *
 * 偏离 design.md §3 的记录：
 * - design 原写 `playbackState: StateFlow<PlaybackState>`，但 core:model 并无 `PlaybackState`
 *   类型（PlaybackModels 仅有 RepeatMode/PlayerConfig/QueueSnapshotData/PlaybackSessionInfo/
 *   RecentPlayEntry）；现有 `PlayerConnection.playbackState` 为 `StateFlow<Int>`
 *   （Media3 `Player.STATE_*` 整型）。P1 取 `StateFlow<Int>` 对齐现状，避免新模型。
 * - `enqueue(ids, index)` / `seekTo(ms)` / `setRepeatMode/setShuffleEnabled` 保持原签名意图；
 *   `setShuffleEnabled` 另配 `setRepeatMode(RepeatMode)` 模型重载（复用 model 现有模型）。
 * - 现有 `PlaybackController`/`PlayerConnection` 不动，P2 再做适配实现。
 *
 * 二期预留（TODO，不实现）：托盘/SMTC/音频焦点（D2 桌面 MVP 决策）。
 */
interface PlayerPort {
    /** 播放状态（Media3 Player.STATE_* 整型，对齐 PlayerConnection 现状） */
    val playbackState: StateFlow<Int>
    val playbackError: StateFlow<String?>
    val playerConfig: StateFlow<PlayerConfig>

    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun enqueue(ids: List<String>, index: Int)
    fun setRepeatMode(mode: Int)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleEnabled(enabled: Boolean)
}

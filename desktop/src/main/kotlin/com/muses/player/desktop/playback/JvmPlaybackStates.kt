package com.muses.player.desktop.playback

/**
 * S2 桌面播放状态整型（对齐 Media3 `Player.STATE_*`，供 [JvmPlayerPort.playbackState] 使用）。
 *
 * commonMain 的 [PlayerPort] 取 `StateFlow<Int>` 即为对齐安卓侧 [PlayerConnection] 现状；
 * 桌面侧无 Media3 依赖，此处按 Media3 同值复刻，避免桌面模块引入 `androidx.media3`：
 * IDLE=1 / BUFFERING=2 / READY=3 / ENDED=4（见 Media3 `Player` 常量定义）。
 */
object JvmPlaybackStates {
    const val STATE_IDLE = 1
    const val STATE_BUFFERING = 2
    const val STATE_READY = 3
    const val STATE_ENDED = 4
}

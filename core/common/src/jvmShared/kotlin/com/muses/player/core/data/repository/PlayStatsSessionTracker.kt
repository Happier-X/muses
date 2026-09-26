package com.muses.player.core.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 设备本地日键（ISO `yyyy-MM-dd`）：commonMain 无时区/日历 API，日键统一在这里算好再交给仓库。 */
object PlayStatsDay {
    fun of(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis), zone).toString()
}

/**
 * 听歌统计埋点内核（安卓 PlaybackService 与桌面 JvmPlayerPort 共用）。
 *
 * 只累计「播放中的墙上时间」：
 * - [onSongStarted] 登记一次播放（次数 +1）并按需开启计时段；
 * - [onPlaybackState] 上报当前曲与是否在播（转场/播放/暂停/停止时调用），内部把上一段结算入库；
 * - [checkpoint] 播放中每 [TICK_MS] 调一次，结算已播时长并续段——既让统计页接近实时，
 *   也把进程被杀时的未落盘损失压在一个 tick 以内；
 * - [flush] 服务/进程结束前调用（结算但不续段）。
 *
 * 状态切换经 [mutex] 串行，写盘在锁外；状态未变时的重复上报只刷新段起点，不会重复计数。
 */
class PlayStatsSessionTracker(
    private val repository: PlayStatsRepository,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val dayOf: (Long) -> String = { PlayStatsDay.of(it) },
) {

    companion object {
        /** 落盘节拍：统计页据此近似实时刷新，同时限制写盘频率 */
        const val TICK_MS = 20_000L
    }

    private val mutex = Mutex()
    private var counting = false
    private var segmentStartMs = 0L

    /**
     * 登记一次播放（歌曲开始播放时调用）：当日次数 +1、该曲累计次数 +1，并同步计时段状态。
     *
     * @param isPlaying 调用瞬间是否已在播（切歌时通常尚未起播，等 [onPlaybackState] 补起段）
     */
    suspend fun onSongStarted(
        songId: String,
        title: String,
        subtitle: String,
        coverUri: String?,
        isPlaying: Boolean,
    ) {
        // 埋点失败不打断播放（次数最多漏记一次）
        runCatching { repository.recordPlay(songId, title, subtitle, coverUri, dayOf(now())) }
            .onFailure { e -> if (e is CancellationException) throw e }
        onPlaybackState(songId, isPlaying)
    }

    /** 上报播放状态（转场 / 播放 / 暂停 / 停止） */
    suspend fun onPlaybackState(songId: String?, isPlaying: Boolean) {
        val settled = mutex.withLock {
            val delta = settleLocked()
            if (isPlaying && songId != null) {
                counting = true
                segmentStartMs = now()
            }
            delta
        }
        persist(settled)
    }

    /** 播放中定期结算并续段（是否在播的语义不变） */
    suspend fun checkpoint() {
        val settled = mutex.withLock {
            if (!counting) return
            val delta = settleLocked()
            counting = true
            segmentStartMs = now()
            delta
        }
        persist(settled)
    }

    /** 服务销毁/进程退出前结算（不续段） */
    suspend fun flush() {
        val settled = mutex.withLock { settleLocked() }
        persist(settled)
    }

    private fun settleLocked(): Long {
        if (!counting) return 0L
        val end = now()
        counting = false
        return (end - segmentStartMs).coerceAtLeast(0L)
    }

    private suspend fun persist(ms: Long) {
        if (ms <= 0L) return
        // 写盘失败只损失这一段时长，不影响播放链路
        runCatching { repository.addListenMs(ms, dayOf(now())) }
            .onFailure { e -> if (e is CancellationException) throw e }
    }
}

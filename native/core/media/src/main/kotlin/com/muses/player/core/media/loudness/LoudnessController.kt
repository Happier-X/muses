package com.muses.player.core.media.loudness

import androidx.media3.common.Player
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 响度均衡应用器：挂在服务侧的 ExoPlayer 上（MediaController 无 volume 能力）。
 *
 * 语义（spec/frontend/features-player.md 响度均衡）：
 * - 切歌必重算，禁止串曲增益
 * - 开关变化立即对当前曲重设
 * - 关闭或无标签 → volume 1.0
 *
 * @param scope 调用方提供的协程作用域（服务生命周期内）
 */
class LoudnessController(
    private val player: Player,
    private val settingsRepository: SettingsRepository,
    private val songDao: SongDao,
    private val scope: CoroutineScope,
) {

    /** 由 settings 收集协程（scope 调度线程）写、player 回调（主线程）读，需保证可见性 */
    @Volatile
    private var enabled = false
    private var settingsJob: Job? = null

    /** 单飞写入：快速连点切歌时取消在途查询，避免旧曲增益乱序覆盖新曲 volume */
    private var applyJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            applyForCurrentItem()
        }
    }

    fun start() {
        player.addListener(playerListener)
        settingsJob = scope.launch {
            settingsRepository.loudnessEnabled.collect { newEnabled ->
                enabled = newEnabled
                applyForCurrentItem()
            }
        }
    }

    fun stop() {
        player.removeListener(playerListener)
        settingsJob?.cancel()
        settingsJob = null
        applyJob?.cancel()
        applyJob = null
        // 停止时恢复满幅，避免残留增益影响后续会话
        player.volume = LoudnessCalculator.PLAYBACK_VOLUME_MAX.toFloat()
    }

    private fun applyForCurrentItem() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        applyJob?.cancel()
        applyJob = scope.launch {
            val gainDb = runCatching { songDao.getById(mediaId)?.replayGainTrackDb }.getOrNull()
            val normalized = gainDb?.let { LoudnessCalculator.normalizeReplayGainDbValue(it) }
            // 取消后不再落笔，避免 stop() 复位 1.0 后又被旧任务覆盖
            if (isActive) {
                player.volume = LoudnessCalculator.dbToPlaybackVolume(normalized, enabled).toFloat()
            }
        }
    }
}

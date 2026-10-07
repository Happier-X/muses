package com.muses.player.core.media.playback

import com.muses.player.core.model.MAX_VOLUME_BOOST_DB
import com.muses.player.core.model.volumeBoostMillibels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 音频效果器接口，便于验证真实设备失败后的恢复逻辑。 */
internal interface VolumeBoostEffect {
    var targetGainMillibels: Int
    var enabled: Boolean
    fun release()
}

data class VolumeBoostStatus(
    val sessionId: Int = 0,
    val requestedDb: Int = 0,
    val appliedDb: Int? = 0,
    val error: Throwable? = null,
)

/** 保存目标增益，真实会话就绪后下发；临时失败最多重试三次。所有操作在播放主线程执行。 */
internal class VolumeBoostSession(
    private val scope: CoroutineScope,
    private val createEffect: (Int) -> VolumeBoostEffect,
) {
    private val mutableStatus = MutableStateFlow(VolumeBoostStatus())
    val status = mutableStatus.asStateFlow()
    private var effect: VolumeBoostEffect? = null
    private var retryJob: Job? = null

    fun bind(sessionId: Int) {
        val validId = sessionId.coerceAtLeast(0)
        if (validId == status.value.sessionId) return
        cancelRetry()
        releaseEffect()
        mutableStatus.value = status.value.copy(sessionId = validId, appliedDb = null, error = null)
        refresh()
    }

    fun apply(db: Int) {
        val targetDb = db.coerceIn(0, MAX_VOLUME_BOOST_DB)
        val current = status.value
        if (targetDb == current.requestedDb && current.appliedDb == targetDb && current.error == null) return
        mutableStatus.value = current.copy(requestedDb = targetDb, appliedDb = null, error = null)
        refresh()
    }

    /** 播放恢复时再次校验读回值，恢复被系统关闭或临时失去控制的效果器。 */
    fun refresh() {
        cancelRetry()
        if (applyOnce()) return
        retryJob = scope.launch {
            for (waitMs in listOf(300L, 1_000L, 3_000L)) {
                delay(waitMs)
                if (applyOnce()) break
            }
        }
    }

    private fun applyOnce(): Boolean {
        val current = status.value
        if (current.requestedDb == 0) {
            releaseEffect()
            mutableStatus.value = current.copy(appliedDb = 0, error = null)
            return true
        }
        // 播放器尚未创建输出，不把等待会话误报成设备不支持。
        if (current.sessionId == 0) return true
        val result = runCatching {
            val activeEffect = effect ?: createEffect(current.sessionId).also { effect = it }
            val targetGain = volumeBoostMillibels(current.requestedDb)
            activeEffect.targetGainMillibels = targetGain
            activeEffect.enabled = true
            check(activeEffect.enabled && activeEffect.targetGainMillibels == targetGain) {
                "音量增益读回校验失败：目标 ${current.requestedDb} dB"
            }
        }
        mutableStatus.value = current.copy(
            appliedDb = current.requestedDb.takeIf { result.isSuccess },
            error = result.exceptionOrNull(),
        )
        if (result.isFailure) releaseEffect()
        return result.isSuccess
    }

    fun release() {
        cancelRetry()
        releaseEffect()
        mutableStatus.value = VolumeBoostStatus()
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    private fun releaseEffect() {
        runCatching { effect?.release() }
        effect = null
    }
}

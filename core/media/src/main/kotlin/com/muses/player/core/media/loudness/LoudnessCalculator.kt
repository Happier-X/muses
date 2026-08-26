package com.muses.player.core.media.loudness

import kotlin.math.abs
import kotlin.math.pow

/**
 * 响度均衡纯函数（移植自 Web 层 src/features/player/loudness.ts，语义照搬）：
 * - 仅标签增益，禁止无标签写假增益
 * - Opus R128 的 Q7.8 整数按 ÷256 换算
 * - 开启时叠加 +6 dB preamp（纯 RG 目标偏安静）
 * - clamp 到 [0.1, 1.0]：无法把过静曲放大超过系统满幅
 */
object LoudnessCalculator {

    const val PLAYBACK_VOLUME_MIN = 0.1
    const val PLAYBACK_VOLUME_MAX = 1.0

    /** 响度均衡开启时叠加的 preamp（dB），#51 听感补偿 */
    const val LOUDNESS_PREAMP_DB = 6.0

    /** 合理 track gain 范围（dB）；超出视为非法或需 Q7.8 换算 */
    private const val REPLAY_GAIN_DB_ABS_MAX = 30.0

    /**
     * 将数值规范为 track gain dB。
     * 常规 RG 已是 dB；|value| 超出 ±30 时按 Q7.8 整数 ÷256 再校验；
     * 无法落入合理区间返回 null（禁止写假增益）。
     */
    fun normalizeReplayGainDbValue(value: Double): Double? {
        if (value.isNaN() || value.isInfinite()) return null
        if (abs(value) <= REPLAY_GAIN_DB_ABS_MAX) return value
        val asQ78 = value / 256.0
        if (!asQ78.isNaN() && !asQ78.isInfinite() && abs(asQ78) <= REPLAY_GAIN_DB_ABS_MAX) {
            return asQ78
        }
        return null
    }

    /**
     * 计算播放音量系数。
     * @param trackGainDb 库内 ReplayGain（null = 无标签）
     * @param enabled 响度均衡开关；关闭或无标签一律 1.0
     */
    fun dbToPlaybackVolume(trackGainDb: Double?, enabled: Boolean): Double {
        if (!enabled || trackGainDb == null) return PLAYBACK_VOLUME_MAX
        val linear = 10.0.pow((trackGainDb + LOUDNESS_PREAMP_DB) / 20.0)
        return linear.coerceIn(PLAYBACK_VOLUME_MIN, PLAYBACK_VOLUME_MAX)
    }
}

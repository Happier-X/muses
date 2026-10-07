package com.muses.player.core.model

/**
 * 播放器内部的音量补偿参数。
 *
 * 默认保留音源的原始幅度，不增加固定响度补偿，与参考播放器的默认输出一致。
 * 零增益不会创建系统音效，不读取旧的用户增益设置。
 */

/** 默认额外增益（dB）：0 表示原始幅度输出。 */
const val PLAYBACK_VOLUME_COMPENSATION_DB: Int = 0

/** 上限档位（dB）：再高易被效果器限幅，听感发闷。 */
const val MAX_VOLUME_BOOST_DB: Int = 12

/** dB → 毫贝（LoudnessEnhancer.setTargetGain 的单位），并钳制到档位上限。 */
fun volumeBoostMillibels(db: Int): Int = db.coerceIn(0, MAX_VOLUME_BOOST_DB) * 100

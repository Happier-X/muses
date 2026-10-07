package com.muses.player.core.model

/**
 * 播放音量增益（预放大）档位。
 *
 * 背景：Muses 走 ExoPlayer 原样直出（player volume 恒 1.0），不做任何响度提升；
 * 主流音乐 App 普遍带响度增强/音效增益，同一首歌在系统音量相同时听起来更响，
 * 因此提供应用级增益（安卓经 LoudnessEnhancer 施加，自带限幅）。
 */

/** 可选档位（dB），0 = 关闭。 */
val VOLUME_BOOST_STEPS_DB: List<Int> = listOf(0, 3, 6, 9, 12)

/** 默认档位（dB）：原生直出偏小，默认 +6 dB。 */
const val DEFAULT_VOLUME_BOOST_DB: Int = 6

/** 上限档位（dB）：再高易被效果器限幅，听感发闷。 */
const val MAX_VOLUME_BOOST_DB: Int = 12

/** dB → 毫贝（LoudnessEnhancer.setTargetGain 的单位），并钳制到档位上限。 */
fun volumeBoostMillibels(db: Int): Int = db.coerceIn(0, MAX_VOLUME_BOOST_DB) * 100

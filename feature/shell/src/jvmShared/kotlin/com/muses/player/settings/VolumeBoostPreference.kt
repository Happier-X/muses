package com.muses.player.settings

import androidx.compose.runtime.Composable

/** 播放音量增益设置行（安卓经 LoudnessEnhancer 生效；桌面暂未接线）。 */
@Composable
expect fun VolumeBoostPreference()

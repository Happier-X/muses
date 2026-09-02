package com.muses.player.feature.player.lyric

import androidx.compose.runtime.Composable

@Composable
fun rememberBilibiliLyricOffset(mediaId: String?): Int = 0

fun effectiveBilibiliLyricAdvance(globalAdvanceMs: Int, trackOffsetMs: Int): Long = 0L

fun isBilibiliMediaId(mediaId: String?): Boolean = false

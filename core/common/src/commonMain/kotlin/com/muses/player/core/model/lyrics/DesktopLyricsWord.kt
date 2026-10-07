package com.muses.player.core.model.lyrics

/** 原文中的字符范围和真实逐字时间轴；结束偏移不包含在范围内。 */
data class DesktopLyricsWord(
    val startOffset: Int,
    val endOffset: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
) {
    fun progressAt(positionMs: Long): Float = when {
        positionMs < startTimeMs -> 0f
        endTimeMs <= startTimeMs -> 1f
        else -> ((positionMs - startTimeMs).toDouble() / (endTimeMs - startTimeMs)).toFloat().coerceIn(0f, 1f)
    }
}

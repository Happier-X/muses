package com.muses.player.feature.player.lyric

import com.muses.player.core.lyrics.model.LyricLine
import kotlin.math.max

internal fun sourceLyricInterludes(lines: List<LyricLine>): List<LyricInterlude> = buildList {
    lines.forEachIndexed { index, line ->
        val start = if (index == 0) {
            0L
        } else {
            val previous = lines[index - 1]
            max(
                previous.timeMs + (previous.durationMs ?: 0L),
                previous.syllables.maxOfOrNull { it.endTimeMs } ?: previous.timeMs,
            )
        }
        val countdownEnd = (line.timeMs - 250L).coerceAtLeast(start)
        if (countdownEnd - start >= 4_000L) {
            add(LyricInterlude(start, countdownEnd, line.timeMs, index))
        }
    }
}

internal fun sourceLineActivationTimeMs(line: LyricLine): Long =
    line.syllables.minOfOrNull { it.startTimeMs } ?: line.timeMs

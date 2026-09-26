package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * AMLL 纵向滚动规则的 Compose 移植。
 * 对照版本：86200dead453bb067e554e989110cbadca8d4756。
 * 来源及许可见 docs/歌词滚动移植.md。
 */
internal object AmllScrollPhysics {
    fun positionSpring(
        intervalMs: Long?,
        seeking: Boolean = false,
        interlude: Boolean = false,
        ended: Boolean = false,
    ): SpringSpec<Float> {
        val stiffness: Float
        val damping: Float
        when {
            seeking || interlude -> {
                stiffness = 90f
                damping = 15f
            }
            ended -> {
                stiffness = 140f
                damping = 22f
            }
            intervalMs == null -> {
                stiffness = 90f
                damping = 15f
            }
            else -> {
                val ratio = (1f - (intervalMs.coerceIn(100L, 800L) - 100L) / 700f).pow(.2f)
                stiffness = 170f + ratio * 50f
                damping = sqrt(stiffness) * 2.2f
            }
        }
        // 上游求解器把过阻尼分支按临界阻尼求解，不能直接传 1.1 给 Compose。
        return spring(
            dampingRatio = (damping / (2f * sqrt(stiffness))).coerceAtMost(1f),
            stiffness = stiffness,
            visibilityThreshold = .01f,
        )
    }

    val scaleSpring: SpringSpec<Float> = spring(
        dampingRatio = .5f,
        stiffness = 100f,
        visibilityThreshold = .0001f,
    )

    /** 只给目标位置仍在屏幕内或下方的行累计延迟；焦点之后逐级除以 1.05。 */
    fun staggerDelays(
        rowBottoms: List<Float>,
        firstIndex: Int,
        focusIndex: Int,
        enabled: Boolean,
    ): List<Long> {
        var delay = 0f
        var step = if (enabled) 50f else 0f
        return rowBottoms.mapIndexed { offset, bottom ->
            val result = delay.toLong()
            if (bottom >= 0f) {
                delay += step
                if (firstIndex + offset >= focusIndex) step /= 1.05f
            }
            result
        }
    }

    fun blur(index: Int, focusIndex: Int, lastActiveIndex: Int, active: Boolean, browsing: Boolean, narrow: Boolean): Float {
        if (active || browsing) return 0f
        val distance = if (index < focusIndex) focusIndex - index + 1 else kotlin.math.abs(index - lastActiveIndex)
        return ((1 + distance) * if (narrow) .8f else 1f).coerceAtMost(5f)
    }

    fun focusTop(viewportHeight: Int, rowHeight: Float, position: Float): Float =
        viewportHeight * position - rowHeight * .5f
}

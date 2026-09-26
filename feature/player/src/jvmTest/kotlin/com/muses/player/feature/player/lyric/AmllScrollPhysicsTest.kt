package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.FloatSpringSpec
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmllScrollPhysicsTest {
    @Test
    fun 正常播放轨迹与上游临界阻尼解一致() {
        val spec = AmllScrollPhysics.positionSpring(500)
        val animation = FloatSpringSpec(spec.dampingRatio, spec.stiffness, .01f)
        val omega = sqrt(spec.stiffness)
        for (millis in listOf(16, 50, 100, 250, 500, 1000)) {
            val t = millis / 1000f
            val expected = 120f - (120f + t * omega * 120f) * exp(-omega * t)
            assertEquals(expected, animation.getValueFromNanos(millis * 1_000_000L, 0f, 120f, 0f), .002f)
        }
    }

    @Test
    fun 快歌提高刚度且普通切行不回弹() {
        val fast = AmllScrollPhysics.positionSpring(100)
        val slow = AmllScrollPhysics.positionSpring(800)
        assertEquals(220f, fast.stiffness)
        assertEquals(170f, slow.stiffness)
        assertEquals(1f, fast.dampingRatio)
        assertEquals(1f, slow.dampingRatio)
        assertEquals(fast.stiffness, AmllScrollPhysics.positionSpring(0).stiffness)
        assertEquals(slow.stiffness, AmllScrollPhysics.positionSpring(9000).stiffness)
    }

    @Test
    fun 连续切行保留像素速度而非重新从零加速() {
        val spec = AmllScrollPhysics.positionSpring(350)
        val animation = FloatSpringSpec(spec.dampingRatio, spec.stiffness, .01f)
        val time = 100_000_000L
        val position = animation.getValueFromNanos(time, 0f, 100f, 0f)
        val velocity = animation.getVelocityFromNanos(time, 0f, 100f, 0f)
        val continued = animation.getValueFromNanos(16_000_000L, position, 200f, velocity)
        val restarted = animation.getValueFromNanos(16_000_000L, position, 200f, 0f)
        assertTrue(continued > restarted)
        assertEquals(velocity, animation.getVelocityFromNanos(0L, position, 200f, velocity), .001f)
    }

    @Test
    fun 跳转间奏首句使用相同慢速弹簧() {
        val seeking = AmllScrollPhysics.positionSpring(100, seeking = true)
        assertEquals(90f, seeking.stiffness)
        assertEquals(15f / (2f * sqrt(90f)), seeking.dampingRatio)
        assertEquals(seeking.stiffness, AmllScrollPhysics.positionSpring(100, interlude = true).stiffness)
        assertEquals(seeking.stiffness, AmllScrollPhysics.positionSpring(null).stiffness)
        assertEquals(140f, AmllScrollPhysics.positionSpring(100, ended = true).stiffness)
    }

    @Test
    fun 屏外行不累计延迟且焦点后延迟逐级递减() {
        assertEquals(
            listOf(0L, 0L, 50L, 100L, 147L, 192L),
            AmllScrollPhysics.staggerDelays(listOf(-10f, 40f, 100f, 160f, 220f, 280f), 0, 2, true),
        )
        assertEquals(listOf(0L, 0L, 0L), AmllScrollPhysics.staggerDelays(listOf(10f, 70f, 130f), 4, 5, false))
    }

    @Test
    fun 多行歌词以行块中心对齐百分之三十五高度() {
        assertEquals(290f, AmllScrollPhysics.focusTop(1000, 120f, .35f))
        assertEquals(230f, AmllScrollPhysics.focusTop(1000, 240f, .35f))
    }

    @Test
    fun 浏览去模糊且合唱行都清晰() {
        assertEquals(0f, AmllScrollPhysics.blur(8, 4, 5, false, true, true))
        assertEquals(0f, AmllScrollPhysics.blur(5, 4, 5, true, false, true))
        assertEquals(2.4f, AmllScrollPhysics.blur(3, 4, 5, false, false, true))
        assertEquals(1.6f, AmllScrollPhysics.blur(6, 4, 5, false, false, true))
        assertEquals(5f, AmllScrollPhysics.blur(20, 4, 5, false, false, true))
    }
}

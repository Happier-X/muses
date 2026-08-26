package com.muses.player.core.media.loudness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LoudnessCalculatorTest {

    @Test
    fun `normalize keeps regular dB values within range`() {
        assertEquals(-6.54, LoudnessCalculator.normalizeReplayGainDbValue(-6.54)!!, 1e-9)
        assertEquals(3.0, LoudnessCalculator.normalizeReplayGainDbValue(3.0)!!, 1e-9)
        assertEquals(30.0, LoudnessCalculator.normalizeReplayGainDbValue(30.0)!!, 1e-9)
        assertEquals(-30.0, LoudnessCalculator.normalizeReplayGainDbValue(-30.0)!!, 1e-9)
    }

    @Test
    fun `normalize converts Q7_8 integers by dividing 256`() {
        // Opus R128_TRACK_GAIN 典型值：Q7.8 整数
        val result = LoudnessCalculator.normalizeReplayGainDbValue(1024.0)!!
        assertEquals(4.0, result, 1e-9)
        val negative = LoudnessCalculator.normalizeReplayGainDbValue(-1536.0)!!
        assertEquals(-6.0, negative, 1e-9)
    }

    @Test
    fun `normalize converts borderline values through Q7_8 fallback`() {
        // 超出 ±30 但 ÷256 后落在区间内：按 Q7.8 解释（Web 层同语义）
        assertEquals(0.12109375, LoudnessCalculator.normalizeReplayGainDbValue(31.0)!!, 1e-9)
        assertEquals(-0.12109375, LoudnessCalculator.normalizeReplayGainDbValue(-31.0)!!, 1e-9)
    }

    @Test
    fun `normalize rejects values that stay out of range after Q7_8 conversion`() {
        assertNull(LoudnessCalculator.normalizeReplayGainDbValue(100000.0))
        assertNull(LoudnessCalculator.normalizeReplayGainDbValue(-999999.0))
        assertNull(LoudnessCalculator.normalizeReplayGainDbValue(Double.NaN))
        assertNull(LoudnessCalculator.normalizeReplayGainDbValue(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `volume is 1_0 when disabled or gain missing`() {
        assertEquals(1.0, LoudnessCalculator.dbToPlaybackVolume(null, enabled = true), 1e-9)
        assertEquals(1.0, LoudnessCalculator.dbToPlaybackVolume(-6.54, enabled = false), 1e-9)
        assertEquals(1.0, LoudnessCalculator.dbToPlaybackVolume(null, enabled = false), 1e-9)
    }

    @Test
    fun `volume applies preamp and clamps to bounds when enabled`() {
        // -6 dB gain + 6 dB preamp = 0 dB → 线性 1.0（不 clamp）
        assertEquals(1.0, LoudnessCalculator.dbToPlaybackVolume(-6.0, enabled = true), 1e-9)

        // -12 dB + 6 = -6 dB → 10^(-6/20) ≈ 0.501，在区间内不 clamp
        val quiet = LoudnessCalculator.dbToPlaybackVolume(-12.0, enabled = true)
        assertEquals(0.501187, quiet, 1e-5)

        // 正增益 + preamp 超过 0 dB → 被 clamp 到上限 1.0（无法放大超过满幅）
        assertEquals(1.0, LoudnessCalculator.dbToPlaybackVolume(+3.0, enabled = true), 1e-9)

        // 极端负增益 → 被 clamp 到下限 0.1
        val floor = LoudnessCalculator.dbToPlaybackVolume(-40.0, enabled = true)
        assertTrue(abs(floor - LoudnessCalculator.PLAYBACK_VOLUME_MIN) < 1e-9)
    }
}

package com.muses.player.feature.player.backdrop

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FlowFrameTest {
    @Test
    fun pureCoverKeepsItsColorAtEveryPhase() {
        val cover = CoverPixels(IntArray(16) { 0xFF336699.toInt() }, 4, 4)
        val first = renderFlowFrame(cover, 320, 640, 0.0)
        val later = renderFlowFrame(cover, 320, 640, 65.0)

        assertContentEquals(first.pixels, later.pixels)
        assertEquals(1, first.pixels.toSet().size)
    }

    @Test
    fun differentCoverRegionsMoveOverTime() {
        val cover = CoverPixels(IntArray(64) { index ->
            (0xFF000000 or ((index * 3).toLong() shl 16) or ((index * 2).toLong() shl 8) or index.toLong()).toInt()
        }, 8, 8)

        val first = renderFlowFrame(cover, 640, 960, 0.0)
        val later = renderFlowFrame(cover, 640, 960, 23.0)

        assertNotEquals(first.pixels.toList(), later.pixels.toList())
    }

    @Test
    fun outputIsBoundedEvenOnLargeScreens() {
        val cover = CoverPixels(intArrayOf(0xFF112233.toInt()), 1, 1)
        val frame = renderFlowFrame(cover, 8000, 8000, 0.0)

        assertEquals(160, frame.width)
        assertEquals(240, frame.height)
        assertEquals(frame.width * frame.height, frame.pixels.size)
    }

    @Test
    fun lightCoverDoesNotWashOutTheBackground() {
        val cover = CoverPixels(IntArray(16) { 0xFFFFFFFF.toInt() }, 4, 4)
        val frame = renderFlowFrame(cover, 320, 640, 0.0)

        assertEquals(158, (frame.pixels.first() shr 16) and 255)
        assertEquals(158, (frame.pixels.first() shr 8) and 255)
        assertEquals(158, frame.pixels.first() and 255)
    }
}

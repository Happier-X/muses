package com.muses.player.feature.player.backdrop

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverContentColorTest {
    @Test
    fun missingArtworkUsesWhite() {
        assertEquals(Color.White, coverContentColor(null))
    }

    @Test
    fun picksCoverHueButKeepsTextBright() {
        val cover = CoverPixels(IntArray(64) { 0xFFDA4830.toInt() }, 8, 8)
        val color = coverContentColor(cover)
        assertTrue(color.red > color.green)
        assertTrue(color.green > color.blue)
        assertTrue(color.blue >= 0.78f)
    }

    @Test
    fun ignoresTransparentAndNearlyBlackPixels() {
        val pixels = IntArray(64) { index -> if (index % 3 == 0) 0x00FF0000 else 0xFF2040B0.toInt() }
        val color = coverContentColor(CoverPixels(pixels, 8, 8))
        assertTrue(color.blue > color.red)
    }
}

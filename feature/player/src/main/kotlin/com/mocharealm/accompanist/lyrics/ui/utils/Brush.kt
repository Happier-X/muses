/*
 * Vendored third-party source - muses
 *
 * Origin : Accompanist lyrics-ui 1.0.19 (AMLL official Compose implementation)
 * Repo   : https://github.com/6xingyv/accompanist-lyrics-ui
 * License: Apache License 2.0  (https://www.apache.org/licenses/LICENSE-2.0)
 * Author : The Accompanist / MochaRealm Authors
 *
 * The package name is intentionally kept as `com.mocharealm.accompanist.*` so the
 * code can be diffed against upstream. Local modifications:
 *  - expect/actual (Kotlin Multiplatform) collapsed into plain Kotlin for this
 *    single-target Android module: Char.isCjk / isArabic / isDevanagari now live
 *    directly in utils/String.kt
 *  - com.mocharealm.gaze.capsule.ContinuousRoundedRectangle replaced with
 *    androidx.compose.foundation.shape.RoundedCornerShape, dropping the
 *    gaze-capsule dependency (only used to clip a line item)
 */

package com.mocharealm.accompanist.lyrics.ui.utils

import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp

val LayerPaint = Paint()

fun Brush.Companion.easedHorizontalGradient(
    vararg colorStops: Pair<Float, Color>,
    easing: Easing = EaseInQuart,
    endX: Float = 1f,
    steps: Int = 100,
): Brush {
    // Edge cases
    if (colorStops.isEmpty()) {
        return SolidColor(Color.Transparent)
    }
    if (colorStops.size == 1) {
        return SolidColor(colorStops[0].second)
    }
    val sortedStops = colorStops.sortedBy { it.first }

    val finalFineGrainedStops = mutableListOf<Pair<Float, Color>>()

    for (i in 0 until sortedStops.size - 1) {
        val startStop = sortedStops[i]
        val endStop = sortedStops[i + 1]

        val startFraction = startStop.first
        val endFraction = endStop.first
        val startColor = startStop.second
        val endColor = endStop.second

        for (j in 0..steps) {
            val localLinearProgress = j.toFloat() / steps

            val localEasedProgress = easing.transform(localLinearProgress)

            val globalFraction = lerp(startFraction, endFraction, localLinearProgress)

            val stopColor = lerp(startColor, endColor, localEasedProgress)

            finalFineGrainedStops.add(globalFraction to stopColor)
        }
    }
    finalFineGrainedStops.add(sortedStops.last())


    return Brush.horizontalGradient(colorStops = finalFineGrainedStops.toTypedArray(), endX = endX)
}
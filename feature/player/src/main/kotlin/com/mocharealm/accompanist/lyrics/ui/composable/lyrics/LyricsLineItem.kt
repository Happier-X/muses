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

package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun LyricsLineItem(
    isFocused: Boolean,
    isRightAligned: Boolean,
    onLineClicked: () -> Unit,
    onLinePressed: () -> Unit,
    blurRadius: () -> Float,
    modifier: Modifier = Modifier,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.4f,
    blendMode: BlendMode = BlendMode.SrcOver,
    isInteractive: Boolean = true,
    content: @Composable () -> Unit
) {
    val scaleState by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.98f,
        animationSpec = if (isFocused) {
            tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = EaseInOut)
        },
        label = "scale"
    )

    val alphaState by animateFloatAsState(
        targetValue = if (isFocused) activeAlpha else inactiveAlpha,
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scaleState
                scaleY = scaleState
                alpha = alphaState
                transformOrigin = TransformOrigin(if (isRightAligned) 1f else 0f, 1f)
                this.blendMode = blendMode
                compositingStrategy = CompositingStrategy.Offscreen

                val radius = blurRadius()
                if (radius > 0f) {
                    renderEffect = BlurEffect(
                        radiusX = radius,
                        radiusY = radius,
                        edgeTreatment = TileMode.Clamp
                    )
                }
            }
            .then(
                if (isInteractive) Modifier.clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = onLineClicked,
                        onLongClick = onLinePressed
                    )
                else Modifier
            )

    ) {
        content()
    }
}

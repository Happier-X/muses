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

package com.mocharealm.accompanist.lyrics.ui.utils.modifier

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

// 本地化改动：上游为 JetBrains Compose（KMP）的 ExperimentalAnimatableApi 注解；
// AndroidX Compose 1.12 起 DeferredTargetAnimation 已稳定、该注解被移除，故此处不再 opt-in
class SpringPlacementModifierNode(
    var lookaheadScope: LookaheadScope,
    var itemKey: Any,
    var isManualScrolling: Boolean,
    var stiffness: Float
) : ApproachLayoutModifierNode, Modifier.Node() {
    private var offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)

    private var isFirstFrame = true

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        val target = with(lookaheadScope) {
            lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
        }
        // 本地改动：原实现非手动滚动时用 spring 回位。列表顶部目标不可达时，
        // lookahead 理论位置与实际位置出现偏差，弹簧把行从上方弹下来（"掉下来"观感）。
        // 列表滚动本身已是平滑动画，行内再叠弹簧属于双重动画，统一改为 snap 直接跟随。
        offsetAnimation.updateTarget(target, coroutineScope, snap())
        return !offsetAnimation.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val coordinates = coordinates
            if (coordinates != null) {
                val target = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
                }

                val animatedOffset = offsetAnimation.updateTarget(
                    target,
                    coroutineScope,
                    snap()
                )

                isFirstFrame = false

                val placementOffset = with(lookaheadScope) {
                    lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
                }

                val delta = animatedOffset - placementOffset
                placeable.place(delta.x, delta.y)
            } else {
                placeable.place(0, 0)
            }
        }
    }

    fun updateState(newScope: LookaheadScope, newKey: Any, newIsManualScrolling: Boolean, newStiffness: Float) {
        lookaheadScope = newScope
        isManualScrolling = newIsManualScrolling
        stiffness = newStiffness
        if (itemKey != newKey) {
            itemKey = newKey
            offsetAnimation = DeferredTargetAnimation(IntOffset.VectorConverter)
            isFirstFrame = true
        }
    }
}

data class SpringPlacementNodeElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val stiffness: Float
) : ModifierNodeElement<SpringPlacementModifierNode>() {
    override fun update(node: SpringPlacementModifierNode) {
        node.updateState(lookaheadScope, itemKey, isManualScrolling, stiffness)
    }
    override fun create(): SpringPlacementModifierNode =
        SpringPlacementModifierNode(lookaheadScope, itemKey, isManualScrolling, stiffness)
}

fun Modifier.springPlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float
) = this.then(SpringPlacementNodeElement(lookaheadScope, itemKey, isManualScrolling, stiffness))
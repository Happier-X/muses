package com.muses.player.feature.player.lyric

/*
 * 手搓复刻 — Apple Music 沉浸式歌词弹簧
 * 思想与参数对齐 AMLL 官方 SpringPlacementModifier（stiffness 170..220, dampingRatio 1.1 过阻尼，200ms 回位无振荡）
 * 来源：feature/player/src/main/kotlin/com/mocharealm/accompanist/lyrics/ui/utils/modifier/SpringPlacementModifier.kt
 * 区别：独立文件，包名归属 feature:player，不依赖 lyrics-ui 渲染层，仅保留弹簧物理
 * 许可：原文件 Apache 2.0，此手搓版本同样遵循 Apache 2.0
 */

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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

class AppleSpringPlacementNode(
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
        // Apple-like 过阻尼：dampingRatio 1.1, stiffness 170..220，按距离动态
        offsetAnimation.updateTarget(
            target,
            coroutineScope,
            if (isFirstFrame || isManualScrolling) snap() else spring(dampingRatio = 1.1f, stiffness = stiffness)
        )
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
                    if (isFirstFrame || isManualScrolling) snap() else spring(dampingRatio = 1.1f, stiffness = stiffness)
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

data class AppleSpringPlacementElement(
    val lookaheadScope: LookaheadScope,
    val itemKey: Any,
    val isManualScrolling: Boolean,
    val stiffness: Float
) : ModifierNodeElement<AppleSpringPlacementNode>() {
    override fun update(node: AppleSpringPlacementNode) {
        node.updateState(lookaheadScope, itemKey, isManualScrolling, stiffness)
    }
    override fun create(): AppleSpringPlacementNode =
        AppleSpringPlacementNode(lookaheadScope, itemKey, isManualScrolling, stiffness)
}

fun Modifier.appleSpringPlacement(
    lookaheadScope: LookaheadScope,
    itemKey: Any,
    isManualScrolling: Boolean,
    stiffness: Float
) = this.then(AppleSpringPlacementElement(lookaheadScope, itemKey, isManualScrolling, stiffness))

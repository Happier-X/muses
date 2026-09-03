package com.muses.player.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 手写 fill 风格播放控制图标，几何形状与 Lucide 保持一致。
 * Lucide 图标集不提供 fill 变体，因此需要手动定义实心填充版本。
 */
object FillIcons {

    /**
     * 实心播放按钮：右指等边三角形（对齐 Lucide Play 几何）
     * Lucide Play: M5 3l14 9-14 9V3z
     * Fill 版本：同样的三角形路径，stroke 改为 fill
     */
    val Play: ImageVector = ImageVector.Builder(
        name = "FillPlay",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 3f)
            lineTo(19f, 12f)
            lineTo(5f, 21f)
            close()
        }
    }.build()

    /**
     * 实心暂停按钮：两根竖线（对齐 Lucide Pause 几何）
     * Lucide Pause: M6 4h4v16H6zm8 0h4v16h-4z
     * Fill 版本：同样的两个矩形
     */
    val Pause: ImageVector = ImageVector.Builder(
        name = "FillPause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // 左竖线
            moveTo(6f, 4f)
            horizontalLineTo(10f)
            verticalLineTo(20f)
            horizontalLineTo(6f)
            close()
            // 右竖线
            moveTo(14f, 4f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            horizontalLineTo(14f)
            close()
        }
    }.build()

    /**
     * 上一曲：左指三角形 + 左侧竖线（对齐 Lucide SkipBack 几何）
     * Lucide SkipBack: M19 20 9 12l10-8v16zm-7-8V4H6v16h6z
     * Fill 版本
     */
    val SkipBack: ImageVector = ImageVector.Builder(
        name = "FillSkipBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // 左侧竖条
            moveTo(6f, 4f)
            horizontalLineTo(12f)
            verticalLineTo(20f)
            horizontalLineTo(6f)
            close()
            // 右侧三角形
            moveTo(19f, 4f)
            lineTo(9f, 12f)
            lineTo(19f, 20f)
            close()
        }
    }.build()

    /**
     * 下一曲：右指三角形 + 右侧竖线（对齐 Lucide SkipForward 几何）
     * Lucide SkipForward: M5 4l10 8-10 8V4zm7 8V4h6v16h-6z
     * Fill 版本
     */
    val SkipForward: ImageVector = ImageVector.Builder(
        name = "FillSkipForward",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // 左侧三角形
            moveTo(5f, 4f)
            lineTo(15f, 12f)
            lineTo(5f, 20f)
            close()
            // 右侧竖条
            moveTo(12f, 4f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            horizontalLineTo(12f)
            close()
        }
    }.build()
}

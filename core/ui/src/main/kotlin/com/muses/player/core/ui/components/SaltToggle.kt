package com.muses.player.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltDarkColors

/**
 * `.m-toggle` —— iOS 风格开关（MToggle.vue 一比一翻译）。
 *
 * 视觉契约：
 * - 轨道：51×31px，圆角 15.5px，未开启 = --m-surface-3，开启 = --m-primary；
 * - 滑块：27×27px，白色圆形，margin 2px（轨道内居中偏左/右）；
 * - 开启时滑块偏移 20px（51 - 27 - 2*2 = 20）；
 * - 滑块动画：spring(dampingRatio=0.7, stiffness=400)；
 * - disabled：opacity 0.4。
 */
@Composable
fun SaltToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val salt = LocalSaltColors.current
    val isDark = salt === SaltDarkColors

    // 轨道尺寸
    val trackWidth = 51.dp
    val trackHeight = 31.dp
    val thumbSize = 27.dp
    val thumbMargin = 2.dp
    val thumbOffset = 20.dp // trackWidth - thumbSize - thumbMargin * 2

    val thumbOffsetDp by animateFloatAsState(
        targetValue = if (checked) thumbOffset.value else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "m-toggle-thumb-offset",
    )

    val trackColor = if (checked) salt.primary else salt.surface3

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(15.5.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbMargin + thumbOffsetDp.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    if (isDark) {
                        // 暗色模式滑块稍暗
                        androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                    } else {
                        androidx.compose.ui.graphics.Color.White
                    },
                ),
        )
    }
}

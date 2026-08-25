package com.muses.player.nativem1.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 触控区/图标尺寸档位 —— 对照 MIconButton.vue 的三档规格：
 * - md：40px 触控区（图标 20px）
 * - sm：36px 触控区（图标 18px）
 * - lg：48px 触控区（图标 28px，播放页主控用）
 */
enum class SaltIconButtonSize(val touchSize: Dp, val iconSize: Dp) {
    SM(36.dp, 18.dp),
    MD(40.dp, 20.dp),
    LG(48.dp, 28.dp),
}

/**
 * `.m-icon-button` —— 统一图标按钮（MIconButton.vue 一比一翻译）。
 *
 * 视觉契约（源码注释逐条保留）：
 * - 透明底，无背景圆底、无缩放、无涟漪；
 * - 点击反馈 = **图标按下变暗约 15%**（opacity 255→230 ≈ 0.85 alpha，
 *   `transition: opacity 0.15s ease`）；
 * - disabled：整体 opacity 0.4（Web `&:disabled { opacity: .4 }`，
 *   且 pointer-events:none 不触发按压态）；
 * - 图标色继承调用方文字色（Web `color: inherit` → 默认取 [LocalContentColor]，
 *   可用 [tint] 显式覆盖）。
 */
@Composable
fun SaltIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: SaltIconButtonSize = SaltIconButtonSize.MD,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    /** 覆盖档位默认图标尺寸（如 MiniPlayer 用 md 触控区 + 18px 图标） */
    iconSizeOverride: Dp? = null,
) {
    SaltIconButton(
        onClick = onClick,
        modifier = modifier,
        size = size,
        enabled = enabled,
        contentDescription = contentDescription,
        icon = {
            Icon(
                imageVector = imageVector,
                contentDescription = null, // 无障碍语义挂在按钮容器上
                tint = tint,
                modifier = Modifier.size(iconSizeOverride ?: size.iconSize),
            )
        },
    )
}

/**
 * `.m-icon-button` 插槽形态：icon lambda 自由绘制（对照 `<slot />`）。
 * 按压变暗作用于整个插槽内容（对应 `&:active &__icon { opacity: .85 }`）。
 */
@Composable
fun SaltIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: SaltIconButtonSize = SaltIconButtonSize.MD,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // transition: opacity 0.15s ease（作用在 __icon 上）
    val iconAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.85f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "m-icon-button-icon-alpha",
    )

    Box(
        modifier = modifier
            .size(size.touchSize)
            // 无涟漪：Web 用 -webkit-tap-highlight-color: transparent + 自绘按压态
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // __icon 层：disabled 整体 0.4；enabled 按压时 0.85（二者不会叠加，
        // 与 Web disabled 时 pointer-events:none 不触发 :active 的行为一致）
        val combinedAlpha = if (enabled) iconAlpha else 0.4f
        Box(
            modifier = Modifier.alpha(combinedAlpha),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

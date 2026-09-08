package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

/**
 * 触控区/图标尺寸档位（沿用原三档规格）：
 * - md：40px 触控区（图标 20px）；sm：36px/18px；lg：48px/28px（播放页主控用）
 */
enum class MusesIconButtonSize(val touchSize: Dp, val iconSize: Dp) {
    SM(36.dp, 18.dp),
    MD(40.dp, 20.dp),
    LG(48.dp, 28.dp),
}

/**
 * 统一图标按钮（miuix IconButton；透明底、无涟漪，按压反馈由 miuix 承担）。
 *
 * 图标色继承调用方文字色（默认取 [LocalContentColor]，可用 [tint] 显式覆盖）；
 * disabled 整体置灰由 miuix 处理。
 */
@Composable
fun MusesIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: MusesIconButtonSize = MusesIconButtonSize.MD,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    /** 覆盖档位默认图标尺寸（如 MiniPlayer 用 md 触控区 + 18px 图标） */
    iconSizeOverride: Dp? = null,
) {
    MusesIconButton(
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
 * 插槽形态：icon lambda 自由绘制。
 */
@Composable
fun MusesIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: MusesIconButtonSize = MusesIconButtonSize.MD,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: @Composable () -> Unit,
) {
    MiuixIconButton(
        onClick = onClick,
        modifier = modifier
            .size(size.touchSize)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        enabled = enabled,
        minWidth = size.touchSize,
        minHeight = size.touchSize,
        content = {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        },
    )
}

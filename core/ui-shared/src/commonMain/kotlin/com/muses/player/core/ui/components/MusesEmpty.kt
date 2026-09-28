package com.muses.player.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.muses.player.core.ui.icons.TablerIcons

/**
 * 使用 Miuix 基础组件与主题样式组合的空状态。Miuix 没有专用空状态组件，
 * 因此沿用其 Text、Icon、主题文字样式和语义颜色；图标置于轻量描边容器内。
 * 在有界容器内水平、垂直居中；宽度占满可用空间，保证宽屏布局中内容仍相对容器居中。
 */
@Composable
fun MusesEmpty(
    title: String = "空空如也~",
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    bottomInset: Dp = 0.dp,
) {
    val scheme = MiuixTheme.colorScheme

    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(y = -(bottomInset / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .border(
                        width = 1.dp,
                        color = scheme.onBackgroundVariant.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon ?: TablerIcons.Inbox,
                    contentDescription = null,
                    tint = scheme.onBackgroundVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = title,
                style = MiuixTheme.textStyles.main,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MiuixTheme.textStyles.body2,
                    color = scheme.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

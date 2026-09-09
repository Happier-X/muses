package com.muses.player.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 列表行（miuix 令牌 + 标准 Row：保证 LazyColumn 滚动正常）。
 *
 * - `combinedClickable` 仅在 Row 上（内容区），Row 之外的 Column 无 clickable 修饰，
 *   不会拦截 LazyColumn 的垂直拖拽手势；
 * - 长按与点击共存（combinedClickable 语义）；
 * - 行高 56dp 保底 + horizontalDivider；after 槽可放 IconButton（内嵌 clickable 不影响外层滚动）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusesListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    dividers: Boolean = true,
    chevron: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    after: @Composable RowScope.() -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val scheme = MiuixTheme.colorScheme

    Column(modifier = modifier) {
        // clickable 仅挂 Row：LazyColumn 通过 Column 子树外的拖拽检测手势滚动
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = { onClick?.invoke() },
                            onLongClick = onLongClick,
                        )
                    } else Modifier,
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) leading()
            Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                Row(
                    modifier = Modifier.heightIn(min = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.main,
                        lineHeight = (17f * 1.35f).sp,
                        color = scheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        after()
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.footnote1,
                        lineHeight = (13f * 1.35f).sp,
                        color = scheme.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (chevron) {
                Icon(
                    imageVector = TablerIcons.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp).width(16.dp),
                )
            }
        }
        if (dividers) HorizontalDivider()
    }
}

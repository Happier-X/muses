package com.muses.player.nativem1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
// 注：icons-extended 1.7.8 无 automirrored 版 ChevronRight，退回 Filled（LTR 下视觉一致）
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.nativem1.theme.LocalSaltColors
import com.muses.player.nativem1.theme.SaltSpacing

/**
 * `.m-list-item` —— 列表行（MListItem.vue 一比一翻译；MList 容器在 Compose 中
 * 由页面直接用 LazyColumn/Column + 行分隔线表达，容器底色走页面背景）。
 *
 * 视觉契约：
 * - `min-height: var(--m-list-row-h)`(56dp)；`padding-left: 16px(+safe-left)`；
 * - `__inner`：`padding: 12px 16px(safe-right) 12px 0`，标题区 `min-height: 28px`；
 * - 标题 17px / line-height 1.35，单行省略（min-width:0 收缩）；
 * - 副标题 13px / `--m-text-2` / margin-top 2px；
 * - after 右侧插槽：`flex-shrink:0; padding-left:4px; gap:4px`；
 * - 分隔线：底部物理 1px `--m-hairline`，左缩进 16px（`::after` + scaleY(1/dpr)）；
 * - link 按压态：浅色 rgba(0,0,0,.1) / 深色 rgba(255,255,255,.1)（无涟漪，
 *   Web 用 :active 背景色）；
 * - chevron：16px 右箭头 opacity .2 margin-left 12 margin-right 4。
 */
@Composable
fun SaltListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** 点击回调；null = 非 link 行（无按压态、不可点） */
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    dividers: Boolean = true,
    chevron: Boolean = false,
    strongTitle: Boolean = false,
    /** 左侧 media 插槽（封面等），不额外加间距 —— Web 由调用方控制间距 */
    leading: (@Composable () -> Unit)? = null,
    /** 右侧 after 插槽（时长/按钮等） */
    after: @Composable RowScope.() -> Unit = {},
) {
    val salt = LocalSaltColors.current
    val isDark = salt === com.muses.player.nativem1.theme.SaltDarkColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isLink = onClick != null

    val pressBackground = if (isLink && pressed) {
        // :active 背景高亮（明暗两套配方）
        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = modifier
            .heightIn(min = SaltSpacing.listRowHeight)
            .fillMaxWidth()
            // --link 按压态背景（画在最底层，内容之上无遮挡）
            .background(pressBackground)
            .drawBehind {
                if (dividers) {
                    // ::after 物理像素 1px 分隔线，left 从 16px 起
                    drawRect(
                        color = salt.hairline,
                        topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), size.height - 1f),
                        size = androidx.compose.ui.geometry.Size(size.width - 16.dp.toPx(), 1f),
                    )
                }
            }
            .then(
                when {
                    onClick != null && onLongClick != null -> Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    onClick != null -> Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                    else -> Modifier
                },
            )
            .padding(start = 16.dp), // padding-left: calc(16px + safe-left)
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
        }

        // __inner：flex 子项必须可收缩（源码注释：长文本撑宽会把 after 推出右侧）；
        // padding: 12px 16px(+safe-right) 12px 0
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, end = 16.dp, bottom = 12.dp),
        ) {
            // __title-wrap：min-height 28px，标题与 after 两端对齐
            Row(
                modifier = Modifier.heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    lineHeight = (17f * 1.35f).sp, // line-height 1.35
                    fontWeight = if (strongTitle) FontWeight.SemiBold else FontWeight.Normal,
                    color = salt.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 占满剩余宽度 → after 靠右（对应 __title-wrap space-between +
                    // __after margin-left:auto；fill=true 保证 after 贴到行尾）
                    modifier = Modifier.weight(1f),
                )
                // __after：margin-left:auto + padding-left 4px + gap 4px
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    after()
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = (13f * 1.35f).sp,
                    color = salt.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (chevron) {
            // __chevron：opacity .2 / margin-left 12 / margin-right 4
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = salt.text.copy(alpha = 0.2f),
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .width(16.dp),
            )
        }
    }
}

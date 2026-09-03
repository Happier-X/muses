package com.muses.player.core.ui.components

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
import com.muses.player.core.ui.icons.LucideIcons
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing

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
    /** 行度量（行高/字号/间距集），页面级覆盖用预设如 [SaltListItemMetrics.SongsDense] */
    metrics: SaltListItemMetrics = SaltListItemMetrics(),
    /** 左侧 media 插槽（封面等），不额外加间距 —— Web 由调用方控制间距 */
    leading: (@Composable () -> Unit)? = null,
    /** 右侧 after 插槽（时长/按钮等） */
    after: @Composable RowScope.() -> Unit = {},
) {
    val salt = LocalSaltColors.current
    val isDark = salt === com.muses.player.core.ui.theme.SaltDarkColors
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
            .heightIn(min = metrics.rowMinHeight)
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
                .padding(top = metrics.innerVerticalPadding, end = 16.dp, bottom = metrics.innerVerticalPadding),
        ) {
            // __title-wrap：min-height 28px，标题与 after 两端对齐
            Row(
                modifier = Modifier.heightIn(min = metrics.titleWrapMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = metrics.titleFontSize,
                    lineHeight = metrics.titleLineHeight,
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
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(metrics.afterGap),
                    modifier = Modifier.padding(start = metrics.afterStartPadding),
                ) {
                    after()
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = metrics.subtitleFontSize,
                    lineHeight = metrics.subtitleLineHeight,
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
                imageVector = LucideIcons.ChevronRight,
                contentDescription = null,
                tint = salt.text.copy(alpha = 0.2f),
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .width(16.dp),
            )
        }
    }
}

/**
 * 列表行度量集 —— 承载页面级 :deep 覆盖（如 Web `.songs-page :deep(.m-list-item)`），
 * 避免 SaltListItem 参数爆炸。默认值 = MListItem.vue 全局规格。
 */
data class SaltListItemMetrics(
    val rowMinHeight: Dp = 56.dp,
    val innerVerticalPadding: Dp = 12.dp,
    val titleWrapMinHeight: Dp = 28.dp,
    val titleFontSize: TextUnit = 17.sp,
    val titleLineHeight: TextUnit = (17f * 1.35f).sp,
    val subtitleFontSize: TextUnit = 13.sp,
    val subtitleLineHeight: TextUnit = (13f * 1.35f).sp,
    /** __after 左侧 padding（Web padding-left，椒盐歌曲行为 0 紧贴文字区） */
    val afterStartPadding: Dp = 4.dp,
    /** __after 内部 gap（Web gap，椒盐歌曲行为 0） */
    val afterGap: Dp = 4.dp,
) {
    companion object {
        /**
         * Web `.songs-page :deep(.m-list-item)` 椒盐歌曲行覆盖：
         * 行高对齐椒盐 72dp、inner 上下 6px、标题 16px/1.3、副文字 12px/1.3、
         * after 紧贴文字区（padding-left/gap = 0）。
         */
        val SongsDense = SaltListItemMetrics(
            rowMinHeight = 72.dp,
            innerVerticalPadding = 6.dp,
            titleWrapMinHeight = 24.dp,
            titleFontSize = 16.sp,
            titleLineHeight = (16f * 1.3f).sp,
            subtitleFontSize = 12.sp,
            subtitleLineHeight = (12f * 1.3f).sp,
            afterStartPadding = 0.dp,
            afterGap = 0.dp,
        )
    }
}

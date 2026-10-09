package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 歌曲列表统一的更多入口，图标与触控区域使用同一套尺寸。 */
@Composable
fun SongMoreButton(onClick: () -> Unit) {
    MusesIconButton(
        onClick = onClick,
        imageVector = TablerIcons.MoreVert,
        contentDescription = "更多歌曲操作",
        size = SongListLayout.actionButtonSize,
        tint = MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

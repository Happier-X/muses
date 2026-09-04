package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius

/**
 * 跨平台曲目行数据（平台无关，只承载展示信息）。
 *
 * Android 端由调用方从 [com.muses.player.core.data.db.SongEntity] 映射而来；
 * Desktop 端由调用方从同样的 [com.muses.player.core.data.db.SongEntity] 映射而来。
 */
data class SongItem(
    val id: String,
    val title: String,
    val artist: String? = null,
    val albumTitle: String? = null,
)

/**
 * 跨平台曲目行组件（U5 曲目列表共用化）。
 *
 * 视觉契约（对照桌面 LibraryScreen SongRow + SaltListItem 风格）：
 * - 行高自适应，内缩 padding 12dp;
 * - 标题：salt.text / 14sp，当前曲 primary 色 + SemiBold;
 * - 副标题：artist - albumTitle，salt.text2 / 12sp，单行省略;
 * - 当前行：surface1 背景 + primary 色标题 + 尾部播放图标;
 * - 按压态：透明叠层（无涟漪），对齐 SaltListItem 约定;
 * - 纯 UI 组件，零平台依赖，所有业务逻辑经回调注入。
 *
 * @param song 曲目数据
 * @param isCurrent 是否为当前播放曲
 * @param onClick 点击回调
 * @param onLongClick 长按回调（null = 不支持长按；安卓旧曲库屏传入「加入歌单」弹层）
 */
@Composable
fun SongListItem(
    song: SongItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val salt = LocalSaltColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val bgColor = when {
        isCurrent -> salt.surface1
        pressed -> salt.surface1.copy(alpha = 0.5f)
        else -> salt.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SaltRadius.sm))
            .background(bgColor)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 文字区：标题 + 副标题
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = song.title,
                color = if (isCurrent) salt.primary else salt.text,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(song.artist, song.albumTitle)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = salt.text2,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 当前曲播放指示器
        if (isCurrent) {
            Icon(
                imageVector = TablerIcons.PlayFill,
                contentDescription = "正在播放",
                tint = salt.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }
    }
}

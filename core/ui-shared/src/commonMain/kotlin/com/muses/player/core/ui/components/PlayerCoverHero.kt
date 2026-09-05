package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.muses.player.core.ui.icons.TablerIcons

/**
 * `.player-page__cover-hero` —— 播放页封面 hero（aspect 1 正方形，圆角 12dp）。
 *
 * 对齐 Capacitor player-page__cover-hero：容器 max-height min(50vh,420px) +
 * cover-hero-img aspect 1 contain。封面加载走 Coil（file://`/`content://`/`data:`/`https:` 均可）。
 */
@Composable
fun PlayerCoverHero(
    coverUri: String?,
    modifier: Modifier = Modifier,
    screenHeight: Dp = 800.dp,
    screenWidth: Dp = 360.dp,
    isNarrowHeight: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val maxHeroHeight = minOf(screenHeight * 0.5f, 420.dp)
    val narrowMaxWidth = if (isNarrowHeight) minOf(screenWidth * 0.34f, 150.dp) else null
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeroHeight),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val targetSize = when {
            narrowMaxWidth != null -> narrowMaxWidth
            else -> minOf(availableWidth, availableHeight)
        }
        Box(
            modifier = Modifier
                .size(targetSize)
                .aspectRatio(1f)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!coverUri.isNullOrBlank()) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                )
            } else {
                Icon(TablerIcons.MusicNoteOutlined, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
            }
        }
    }
}

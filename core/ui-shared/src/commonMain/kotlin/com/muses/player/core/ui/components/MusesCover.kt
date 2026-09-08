package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import top.yukonga.miuix.kmp.basic.Icon
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** MCover 圆角档位：sm → `--m-radius-sm`(8) / md → `--m-radius-card`(12) */
enum class MusesCoverRadius(val value: Dp) {
    SM(8.dp),
    MD(12.dp),
}

/**
 * `.m-cover` —— 音乐封面及稳定占位（MCover.vue 一比一翻译）。
 *
 * 视觉契约：
 * - 尺寸：Web 'md' = 52px / 'sm' = 48px（此处以 [size] Dp 直传，默认 52dp）；
 * - 底色 `--m-surface-2`、图标色 `--m-text-2`；
 * - 占位图标 `--m-list-icon`(24px)，tabler music；
 * - 图片 `object-fit: cover`；圆角按 [radius] 档位。
 */
@Composable
fun MusesCover(
    uri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    radius: MusesCoverRadius = MusesCoverRadius.MD,
    contentDescription: String? = null,
) {
    val scheme = MiuixTheme.colorScheme
    val shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.value)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(scheme.surfaceVariant)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 占位（src 为空或未加载时透出）
        Icon(
            imageVector = TablerIcons.MusicNoteOutlined, // tabler music
            contentDescription = null,
            tint = scheme.onBackgroundVariant,
            modifier = Modifier.size(24.dp), // __placeholder-icon: --m-list-icon
        )
        if (!uri.isNullOrBlank()) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(shape),
            )
        }
    }
}

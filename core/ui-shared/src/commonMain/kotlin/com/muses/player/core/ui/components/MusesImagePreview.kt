package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 图片预览弹窗（miuix OverlayDialog；封面大图等灯箱场景）。
 *
 * 遮罩统一走官方窗口级 dim（与 [MusesBottomSheet]、[MusesDialog]、[MusesActionsSheet] 同源），
 * 禁止手写 Dialog + Black.copy 遮罩，避免各处灰度不一。
 * 调用方保持 `?.let { MusesImagePreview(...) }` 条件组合写法，内部固定 `show = true`。
 */
@Composable
fun MusesImagePreview(
    imageUrl: String,
    onDismiss: () -> Unit,
    title: String = "封面预览",
    modifier: Modifier = Modifier,
) {
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        modifier = modifier,
        content = {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        },
    )
}

package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing

/**
 * `.m-empty` —— 空态（MEmpty.vue 一比一翻译）。
 *
 * 视觉契约：
 * - 纵向排列居中，`gap: var(--m-spacing-sub)`(12dp)，
 *   `padding: 56px var(--m-spacing)`；
 * - 图标壳：72px 圆形 `--m-surface-2` 底、`--m-text-3` 图标色，
 *   图标 `--m-list-icon`(24px)；
 * - 标题 17px / 600 / 1.35 / `--m-text`；
 * - 描述 15px / 1.4 / `--m-text-2`。
 */
@Composable
fun SaltEmpty(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
) {
    val salt = LocalSaltColors.current

    Column(
        modifier = modifier.padding(horizontal = SaltSpacing.spacing, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(salt.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // aria-hidden
                    tint = salt.text3,
                    modifier = Modifier.size(SaltSpacing.listIcon),
                )
            }
        }
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = (17f * 1.35f).sp,
            color = salt.text,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                fontSize = 15.sp,
                lineHeight = (15f * 1.4f).sp,
                color = salt.text2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

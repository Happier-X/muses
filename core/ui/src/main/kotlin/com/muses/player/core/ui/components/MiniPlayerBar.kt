package com.muses.player.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalMusesHazeState
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltDarkColors
import com.muses.player.core.ui.theme.SaltShadowLayer
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.core.ui.theme.musesBottomBarHazeStyle
import com.muses.player.core.ui.theme.saltShadow
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * `.mini-player` —— 底部迷你播放条（MiniPlayer.vue 一比一翻译）。
 * 数据经参数传入（P1 接线时由 app 层喂 ViewModel 状态），本组件不接 ViewModel。
 *
 * 视觉契约：
 * - 高 64px；左右 18px 悬浮（定位由页面控制，此处只画胶囊本体）；
 *   底部避让 safe-bottom + 8px 同样由页面布局负责；
 * - 液态玻璃（真磨砂）：`--m-glass-bg` + Haze `blur 20dp` + 白/黑 tint（暗 0.42 / 明 0.56），
 *   `border-radius: 40px` 胶囊 + `border: 1px solid rgba(255,255,255,.5)`（暗色 .12）；
 *   Haze 生效时由 [LocalMusesHazeState] 的 `hazeEffect` 提供实时背景模糊，
 *   无 Haze 时回退为 0.75 alpha 的纯色底（见 [SaltColors.glassBg]）；
 * - box-shadow：`inset 0 1px 0 rgba(255,255,255,.65)`（暗 .1）+
 *   `0 4px 16px rgba(0,0,0,.08)`（暗 .35）；
 * - 行内 gap `--m-spacing-sub`(12px)、水平 padding `--m-spacing`(16px)；
 * - 封面 48px；标题 15px/600/1.25 单行省略；副标题 13px/1.3/`--m-text-2`
 *   单行省略，两行间距 3px（`__info { gap: 3px }`）；
 * - 控制组 gap 2px，图标 18px 实心（Tabler Filled 系）；
 * - 无歌空态：显示「暂无播放歌曲 / 未知艺术家 - 未知专辑」占位文案，
 *   整条不可点、播放键禁用（`.mini-player--empty` + aria-disabled）。
 */
@Composable
fun MiniPlayerBar(
    title: String,
    subtitle: String,
    coverUri: String?,
    isPlaying: Boolean,
    onOpenPlayer: () -> Unit,
    onTogglePlayback: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    /** 是否有当前曲目（false = 空态：整条不可点、播放键禁用） */
    hasSong: Boolean = true,
) {
    val salt = LocalSaltColors.current
    val isDark = salt === SaltDarkColors
    val capsuleShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
    val hazeState = LocalMusesHazeState.current
    val bottomHazeStyle = if (hazeState != null) musesBottomBarHazeStyle(isDark) else null

    // box-shadow 双套配方（明 / 暗），层序与 SCSS 一致：先 inset 高光、后外投影
    val shadowLayers: List<SaltShadowLayer> = if (isDark) {
        listOf(
            SaltShadowLayer(offsetY = 1.dp, color = Color.White.copy(alpha = 0.1f), inset = true),
            SaltShadowLayer(offsetY = 4.dp, blurRadius = 16.dp, color = Color.Black.copy(alpha = 0.35f)),
        )
    } else {
        listOf(
            SaltShadowLayer(offsetY = 1.dp, color = Color.White.copy(alpha = 0.65f), inset = true),
            SaltShadowLayer(offsetY = 4.dp, blurRadius = 16.dp, color = Color.Black.copy(alpha = 0.08f)),
        )
    }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.5f)

    val clickInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            // 阴影会溢出边界绘制，父级不要 clipToBounds
            .saltShadow(shape = capsuleShape, layers = shadowLayers)
            .clip(capsuleShape)
            .then(
                if (hazeState != null && bottomHazeStyle != null) {
                    Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = bottomHazeStyle)
                } else {
                    Modifier.background(color = salt.glassBg, shape = capsuleShape)
                },
            )
            .border(border = BorderStroke(1.dp, borderColor), shape = capsuleShape)
            .clickable(
                interactionSource = clickInteraction,
                indication = null,
                enabled = hasSong,
                onClick = onOpenPlayer,
            )
            .padding(horizontal = SaltSpacing.spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SaltSpacing.spacingSub),
    ) {
        SaltCover(uri = coverUri, size = 48.dp, radius = SaltCoverRadius.MD)

        // __info：gap 3px，flex:1 min-width:0
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title, // 默认「暂无播放歌曲」由调用方按空态传
                fontSize = 15.sp,
                lineHeight = (15f * 1.25f).sp,
                fontWeight = FontWeight.SemiBold,
                color = salt.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle, // 「{artist} - {album}」由调用方拼装
                fontSize = 13.sp,
                lineHeight = (13f * 1.3f).sp,
                color = salt.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // __controls：gap 2px，图标 18px（md 触控区 40px 不变）
        // 控制区需消费点击避免冒泡至外层 Row 的 onOpenPlayer（修复点击播放按钮同时打开播放页）
        // 外层 Box 以空 clickable 消费非按钮区域的 gap 点击；按钮自身可点击已天然拦截冒泡
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SaltIconButton(
                    onClick = onTogglePlayback,
                    imageVector = if (isPlaying) TablerIcons.PauseFill else TablerIcons.PlayFill, // fill 风格播放/暂停
                    contentDescription = if (isPlaying) "暂停播放" else "继续播放",
                    enabled = hasSong, // :disabled="!currentSong || status==='loading'"
                    tint = salt.text, // __btn { color: var(--m-text) }
                    iconSizeOverride = 18.dp, // __icon { width: 18px }
                )
                SaltIconButton(
                    onClick = onOpenQueue,
                    imageVector = TablerIcons.QueueMusic, // tabler playlist
                    contentDescription = "打开播放队列",
                    tint = salt.text,
                    iconSizeOverride = 18.dp,
                )
            }
        }
    }
}

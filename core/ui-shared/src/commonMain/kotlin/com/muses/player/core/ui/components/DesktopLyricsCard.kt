package com.muses.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.model.lyrics.DesktopLyricsWord
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 两端透明悬浮歌词；触摸或悬停才显示操作，拖动由各平台宿主处理。 */
@Composable
fun DesktopLyricsCard(primary: String, secondary: String?, onClose: () -> Unit, modifier: Modifier = Modifier,
    words: List<DesktopLyricsWord> = emptyList(), positionMs: Long = 0, isPlaying: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var revealVersion by remember { mutableStateOf(0) }
    var touched by remember { mutableStateOf(false) }
    LaunchedEffect(revealVersion) {
        if (revealVersion > 0) {
            touched = true
            delay(4000)
            touched = false
        }
    }
    val shadow = Shadow(Color.Black, Offset(0f, 1f), 5f)
    Box(modifier.fillMaxWidth().hoverable(interaction)
        .clickable(interactionSource = interaction, indication = null,
            onClickLabel = "显示歌词操作", onClick = { revealVersion++ })
        .padding(horizontal = 8.dp, vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DesktopWordLyrics(primary, words, positionMs, isPlaying,
                style = MiuixTheme.textStyles.main.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, shadow = shadow, textAlign = TextAlign.Center))
            secondary?.let {
                Text(it, modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp, lineHeight = 20.sp,
                        color = Color.White, shadow = shadow, textAlign = TextAlign.Center),
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (hovered || touched) {
            MusesIconButton(onClick = onClose, imageVector = TablerIcons.Close, contentDescription = "关闭桌面歌词",
                tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape))
        }
    }
}

/** 按实际字形区域逐字填色，支持换行；帧时钟只补足播放进度采样之间的空隙。 */
@Composable
private fun DesktopWordLyrics(text: String, words: List<DesktopLyricsWord>, positionMs: Long, isPlaying: Boolean, style: TextStyle) {
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var displayPosition by remember(text) { mutableStateOf(positionMs) }
    val masks = remember(text, words, layout) {
        val result = layout
        words.map { word ->
            val start = word.startOffset.coerceIn(0, text.length)
            val end = word.endOffset.coerceIn(start, text.length)
            word to if (result == null) emptyList() else (start until end).mapNotNull { offset ->
                val line = result.getLineForOffset(offset)
                if (offset >= result.getLineEnd(line, visibleEnd = true)) null
                else result.getBoundingBox(offset).takeIf { it.width > 0f }?.let { bounds ->
                    bounds to (result.getBidiRunDirection(offset) == ResolvedTextDirection.Rtl)
                }
            }
        }
    }
    LaunchedEffect(text, words, positionMs, isPlaying) {
        displayPosition = positionMs
        if (isPlaying && words.isNotEmpty()) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { frame -> displayPosition = positionMs + (frame - start) / 1_000_000 }
            }
        }
    }
    Box(Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.fillMaxWidth(), style = style, color = Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis, onTextLayout = { layout = it })
        if (words.isNotEmpty()) {
            Text(text, style = style.copy(shadow = null), color = Color(0xFF8FDFFF), maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }.drawWithContent {
                    val path = Path()
                    for ((word, characters) in masks) {
                        val progress = word.progressAt(displayPosition)
                        if (progress <= 0f) continue
                        var remaining = characters.sumOf { it.first.width.toDouble() }.toFloat() * progress
                        for ((bounds, rtl) in characters) {
                            val filled = remaining.coerceIn(0f, bounds.width)
                            if (filled > 0f) {
                                path.addRect(bounds.copy(
                                    left = if (rtl) bounds.right - filled else bounds.left,
                                    right = if (rtl) bounds.right else bounds.left + filled))
                            }
                            remaining -= bounds.width
                        }
                    }
                    clipPath(path) { this@drawWithContent.drawContent() }
                })
        }
    }
}

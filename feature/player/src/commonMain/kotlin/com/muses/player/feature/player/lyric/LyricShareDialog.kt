package com.muses.player.feature.player.lyric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.feature.player.lyric.meloXLiquidButton
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
internal fun LyricShareDialog(
    state: PlaybackUiState,
    lines: List<LyricLine>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    /** U17：分享动作回调（安卓系统分享面板 / 桌面剪贴板） */
    onShareText: (String) -> Unit,
) {
    val foreground = MiuixTheme.colorScheme.onBackground
    val surfaceForeground = MiuixTheme.colorScheme.onSurface
    val scope = rememberCoroutineScope()
    var selected by remember(lines, initialIndex) { mutableStateOf(setOf(initialIndex.coerceIn(lines.indices))) }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("取消", color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                Text("分享歌词", Modifier.weight(1f), color = foreground, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("全选", color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable { selected = lines.indices.toSet() }.padding(8.dp))
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(state.artworkUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).squircleClip(12.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(state.title, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
                    Text(state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = foreground.copy(alpha = .58f))
                    Text("已选择 ${selected.size} 行 · 无字符数量限制", style = MiuixTheme.textStyles.footnote1, color = foreground.copy(alpha = .48f), modifier = Modifier.padding(top = 4.dp))
                }
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(lines, key = { index, line -> "share-$index-${line.timeMs}" }) { index, line ->
                    val chosen = index in selected
                    val shape = RoundedCornerShape(when {
                        !chosen -> 14.dp
                        index - 1 !in selected && index + 1 in selected -> 14.dp
                        index - 1 in selected && index + 1 !in selected -> 14.dp
                        else -> 5.dp
                    })
                    Box(
                        Modifier.fillMaxWidth().clip(shape)
                            .background(if (chosen) MiuixTheme.colorScheme.primary.copy(alpha = .18f) else MiuixTheme.colorScheme.surface)
                            .clickable {
                                selected = if (chosen) selected - index else selected + index
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(line.text, color = surfaceForeground, style = MiuixTheme.textStyles.main, lineHeight = 23.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            error?.let { Text(it, color = MiuixTheme.colorScheme.error, style = MiuixTheme.textStyles.footnote1, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShareButton("分享文本", Modifier.weight(1f), selected.isNotEmpty() && !generating) {
                    val chosen = selected.sorted().mapNotNull(lines::getOrNull)
                    val text = buildString {
                        append(chosen.joinToString("\n") { it.text })
                        append("\n——《${state.title}》 · ${state.artist}")
                    }
                    onShareText(text)
                }
                ShareButton(if (generating) "生成中…" else "生成图片", Modifier.weight(1f), selected.isNotEmpty() && !generating) {
                    generating = true; error = null
                    scope.launch {
                        runCatching { shareLyricImage(state, selected.sorted().mapNotNull(lines::getOrNull)) }
                            .onFailure { error = it.message ?: "歌词图片生成失败" }
                        generating = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareButton(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).meloXLiquidButton(
            shape = RoundedCornerShape(25.dp),
            enabled = enabled,
            surfaceColor = MiuixTheme.colorScheme.primary.copy(alpha = .12f),
        ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface.copy(alpha = .35f), fontWeight = FontWeight.SemiBold)
    }
}

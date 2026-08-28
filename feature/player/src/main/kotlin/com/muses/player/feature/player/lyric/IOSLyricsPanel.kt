package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Immersive iOS 歌词面板 — 直接复刻 Immersive-Android 的 IOSLyricsPanel
 *
 * 逐行/逐词契约（与 Immersive 一致）：
 * - 逐词 alpha / ExtraBold：当前行已唱词 1.0 ExtraBold，未唱 0.42 Regular，正在唱的词 ExtraBold 1.0；
 *   非当前行 0.45-0.5 Regular；wordFadeWidth 0.5 用词级二段近似（词内不细分字符，符合 Immersive 以 word 为原子）
 * - 翻译/音译：translationEnabled 控制显隐，仅当该行 translatedLyric/romanLyric 非空时渲染（与 PlayerViewModel hasTranslation 配合）
 * - 和声：line.isBG 时文字 italic、alpha 更低、附「· 和声」标记，字号略小
 * - 点击跳转：点击任意行 seekTo(line.startTime)
 * - 自动滚动：当前行居中（LazyColumn animateScrollToItem current-2，align center 0.5）
 * - 空态、Fab 显隐（180ms fade，3s idle 隐藏）与 Immersive 交互一致
 */
@Composable
fun IOSLyricsPanel(
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    translationEnabled: Boolean,
    hasTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    showPlayFab: Boolean,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    var chromeVisible by remember { mutableStateOf(false) }
    var chromeIdleJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun revealChrome() {
        chromeVisible = true
        chromeIdleJob?.cancel()
        chromeIdleJob = scope.launch {
            delay(3000)
            chromeVisible = false
        }
    }

    DisposableEffect(lines) {
        onDispose { chromeIdleJob?.cancel() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { if (lines.isNotEmpty()) revealChrome() },
    ) {
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(40.dp))
                    Text("暂无歌词", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "未找到内嵌歌词或同目录同名 .lrc 文件，可在刮削页获取。",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            val currentIdx = remember(lines, lyricPosition) { computeCurrentIndex(lines, lyricPosition) }

            LaunchedEffect(currentIdx) {
                if (currentIdx >= 0) {
                    val target = (currentIdx - 2).coerceAtLeast(0)
                    runCatching { listState.animateScrollToItem(target) }
                }
            }

            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) revealChrome()
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, _ -> revealChrome() }
                    },
                contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lines, key = { idx, line -> "${line.startTime}-$idx" }) { idx, line ->
                    val isCurrent = idx == currentIdx
                    val bg = if (isCurrent) Color.White.copy(alpha = 0.10f) else Color.Transparent
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .clickable {
                                onSeek(line.startTime.toLong())
                                revealChrome()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Immersive 逐词：wordFadeWidth 0.5 二段近似，已唱/未唱以词边界区分；当前唱词 ExtraBold
                        val annotated = remember(line, lyricPosition, isCurrent) {
                            buildAnnotatedString {
                                if (line.words.isEmpty()) {
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = if (isCurrent) 1f else 0.5f))) { append("") }
                                } else {
                                    line.words.forEach { w ->
                                        // Immersive 标准：非当前行统一半透；当前行区分已唱/未唱/正在
                                        val alpha = when {
                                            !isCurrent -> if (line.isBG) 0.38f else 0.50f
                                            lyricPosition >= w.endTime -> 1f
                                            lyricPosition < w.startTime -> 0.42f
                                            else -> 1f
                                        }
                                        val weight = when {
                                            isCurrent && lyricPosition in w.startTime..w.endTime -> FontWeight.ExtraBold
                                            isCurrent && lyricPosition >= w.endTime -> FontWeight.Bold
                                            isCurrent -> FontWeight.Normal
                                            else -> FontWeight.Normal
                                        }
                                        val style = SpanStyle(
                                            color = Color.White.copy(alpha = alpha),
                                            fontWeight = weight,
                                            fontSize = if (isCurrent) 18.sp else 15.sp,
                                            fontStyle = if (line.isBG) FontStyle.Italic else FontStyle.Normal,
                                            letterSpacing = if (isCurrent && lyricPosition in w.startTime..w.endTime) 0.2.sp else 0.sp,
                                        )
                                        withStyle(style) { append(w.word) }
                                    }
                                }
                            }
                        }
                        Text(
                            text = annotated,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = if (isCurrent) 1f else 0.98f
                                    scaleY = if (isCurrent) 1f else 0.98f
                                    alpha = if (isCurrent) 1f else 0.96f
                                },
                            lineHeight = 26.sp,
                        )
                        // 翻译：仅 translationEnabled 且非空，当前行 0.88，非当前 0.45
                        if (translationEnabled && line.translatedLyric.isNotBlank()) {
                            Text(
                                text = line.translatedLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.45f),
                                fontSize = if (isCurrent) 13.sp else 12.sp,
                                fontStyle = if (line.isBG) FontStyle.Italic else FontStyle.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        }
                        // 音译：romanLyric
                        if (translationEnabled && line.romanLyric.isNotBlank()) {
                            Text(
                                text = line.romanLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.38f),
                                fontSize = 11.sp,
                                fontStyle = if (line.isBG) FontStyle.Italic else FontStyle.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (line.isBG && line.words.isNotEmpty()) {
                            Text("· 和声", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontStyle = FontStyle.Italic)
                        }
                        if (line.isDuet && line.words.isNotEmpty()) {
                            Text("· 合唱", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 浮动操作：对齐 Capacitor lyric-fabs clear 风格 — 透明底 + 白字 text-white/80，翻译键 is-active 仅翻译，3s idle 隐藏
        val showFabContainer = hasTranslation || showPlayFab
        if (showFabContainer) {
            val fabAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 180),
                label = "fab-alpha",
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .alpha(fabAlpha)
                    .graphicsLayer { alpha = fabAlpha },
                horizontalArrangement = if (hasTranslation && showPlayFab) Arrangement.SpaceBetween else if (hasTranslation) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasTranslation) {
                    SaltIconButton(
                        onClick = {
                            onToggleTranslation()
                            revealChrome()
                        },
                        imageVector = Icons.Filled.Translate,
                        contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译",
                        tint = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.8f),
                        enabled = chromeVisible,
                    )
                    if (showPlayFab) Spacer(Modifier.weight(1f))
                }
                if (showPlayFab) {
                    SaltIconButton(
                        onClick = {
                            onPlayPause()
                            revealChrome()
                        },
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        size = SaltIconButtonSize.LG,
                        enabled = chromeVisible,
                    )
                }
            }
        }
    }
}

internal fun computeCurrentIndex(lines: List<AmllLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) idx = i else break
    }
    if (idx == -1) return 0
    val last = lines.last()
    if (positionMs > last.endTime) return lines.lastIndex
    return idx.coerceIn(0, lines.lastIndex)
}

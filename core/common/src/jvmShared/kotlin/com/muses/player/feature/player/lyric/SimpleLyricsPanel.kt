package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 简化版歌词面板 - 使用  数据模型
 *
 * 这是一个临时方案，提供基本的歌词显示和滚动功能。
 * 后续可以替换为完整的 LyricsPanel。
 *
 * 09-05-desktop-player-lyrics Y1：自 feature:player 上收 :core:common jvmShared
 * （同包名透传，安卓零改动；纯 Compose + coroutines + 数学，无平台 API）。
 */
@Composable
fun SimpleLyricsPanel(
    lines: List<AmllLyricLine>,
    positionMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    // 以下参数为兼容旧接口，暂时忽略
    syncedLyrics: Any? = null,
    positionProvider: (() -> Long)? = null,
    translationEnabled: Boolean = true,
    hasTranslation: Boolean = false,
    onToggleTranslation: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    showPlayFab: Boolean = false,
    isTablet: Boolean = false,
    albumArtUri: String? = null,
    onLyricAtTopChange: (Boolean) -> Unit = {},
) {
    // 使用 positionProvider 如果提供
    val effectivePositionMs = positionProvider?.invoke() ?: positionMs
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    // 计算当前行索引
    val currentIndex = remember(lines, effectivePositionMs) {
        computeCurrentIndex(lines, effectivePositionMs)
    }

    // 自动滚动到当前行
    LaunchedEffect(currentIndex, isPlaying) {
        if (currentIndex >= 0 && !isUserScrolling) {
            scrollJob?.cancel()
            scrollJob = scope.launch {
                delay(100) // 防抖
                listState.animateScrollToItem(
                    index = currentIndex,
                    scrollOffset = -200 // 偏移到屏幕上方
                )
            }
        }
    }

    // 检测用户手动滚动
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isUserScrolling = true
                } else {
                    delay(3000) // 3秒后恢复自动滚动
                    isUserScrolling = false
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (lines.isEmpty()) {
            // 空状态
            Text(
                text = "暂无歌词",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 200.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(lines) { index, line ->
                    LyricLineItem(
                        line = line,
                        isActive = index == currentIndex,
                        positionMs = effectivePositionMs,
                        onClick = {
                            onSeek(line.startTime.toLong())
                            isUserScrolling = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * 歌词行项
 */
@Composable
private fun LyricLineItem(
    line: AmllLyricLine,
    isActive: Boolean,
    positionMs: Long,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.45f,
        animationSpec = tween(durationMillis = 300),
        label = "lyricAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 0.92f,
        animationSpec = tween(durationMillis = 300),
        label = "lyricScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 主歌词
        Text(
            text = line.words.joinToString("") { it.word },
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 20.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // 翻译
        if (line.translatedLyric.isNotBlank()) {
            Text(
                text = line.translatedLyric,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }

        // 音译
        if (line.romanLyric.isNotBlank()) {
            Text(
                text = line.romanLyric,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}

/**
 * 计算当前行索引
 */
private fun computeCurrentIndex(lines: List<AmllLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1

    var idx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) {
            idx = i
        } else {
            break
        }
    }

    if (idx == -1) return 0

    val last = lines.last()
    if (positionMs > last.endTime) return lines.lastIndex

    return idx.coerceIn(0, lines.lastIndex)
}

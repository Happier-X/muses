package com.muses.player.feature.player.lyric

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 全量移植 — 瓜子音乐 Apple 歌词引擎（干净重写）
 * 逆向参数：VIEWPORT_ANCHOR 0.32, APPLE_WINDOW_BEFORE 3 / AFTER 15, lyricScrollSpring 1.16/237, amllPosYComposeStiffness 170..220
 * 目标：与 music.apk 的 AppleLyricsList 行为 1:1，但以 SyncedLyrics 为输入，不依赖 defpackage 混淆码
 * 已按 AMLL 的 spring.ts 1:1 复刻动态弹簧：getPosYSpringPolicy
 */
private const val VIEWPORT_ANCHOR = 0.32f
private const val APPLE_WINDOW_BEFORE = 3
private const val APPLE_WINDOW_AFTER = 15

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GzLyricsView(
    syncedLyrics: SyncedLyrics?,
    positionProvider: () -> Int,
    translationEnabled: Boolean,
    hasTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    showPlayFab: Boolean,
    isTablet: Boolean,
    albumArtUri: String? = null,
    onLyricAtTopChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val lines = syncedLyrics?.lines ?: emptyList()
    val listState = rememberLazyListState()

    var currentIndex by remember(syncedLyrics) { mutableStateOf(computeCurrentIndexNative(lines, positionProvider().toLong())) }
    LaunchedEffect(syncedLyrics) {
        while (true) {
            val pos = positionProvider().toLong()
            val newIdx = computeCurrentIndexNative(lines, pos)
            if (newIdx != currentIndex) currentIndex = newIdx
            delay(100)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 1 }
            .collectLatest { atTop -> onLyricAtTopChange(atTop) }
    }

    var isUserScrolling by remember { mutableStateOf(false) }
    var autoResumeJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    // FAB 显隐
    var chromeVisible by remember { mutableStateOf(false) }
    var chromeIdleJob by remember { mutableStateOf<Job?>(null) }
    fun revealChrome() {
        chromeVisible = true
        chromeIdleJob?.cancel()
        chromeIdleJob = scope.launch {
            delay(3000)
            chromeVisible = false
        }
    }
    DisposableEffect(Unit) { onDispose { chromeIdleJob?.cancel() } }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
            autoResumeJob?.cancel()
            revealChrome()
        } else if (isUserScrolling) {
            autoResumeJob?.cancel()
            autoResumeJob = scope.launch {
                delay(2500)
                isUserScrolling = false
                if (currentIndex >= 0) {
                    try { listState.animateScrollToItem(currentIndex) } catch (_: Exception) {}
                }
            }
        }
    }

    // Apple 窗口裁剪：仅渲染 current 前 3、后 15（与 music.apk 一致），减少过长列表开销
    val windowedIndices = remember(currentIndex, lines.size) {
        if (lines.isEmpty() || currentIndex < 0) 0 until lines.size
        else {
            val start = (currentIndex - APPLE_WINDOW_BEFORE).coerceAtLeast(0)
            val end = (currentIndex + APPLE_WINDOW_AFTER).coerceAtMost(lines.size - 1)
            start..end
        }
    }
    val windowedLines = remember(windowedIndices, lines) {
        windowedIndices.map { it to lines[it] }
    }

    Box(
        modifier = modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { revealChrome() }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            val anchor = viewportHeight * VIEWPORT_ANCHOR
            val anchorPx = with(LocalDensity.current) { anchor.toPx().toInt() }

            LaunchedEffect(currentIndex, anchorPx) {
                if (lines.isEmpty() || currentIndex < 0) return@LaunchedEffect
                try {
                    Log.d("GzLyrics", "anchorScroll index=$currentIndex anchorPx=$anchorPx")
                    listState.animateScrollToItem(currentIndex, scrollOffset = -anchorPx)
                } catch (_: Exception) {}
            }

            if (lines.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(40.dp))
                        Text("暂无歌词", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("未找到内嵌歌词或同目录同名 .lrc 文件，可在刮削页获取。", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            } else {
                val screenWidthDp = LocalConfiguration.current.screenWidthDp
                val mainFontSize = if (isTablet) (screenWidthDp * 0.024f).coerceIn(20f, 30f) else (screenWidthDp * 0.075f).coerceIn(26f, 32f)

                LookaheadScope {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = anchor, bottom = viewportHeight - anchor),
                    ) {
                        itemsIndexed(windowedLines, key = { _, pair -> pair.first }) { _, pair ->
                            val (globalIdx, line) = pair
                            val distance = abs(globalIdx - currentIndex)
                            val isCurrent = globalIdx == currentIndex
                            // 动态弹簧：按 interval 计算，与 AMLL getPosYSpringPolicy 1:1
                            val intervalMs = if (globalIdx > 0 && globalIdx < lines.size) {
                                (lines[globalIdx].start - lines[globalIdx - 1].start).toLong()
                            } else null
                            val (stiffness, dampingRatio) = remember(intervalMs) { getPosYSpringPolicy(intervalMs) }
                            Box(
                                modifier = Modifier.appleSpringPlacement(
                                    lookaheadScope = this@LookaheadScope,
                                    itemKey = globalIdx,
                                    isManualScrolling = isUserScrolling,
                                    stiffness = stiffness,
                                    dampingRatio = dampingRatio
                                )
                            ) {
                                NativeKaraokeLine(
                                    line = line,
                                    isCurrent = isCurrent,
                                    distance = distance,
                                    positionProvider = if (isCurrent) positionProvider else null,
                                    translationEnabled = translationEnabled,
                                    fontSize = mainFontSize.sp,
                                    onSeek = { onSeek(it); revealChrome() },
                                )
                            }
                        }
                    }
                }
            }
        }
        // FAB
        val showFabContainer = hasTranslation || showPlayFab
        if (showFabContainer) {
            val fabAlpha by animateFloatAsState(targetValue = if (chromeVisible) 1f else 0f, animationSpec = tween(200), label = "gz-fab")
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).graphicsLayer { alpha = fabAlpha },
                horizontalArrangement = if (hasTranslation && showPlayFab) Arrangement.SpaceBetween else if (hasTranslation) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasTranslation) {
                    SaltIconButton(onClick = { onToggleTranslation(); revealChrome() }, imageVector = Icons.Filled.Translate, contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译", tint = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.8f), enabled = chromeVisible)
                    if (showPlayFab) Spacer(Modifier.weight(1f))
                }
                if (showPlayFab) {
                    SaltIconButton(onClick = { onPlayPause(); revealChrome() }, imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = Color.White, size = SaltIconButtonSize.LG, enabled = chromeVisible)
                }
            }
        }
    }
}

// 复刻 AMLL 的 getPosYSpringPolicy（spring.ts），返回 stiffness/dampingRatio 对，已适配 Compose 的 mass 0.9
private fun getPosYSpringPolicy(intervalMs: Long?): Pair<Float, Float> {
    // SLOW 90/15, MEDIUM 140/22, NORMAL 170..220
    val mass = 0.9f
    return when {
        intervalMs == null -> {
            val stiffness = 90f
            val damping = 15f
            val dampingRatio = damping / (2f * sqrt(stiffness * mass))
            stiffness to dampingRatio
        }
        else -> {
            val clamped = intervalMs.coerceIn(100, 800)
            var ratio = 1f - ((clamped - 100f) / 700f)
            ratio = ratio.pow(0.2f)
            val stiffness = 170f + ratio * (220f - 170f)
            val damping = sqrt(stiffness) * 2.2f
            val dampingRatio = damping / (2f * sqrt(stiffness * mass))
            stiffness to dampingRatio
        }
    }
}

// 复用 amllPosYComposeStiffness 公式（已逆向，保留）
private fun amllPosYComposeStiffness(durationMs: Long): Float {
    val j = durationMs.coerceIn(100, 800)
    return ((((Math.pow((1f - ((j - 100f) / 700f).toDouble()), 0.2).toFloat()) * 50f) + 170f) / 0.9f)
}

package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
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

/**
 * 自研原生歌词面板 — 替换 LyricWebView
 * - LazyColumn 居中滚动（currentIndex 变化时 animateScrollToItem）
 * - 距离衰减：alpha/scale/blur 按 |index-current| 计算
 * -逐词 lerp 由 NativeKaraokeLine 内部按 positionProvider 帧驱动（仅当前行重组）
 * - isAtTop 回调供外层下滑关闭分流
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NativeLyricsPanel(
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

    DisposableEffect(Unit) {
        onDispose { chromeIdleJob?.cancel() }
    }

    val lines = syncedLyrics?.lines ?: emptyList()
    val listState = rememberLazyListState()

    // 当前行索引：100ms 粒度轮询，仅索引变化触发重组 — 调试日志
    var currentIndex by remember(syncedLyrics) { mutableStateOf(computeCurrentIndexNative(lines, positionProvider().toLong())) }
    LaunchedEffect(syncedLyrics) {
        while (true) {
            val pos = positionProvider().toLong()
            val newIdx = computeCurrentIndexNative(lines, pos)
            Log.d("LyricDebug", "poll pos=$pos newIdx=$newIdx current=$currentIndex lines=${lines.size} isPlaying=${isPlaying}")
            if (newIdx != currentIndex) {
                Log.d("LyricDebug", "index change $currentIndex -> $newIdx")
                currentIndex = newIdx
            }
            delay(100)
        }
    }

    // isAtTop 判定：首项完全可见
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 1 }
            .collectLatest { atTop -> onLyricAtTopChange(atTop) }
    }

    // 用户手势期间暂停自动居中 3s（与 Web 版 isUserScrolling 语义对齐）
    var isUserScrolling by remember { mutableStateOf(false) }

    var autoResumeJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
            autoResumeJob?.cancel()
            revealChrome()
        } else if (isUserScrolling) {
            autoResumeJob?.cancel()
            autoResumeJob = scope.launch {
                delay(3000)
                isUserScrolling = false
                // 恢复后立即居中到当前行（同样 spring 回弹）
                if (currentIndex >= 0) {
                    try { listState.animateScrollToItem(currentIndex) } catch (_: Exception) {}
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { revealChrome() },
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
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            val mainFontSize = if (isTablet) {
                (screenWidthDp * 0.024f).coerceIn(20f, 30f)
            } else {
                (screenWidthDp * 0.075f).coerceIn(26f, 32f)
            }.sp

            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val viewportHeight = maxHeight
                val anchor = viewportHeight * 0.32f
                val anchorPx = with(androidx.compose.ui.platform.LocalDensity.current) { anchor.toPx().toInt() }
                // 自动滚动需在 BoxWithConstraints 内以获取 anchorPx
                androidx.compose.runtime.LaunchedEffect(currentIndex, anchorPx) {
                    if (lines.isEmpty() || currentIndex < 0) return@LaunchedEffect
                    try {
                        Log.d("LyricDebug", "anchorScroll index=$currentIndex anchorPx=$anchorPx")
                        // 使当前行位于 32% 锚点：scrollOffset = -anchorPx
                        listState.animateScrollToItem(currentIndex, scrollOffset = -anchorPx)
                    } catch (_: Exception) {}
                }
                LookaheadScope {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = anchor, bottom = viewportHeight - anchor),
                    ) {
                        itemsIndexed(lines, key = { idx, _ -> idx }) { idx, line ->
                            val distance = if (currentIndex < 0) abs(idx - 0) else abs(idx - currentIndex)
                            val isCurrent = idx == currentIndex
                            val stiffness = if (kotlin.math.abs(idx - currentIndex) <= 1) 220f else 170f
                            Box(
                                modifier = Modifier.appleSpringPlacement(
                                    lookaheadScope = this@LookaheadScope,
                                    itemKey = idx,
                                    isManualScrolling = isUserScrolling,
                                    stiffness = stiffness
                                ).animateItem(placementSpec = spring(stiffness = 80f, dampingRatio = 0.6f))
                            ) {
                                NativeKaraokeLine(
                                    line = line,
                                    isCurrent = isCurrent,
                                    distance = distance,
                                    positionProvider = if (isCurrent) positionProvider else null,
                                    translationEnabled = translationEnabled,
                                    fontSize = mainFontSize,
                                    onSeek = {
                                        onSeek(it)
                                        revealChrome()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB：翻译键 + 播放/暂停，3s idle 隐藏
        val showFabContainer = hasTranslation || showPlayFab
        if (showFabContainer) {
            val fabAlpha by animateFloatAsState(
                targetValue = if (chromeVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 200),
                label = "lyric-fab-alpha",
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
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

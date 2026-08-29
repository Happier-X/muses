package com.muses.player.feature.player.lyric

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 沉浸式歌词面板 — 采用 accompanist lyrics-ui（AMLL 官方 Compose 实现）的卡拉OK 渲染器。
 *
 * 与自绘版的关键差异（即原先「跟 AMLL 效果差得远」的根因）：
 * - 逐词渐变是 **连续插值**（Canvas saveLayer + BlendMode.DstIn 位图遮罩按进度扫过），
 *   而非按词边界做二段 alpha 切换；长词还会启用字符级 Bounce/Swell/DipAndRise 动画
 * - 非当前行按 distance 权重施加 **真实高斯模糊**（RenderEffect）+ 缩放/透明度衰减
 * - 当前行以 BlendMode.Plus 叠加，得到 AMLL 标志性的发光质感
 * - 自动滚动由渲染器内部按 focus 行计算（不再由外部 animateScrollToItem 粗粒度驱动）
 * - 间奏段显示呼吸点（KaraokeBreathingDots），和声行独立样式
 *
 * @param syncedLyrics 解析后的原始歌词模型（携带 syllables 逐词时间轴；null 或空行走空态）
 * @param positionProvider 每帧回调的播放位置（ms）。必须是「不触发重组」的读取源，
 *   由外层以 VM 轮询值为锚点、每帧线性外推得到，详见 rememberLyricPositionProvider
 */
@Composable
fun LyricsPanel(
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
    modifier: Modifier = Modifier,
) {
    // 浮动按钮显隐：默认隐藏，点击/滑动歌词后显示，3s 后隐藏
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { revealChrome() },
    ) {
        val lyrics = syncedLyrics
        if (lyrics == null || lyrics.lines.isEmpty()) {
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

            // 用户滚动歌词时露出 chrome（对应原版 wheel/touchmove）
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) revealChrome()
            }

            val currentTextStyle = LocalTextStyle.current
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            // 当前行定位基准：KaraokeLyricsView 会把当前行滚到距列表视口顶部 offset 处
            // （默认 32dp 会贴顶）。AMLL 观感为当前行垂直居中略偏上，
            // 取屏幕高度 42%，给下方翻译/音译行留出视觉平衡。
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            val lyricAnchorOffset = (screenHeightDp * 0.42f).dp
            // 字号：AMLL 默认 clamp(22px, 6.5vw, 32px)；手机端取 7.5vw 以贴合原生观感（360dp → 27sp，上限 32sp）
            val mainFontSize = if (isTablet) {
                (screenWidthDp * 0.024f).coerceIn(20f, 30f)
            } else {
                (screenWidthDp * 0.075f).coerceIn(26f, 32f)
            }.sp
            // TextMotion.Animated：逐词渐变时字形度量连续变化，避免整字跳变
            val normalStyle = remember(currentTextStyle, mainFontSize) {
                currentTextStyle.copy(
                    fontSize = mainFontSize,
                    fontWeight = FontWeight.Bold,
                    textMotion = TextMotion.Animated,
                )
            }
            val accompanimentStyle = remember(normalStyle, mainFontSize) {
                normalStyle.copy(fontSize = mainFontSize * 0.7f)
            }
            val phoneticStyle = remember(normalStyle) {
                normalStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal)
            }

            KaraokeLyricsView(
                listState = listState,
                lyrics = lyrics,
                currentPosition = positionProvider,
                onLineClicked = { line ->
                    onSeek(line.start.toLong())
                    revealChrome()
                },
                onLinePressed = { revealChrome() },
                showTranslation = translationEnabled,
                showPhonetic = translationEnabled,
                normalLineTextStyle = normalStyle,
                accompanimentLineTextStyle = accompanimentStyle,
                phoneticTextStyle = phoneticStyle,
                textColor = Color.White,
                offset = lyricAnchorOffset,
                // 非当前行高斯模糊：AMLL 的标志性景深（低版本系统由库内部降级）
                useBlurEffect = true,
                blendMode = BlendMode.Plus,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Plus 叠加需离屏合成，否则会与背景直接混合丢失发光
                        blendMode = BlendMode.Plus
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
            )
        }

        // 浮动操作：翻译键（仅 hasTranslation 时）+ 播放/暂停（非平板），3s idle 隐藏
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

/** 保留给五行小窗等自绘场景的当前行索引计算（卡拉OK 面板已由渲染器内部处理） */
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

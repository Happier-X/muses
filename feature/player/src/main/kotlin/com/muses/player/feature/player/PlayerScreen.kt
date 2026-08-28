package com.muses.player.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.muses.player.core.ui.components.SaltCover
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.feature.player.lyric.AmllLyricLine
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 沉浸式播放页 —— 原生 Compose 重构（废弃 WebView 方案）。
 *
 * 布局契约（复刻 Capacitor PlayerPage.vue）：
 * - 背景：封面模糊 + 渐变（非纯黑），切歌同步更新，MuMu 上非黑屏保障
 * - 顶部：标题/艺术家固定头部（安全区避让）
 * - 中部：封面 hero + 五行小窗（当前行居中高亮，非当前弱化）
 * - 进度：Slider + 时间行 + 缓冲提示
 * - 控制：上一曲 / 播放暂停 / 下一曲
 * - 模式：循环 / 随机 / 队列 / 更多（编辑）
 * - 歌词：原生 LazyColumn + 逐词高亮 + 翻译开关 + 随进度自动滚动 + 点击跳播
 * - 横屏平板：左右双栏 + 底部全宽控制条
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onOpenQueue: () -> Unit = {},
    onOpenEditMeta: () -> Unit = {},
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsStateWithLifecycle()
    val parsedLines by viewModel.parsedLines.collectAsStateWithLifecycle()
    val hasTranslation by viewModel.hasTranslation.collectAsStateWithLifecycle()
    val translationEnabled by viewModel.translationEnabled.collectAsStateWithLifecycle()
    val lyricPosition by viewModel.lyricPosition.collectAsStateWithLifecycle()
    val stickyCover by viewModel.stickyCover.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isTabletLayout = remember(configuration) {
        configuration.screenWidthDp >= 768 && configuration.screenHeightDp < configuration.screenWidthDp
    }

    val title = currentMediaItem?.mediaMetadata?.title?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: ""
    val artist = currentMediaItem?.mediaMetadata?.artist?.toString()?.trim() ?: ""
    val hasSong = currentMediaItem != null && title.isNotEmpty()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF05070D))) {
        // 背景层：封面模糊 + 渐变（始终可见，非纯黑）
        PlayerBackground(
            coverUri = stickyCover,
            modifier = Modifier.fillMaxSize(),
        )

        if (!hasSong) {
            // 空态：保留沉浸底色与背景渐变，中央提示（避免黑屏误判为渲染失败）
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("暂无播放歌曲", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("从歌曲列表选择一首音乐后，即可进入沉浸式播放。", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            if (isTabletLayout) {
                TabletPlayerLayout(
                    title = title,
                    artist = artist,
                    coverUri = stickyCover,
                    lines = parsedLines,
                    lyricPosition = lyricPosition,
                    hasTranslation = hasTranslation,
                    translationEnabled = translationEnabled,
                    onToggleTranslation = { viewModel.toggleTranslation() },
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleModeEnabled,
                    onSeek = { viewModel.seekTo(it); viewModel.onSeekEnd(it) },
                    onSeekStart = { viewModel.onSeekStart() },
                    onSeekEnd = { viewModel.onSeekEnd(it) },
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.skipToPrevious() },
                    onNext = { viewModel.skipToNext() },
                    onToggleRepeat = {
                        val next = if (repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
                        viewModel.setRepeatMode(next)
                    },
                    onToggleShuffle = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    onClose = onClose,
                )
            } else {
                PhonePlayerLayout(
                    title = title,
                    artist = artist,
                    coverUri = stickyCover,
                    lines = parsedLines,
                    lyricPosition = lyricPosition,
                    hasTranslation = hasTranslation,
                    translationEnabled = translationEnabled,
                    onToggleTranslation = { viewModel.toggleTranslation() },
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleModeEnabled,
                    onSeek = { viewModel.seekTo(it) },
                    onSeekStart = { viewModel.onSeekStart() },
                    onSeekEnd = { viewModel.onSeekEnd(it) },
                    onPlayPause = { viewModel.playPause() },
                    onPrevious = { viewModel.skipToPrevious() },
                    onNext = { viewModel.skipToNext() },
                    onToggleRepeat = {
                        val next = if (repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_ONE
                        viewModel.setRepeatMode(next)
                    },
                    onToggleShuffle = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                    onOpenQueue = onOpenQueue,
                    onOpenEditMeta = onOpenEditMeta,
                    onClose = onClose,
                )
            }
        }

        // 限流/播放错误条
        if (playbackError != null) {
            androidx.compose.material3.Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.retryPlayback() }) {
                        Text("重试", color = Color.White)
                    }
                },
                containerColor = Color(0xCC1A1A1A),
                contentColor = Color.White,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(playbackError!!, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp).clickable { viewModel.clearPlaybackError() },
                    )
                }
            }
        }
    }
}

// ---------- 背景：封面模糊 + 渐变 ----------

@Composable
private fun PlayerBackground(coverUri: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        if (!coverUri.isNullOrBlank()) {
            // 底层模糊封面（放大 1.08 避免 blur 边缘漏底）
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.08f, scaleY = 1.08f).blur(28.dp),
            )
            // 渐变遮罩保证前景文字可读（顶部浅、底部深，复刻 MeloXFlowingLightBackdrop 的纵向渐变）
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color(0xFF05070D).copy(alpha = 0.55f),
                            Color(0xFF05070D).copy(alpha = 0.92f),
                        ),
                    ),
                ),
            )
            // 顶部光晕（微弱径向高光，非纯黑证明）
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(0.5f * 1080f, 0.28f * 1920f),
                        radius = 900f,
                    ),
                ),
            )
        } else {
            // 无封面时仍为带渐变的深色背景（非纯黑，满足 AC）
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1E2E), Color(0xFF0D0F1A), Color(0xFF05070D)),
                    ),
                ),
            )
        }
    }
}

// ---------- 手机布局：双面板（可横滑切换） ----------

@Composable
private fun PhonePlayerLayout(
    title: String,
    artist: String,
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    onClose: () -> Unit,
) {
    var showLyrics by remember { mutableStateOf(false) }

    val showLyricsState = androidx.compose.runtime.rememberUpdatedState(showLyrics)
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
            .pointerInput(showLyrics) {
                var totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDx = 0f },
                    onDragEnd = {
                        val cur = showLyricsState.value
                        if (totalDx < -60f && !cur) showLyrics = true
                        else if (totalDx > 60f && cur) showLyrics = false
                    },
                    onHorizontalDrag = { _, dragAmount -> totalDx += dragAmount },
                )
            },
    ) {
        // 固定头部：标题/艺术家 + 关闭（左）与队列（右）
        PlayerTopHeader(title = title, artist = artist, onClose = onClose, onOpenQueue = onOpenQueue)

        // 分段指示器（复刻左右滑提示）
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { idx ->
                val isSelected = (if (showLyrics) 1 else 0) == idx
                Box(
                    Modifier.padding(horizontal = 4.dp).size(width = if (isSelected) 20.dp else 6.dp, height = 6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)).clickable { showLyrics = idx == 1 },
                )
            }
        }

        // 主内容：AnimatedContent 双面板
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "player-phone-pager",
            ) { isLyrics ->
                if (!isLyrics) {
                    InfoPanelPhone(
                        coverUri = coverUri,
                        lines = lines,
                        lyricPosition = lyricPosition,
                        position = position,
                        duration = duration,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        repeatMode = repeatMode,
                        shuffleEnabled = shuffleEnabled,
                        onSeek = onSeek,
                        onSeekStart = onSeekStart,
                        onSeekEnd = onSeekEnd,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onToggleRepeat = onToggleRepeat,
                        onToggleShuffle = onToggleShuffle,
                        onOpenQueue = onOpenQueue,
                        onOpenEditMeta = onOpenEditMeta,
                    )
                } else {
                    LyricPanel(
                        lines = lines,
                        lyricPosition = lyricPosition,
                        translationEnabled = translationEnabled,
                        hasTranslation = hasTranslation,
                        onToggleTranslation = onToggleTranslation,
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onSeek = onSeek,
                        showPlayFab = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPanelPhone(
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        // 封面 hero
        CoverHero(coverUri = coverUri, modifier = Modifier.size(272.dp))
        Spacer(Modifier.height(18.dp))
        // 五行小窗
        MetaWindow(lines = lines, lyricPosition = lyricPosition, modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp))
        Spacer(Modifier.height(18.dp))
        // 进度
        ProgressSection(
            position = position,
            duration = duration,
            isBuffering = isBuffering,
            onSeekStart = onSeekStart,
            onSeekEnd = onSeekEnd,
        )
        Spacer(Modifier.height(12.dp))
        // 三键控制
        ControlsRow(isPlaying = isPlaying, onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext)
        Spacer(Modifier.height(12.dp))
        // mode-bar
        ModeBarRow(
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            onToggleRepeat = onToggleRepeat,
            onToggleShuffle = onToggleShuffle,
            onOpenQueue = onOpenQueue,
            onOpenEditMeta = onOpenEditMeta,
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- 平板布局：左右双栏 + 底部全宽控制条 ----------

@Composable
private fun TabletPlayerLayout(
    title: String,
    artist: String,
    coverUri: String?,
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    hasTranslation: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PlayerTopHeader(title = title, artist = artist, onClose = onClose, onOpenQueue = onOpenQueue)
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // 左栏：封面居中
            Column(
                modifier = Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CoverHero(coverUri = coverUri, modifier = Modifier.size(360.dp))
                // 平板隐藏五行小窗（契约：mimic Web 平板逻辑），仅封面居中
            }
            // 右栏：完整歌词
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                LyricPanel(
                    lines = lines,
                    lyricPosition = lyricPosition,
                    translationEnabled = translationEnabled,
                    hasTranslation = hasTranslation,
                    onToggleTranslation = onToggleTranslation,
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onSeek = onSeek,
                    showPlayFab = false,
                )
            }
        }
        // 底部全宽控制条
        Column(
            Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.18f)).padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            ProgressSection(position = position, duration = duration, isBuffering = isBuffering, onSeekStart = onSeekStart, onSeekEnd = onSeekEnd)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SaltIconButton(onClick = onToggleRepeat, imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环", tint = Color.White.copy(alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.6f))
                    SaltIconButton(onClick = onToggleShuffle, imageVector = Icons.Filled.Shuffle, contentDescription = if (shuffleEnabled) "关闭随机" else "随机播放", tint = Color.White.copy(alpha = if (shuffleEnabled) 1f else 0.6f))
                }
                ControlsRow(isPlaying = isPlaying, onPrevious = onPrevious, onPlayPause = onPlayPause, onNext = onNext, compact = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SaltIconButton(onClick = onOpenQueue, imageVector = Icons.Filled.QueueMusic, contentDescription = "播放队列", tint = Color.White)
                    SaltIconButton(onClick = onOpenEditMeta, imageVector = Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White)
                }
            }
        }
    }
}

// ---------- 顶部固定头部 ----------

@Composable
private fun PlayerTopHeader(
    title: String,
    artist: String,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SaltIconButton(onClick = onClose, imageVector = Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (artist.isNotEmpty()) {
                Text(artist, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
        SaltIconButton(onClick = onOpenQueue, imageVector = Icons.Filled.QueueMusic, contentDescription = "播放队列", tint = Color.White.copy(alpha = 0.9f))
    }
}

// ---------- 封面 hero ----------

@Composable
private fun CoverHero(coverUri: String?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier.clip(shape).background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!coverUri.isNullOrBlank()) {
            AsyncImage(
                model = coverUri,
                contentDescription = "封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape),
            )
        } else {
            Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
        }
    }
}

// ---------- 五行小窗 ----------

@Composable
private fun MetaWindow(lines: List<AmllLyricLine>, lyricPosition: Long, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) {
        Box(modifier = modifier.heightIn(min = 92.dp), contentAlignment = Alignment.Center) {
            Text("暂无歌词", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
        }
        return
    }
    val currentIdx = remember(lines, lyricPosition) { computeCurrentIndex(lines, lyricPosition) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (offset in -2..2) {
            val idx = currentIdx + offset
            val line = lines.getOrNull(idx)
            val raw = line?.words?.joinToString("") { it.word }?.trim() ?: ""
            val isCurrent = offset == 0
            // 占位空行保持高度稳定，避免跳动
            Text(
                text = if (raw.isEmpty()) " " else raw,
                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.42f),
                fontSize = if (isCurrent) 15.sp else 13.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    scaleX = if (isCurrent) 1.04f else 0.96f
                    scaleY = if (isCurrent) 1.04f else 0.96f
                    alpha = if (isCurrent) 1f else if (raw.isEmpty()) 0f else 0.95f
                },
            )
        }
    }
}

// ---------- 进度条 ----------

@Composable
private fun ProgressSection(
    position: Long,
    duration: Long,
    isBuffering: Boolean,
    onSeekStart: () -> Unit,
    onSeekEnd: (Long) -> Unit,
) {
    var previewMs by remember { mutableStateOf<Long?>(null) }
    val displayPos = previewMs ?: position
    val max = duration.coerceAtLeast(1L).toFloat()

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = displayPos.coerceIn(0L, duration.coerceAtLeast(0L)).toFloat(),
            onValueChange = { v ->
                if (previewMs == null) onSeekStart()
                previewMs = v.toLong()
            },
            onValueChangeFinished = {
                val target = previewMs ?: position
                previewMs = null
                onSeekEnd(target.coerceIn(0L, duration.coerceAtLeast(0L)))
            },
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                thumbColor = Color.White,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(displayPos), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            if (isBuffering) {
                Text("缓冲中", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(if (duration > 0) formatTime(duration) else "--:--", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

// ---------- 控制行 ----------

@Composable
private fun ControlsRow(isPlaying: Boolean, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, compact: Boolean = false) {
    val mainSize = if (compact) SaltIconButtonSize.MD else SaltIconButtonSize.LG
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp), verticalAlignment = Alignment.CenterVertically) {
        SaltIconButton(onClick = onPrevious, imageVector = Icons.Filled.SkipPrevious, contentDescription = "上一曲", size = SaltIconButtonSize.MD, tint = Color.White)
        SaltIconButton(onClick = onPlayPause, imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", size = mainSize, tint = Color.White)
        SaltIconButton(onClick = onNext, imageVector = Icons.Filled.SkipNext, contentDescription = "下一曲", size = SaltIconButtonSize.MD, tint = Color.White)
    }
}

@Composable
private fun ModeBarRow(
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEditMeta: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SaltIconButton(onClick = onToggleRepeat, imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, contentDescription = if (repeatMode == Player.REPEAT_MODE_ONE) "单曲循环" else "列表循环", tint = Color.White.copy(alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.62f))
        SaltIconButton(onClick = onToggleShuffle, imageVector = Icons.Filled.Shuffle, contentDescription = if (shuffleEnabled) "关闭随机" else "随机播放", tint = Color.White.copy(alpha = if (shuffleEnabled) 1f else 0.62f))
        SaltIconButton(onClick = onOpenQueue, imageVector = Icons.Filled.QueueMusic, contentDescription = "播放队列", tint = Color.White.copy(alpha = 0.92f))
        SaltIconButton(onClick = onOpenEditMeta, imageVector = Icons.Filled.MoreVert, contentDescription = "更多", tint = Color.White.copy(alpha = 0.92f))
    }
}

// ---------- 歌词面板：原生 LazyColumn + 逐词高亮 ----------

@Composable
private fun LyricPanel(
    lines: List<AmllLyricLine>,
    lyricPosition: Long,
    translationEnabled: Boolean,
    hasTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    showPlayFab: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        if (lines.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(40.dp))
                    Text("暂无歌词", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("可在刮削页获取歌词", color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
        } else {
            val listState = rememberLazyListState()
            val currentIdx = remember(lines, lyricPosition) { computeCurrentIndex(lines, lyricPosition) }
            LaunchedEffect(currentIdx) {
                if (currentIdx >= 0) {
                    val target = (currentIdx - 2).coerceAtLeast(0)
                    // 平滑滚动到当前行附近居中
                    runCatching { listState.animateScrollToItem(target) }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(lines, key = { idx, line -> "${line.startTime}-$idx" }) { idx, line ->
                    val isCurrent = idx == currentIdx
                    val bg = if (isCurrent) Color.White.copy(alpha = 0.10f) else Color.Transparent
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg).clickable { onSeek(line.startTime.toLong()) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 逐词高亮：MeloXIOSLyricsPanel 风格，已唱白色，未唱半透
                        val annotated = remember(line, lyricPosition, isCurrent) {
                            buildAnnotatedString {
                                if (line.words.isEmpty()) {
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = if (isCurrent) 1f else 0.55f))) { append("") }
                                } else {
                                    line.words.forEach { w ->
                                        val alpha = when {
                                            !isCurrent -> 0.5f
                                            lyricPosition >= w.endTime -> 1f
                                            lyricPosition < w.startTime -> 0.42f
                                            else -> 1f
                                        }
                                        val weight = if (isCurrent && lyricPosition in w.startTime..w.endTime) FontWeight.ExtraBold else if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        withStyle(SpanStyle(color = Color.White.copy(alpha = alpha), fontWeight = weight, fontSize = if (isCurrent) 18.sp else 15.sp)) {
                                            append(w.word)
                                        }
                                    }
                                }
                            }
                        }
                        Text(text = annotated, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 26.sp)
                        // 翻译
                        if (translationEnabled && line.translatedLyric.isNotBlank()) {
                            Text(
                                text = line.translatedLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.45f),
                                fontSize = if (isCurrent) 13.sp else 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                        if (translationEnabled && line.romanLyric.isNotBlank()) {
                            Text(
                                text = line.romanLyric,
                                color = if (isCurrent) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.38f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (line.isBG && line.words.isNotEmpty()) {
                            Text("· 和声", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 浮动操作：翻译开关（左下）与播放暂停（右下，仅手机）
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasTranslation) {
                androidx.compose.material3.FilledTonalIconButton(
                    onClick = onToggleTranslation,
                    modifier = Modifier.size(40.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.14f),
                        contentColor = if (translationEnabled) Color.Black else Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Translate, contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译", modifier = Modifier.size(18.dp))
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            if (showPlayFab) {
                androidx.compose.material3.FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ---------- 工具 ----------

private fun computeCurrentIndex(lines: List<AmllLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) idx = i else break
    }
    if (idx == -1) return 0
    // 若已超出末句 end，仍保持末句高亮（避免播完全行失活）
    val last = lines.last()
    if (positionMs > last.endTime) return lines.lastIndex
    return idx.coerceIn(0, lines.lastIndex)
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ---------- 队列页（保持原生命航，轻微 Salt 化） ----------

@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val currentIndex = queue.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }

    Column(
        modifier = modifier.fillMaxSize().background(LocalSaltColors.current.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = SaltSpacing.spacing, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LocalSaltColors.current.text)
            Row {
                if (queue.isNotEmpty()) {
                    Icon(Icons.Filled.Delete, contentDescription = "清空队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable { viewModel.clearQueue() })
                    Spacer(Modifier.width(16.dp))
                }
                Icon(Icons.Filled.Close, contentDescription = "关闭队列", tint = LocalSaltColors.current.text.copy(alpha = 0.8f), modifier = Modifier.size(22.dp).clickable(onClick = onClose))
            }
        }

        val salt = LocalSaltColors.current
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = salt.text.copy(alpha = 0.6f))
            }
        } else {
            val surfaceVariant = salt.surfaceVariant
            val hairline = salt.hairline
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier.background(if (isCurrent) surfaceVariant else Color.Transparent).drawBehind {
                            drawRect(color = hairline, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
                        },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.playAtIndex(index) }.padding(horizontal = SaltSpacing.spacing, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.mediaMetadata.title?.toString() ?: "未知歌曲", color = salt.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.mediaMetadata.artist?.toString() ?: "未知歌手", color = salt.text2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text((index + 1).toString(), color = salt.text2, fontSize = 13.sp)
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.Filled.Close, contentDescription = "从队列删除", tint = salt.text2, modifier = Modifier.size(18.dp).clickable { viewModel.removeQueueItemAt(index) })
                        }
                    }
                }
            }
        }
    }
}

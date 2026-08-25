package com.muses.player.feature.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.muses.player.core.model.Song
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.feature.player.lyric.AmllWebView

/**
 * 播放页 —— PlayerPage.vue 结构翻译（P4 首版）。
 *
 * 层级：AMLL WebView 全屏底层（流体背景+完整歌词，M2 交付物）→
 * info/歌词双面板（Crossfade 切换）→ 固定头部 + FAB。
 *
 * 已知简化（P4 后续迭代项）：
 * - 面板切换用 Crossfade 渐隐渐显替代横滑动画
 * - 下滑关闭手势未实现（当前用左上角关闭按钮）
 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onOpenQueue: () -> Unit = {},
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val position by viewModel.position.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val isSeeking by viewModel.isSeeking.collectAsState()
    val lyricsJson by viewModel.lyricsJson.collectAsState()
    val lyricPosition by viewModel.lyricPosition.collectAsState()
    val hasTranslation by viewModel.hasTranslation.collectAsState()
    val translationEnabled by viewModel.translationEnabled.collectAsState()

    val song = currentMediaItem?.let { item ->
        Song(
            id = item.mediaId,
            sourceId = "",
            path = item.localConfiguration?.uri?.toString() ?: "",
            title = item.mediaMetadata.title?.toString() ?: "未知歌曲",
            artist = item.mediaMetadata.artist?.toString(),
            album = item.mediaMetadata.albumTitle?.toString(),
            durationMs = if (duration > 0) duration else 0L,
            coverUri = item.mediaMetadata.artworkUri?.toString(),
        )
    }

    var seekPosition by remember { mutableFloatStateOf(0f) }
    var showLyricsPanel by remember { mutableIntStateOf(0) } // 0=info 1=歌词

    Box(modifier = modifier.fillMaxSize()) {
        // ---- AMLL 底层：背景 + 歌词 ----
        AmllWebView(
            payloadJson = lyricsJson,
            positionMsFlow = viewModel.lyricPosition,
            isPlaying = viewModel.isPlaying,
            modifier = Modifier.fillMaxSize(),
        )

        androidx.compose.animation.AnimatedContent(
            targetState = showLyricsPanel,
            transitionSpec = {
                // 横滑方向语义：切到歌词=新页从右入、旧页向左出；反向反之
                if (targetState > initialState) {
                    (androidx.compose.animation.slideInHorizontally { it } +
                        androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutHorizontally { -it } +
                            androidx.compose.animation.fadeOut())
                } else {
                    (androidx.compose.animation.slideInHorizontally { -it } +
                        androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutHorizontally { it } +
                            androidx.compose.animation.fadeOut())
                }
            },
            label = "player-panels",
        ) { panel ->
            when (panel) {
                // ===== info 面板 =====
                0 -> Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 固定头部：标题 + 关闭 + 队列
                    var closeDrag by remember { mutableFloatStateOf(0f) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = SaltSpacing.spacing, vertical = 8.dp)
                            // 下滑关闭手势：累计下拉超 120dp 触发 onClose（Web 层拖拽系统简化版）
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dy ->
                                        closeDrag += dy
                                        // 下拉超阈值即关闭播放页（简化版，完整回弹闭环待 P4.3）
                                        if (closeDrag > 400f) {
                                            onClose()
                                        }
                                    },
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭播放页",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onClose),
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = song?.title ?: "未在播放",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.Filled.QueueMusic,
                            contentDescription = "打开播放队列",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onOpenQueue),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // 封面 hero
                    AsyncImage(
                        model = song?.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    )

                    Spacer(Modifier.weight(1f))

                    // 进度条 + 时间行
                    Column(Modifier.padding(horizontal = SaltSpacing.spacing)) {
                        Slider(
                            value = if (isSeeking) seekPosition else if (duration > 0) position.toFloat() / duration else 0f,
                            onValueChange = { seekPosition = it; viewModel.onSeekStart() },
                            onValueChangeFinished = { viewModel.onSeekEnd((seekPosition * duration).toLong()) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                formatTime(if (isSeeking) (seekPosition * duration).toLong() else position),
                                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                            )
                            Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ===== 歌词面板：透出 WebView 完整歌词 =====
                else -> Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {}, // 拦截层：让下层 WebView 接收滚动手势由其自行处理
                )
            }
        }

        // ---- 底部控制区（两面板共用常驻）----
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.25f))
                .navigationBarsPadding()
                .padding(horizontal = SaltSpacing.spacing, vertical = 8.dp),
        ) {
            Slider(
                value = if (isSeeking) seekPosition else if (duration > 0) position.toFloat() / duration else 0f,
                onValueChange = { seekPosition = it; viewModel.onSeekStart() },
                onValueChangeFinished = { viewModel.onSeekEnd((seekPosition * duration).toLong()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatTime(if (isSeeking) (seekPosition * duration).toLong() else position),
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                )
                Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = if (shuffleModeEnabled) "关闭随机播放" else "开启随机播放",
                    tint = if (shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                )
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "上一曲",
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { viewModel.skipToPrevious() },
                )
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { viewModel.playPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "下一曲",
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { viewModel.skipToNext() },
                )
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "播放模式",
                    tint = if (repeatMode == Player.REPEAT_MODE_ONE) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            viewModel.setRepeatMode(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    else -> Player.REPEAT_MODE_OFF
                                },
                            )
                        },
                )
            }
        }

        // ---- 翻译 FAB（左下、控制区上方）----
        if (hasTranslation) {
            Icon(
                Icons.Filled.Translate,
                contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译",
                tint = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 150.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(8.dp)
                    .clickable { viewModel.toggleTranslation() },
            )
        }

        // ---- 歌词/控制面板切换按钮（右上、队列按钮下方）----
        Text(
            text = if (showLyricsPanel == 0) "歌词" else "封面",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 56.dp, end = SaltSpacing.spacing)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable { showLyricsPanel = if (showLyricsPanel == 0) 1 else 0 }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 格式化时长为 m:ss */
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** 队列页 —— QueuePage.vue 一比一翻译 */
@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val salt = com.muses.player.core.ui.theme.LocalSaltColors.current
    val queue by viewModel.queue.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val currentIndex = queue.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.muses.player.core.ui.theme.LocalSaltColors.current.surface),
    ) {
        // __header：标题 + 清空/关闭
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = SaltSpacing.spacing, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row {
                if (queue.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清空队列",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.clearQueue() },
                    )
                }
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭队列",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onClose),
                )
            }
        }

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("队列为空", color = Color.White.copy(alpha = 0.6f))
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier.background(
                            if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playAtIndex(index) }
                                .padding(horizontal = SaltSpacing.spacing, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.mediaMetadata.title?.toString() ?: "未知歌曲",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString() ?: "未知歌手",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                (index + 1).toString(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "从队列删除",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.removeQueueItemAt(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

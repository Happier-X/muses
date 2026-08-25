package com.muses.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import com.muses.player.core.ui.theme.SaltSpacing
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltActionsSheet
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.muses.player.core.model.Song
import com.muses.player.feature.player.lyric.AmllWebView

/** 播放页基础形态：全屏封面 + 控制按钮 + 进度条 */
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
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

    // 从 MediaItem 提取歌曲信息
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

    // 拖拽中的临时位置
    var seekPosition by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // AMLL 底层：歌词 + 流体背景（无词时背景照常渲染，spec 契约）
        AmllWebView(
            payloadJson = lyricsJson,
            positionMsFlow = viewModel.lyricPosition,
            isPlaying = viewModel.isPlaying,
            modifier = Modifier.fillMaxSize(),
        )

        // 左下翻译 FAB：仅当歌词含译文/音译时渲染；白字低视觉权重，激活态高亮
        if (hasTranslation) {
            IconButton(
                onClick = { viewModel.toggleTranslation() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
            ) {
                Icon(
                    Icons.Filled.Translate,
                    contentDescription = if (translationEnabled) "隐藏翻译" else "显示翻译",
                    tint = if (translationEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                )
            }
        }

        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 封面
            AsyncImage(
                model = song?.coverUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentScale = ContentScale.Crop,
            )

            Spacer(Modifier.height(24.dp))

            // 歌曲标题
            Text(
                text = song?.title ?: "未在播放",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            // 艺术家
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // 进度条
            Slider(
                value = if (isSeeking) seekPosition else if (duration > 0) position.toFloat() / duration else 0f,
                onValueChange = { value ->
                    seekPosition = value
                    viewModel.onSeekStart()
                },
                onValueChangeFinished = {
                    viewModel.onSeekEnd((seekPosition * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )

            // 时间显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(if (isSeeking) (seekPosition * duration).toLong() else position),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 随机播放
                IconButton(
                    onClick = { viewModel.setShuffleModeEnabled(!shuffleModeEnabled) },
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = if (shuffleModeEnabled) "关闭随机播放" else "开启随机播放",
                        tint = if (shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                    )
                }

                // 上一首
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // 播放/暂停
                IconButton(
                    onClick = { viewModel.playPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // 下一首
                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // 播放模式
                IconButton(
                    onClick = {
                        val nextMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        viewModel.setRepeatMode(nextMode)
                    },
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> "顺序播放"
                            Player.REPEAT_MODE_ALL -> "列表循环"
                            Player.REPEAT_MODE_ONE -> "单曲循环"
                            else -> "播放模式"
                        },
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color.White else Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

/** 队列页 —— QueuePage.vue 一比一翻译 */
@Composable
fun QueueScreen(
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val salt = com.muses.player.core.ui.theme.LocalSaltColors.current
    val queue by viewModel.queue.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val currentIndex = queue.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }
    val playerConnection = viewModel.playerConnection

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(salt.surface),
    ) {
        // __header：标题 + 清空/关闭按钮
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SaltSpacing.spacing)
                .padding(top = SaltSpacing.spacingSub),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "播放队列",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = salt.text,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (queue.isNotEmpty()) {
                    SaltIconButton(onClick = { playerConnection.clearQueueItems() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "清空队列", tint = salt.text2)
                    }
                }
                SaltIconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭队列", tint = salt.text2)
                }
            }
        }

        // __body：空态 / 列表
        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SaltEmpty(
                    title = "队列为空",
                    description = "从歌曲列表中添加歌曲即可开始播放。",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(queue, key = { _, item -> item.mediaId }) { index, item ->
                    val isCurrent = index == currentIndex
                    Box(
                        Modifier.background(
                            color = if (isCurrent) salt.primary.copy(alpha = 0.1f) else Color.Transparent,
                        ),
                    ) {
                        SaltListItem(
                            title = item.mediaMetadata.title?.toString() ?: "未知歌曲",
                            subtitle = item.mediaMetadata.artist?.toString() ?: "未知歌手",
                            onClick = { playerConnection.playAtIndex(index) },
                            after = {
                                // __row-index：序号 + 移除按钮
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 13.sp,
                                    color = salt.text2,
                                )
                                SaltIconButton(
                                    size = SaltIconButtonSize.SM,
                                    onClick = { playerConnection.removeQueueItemAt(index) },
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "从队列删除",
                                        tint = salt.text2,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

}

/** 格式化时长为 mm:ss */
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

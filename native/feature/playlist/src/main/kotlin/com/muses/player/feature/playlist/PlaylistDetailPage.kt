package com.muses.player.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.SaltCover
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.theme.LocalSaltColors

/**
 * 歌单详情页 —— PlaylistDetailPage.vue 一比一翻译。
 *
 * 结构对照（BEM 类名见各段注释）：
 * - navbar：back 返回 + 标题=歌单名（缺省「歌单」）+ 右侧播放全部按钮
 *   （resolvedSongs 为空时 disabled）
 * - 三态：歌单不存在 / 歌单是空的 / 虚拟列表
 * - 行 `.playlist-detail-page__row`：m-cover 48/radius-sm + 标题 +
 *   「artist - album」副标题 + 当前播放行 primary 10% 高亮 + 移除按钮(#ff3b30)
 */
@Composable
fun PlaylistDetailPage(
    playlistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    viewModel.bind(playlistId)
    val salt = LocalSaltColors.current
    val detail by viewModel.detail.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()

    val playlist = detail?.playlist
    val songs = detail?.songs.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // ---- navbar：back + title + playAll ----
        SaltNavbar(
            title = playlist?.name ?: "歌单",
            left = {
                SaltIconButton(
                    onClick = onBack,
                    contentDescription = "返回",
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            right = {
                SaltIconButton(
                    onClick = { viewModel.playAll() },
                    enabled = songs.isNotEmpty(),
                    contentDescription = "播放全部",
                ) {
                    Icon(Icons.Filled.PlayCircle, contentDescription = null)
                }
            },
        )

        // ---- .playlist-detail-page__content 三态 ----
        when {
            playlist == null -> {
                SaltEmpty(
                    title = "歌单不存在",
                    description = "可能已被删除。",
                    modifier = Modifier.weight(1f),
                )
            }
            songs.isEmpty() -> {
                SaltEmpty(
                    title = "歌单是空的",
                    description = "在歌曲页点「更多」→「加入歌单」添加歌曲。",
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(songs.size, key = { songs[it].id }) { index ->
                        val song = songs[index]
                        val isPlaying = currentSongId == song.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isPlaying) {
                                        Modifier.background(salt.primary.copy(alpha = 0.1f))
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            DetailSongRow(
                                song = song,
                                isPlaying = isPlaying,
                                onPlay = { viewModel.playSongFromList(song.id) },
                                onRemove = { viewModel.remove(song.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 重命名入口在列表页操作面板；详情页保留对话框状态以防外部触发
    if (viewModel.renameVisible) {
        NameEditDialog(
            title = "重命名歌单",
            initialName = playlist?.name.orEmpty(),
            label = "歌单名称",
            onDismiss = { viewModel.dismissRename() },
            onConfirm = {
                viewModel.rename(it)
                viewModel.dismissRename()
            },
        )
    }
}

/** 单行：封面 48/sm + 标题/「artist - album」+ 移除按钮（#ff3b30） */
@Composable
private fun DetailSongRow(
    song: Song,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val salt = LocalSaltColors.current
    SaltListItem(
        title = song.title,
        subtitle = listOfNotNull(song.artist, song.album)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifEmpty { null },
        strongTitle = true,
        onClick = onPlay,
        leading = {
            SaltCover(uri = song.coverUri, size = 48.dp, radius = SaltCoverRadius.SM)
        },
        after = {
            SaltIconButton(
                onClick = onRemove,
                contentDescription = "从歌单移除 ${song.title}",
            ) {
                Icon(
                    Icons.Filled.RemoveCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF3B30),
                )
            }
        },
    )
}

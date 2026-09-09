package com.muses.player.feature.playlist

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesTopBar

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
    viewModel: PlaylistDetailViewModel = koinViewModel(),
) {
    viewModel.bind(playlistId)
    val scheme = MiuixTheme.colorScheme
    val detail by viewModel.detail.collectAsState()
    val currentSongId by viewModel.currentSongId.collectAsState()

    val playlist = detail?.playlist
    val songs = detail?.songs.orEmpty()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            MusesTopBar(
                title = playlist?.name ?: "歌单",
                onBack = onBack,
                actions = {
                    MusesIconButton(
                        onClick = { viewModel.playAll() },
                        enabled = songs.isNotEmpty(),
                        contentDescription = "播放全部",
                    ) {
                        Icon(TablerIcons.Play, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        // overlay 层：与原外层 Box 严格对应（对话框/浮层挂载域，层级 1:1）
        Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                playlist == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusesEmpty(title = "歌单不存在", description = "可能已被删除。")
                    }
                }
                songs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusesEmpty(
                            title = "歌单是空的",
                            description = "在歌曲页点「更多」→「加入歌单」添加歌曲。",
                        )
                    }
                }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                    items(songs.size, key = { songs[it].id }) { index ->
                        val song = songs[index]
                        val isPlaying = currentSongId == song.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isPlaying) {
                                        Modifier.background(scheme.primary.copy(alpha = 0.1f))
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
                // 顶栏已上收 Scaffold topBar 槽（原生小顶栏 + 返回）
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
    val scheme = MiuixTheme.colorScheme
    MusesListRow(
        title = song.title,
        subtitle = listOfNotNull(song.artist, song.album)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifEmpty { null },
        onClick = onPlay,
        leading = {
            MusesCover(uri = song.coverUri, size = 48.dp, radius = MusesCoverRadius.SM)
        },
        after = {
            MusesIconButton(
                onClick = onRemove,
                contentDescription = "从歌单移除 ${song.title}",
            ) {
                Icon(
                    TablerIcons.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFF3B30),
                )
            }
        },
    )
}

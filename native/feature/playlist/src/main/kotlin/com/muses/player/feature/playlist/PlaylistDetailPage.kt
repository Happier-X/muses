package com.muses.player.feature.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.model.PlaylistWithSongs

/**
 * 播放列表详情：歌曲按播放顺序展示。
 * 排序先用上移/下移按钮保证功能闭环，拖拽排序后置（implement.md 阶段 2）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailPage(
    playlistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    viewModel.bind(playlistId)
    val detail by viewModel.detail.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        detail?.playlist?.name ?: "播放列表",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("重命名") }, onClick = {
                            menuExpanded = false
                            viewModel.showRename()
                        })
                        DropdownMenuItem(text = { Text("删除播放列表") }, onClick = {
                            menuExpanded = false
                            viewModel.deletePlaylist()
                            onBack()
                        })
                    }
                },
            )
        },
    ) { innerPadding ->
        val songs = detail?.songs.orEmpty()
        if (detail == null || songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (detail == null) "加载中…" else "播放列表为空",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(songs.size, key = { songs[it].id }) { index ->
                    val song = songs[index]
                    ListItem(
                        headlineContent = {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(
                                listOfNotNull(song.artist, song.album).joinToString(" · ")
                                    .ifEmpty { "未知艺术家" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            Row(modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = { viewModel.move(index, index - 1) },
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移")
                                }
                                IconButton(
                                    enabled = index < songs.lastIndex,
                                    onClick = { viewModel.move(index, index + 1) },
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移")
                                }
                                IconButton(onClick = { viewModel.remove(song.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "移除歌曲")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (viewModel.renameVisible) {
        NameEditDialog(
            title = "重命名播放列表",
            initialName = detail?.playlist?.name.orEmpty(),
            onDismiss = { viewModel.dismissRename() },
            onConfirm = {
                viewModel.rename(it)
                viewModel.dismissRename()
            },
        )
    }
}

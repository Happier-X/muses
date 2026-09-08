package com.muses.player.feature.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import com.muses.player.core.ui.components.MusesBottomSheet
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
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.model.Playlist

/**
 * 「加入播放列表」底部弹层（可复用组件）：
 * 列出全部播放列表，点选即把 [songIds] 全部追加进去；
 * 底部提供「新建播放列表并加入」。
 *
 * 本次不接入 library 长按菜单（M1 冲突面），调用方后续直接挂载即可。
 */
@Composable
fun AddToPlaylistSheet(
    songIds: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddToPlaylistViewModel = koinViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    MusesBottomSheet(onDismiss = onDismiss, title = "加入播放列表", modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable {
                            viewModel.addTo(playlist.id, songIds)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(TablerIcons.QueueMusic, contentDescription = null)
                    Text(
                        playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
            }
            item(key = "create") {
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable { showCreateDialog = true }
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(TablerIcons.Add, contentDescription = null)
                    Text(
                        "新建播放列表并加入",
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameEditDialog(
            title = "新建播放列表",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createAndAdd(name, songIds)
                showCreateDialog = false
                onDismiss()
            },
        )
    }
}

package com.muses.player.feature.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.PlaylistRepository
import com.muses.player.core.model.Playlist
import com.muses.player.core.ui.components.SaltActionsSheet
import com.muses.player.core.ui.components.SaltActionItem
import com.muses.player.core.ui.components.SaltCover
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.theme.LocalSaltColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单页 —— PlaylistsPage.vue 一比一翻译。
 *
 * 结构对照（BEM 类名见各段注释）：
 * - navbar：SaltNavbar(title=歌单, right=新建按钮 32dp clear rounded + add 图标 16px)
 * - 空态：m-empty「还没有歌单 / 点右上角新建，或在歌曲页「更多」加入歌单。」(icon=list)
 * - 列表行 `.playlists-page__row`（min-height --m-list-row-h、hairline 分隔）：
 *   m-cover 48/radius-sm(placeholder=list) → 标题 17/600 单行省略 +
 *   「N 首」13px text2 → more 按钮（16px 图标）
 * - 排序 updatedAt desc；validCount = playlist.songIds ∩ 当前曲库（曲库删除实时联动）
 */
data class PlaylistRow(val playlist: Playlist, val name: String, val validCount: Int)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {

    /** playlists(updatedAt desc) × 曲库 → 行数据；曲库删除时 validCount 实时联动（listRows computed） */
    val rows: StateFlow<List<PlaylistRow>> =
        combine(
            repository.observePlaylists(),
            repository.observeValidCounts(),
        ) { playlists, validCounts ->
            playlists
                .sortedByDescending { it.updatedAt }
                .map { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        name = playlist.name,
                        validCount = validCounts[playlist.id] ?: 0,
                    )
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createPlaylist(name.trim()) }
    }

    fun renamePlaylist(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renamePlaylist(id, name.trim()) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { repository.deletePlaylist(id) }
    }
}

private enum class NameDialogMode { CREATE, RENAME }

/** m-dialog（新建/重命名共用）的状态组 */
private data class NameDialogState(
    val mode: NameDialogMode,
    val initialName: String,
    val targetId: String? = null,
)

@Composable
fun PlaylistsPage(
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val salt = LocalSaltColors.current
    val rows by viewModel.rows.collectAsState()

    // Vue ref 组：activePlaylistId / isActionsOpen / isNameAlertOpen / isDeleteAlertOpen
    var actionsTargetId by remember { mutableStateOf<String?>(null) }
    var nameDialog by remember { mutableStateOf<NameDialogState?>(null) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // ---- .playlists-page__navbar-wrap：m-navbar title=歌单 ----
        SaltNavbar(
            title = "歌单",
            right = {
                SaltIconButton(
                    onClick = {
                        nameDialog = NameDialogState(NameDialogMode.CREATE, initialName = "")
                    },
                    size = com.muses.player.core.ui.components.SaltIconButtonSize.SM,
                    contentDescription = "新建歌单",
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            },
        )

        // ---- .playlists-page__content ----
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SaltEmpty(
                    title = "还没有歌单",
                    description = "点右上角新建，或在歌曲页「更多」加入歌单。",
                    icon = Icons.Filled.QueueMusic,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(rows, key = { it.playlist.id }) { row ->
                    SaltListItem(
                        title = row.name,
                        subtitle = "${row.validCount} 首",
                        strongTitle = true,
                        onClick = { onOpenPlaylist(row.playlist.id) },
                        leading = {
                            SaltCover(uri = null, size = 48.dp, radius = SaltCoverRadius.SM)
                        },
                        after = {
                            SaltIconButton(
                                onClick = { actionsTargetId = row.playlist.id },
                                contentDescription = "更多歌单操作",
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = salt.text2,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    // ---- m-actions：歌单操作（重命名 / 删除 / 取消）----
    actionsTargetId?.let { targetId ->
        SaltActionsSheet(
            opened = true,
            onDismiss = { actionsTargetId = null },
            label = "歌单操作",
            items = listOf(
                SaltActionItem(label = "重命名", onClick = {
                    val current = rows.firstOrNull { it.playlist.id == targetId }?.name.orEmpty()
                    nameDialog = NameDialogState(NameDialogMode.RENAME, current, targetId)
                    actionsTargetId = null
                }),
                SaltActionItem(label = "删除", onClick = {
                    deleteTargetId = targetId
                    actionsTargetId = null
                }),
                SaltActionItem(label = "取消", onClick = { actionsTargetId = null }),
            ),
        )
    }

    // ---- m-dialog：新建/重命名歌单名（mListInput label=歌单名称）----
    nameDialog?.let { dialog ->
        NameEditDialog(
            title = if (dialog.mode == NameDialogMode.CREATE) "新建歌单" else "重命名歌单",
            initialName = dialog.initialName,
            label = "歌单名称",
            onDismiss = { nameDialog = null },
            onConfirm = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    val targetId = dialog.targetId
                    if (targetId != null) {
                        viewModel.renamePlaylist(targetId, trimmed)
                    } else {
                        viewModel.createPlaylist(trimmed)
                    }
                }
                nameDialog = null
            },
        )
    }

    // ---- m-dialog：删除确认（deleteMessage 文案逐字对齐）----
    deleteTargetId?.let { targetId ->
        val name = rows.firstOrNull { it.playlist.id == targetId }?.name ?: "该歌单"
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("删除歌单") },
            text = {
                Text(
                    "确定删除「$name」？此操作不可撤销。",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = salt.text2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(targetId)
                    deleteTargetId = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text("取消") }
            },
        )
    }
}

/** 新建/重命名共用对话框（m-dialog + mListInput） */
@Composable
internal fun NameEditDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    label: String = "名称",
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

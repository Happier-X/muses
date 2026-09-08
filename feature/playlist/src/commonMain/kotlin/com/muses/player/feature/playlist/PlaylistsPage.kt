package com.muses.player.feature.playlist

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.muses.player.core.ui.theme.LocalHazeBlurState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.PlaylistRepository
import com.muses.player.core.model.Playlist
import com.muses.player.core.ui.components.MusesActionsSheet
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.components.MusesActionItem
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesNavbar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单页 —— PlaylistsPage.vue 一比一翻译。
 *
 * 结构对照（BEM 类名见各段注释）：
 * - navbar：MusesNavbar(title=歌单, right=新建按钮 32dp clear rounded + add 图标 16px)
 * - 空态：m-empty「还没有歌单 / 点右上角新建，或在歌曲页「更多」加入歌单。」(icon=list)
 * - 列表行 `.playlists-page__row`（min-height --m-list-row-h、hairline 分隔）：
 *   m-cover 48/radius-sm(placeholder=list) → 标题 17/600 单行省略 +
 *   「N 首」13px text2 → more 按钮（16px 图标）
 * - 排序 updatedAt desc；validCount = playlist.songIds ∩ 当前曲库（曲库删除实时联动）
 */
data class PlaylistRow(val playlist: Playlist, val name: String, val validCount: Int)

class PlaylistsViewModel constructor(
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
    viewModel: PlaylistsViewModel = koinViewModel(),
) {
    val scheme = MiuixTheme.colorScheme
    val rows by viewModel.rows.collectAsState()

    // Vue ref 组：activePlaylistId / isActionsOpen / isNameAlertOpen / isDeleteAlertOpen
    var actionsTargetId by remember { mutableStateOf<String?>(null) }
    var nameDialog by remember { mutableStateOf<NameDialogState?>(null) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalHazeBlurState provides hazeState) {
        Box(modifier = modifier.fillMaxSize()) {
            val navbarTopPadding = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }.coerceAtLeast(16.dp) + 44.dp
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(MiuixTheme.colorScheme.background),
            ) {
                if (rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = navbarTopPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        MusesEmpty(
                            title = "还没有歌单",
                            description = "点右上角新建，或在歌曲页「更多」加入歌单。",
                            icon = TablerIcons.QueueMusic,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = navbarTopPadding, bottom = 96.dp),
                    ) {
                items(rows, key = { it.playlist.id }) { row ->
                    MusesListRow(
                        title = row.name,
                        subtitle = "${row.validCount} 首",
                        onClick = { onOpenPlaylist(row.playlist.id) },
                        leading = {
                            MusesCover(uri = null, size = 48.dp, radius = MusesCoverRadius.SM)
                        },
                        after = {
                            MusesIconButton(
                                onClick = { actionsTargetId = row.playlist.id },
                                contentDescription = "更多歌单操作",
                            ) {
                                Icon(
                                    TablerIcons.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = scheme.onBackgroundVariant,
                                )
                            }
                        },
                    )
                        }
                    }
                }
            }
            MusesNavbar(
                title = "歌单",
                modifier = Modifier.align(Alignment.TopCenter),
                right = {
                    MusesIconButton(
                        onClick = {
                            nameDialog = NameDialogState(NameDialogMode.CREATE, initialName = "")
                        },
                        size = com.muses.player.core.ui.components.MusesIconButtonSize.SM,
                        contentDescription = "新建歌单",
                    ) {
                        Icon(
                            TablerIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
            )
        }
    }

    // ---- m-actions：歌单操作（重命名 / 删除 / 取消）----
    actionsTargetId?.let { targetId ->
        MusesActionsSheet(
            opened = true,
            onDismiss = { actionsTargetId = null },
            label = "歌单操作",
            items = listOf(
                MusesActionItem(label = "重命名", onClick = {
                    val current = rows.firstOrNull { it.playlist.id == targetId }?.name.orEmpty()
                    nameDialog = NameDialogState(NameDialogMode.RENAME, current, targetId)
                    actionsTargetId = null
                }),
                MusesActionItem(label = "删除", onClick = {
                    deleteTargetId = targetId
                    actionsTargetId = null
                }),
                MusesActionItem(label = "取消", onClick = { actionsTargetId = null }),
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

    // ---- m-dialog：删除确认（deleteMessage 文案逐字对齐；miuix MusesDialog）----
    deleteTargetId?.let { targetId ->
        val name = rows.firstOrNull { it.playlist.id == targetId }?.name ?: "该歌单"
        MusesDialog(
            onDismiss = { deleteTargetId = null },
            title = "删除歌单",
            message = "确定删除「$name」？此操作不可撤销。",
            confirmText = "删除",
            onConfirm = {
                viewModel.deletePlaylist(targetId)
                deleteTargetId = null
            },
            destructiveConfirm = true,
            dismissText = "取消",
        )
    }
}

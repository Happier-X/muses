package com.muses.player.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.muses.player.core.media.playback.PlayerConnection
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.SaltActionsSheet
import com.muses.player.core.ui.components.SaltActionItem
import com.muses.player.core.ui.components.SaltCover
import com.muses.player.core.ui.components.SaltCoverRadius
import com.muses.player.core.ui.components.SaltEmpty
import com.muses.player.core.ui.components.SaltIconButton
import com.muses.player.core.ui.components.SaltIconButtonSize
import com.muses.player.core.ui.components.SaltListItem
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors

/**
 * 歌曲页 —— SongsPage.vue 一比一翻译。
 *
 * 结构对照（BEM 类名见各段注释）：
 * - `.songs-page__navbar`：SaltNavbar(title=歌曲, right=搜索) + subnavbar
 *   （工具条 ↔ 搜索栏二选一，同一块玻璃无分界线）
 * - 工具条 `.songs-page__toolbar-left`：随机播放按钮 + 歌曲总数；多选时加计数
 * - 列表行：SaltListItem(title, subtitle="artist - album",
 *   leading=封面 54/radius-sm 或多选 checkbox，after=⋮ 三点菜单)
 * - 行点击：多选切换选择；否则全列表入队播放该曲
 * - 空态 m-empty；多选底部操作条 multibar；⋮ 动作单 m-actions
 */
@Composable
fun SongsPage(
    playerConnection: PlayerConnection?,
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = hiltViewModel(),
) {
    // 非 null 时弹出「加入播放列表」弹层（复用 M2 AddToPlaylistSheet）
    var addToPlaylistTarget by remember { mutableStateOf<List<String>?>(null) }
    addToPlaylistTarget?.let { songIds ->
        com.muses.player.feature.playlist.AddToPlaylistSheet(
            songIds = songIds,
            onDismiss = { addToPlaylistTarget = null },
        )
    }
    val salt = LocalSaltColors.current
    val songs by viewModel.songs.collectAsState()

    // ---- 页面状态 ----
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isMultiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var actionSong by remember { mutableStateOf<Song?>(null) }

    fun exitSearch() {
        isSearching = false
        searchQuery = ""
        viewModel.updateSearchQuery("")
    }

    fun exitMultiSelect() {
        isMultiSelect = false
        selectedIds = emptySet()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ---- navbar + subnavbar（同一块玻璃）----
        SaltNavbar(
            title = "歌曲",
            right = {
                SaltIconButton(onClick = {
                    isSearching = true
                    searchQuery = ""
                    if (isMultiSelect) exitMultiSelect()
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "搜索歌曲")
                }
            },
            subnavbar = {
                when {
                    songs.isNotEmpty() && !isSearching -> {
                        // .songs-page__toolbar-left：随机播放按钮 + 歌曲总数
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SaltIconButton(
                                onClick = {
                                    if (songs.isEmpty()) return@SaltIconButton
                                    // 随机播放全部：入队后开 shuffle
                                    playerConnection?.apply {
                                        play(songs.first().id, songs)
                                        setShuffleModeEnabled(true)
                                    }
                                },
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = "随机播放全部")
                            }
                            Text(
                                text = songs.size.toString(),
                                fontSize = 15.sp,
                                color = salt.text,
                            )
                            if (isMultiSelect) {
                                Text(
                                    text = "已选中 ${selectedIds.size} 项",
                                    fontSize = 15.sp,
                                    color = salt.text2,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }

                    songs.isNotEmpty() && isSearching -> {
                        // .songs-page__searchbar
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = salt.text2,
                                modifier = Modifier.size(18.dp),
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "在 ${songs.size} 首歌曲中搜索",
                                        fontSize = 16.sp,
                                        color = salt.text2,
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        viewModel.updateSearchQuery(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 16.sp, color = salt.text),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            SaltTextButton(text = "取消", onClick = { exitSearch() })
                        }
                    }
                }
            },
        )

        // ---- 列表 / 空态 ----
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SaltEmpty(
                    title = "还没有歌曲",
                    description = "请先到音源页添加并扫描音源。",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                // MiniPlayerBar 叠加时底部留白（Web 层 --m-content-pb 同语义）
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                    val checked = isMultiSelect && song.id in selectedIds
                    SaltListItem(
                        title = song.title,
                        subtitle = "${song.artist ?: "未知艺术家"} - ${song.album ?: "未知专辑"}",
                        onClick = {
                            if (isMultiSelect) {
                                selectedIds =
                                    if (song.id in selectedIds) selectedIds - song.id
                                    else selectedIds + song.id
                            } else {
                                playerConnection?.play(song.id, songs)
                            }
                        },
                        onLongClick = if (!isMultiSelect) {
                            {
                                isMultiSelect = true
                                selectedIds = setOf(song.id)
                            }
                        } else {
                            null
                        },
                        leading = {
                            if (isMultiSelect) {
                                // .songs-page__select-box：多选选择框
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .background(
                                            color = if (checked) salt.primary else salt.surface2,
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (checked) salt.primary else salt.text2,
                                            shape = RoundedCornerShape(6.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = salt.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            } else {
                                SaltCover(
                                    uri = song.coverUri,
                                    size = 54.dp,
                                    radius = SaltCoverRadius.SM,
                                )
                            }
                        },
                        after = {
                            if (!isMultiSelect) {
                                // 椒盐式实心三点菜单
                                SaltIconButton(
                                    size = SaltIconButtonSize.SM,
                                    onClick = { actionSong = song },
                                ) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "更多歌曲操作")
                                }
                            }
                        },
                        dividers = true,
                    )
                }
            }
        }
    }

    // ---- ⋮ 动作单（m-actions）----
    val currentId = actionSong?.id
    SaltActionsSheet(
        opened = actionSong != null,
        onDismiss = { actionSong = null },
        label = "歌曲操作",
        items = listOf(
            SaltActionItem(label = "添加到队列", onClick = {
                // TODO(P2b)：PlayerConnection 补 addToQueue 后接线
                actionSong = null
            }),
            SaltActionItem(label = "加入待刮削", onClick = {
                // TODO(M3)：接刮削队列 UI（引擎已在 core:scrape 就绪）
                actionSong = null
            }),
            SaltActionItem(label = "加入歌单…", onClick = {
                // 等动作单关闭后再开歌单弹层（Web 层 180ms 延迟同语义）
                val ids = listOfNotNull(currentId)
                actionSong = null
                if (ids.isNotEmpty()) addToPlaylistTarget = ids
            }),
        ),
    )

    // ---- 多选底部操作条（.songs-page__multibar）----
    if (isMultiSelect) {
        MultiselectBottomBar(
            selectedCount = selectedIds.size,
            onDeleteSelected = {
                viewModel.deleteByIds(selectedIds)
                exitMultiSelect()
            },
            onAddToPlaylist = {
                if (selectedIds.isNotEmpty()) addToPlaylistTarget = selectedIds.toList()
                exitMultiSelect()
            },
            onPlaySelected = {
                val picked = songs.filter { it.id in selectedIds }
                if (picked.isNotEmpty()) playerConnection?.play(picked.first().id, picked)
                exitMultiSelect()
            },
            onCancel = { exitMultiSelect() },
        )
    }
}

/** 多选底部操作条：clear 按钮横排，danger 红 */
@Composable
private fun MultiselectBottomBar(
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlaySelected: () -> Unit,
    onCancel: () -> Unit,
) {
    val salt = LocalSaltColors.current
    val disabled = selectedCount == 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(salt.surface1)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SaltTextButton(text = "永久删除", destructive = true, enabled = !disabled, onClick = onDeleteSelected)
        SaltTextButton(text = "添加到歌单", enabled = !disabled, onClick = onAddToPlaylist)
        SaltTextButton(text = "播放选中队列", enabled = !disabled, onClick = onPlaySelected)
        SaltTextButton(text = "取消", onClick = onCancel)
    }
}

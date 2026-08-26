package com.muses.player.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import com.muses.player.core.ui.components.SaltListItemMetrics
import com.muses.player.core.ui.components.SaltNavbar
import com.muses.player.core.ui.components.SaltTextButton
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.core.ui.theme.SaltRadius
import com.muses.player.core.ui.theme.SaltDarkColors
import com.muses.player.core.ui.theme.SaltShadowLayer
import com.muses.player.core.ui.theme.saltShadow

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

    // ---- 跳转到当前播放（SongsPage.vue scrollToCurrentSong/jump-fab 组）----
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 当前播放歌曲 id（playerState.currentSong.id；null = 未播放）
    val currentSongId: String? = playerConnection?.currentMediaItem?.let { flow ->
        flow.collectAsState().value?.mediaId
    }

    // 列表滚动中防抖 300ms（对照 onListScroll/isListScrolling：滚动时隐藏气泡不挡更多按钮）
    var scrollSettled by remember { mutableStateOf(true) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            scrollSettled = false
        } else {
            delay(300)
            scrollSettled = true
        }
    }

    // 当前歌曲「在列表 / 在可视区」（snapshotFlow 响应滚动帧；visibleItemsInfo 即真实可视区，无 overscan）
    val (currentInList, currentInViewport) = remember(currentSongId, songs) {
        snapshotFlow {
            if (currentSongId == null) return@snapshotFlow false to false
            val idx = songs.indexOfFirst { it.id == currentSongId }
            if (idx < 0) return@snapshotFlow false to false
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.find { it.index == idx }
                ?: return@snapshotFlow true to false
            val visible = item.offset < info.viewportEndOffset &&
                item.offset + item.size > info.viewportStartOffset
            true to visible
        }
    }.collectAsState(Pair(false, false)).value

    // showJumpBubble：在列表 && 滚出可视区 && 非滚动中
    val showJumpBubble = currentInList && !currentInViewport && scrollSettled

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

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                state = listState,
                // MiniPlayerBar 叠加时底部留白（Web 层 --m-content-pb 同语义）
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                    val checked = isMultiSelect && song.id in selectedIds
                    SaltListItem(
                        modifier = Modifier.background(
                            // Web .songs-page__row.is-selected：rgba(var(--m-primary-rgb), .08)
                            color = if (checked) salt.primary.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(SaltRadius.sm),
                        ),
                        title = song.title,
                        subtitle = "${song.artist ?: "未知艺术家"} - ${song.album ?: "未知专辑"}",
                        // Web .songs-page :deep(.m-list-item)：72dp 行高/16-12px 字号/紧凑 after
                        metrics = SaltListItemMetrics.SongsDense,
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
                            } else if (song.coverUri != null) {
                                SaltCover(
                                    uri = song.coverUri,
                                    size = 54.dp,
                                    radius = SaltCoverRadius.SM,
                                )
                                // Web .m-list-item__inner padding-left:12px —— 封面-标题间距对齐椒盐
                                Spacer(Modifier.width(12.dp))
                            } else {
                                // Web .songs-page__cover 无封面覆盖：透明底 32dp 占位图标 opacity .45
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = salt.text.copy(alpha = 0.45f),
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.width(12.dp))
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
                        // Web 版 <m-list :dividers="false">：椒盐歌曲列表无行间分割线
                        dividers = false,
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

    // ---- m-fab.songs-page__jump-fab：跳转到当前播放 ----
    if (showJumpBubble) {
        JumpToCurrentFab(
            onClick = {
                val idx = songs.indexOfFirst { it.id == currentSongId }
                if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
            },
            // fixed right 16px / bottom 96px（对齐椒盐：底距 MiniPlayer 顶 ~24dp）
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
        )
    }
    }
}

/**
 * 跳转当前播放 FAB（`.songs-page__jump-fab` 一比一翻译）：
 * MFab 44px 圆底被页面样式覆盖为液态玻璃配方 —— glass-bg 半透明圆底（blur 由
 * 半透明底承担，同 navbar 策略）+ 顶部内高光 1px + 白色高光描边 + text-2 图标；
 * :active 底色 rgba(primary, 0.5)。
 */
@Composable
private fun JumpToCurrentFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val salt = LocalSaltColors.current
    val isDark = salt === SaltDarkColors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .saltShadow(
                CircleShape,
                // __jump-fab 自身覆盖的 box-shadow：0 2px 8px rgba(0,0,0,.12)（暗色 .3）；
                // 内高光由下方 drawBehind 画（inset 0 1px 0 white .65/.1）
                listOf(
                    SaltShadowLayer(
                        offsetY = 2.dp,
                        blurRadius = 8.dp,
                        color = if (isDark) Color(0x4D000000) else Color(0x1F000000),
                    ),
                ),
            )
            .background(salt.glassBg)
            .drawBehind {
                // inset 0 1px 0 rgba(255,255,255,.65)（暗色 .1）顶部内高光
                drawRect(
                    color = Color.White.copy(alpha = if (isDark) 0.1f else 0.65f),
                    topLeft = Offset.Zero,
                    size = Size(size.width, 1f),
                )
                // border 1px rgba(255,255,255,.5)（暗色 .12）
                drawCircle(
                    color = Color.White.copy(alpha = if (isDark) 0.12f else 0.5f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MyLocation,
            contentDescription = "跳转到当前播放",
            tint = salt.text2,
            modifier = Modifier.size(20.dp),
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

package com.muses.player.feature.library

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.isSystemInDarkTheme
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
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Text
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
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.muses.player.core.playback.PlaybackMeta
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.model.Song
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.muses.player.core.ui.components.MusesActionsSheet
import com.muses.player.core.ui.components.MusesActionItem
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesCoverRadius
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesIconButtonSize
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.theme.LocalHazeBlurState
import com.muses.player.core.ui.theme.MusesShadowLayer
import com.muses.player.core.ui.theme.saltShadow
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * 歌曲页 —— SongsPage.vue 一比一翻译。
 *
 * 结构对照（BEM 类名见各段注释）：
 * - `.songs-page__navbar`：MusesTopBar(title=歌曲, right=搜索) + bottomContent
 *   （工具条 ↔ 搜索栏二选一，同一块玻璃无分界线）
 * - 工具条 `.songs-page__toolbar-left`：随机播放按钮 + 歌曲总数；多选时加计数
 * - 列表行：MusesListRow(title, subtitle="artist - album",
 *   leading=封面 54/radius-sm 或多选 checkbox，after=⋮ 三点菜单)
 * - 行点击：多选切换选择；否则全列表入队播放该曲
 * - 空态 m-empty；多选底部操作条 multibar；⋮ 动作单 m-actions
 */
@Composable
fun SongsPage(
    /** 播放端口（U16：commonMain 感知端口而非安卓 PlayerConnection；null = 未接线） */
    playback: PlaybackPort?,
    /** M3：加入待刮削队列（经回调注入，feature:library 不直接依赖 core:scrape） */
    onEnqueueScrape: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = koinViewModel(),
) {
    // 非 null 时弹出「加入播放列表」弹层（复用 M2 AddToPlaylistSheet）
    var addToPlaylistTarget by remember { mutableStateOf<List<String>?>(null) }
    addToPlaylistTarget?.let { songIds ->
        com.muses.player.feature.playlist.AddToPlaylistSheet(
            songIds = songIds,
            onDismiss = { addToPlaylistTarget = null },
        )
    }
    val scheme = MiuixTheme.colorScheme
    val songs by viewModel.songs.collectAsState()

    // ---- 跳转到当前播放（SongsPage.vue scrollToCurrentSong/jump-fab 组）----
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 当前播放歌曲 id（playerState.currentSong.id；null = 未播放）
    val currentSongId: String? = playback?.currentSongId?.let { flow ->
        flow.collectAsState().value
    }
    val currentMeta by playback?.currentMeta?.collectAsState() ?: remember { mutableStateOf<PlaybackMeta?>(null) }

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

    // 阶段二顶栏换原生：自绘 MusesNavbar 玻璃退役，haze 局部态随之下线；
    // FAB 改吃 TabsLayout 全局 Haze（环境值直达，无需中转）。
    fun doEnqueue(ids: List<String>) {
        if (ids.isEmpty()) return
        onEnqueueScrape(ids)
        val msg = if (ids.size == 1) "已加入待刮削队列" else "已加入 ${ids.size} 首到待刮削队列"
        com.muses.player.core.uishared.platform.PlatformToast.show(msg)
    }
    // 阶段二槽位化：顶栏进 Scaffold topBar（原生大标题折叠），列表进 content，
    // FAB 进 floatingActionButton 槽（自动避让停靠迷你条），多选条见下方 E5 浮层。
    val topBarScrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            MusesTopBar(
                title = "歌曲",
                largeTitle = "歌曲",
                actions = {
                    MusesIconButton(onClick = {
                        isSearching = true
                        searchQuery = ""
                        if (isMultiSelect) exitMultiSelect()
                    }) {
                        Icon(TablerIcons.Search, contentDescription = "搜索歌曲")
                    }
                },
                bottomContent = {
                    when {
                        songs.isNotEmpty() && !isSearching -> {
                            // .songs-page__toolbar-left：随机播放按钮 + 歌曲总数
                            // 随机播放全部：随机挑一首作为起点（对齐原版 onShuffleAll
                            // 「先 shuffle 再取随机第 0 首」——Media3 的 shuffleMode
                            // 只影响后续顺序、不改变当前曲，固定 first 会永远播第一首），
                            // 开 shuffle 保证后续顺序也随机
                            val shuffleAll: () -> Unit = {
                                if (songs.isNotEmpty()) {
                                    playback?.apply {
                                        play(songs.random().id, songs)
                                        setShuffleEnabled(true)
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // 图标与歌曲数同属一个可点区域（用户定案：点数字同样触发随机播放）
                                Row(
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = shuffleAll,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    MusesIconButton(onClick = shuffleAll) {
                                        Icon(TablerIcons.Shuffle, contentDescription = "随机播放全部")
                                    }
                                    Text(
                                        text = songs.size.toString(),
                                        fontSize = 15.sp,
                                        color = scheme.onBackground,
                                    )
                                }
                                if (isMultiSelect) {
                                    Text(
                                        text = "已选中 ${selectedIds.size} 项",
                                        fontSize = 15.sp,
                                        color = scheme.onBackgroundVariant,
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
                                    TablerIcons.Search,
                                    contentDescription = null,
                                    tint = scheme.onBackgroundVariant,
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
                                            color = scheme.onBackgroundVariant,
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            viewModel.updateSearchQuery(it)
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 16.sp, color = scheme.onBackground),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                MusesTextButton(text = "取消", onClick = { exitSearch() })
                            }
                        }
                    }
                },
                scrollBehavior = topBarScrollBehavior,
            )
        },
        floatingActionButton = {
            if (showJumpBubble) {
                JumpToCurrentFab(
                    onClick = {
                        val idx = songs.indexOfFirst { it.id == currentSongId }
                        if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MusesEmpty(
                        title = "还没有歌曲",
                        description = "请先到音源页添加并扫描音源。",
                    )
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                    state = listState,
                    contentPadding = PaddingValues(bottom = if (isMultiSelect) 64.dp else 16.dp),
                ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                    val checked = isMultiSelect && song.id in selectedIds
                    MusesListRow(
                        modifier = Modifier.background(
                            // Web .songs-page__row.is-selected：rgba(var(--m-primary-rgb), .08)
                            color = if (checked) scheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        ),
                        title = run {
                            val useMetaForTitle = song.id == currentSongId
                                && song.metaSources?.title == null
                                && song.tagsVersion < com.muses.player.core.data.db.SongTags.TAGS_VERSION
                            if (useMetaForTitle) currentMeta?.title?.trim()?.takeIf { it.isNotEmpty() } ?: song.title else song.title
                        },
                        subtitle = run {
                            val isCurrent = song.id == currentSongId
                            val useMetaArtist = isCurrent && song.metaSources?.artist == null && song.tagsVersion < com.muses.player.core.data.db.SongTags.TAGS_VERSION
                            val useMetaAlbum = isCurrent && song.metaSources?.album == null && song.tagsVersion < com.muses.player.core.data.db.SongTags.TAGS_VERSION
                            val metaArtist = if (useMetaArtist) currentMeta?.artist?.trim()?.takeIf { it.isNotEmpty() } else null
                            val metaAlbum = if (useMetaAlbum) currentMeta?.album?.trim()?.takeIf { it.isNotEmpty() } else null
                            "${metaArtist ?: song.artist ?: "未知艺术家"} - ${metaAlbum ?: song.album ?: "未知专辑"}"
                        },
                        // Web .songs-page :deep(.m-list-item)：72dp 行高/16-12px 字号/紧凑 after
                        onClick = {
                            if (isMultiSelect) {
                                selectedIds =
                                    if (song.id in selectedIds) selectedIds - song.id
                                    else selectedIds + song.id
                            } else {
                                playback?.play(song.id, songs)
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
                                            color = if (checked) scheme.primary else scheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (checked) scheme.primary else scheme.onBackgroundVariant,
                                            shape = RoundedCornerShape(6.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) {
                                        Icon(
                                            TablerIcons.Check,
                                            contentDescription = null,
                                            tint = scheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            } else {
                                val useMetaCover = song.id == currentSongId && song.metaSources?.cover == null && song.tagsVersion < com.muses.player.core.data.db.SongTags.TAGS_VERSION
                                val displayCover = if (useMetaCover) currentMeta?.coverUri ?: song.coverUri else song.coverUri
                                if (displayCover != null) {
                                    MusesCover(
                                        uri = displayCover,
                                        size = 54.dp,
                                        radius = MusesCoverRadius.SM,
                                    )
                                    // Web .m-list-item__inner padding-left:12px —— 封面-标题间距对齐椒盐
                                    Spacer(Modifier.width(12.dp))
                                } else {
                                // Web .songs-page__cover 无封面覆盖：m-cover 容器仍恒定 54dp（透明底），
                                // 内部居中 32dp 占位图标 opacity .45 —— 行首宽度与有封面状态一致
                                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        TablerIcons.MusicNote,
                                        contentDescription = null,
                                        tint = scheme.onBackground.copy(alpha = 0.45f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                }
                            }
                        },
                        after = {
                            if (!isMultiSelect) {
                                // 椒盐式实心三点菜单
                                MusesIconButton(
                                    size = MusesIconButtonSize.SM,
                                    onClick = { actionSong = song },
                                ) {
                                    Icon(TablerIcons.MoreVert, contentDescription = "更多歌曲操作")
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
    MusesActionsSheet(
        opened = actionSong != null,
        onDismiss = { actionSong = null },
        label = "歌曲操作",
        items = listOf(
            MusesActionItem(label = "加入待刮削", onClick = {
                val ids = listOfNotNull(currentId)
                if (ids.isNotEmpty()) doEnqueue(ids)
                actionSong = null
            }),
            MusesActionItem(label = "添加到队列", onClick = {
                // TODO(P2b)：PlayerConnection 补 addToQueue 后接线
                actionSong = null
            }),
            MusesActionItem(label = "加入歌单…", onClick = {
                // 等动作单关闭后再开歌单弹层（Web 层 180ms 延迟同语义）
                val ids = listOfNotNull(currentId)
                actionSong = null
                if (ids.isNotEmpty()) addToPlaylistTarget = ids
            }),
        ),
    )

    // ---- 多选底部操作条（.songs-page__multibar）：内容层底部浮层，位于停靠迷你条之上 ----
    if (isMultiSelect) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
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
                if (picked.isNotEmpty()) playback?.play(picked.first().id, picked)
                exitMultiSelect()
            },
            onEnqueueScrape = {
                val ids = selectedIds.toList()
                if (ids.isNotEmpty()) doEnqueue(ids)
                exitMultiSelect()
            },
            onCancel = { exitMultiSelect() },
        )
        }
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
    val scheme = MiuixTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val hazeState = LocalHazeBlurState.current as? dev.chrisbanes.haze.HazeState
    // 悬浮钮按椒盐实拍更白：与顶部/底部 surface 0.08 区分，改用纯白基底提亮，避免偏灰
    val fabHazeStyle = if (hazeState != null) {
        if (isDark) {
            dev.chrisbanes.haze.blur.HazeBlurStyle(
                backgroundColor = androidx.compose.ui.graphics.Color(0xFF2A2A2A),
                colorEffects = listOf(dev.chrisbanes.haze.blur.HazeColorEffect.tint(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))),
                blurRadius = 24.dp,
                noiseFactor = 0.01f,
            )
        } else {
            dev.chrisbanes.haze.blur.HazeBlurStyle(
                backgroundColor = androidx.compose.ui.graphics.Color.White,
                colorEffects = listOf(dev.chrisbanes.haze.blur.HazeColorEffect.tint(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.62f))),
                blurRadius = 24.dp,
                noiseFactor = 0.01f,
            )
        }
    } else null
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .saltShadow(
                CircleShape,
                // 与底部 MiniPlayer 同款悬浮：inset 高光 + 外投影 0 4px 16dp，避免仅 2/8 导致不浮
                listOf(
                    MusesShadowLayer(offsetY = 1.dp, color = Color.White.copy(alpha = if (isDark) 0.1f else 0.65f), inset = true),
                    MusesShadowLayer(offsetY = 4.dp, blurRadius = 16.dp, color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f)),
                ),
            )
            .then(
                if (hazeState != null && fabHazeStyle != null) {
                    Modifier.hazeBlur(input = HazeInput.Sources(hazeState), style = fabHazeStyle)
                } else {
                    Modifier.background(scheme.surface.copy(alpha = 0.75f), CircleShape)
                },
            )
            .drawBehind {
                // border 1px rgba(255,255,255,.5)（暗色 .12）—— 内高光已由 saltShadow 的 inset 承担
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
            TablerIcons.MyLocation,
            contentDescription = "跳转到当前播放",
            tint = scheme.onBackgroundVariant,
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
    onEnqueueScrape: () -> Unit = {},
) {
    val scheme = MiuixTheme.colorScheme
    val disabled = selectedCount == 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MusesTextButton(text = "永久删除", destructive = true, enabled = !disabled, onClick = onDeleteSelected)
        MusesTextButton(text = "添加到歌单", enabled = !disabled, onClick = onAddToPlaylist)
        // M3：批量加入待刮削队列（刮削页统一处理）
        MusesTextButton(text = "加入待刮削", enabled = !disabled, onClick = onEnqueueScrape)
        MusesTextButton(text = "播放选中队列", enabled = !disabled, onClick = onPlaySelected)
        MusesTextButton(text = "取消", onClick = onCancel)
    }
}

package com.muses.player.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.icons.TablerIcons
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.overlay.OverlayListPopup

/** 曲库页的 Tab 文案（顺序即索引，勿随意调整） */
private val LibraryTabs = listOf("歌曲", "专辑", "艺术家")

/**
 * 曲库容器：顶部「曲库」标题与右侧的**歌曲 / 专辑 / 艺术家**切换，内容复用三个既有页面。
 *
 * 为什么需要它：窄屏底栏精简为「探索 / 曲库 / 设置」三项后，专辑与艺术家在窄屏失去了入口
 * （原先各自是顶层 Tab）；改由曲库页内部切换，既补回入口，也符合「曲库」这个命名。
 *
 * 三个子页以 `showTopBar = false` 嵌入：顶栏与切换由本容器统一提供，避免出现
 * 「曲库 + 歌曲」两层标题。注意 `SongsPage` 的顶栏里还带「随机播放 + 歌曲总数 + 搜索」工具栏，
 * 嵌入模式下会一并隐藏（如需保留，应把该工具栏上提到本容器）。
 */
@Composable
fun LibraryScreen(
    playback: PlaybackPort?,
    onEnqueueScrape: (List<String>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showTabPopup by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.surface,
        topBar = {
            MusesTopBar(
                title = "曲库",
                actions = {
                    // 顶栏右侧显示当前曲库分类，点按后通过 Miuix 列表弹层切换。
                    Box(contentAlignment = Alignment.CenterEnd) {
                        Button(
                            onClick = { showTabPopup = true },
                            minWidth = 0.dp,
                            minHeight = 40.dp,
                            insideMargin = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(),
                        ) {
                            Text(LibraryTabs[selectedTab])
                            Icon(
                                imageVector = TablerIcons.ChevronDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        OverlayListPopup(
                            show = showTabPopup,
                            alignment = PopupPositionProvider.Align.End,
                            enableWindowDim = false,
                            onDismissRequest = { showTabPopup = false },
                        ) {
                            ListPopupColumn {
                                LibraryTabs.forEachIndexed { index, title ->
                                    DropdownImpl(
                                        text = title,
                                        optionSize = LibraryTabs.size,
                                        isSelected = selectedTab == index,
                                        index = index,
                                        onSelectedIndexChange = {
                                            selectedTab = index
                                            showTabPopup = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> SongsPage(
                    playback = playback,
                    onEnqueueScrape = onEnqueueScrape,
                    showTopBar = false,
                )
                1 -> AlbumsPage(
                    onAlbumClick = onAlbumClick,
                    showTopBar = false,
                )
                else -> ArtistsPage(
                    onArtistClick = onArtistClick,
                    showTopBar = false,
                )
            }
        }
    }
}

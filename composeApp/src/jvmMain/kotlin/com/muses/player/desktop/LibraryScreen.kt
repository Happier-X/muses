package com.muses.player.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muses.player.core.data.db.AlbumEntity
import com.muses.player.core.data.db.ArtistEntity
import com.muses.player.core.data.db.SongEntity
import com.muses.player.core.ui.components.LibraryAlbumGrid
import com.muses.player.core.ui.components.LibraryAlbumItem
import com.muses.player.core.ui.components.LibraryArtistGrid
import com.muses.player.core.ui.components.LibraryArtistItem
import com.muses.player.core.ui.components.LibrarySearchField
import com.muses.player.core.ui.components.LibrarySongList
import com.muses.player.core.ui.components.LibraryTab
import com.muses.player.core.ui.components.LibraryTabBar
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalSaltColors
import com.muses.player.desktop.di.DesktopContainer
import com.muses.player.desktop.playback.DesktopPlayerHook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面库房页（U8 完整版）：标签页 + 网格 + 搜索，调用 ui-shared 共用组件。
 *
 * U5 曲目列表共用化：SongListItem 从 :core:ui-shared 平台无关共用组件消费，
 * 桌面复刻 SongRow 已移除。
 * U8 曲库主页共用化：标签页导航（LibraryTabBar）+ 自适应网格
 * （LibraryAlbumGrid/LibraryArtistGrid）+ 搜索框（LibrarySearchField）+
 * 曲目行复用（LibrarySongList）全部走共用组件；桌面恒平板断点
 * （窗口默认 1280dp ≥768，Adaptive(180.dp) 口径）；
 * DAO 订阅 + 实体映射 + 播放回调在本文件装配（对照桌面 SettingsScreen 共用化写法）。
 */
@Composable
fun LibraryScreen(playerHook: DesktopPlayerHook?) {
    val salt = LocalSaltColors.current
    val hook = remember { playerHook ?: DesktopPlayerHook() }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(LibraryTab.Songs) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    var songs by remember { mutableStateOf<List<SongEntity>>(emptyList()) }
    var albums by remember { mutableStateOf<List<AlbumEntity>>(emptyList()) }
    var artists by remember { mutableStateOf<List<ArtistEntity>>(emptyList()) }
    var albumCovers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var artistCovers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var status by remember { mutableStateOf("") }

    val hookSongs by hook.songs.collectAsState()
    val hookStatus by hook.status.collectAsState()
    val currentSongId by hook.currentSongId.collectAsState()

    fun reload() {
        scope.launch {
            runCatching {
                val db = DesktopContainer.database()
                songs = db.songDao().observeAll().first()
                albums = db.albumDao().observeAll().first()
                artists = db.artistDao().observeAll().first()
                albumCovers = db.albumDao().observeAlbumCovers().first()
                    .associate { it.albumId to it.coverUri }
                artistCovers = db.artistDao().observeArtistCovers().first()
                    .associate { it.artistId to it.coverUri }
                status = ""
            }.onFailure { e ->
                status = "读取曲库失败：${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        hook.refresh()
        reload()
    }
    // 播放 hook 刷新后同步歌曲（播放用 hook.songs，展示用本地 DAO 全量；以本地为准）
    LaunchedEffect(hookSongs) {
        if (hookSongs.isNotEmpty() && songs.isEmpty()) {
            songs = hookSongs
        }
    }

    val effectiveStatus = if (status.isNotBlank()) status else hookStatus

    Column(
        modifier = Modifier.fillMaxSize().background(salt.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "曲库",
            color = salt.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        LibraryTabBar(
            selected = tab,
            onTabSelect = {
                tab = it
                isSearching = false
                searchQuery = ""
            },
        )

        // 搜索框：歌曲页常驻（对照安卓 SongsPage 搜索栏）；专辑/艺术家页暂不过滤
        if (tab == LibraryTab.Songs) {
            if (isSearching) {
                LibrarySearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "在 ${songs.size} 首歌曲中搜索",
                    onCancel = {
                        isSearching = false
                        searchQuery = ""
                    },
                )
            } else {
                DesktopSearchEntry(
                    text = "搜索 ${songs.size} 首歌曲",
                    onClick = { isSearching = true },
                )
            }
        }

        if (effectiveStatus.isNotBlank()) {
            Text(text = effectiveStatus, color = salt.danger, fontSize = 13.sp)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                LibraryTab.Songs -> {
                    val filtered = if (searchQuery.isBlank()) {
                        songs
                    } else {
                        songs.filter { song ->
                            song.title.contains(searchQuery, ignoreCase = true) ||
                                song.artist.orEmpty().contains(searchQuery, ignoreCase = true) ||
                                song.albumTitle.orEmpty().contains(searchQuery, ignoreCase = true)
                        }
                    }
                    LibrarySongList(
                        songs = filtered.map { it.toSongItem() },
                        currentSongId = currentSongId,
                        onPlay = { hook.play(it) },
                        emptyTitle = "曲库为空",
                        emptyDescription = "请在设置页添加 WebDAV 音源后扫描",
                        emptyIcon = TablerIcons.MusicNote,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                LibraryTab.Albums -> {
                    LibraryAlbumGrid(
                        albums = albums.map {
                            it.toLibraryAlbumItem(albumCovers[it.id])
                        },
                        // 桌面恒平板断点（窗口默认 1280dp）
                        isTablet = true,
                        onAlbumClick = { /* 桌面详情页二期（U6 播放页评估同理留后） */ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                LibraryTab.Artists -> {
                    LibraryArtistGrid(
                        artists = artists.map {
                            it.toLibraryArtistItem(artistCovers[it.id])
                        },
                        isTablet = true,
                        onArtistClick = { /* 桌面详情页二期 */ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** 桌面搜索入口行（轻量可点文案；点击后展开共用 LibrarySearchField） */
@Composable
private fun DesktopSearchEntry(text: String, onClick: () -> Unit) {
    val salt = LocalSaltColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(salt.surface1)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = salt.text2,
            fontSize = 14.sp,
        )
    }
}

/** Room SongEntity → 跨平台 SongItem 映射 */
private fun SongEntity.toSongItem() = SongItem(
    id = id,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
)

/** Room AlbumEntity → 跨平台 LibraryAlbumItem 映射 */
private fun AlbumEntity.toLibraryAlbumItem(coverUri: String? = null) = LibraryAlbumItem(
    id = id,
    title = title,
    artist = artist,
    songCount = songCount,
    coverUri = coverUri,
)

/** Room ArtistEntity → 跨平台 LibraryArtistItem 映射 */
private fun ArtistEntity.toLibraryArtistItem(coverUri: String? = null) = LibraryArtistItem(
    id = id,
    name = name,
    songCount = songCount,
    albumCount = albumCount,
    coverUri = coverUri,
)

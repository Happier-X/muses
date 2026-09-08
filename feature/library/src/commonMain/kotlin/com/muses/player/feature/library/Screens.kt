package com.muses.player.feature.library

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.Album
import com.muses.player.core.model.Artist
import com.muses.player.core.model.Song
import com.muses.player.core.ui.components.LibraryAlbumGrid
import com.muses.player.core.ui.components.LibraryAlbumItem
import com.muses.player.core.ui.components.LibraryArtistGrid
import com.muses.player.core.ui.components.LibraryArtistItem
import com.muses.player.core.ui.components.LibrarySearchField
import com.muses.player.core.ui.components.LibrarySongList
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.icons.TablerIcons

// ── 曲库主页（共用化）：标签页 + 网格 + 搜索 ────────────────────────
// U8 曲库主页共用化：本文件为三屏的「共用装配层」——ViewModel 订阅 + 实体映射 +
// 回调注入，纯 UI 片段（tab 栏/搜索框/列表/网格）调用 :core:ui-shared 的
// LibraryComponents。播放页、刮削页不在此列。
// U9 KMP 上收：commonMain 双端共用（android + 桌面 jvm）。
// - 播放连接不进 commonMain：onPlaySong 由宿主注入（安卓传 PlayerConnection，桌面传播放端口）；
// - 「加入播放列表」弹层经 addToPlaylistContent slot 注入（桌面暂无播放列表 UI，
//   传 null 时长按仅不弹菜单，行为与原桌面版一致）。

// ── 歌曲列表 ──────────────────────────────────────────
// U8 共用化：列表区调用共用 LibrarySongList（实体→SongItem 映射 + 播放/长按回调注入）。
// 搜索框调用共用 LibrarySearchField；空态文案保持原样。

@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = koinViewModel(),
    /** 播放回调（宿主注入播放连接/端口；songId + 当前全列表供入队） */
    onPlaySong: (String, List<Song>) -> Unit = { _, _ -> },
    /** 「加入播放列表」弹层 slot（安卓传 AddToPlaylistSheet 装配；null = 长按无菜单） */
    addToPlaylistContent: (@Composable (songIds: List<String>, onDismiss: () -> Unit) -> Unit)? = null,
) {
    val songs by viewModel.songs.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 非 null 时弹出「加入播放列表」底部弹层（M2）
    var addToPlaylistTarget by remember { mutableStateOf<List<String>?>(null) }

    if (addToPlaylistTarget != null && addToPlaylistContent != null) {
        addToPlaylistContent(addToPlaylistTarget!!) { addToPlaylistTarget = null }
    }

    Column(modifier = modifier) {
        if (showSearch) {
            LibrarySearchField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = "搜索歌曲、艺术家、专辑",
                onCancel = {
                    showSearch = false
                    searchQuery = ""
                    viewModel.updateSearchQuery("")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // 搜索入口行（原桌面 DesktopSearchEntry 口径；安卓 Page 层自带 navbar 搜索钮，不消费本屏）
            LibrarySearchEntry(
                text = "搜索 ${songs.size} 首歌曲",
                onClick = { showSearch = true },
            )
        }

        LibrarySongList(
            songs = songs.map { it.toSongItem() },
            currentSongId = null,
            onPlay = { songId -> onPlaySong(songId, songs) },
            onLongClick = if (addToPlaylistContent != null) {
                { songId -> addToPlaylistTarget = listOf(songId) }
            } else null,
            emptyTitle = "曲库为空",
            emptyDescription = if (showSearch) null else "请先在「音源」中添加本地目录或 WebDAV 并扫描",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** 搜索入口行（轻量可点文案；点击后展开共用 LibrarySearchField） */
@Composable
private fun LibrarySearchEntry(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(scheme.surface)
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
            color = scheme.onBackgroundVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIndex: Boolean = false,
    index: Int = 0,
    onLongClick: (() -> Unit)? = null,
) {
    // U8 共用化：共用 SongListItem 已承担曲目行渲染；本函数保留作旧详情屏兼容，
    // 内部转调共用组件（序号/封面/时长等旧细节不再保留，以共用视觉为准）。
    val songItem = remember(song) { song.toSongItem() }
    com.muses.player.core.ui.components.SongListItem(
        song = songItem,
        isCurrent = false,
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
    )
}

/** 安卓 Song → 跨平台 SongItem 映射（曲库主页共用化；标题/艺术家/专辑三字段） */
fun Song.toSongItem() = SongItem(
    id = id,
    title = title,
    artist = artist,
    albumTitle = album,
)

/** 安卓 Album → 跨平台 LibraryAlbumItem 映射（封面经调用方 covers 回填） */
fun Album.toLibraryAlbumItem(coverUri: String? = null) = LibraryAlbumItem(
    id = id,
    title = title,
    artist = artist,
    songCount = songCount,
    coverUri = coverUri,
)

/** 安卓 Artist → 跨平台 LibraryArtistItem 映射（封面经调用方 covers 回填） */
fun Artist.toLibraryArtistItem(coverUri: String? = null) = LibraryArtistItem(
    id = id,
    name = name,
    songCount = songCount,
    albumCount = albumCount,
    coverUri = coverUri,
)

// ── 专辑列表 ──────────────────────────────────────────
// U8 共用化：网格区调用共用 LibraryAlbumGrid（手机恒两列/平板 Adaptive 口径在内）。
// U9：断点判定改 BoxWithConstraints（LocalConfiguration 为安卓专属）。

@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = koinViewModel(),
    onAlbumClick: (String) -> Unit = {},
) {
    val albums by viewModel.albums.collectAsState()
    val covers by viewModel.covers.collectAsState()

    BoxWithConstraints(modifier = modifier) {
        LibraryAlbumGrid(
            albums = albums.map { it.toLibraryAlbumItem(covers[it.id]) },
            // Web 断点口径 viewport ≥768（与 LibraryGridPages.isTabletWidth 同口径）
            isTablet = maxWidth >= 768.dp,
            onAlbumClick = onAlbumClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ── 专辑详情 ──────────────────────────────────────────

@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = koinViewModel(),
    onPlaySong: (String, List<Song>) -> Unit = { _, _ -> },
) {
    viewModel.bind(albumId)
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()

    val scheme = MiuixTheme.colorScheme
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize().background(scheme.background)) {
        // MusesNavbar：左返回箭头（对照 Web LibraryDetailPage navbar）
        com.muses.player.core.ui.components.MusesNavbar(
            title = albumWithSongs?.album?.title ?: "专辑",
            left = {
                Icon(
                    TablerIcons.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
        )
        val songs = albumWithSongs?.songs?.map { it.toDomain() }.orEmpty()
        LibrarySongList(
            songs = songs.map { it.toSongItem() },
            currentSongId = null,
            onPlay = { songId -> onPlaySong(songId, songs) },
            emptyTitle = "专辑中暂无歌曲",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ── 艺术家列表 ──────────────────────────────────────────
// U8 共用化：网格区调用共用 LibraryArtistGrid。
// U9：断点判定改 BoxWithConstraints（同 AlbumsScreen）。

@Composable
fun ArtistsScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistsViewModel = koinViewModel(),
    onArtistClick: (String) -> Unit = {},
) {
    val artists by viewModel.artists.collectAsState()
    val covers by viewModel.covers.collectAsState()

    BoxWithConstraints(modifier = modifier) {
        LibraryArtistGrid(
            artists = artists.map { it.toLibraryArtistItem(covers[it.id]) },
            isTablet = maxWidth >= 768.dp,
            onArtistClick = onArtistClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ── 艺术家详情 ──────────────────────────────────────────

@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = koinViewModel(),
    onPlaySong: (String, List<Song>) -> Unit = { _, _ -> },
) {
    viewModel.bind(artistId)
    val artistWithSongs by viewModel.artistWithSongs.collectAsState()

    val scheme = MiuixTheme.colorScheme
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize().background(scheme.background)) {
        com.muses.player.core.ui.components.MusesNavbar(
            title = artistWithSongs?.artist?.name ?: "艺术家",
            left = {
                Icon(
                    TablerIcons.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.clickable(onClick = onBack),
                )
            },
        )
        val songs = artistWithSongs?.songs?.map { it.toDomain() }.orEmpty()
        LibrarySongList(
            songs = songs.map { it.toSongItem() },
            currentSongId = null,
            onPlay = { songId -> onPlaySong(songId, songs) },
            emptyTitle = "艺术家暂无歌曲",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ── 空态提示 ──────────────────────────────────────────
// U8 共用化：空态统一由共用 SaltEmpty（经 LibrarySongList/LibraryAlbumGrid/LibraryArtistGrid）
// 承担；本函数保留作外部兼容（已无外部引用，新代码勿用）。

@Composable
fun EmptyLibraryHint(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    title: String,
    hint: String,
) {
    com.muses.player.core.ui.components.MusesEmpty(
        title = title,
        description = hint,
        modifier = modifier,
    )
}

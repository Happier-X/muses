package com.muses.player.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import com.muses.player.core.ui.icons.TablerIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.model.Song
import com.muses.player.core.media.playback.PlayerConnection

// ── 歌曲列表 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    viewModel: SongsViewModel = hiltViewModel(),
    playerConnection: PlayerConnection? = null,
) {
    val songs by viewModel.songs.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // 非 null 时弹出「加入播放列表」底部弹层（M2）
    var addToPlaylistTarget by remember { mutableStateOf<List<String>?>(null) }

    addToPlaylistTarget?.let { songIds ->
        com.muses.player.feature.playlist.AddToPlaylistSheet(
            songIds = songIds,
            onDismiss = { addToPlaylistTarget = null },
        )
    }

    Column(modifier = modifier) {
        if (showSearch) {
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("搜索歌曲、艺术家、专辑") },
                leadingIcon = {
                    IconButton(onClick = {
                        showSearch = false
                        searchQuery = ""
                        viewModel.updateSearchQuery("")
                    }) {
                        Icon(TablerIcons.ArrowBack, contentDescription = "关闭搜索")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        }

        if (songs.isEmpty() && !showSearch) {
            EmptyLibraryHint(
                modifier = Modifier.fillMaxSize(),
                icon = { Icon(TablerIcons.Album, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                title = "曲库为空",
                hint = "请先在「音源」中添加本地目录或 WebDAV 并扫描",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongListItem(
                        song = song,
                        onClick = {
                            playerConnection?.play(song.id, songs)
                        },
                        onLongClick = { addToPlaylistTarget = listOf(song.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIndex: Boolean = false,
    index: Int = 0,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIndex) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
        }
        // 封面
        AsyncImage(
            model = song.coverUri,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(end = if (showIndex) 0.dp else 12.dp),
        )
        if (!showIndex) Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(song.artist, song.album).joinToString(" · ")
                    .ifEmpty { "未知艺术家" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ── 专辑列表 ──────────────────────────────────────────

@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
) {
    val albums by viewModel.albums.collectAsState()

    if (albums.isEmpty()) {
        EmptyLibraryHint(
            modifier = modifier,
            icon = { Icon(TablerIcons.Album, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = "暂无专辑",
            hint = "扫描完成后在此浏览专辑",
        )
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                ListItem(
                    headlineContent = {
                        Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(
                            listOfNotNull(album.artist, "${album.songCount} 首歌曲").joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            TablerIcons.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album.id) },
                )
            }
        }
    }
}

// ── 专辑详情 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playerConnection: PlayerConnection? = null,
) {
    viewModel.bind(albumId)
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()

    val salt = com.muses.player.core.ui.theme.LocalSaltColors.current
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize().background(salt.surface)) {
        // SaltNavbar：左返回箭头（对照 Web LibraryDetailPage navbar）
        com.muses.player.core.ui.components.SaltNavbar(
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
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.muses.player.core.ui.components.SaltEmpty(title = "专辑中暂无歌曲")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongListItem(
                        song = song,
                        onClick = { playerConnection?.play(song.id, songs) },
                        showIndex = true,
                        index = index,
                    )
                }
            }
        }
    }
}

// ── 艺术家列表 ──────────────────────────────────────────

@Composable
fun ArtistsScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistsViewModel = hiltViewModel(),
    onArtistClick: (String) -> Unit = {},
) {
    val artists by viewModel.artists.collectAsState()

    if (artists.isEmpty()) {
        EmptyLibraryHint(
            modifier = modifier,
            icon = { Icon(TablerIcons.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = "暂无艺术家",
            hint = "扫描完成后在此浏览艺术家",
        )
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(artists, key = { it.id }) { artist ->
                ListItem(
                    headlineContent = {
                        Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(
                            "${artist.songCount} 首歌曲 · ${artist.albumCount} 张专辑",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            TablerIcons.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist.id) },
                )
            }
        }
    }
}

// ── 艺术家详情 ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playerConnection: PlayerConnection? = null,
) {
    viewModel.bind(artistId)
    val artistWithSongs by viewModel.artistWithSongs.collectAsState()

    val salt = com.muses.player.core.ui.theme.LocalSaltColors.current
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize().background(salt.surface)) {
        com.muses.player.core.ui.components.SaltNavbar(
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
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "艺术家暂无歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongListItem(
                        song = song,
                        onClick = { playerConnection?.play(song.id, songs) },
                        showIndex = true,
                        index = index,
                    )
                }
            }
        }
    }
}

// ── 空态提示 ──────────────────────────────────────────

@Composable
fun EmptyLibraryHint(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    title: String,
    hint: String,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── 工具函数 ──────────────────────────────────────────

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

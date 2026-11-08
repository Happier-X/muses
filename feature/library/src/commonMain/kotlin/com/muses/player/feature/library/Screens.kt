package com.muses.player.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.muses.player.core.data.mapper.toDomain
import com.muses.player.core.playback.PlaybackPort
import org.koin.compose.viewmodel.koinViewModel

// 专辑/艺术家详情：曲目范围交给 SongsPage（scopedSongs），只保留详情页自己的数据装配。

@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = koinViewModel(),
    playback: PlaybackPort? = null,
    onEnqueueScrape: (List<String>) -> Unit = {},
) {
    LaunchedEffect(albumId) { viewModel.bind(albumId) }
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()

    val songs = albumWithSongs?.songs?.map { it.toDomain() }.orEmpty()
    SongsPage(
        playback = playback,
        onEnqueueScrape = onEnqueueScrape,
        modifier = modifier,
        scopedSongs = songs,
        pageTitle = albumWithSongs?.album?.title ?: "专辑",
        showSearch = false,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = koinViewModel(),
    playback: PlaybackPort? = null,
    onEnqueueScrape: (List<String>) -> Unit = {},
) {
    LaunchedEffect(artistId) { viewModel.bind(artistId) }
    val artistWithSongs by viewModel.artistWithSongs.collectAsState()

    val songs = artistWithSongs?.songs?.map { it.toDomain() }.orEmpty()
    SongsPage(
        playback = playback,
        onEnqueueScrape = onEnqueueScrape,
        modifier = modifier,
        scopedSongs = songs,
        pageTitle = artistWithSongs?.artist?.name ?: "艺术家",
        showSearch = false,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    )
}


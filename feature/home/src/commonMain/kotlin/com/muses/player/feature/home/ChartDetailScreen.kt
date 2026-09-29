package com.muses.player.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesTextButton
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SongItem
import com.muses.player.core.ui.components.SongListItem
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChartDetailScreen(
    platform: String,
    chartId: String,
    chartName: String,
    onBack: () -> Unit,
    viewModel: ChartDetailViewModel = koinViewModel(
        parameters = { parametersOf(platform, chartId) },
    ),
) {
    val state by viewModel.state.collectAsState()
    val scheme = MiuixTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = scheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = chartName,
                onBack = onBack,
                actions = {
                    MusesIconButton(
                        onClick = viewModel::refresh,
                        imageVector = TablerIcons.Refresh,
                        contentDescription = "刷新榜单",
                        enabled = !state.loading && !state.refreshing,
                    )
                },
                bottomContent = {
                    if (state.songs.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = viewModel::shuffle,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MusesIconButton(onClick = viewModel::shuffle) {
                                    Icon(TablerIcons.Shuffle, contentDescription = "随机播放已加载歌曲")
                                }
                                Text(
                                    text = state.songs.size.toString(),
                                    style = MiuixTheme.textStyles.body1,
                                    color = scheme.onBackground,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && state.songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(state.error.orEmpty(), color = scheme.onSurfaceVariantSummary)
                    MusesTextButton(onClick = viewModel::retry, text = "重试")
                }
            }

            state.songs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                MusesEmpty(
                    modifier = Modifier.fillMaxSize(),
                    bottomInset = LocalBottomChromePadding.current,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp + LocalBottomChromePadding.current,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(
                    items = state.songs,
                    key = { _, song -> "${song.platform}-${song.songId}" },
                ) { index, song ->
                    SongListItem(
                        song = SongItem(
                            id = "${song.platform}-${song.songId}",
                            title = song.name,
                            artist = song.artist,
                            albumTitle = song.album,
                            coverUri = song.coverUrl,
                        ),
                        isCurrent = false,
                        onClick = { viewModel.play(index) },
                    )
                }
                if (state.hasMore) {
                    item(key = "load-more") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.loadingMore) CircularProgressIndicator()
                            else MusesTextButton(onClick = viewModel::loadMore, text = "加载更多")
                        }
                    }
                }
            }
        }
    }
}

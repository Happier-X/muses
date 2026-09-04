package com.muses.player.feature.playlist

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 播放列表 ViewModel 装配（P2a Hilt→Koin）。 */
val playlistModule = module {
    viewModel { PlaylistsViewModel(get()) }
    viewModel { PlaylistDetailViewModel(get(), get()) }
    viewModel { AddToPlaylistViewModel(get()) }
}

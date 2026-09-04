package com.muses.player.feature.library

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 曲库页面 ViewModel 装配（P2a Hilt→Koin：原 `@HiltViewModel`，调用点语义不变）。 */
val libraryModule = module {
    viewModel { SongsViewModel(get(), get()) }
    viewModel { AlbumsViewModel(get(), get()) }
    viewModel { AlbumDetailViewModel(get()) }
    viewModel { ArtistsViewModel(get(), get()) }
    viewModel { ArtistDetailViewModel(get()) }
    viewModel { AlbumCardsViewModel(get(), get()) }
    viewModel { ArtistCardsViewModel(get(), get()) }
}

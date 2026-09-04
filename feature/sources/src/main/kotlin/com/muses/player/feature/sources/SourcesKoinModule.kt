package com.muses.player.feature.sources

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 音源页 ViewModel 装配（P2a Hilt→Koin）。 */
val sourcesModule = module {
    viewModel {
        SourcesViewModel(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { WebDavBrowseViewModel(get()) }
    viewModel { WebDavFormViewModel(get(), get(), get()) }
}

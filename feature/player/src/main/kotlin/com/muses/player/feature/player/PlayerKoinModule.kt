package com.muses.player.feature.player

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 播放页 ViewModel 装配（P2a Hilt→Koin）。 */
val playerModule = module {
    viewModel { PlayerViewModel(get(), get()) }
    viewModel { QueueViewModel(get()) }
}

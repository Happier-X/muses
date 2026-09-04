package com.muses.player.feature.scrape

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 刮削页 ViewModel 装配（P2a Hilt→Koin；ScrapeReviewViewModel 首参 SavedStateHandle 由 Koin 按导航参数自动供给）。 */
val scrapeFeatureModule = module {
    viewModel { ScrapeViewModel(get(), get(), get(), get(), get()) }
    viewModel { ScrapeReviewViewModel(get(), get(), get(), get(), get()) }
    viewModel { EditMetaViewModel(get(), get(), get()) }
    viewModel { ScrapeQueueAccessViewModel(get()) }
}

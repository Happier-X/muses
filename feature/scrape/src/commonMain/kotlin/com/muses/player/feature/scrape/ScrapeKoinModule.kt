package com.muses.player.feature.scrape

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 刮削页 ViewModel 装配（P2a Hilt→Koin；miuix-nav 无 SavedStateHandle，
 * ScrapeReviewViewModel 改路由直传（songId/queueCsv 经 parametersOf 供给）。 */
val scrapeFeatureModule = module {
    viewModel { ScrapeViewModel(get(), get(), get(), get(), get()) }
    viewModel { params ->
        ScrapeReviewViewModel(
            songId = params[0] as? String,
            queueCsv = params[1] as? String,
            editCloudMetaSearch = get(),
            songRepository = get(),
            writebackOrchestrator = get(),
            queueStore = get(),
        )
    }
    viewModel { EditMetaViewModel(get(), get(), get()) }
    viewModel { ScrapeQueueAccessViewModel(get()) }
}

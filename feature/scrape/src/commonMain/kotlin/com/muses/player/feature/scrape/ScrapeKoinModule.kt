package com.muses.player.feature.scrape

import com.muses.player.core.ai.AI_API_KEY_SOURCE_ID
import com.muses.player.core.ai.AiRecommendConfig
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 刮削页 ViewModel 装配（P2a Hilt→Koin；miuix-nav 无 SavedStateHandle，
 * ScrapeReviewViewModel 改路由直传（songId/queueCsv 经 parametersOf 供给）。 */
val scrapeFeatureModule = module {
    viewModel { ScrapeViewModel(get(), get(), get(), get(), get()) }
    viewModel { params ->
        val settings = get<SettingsRepository>()
        val credentials = get<CredentialsRepository>()
        ScrapeReviewViewModel(
            songId = params[0] as? String,
            queueCsv = params[1] as? String,
            editCloudMetaSearch = get(),
            songRepository = get(),
            writebackOrchestrator = get(),
            queueStore = get(),
            aiMatcher = get(),
            readAiConfig = {
                AiRecommendConfig(
                    baseUrl = settings.aiBaseUrl.first(), model = settings.aiModel.first(),
                    apiKey = credentials.getPassword(AI_API_KEY_SOURCE_ID).orEmpty(),
                )
            },
        )
    }
    viewModel { EditMetaViewModel(get(), get(), get()) }
    viewModel { ScrapeQueueAccessViewModel(get()) }
}

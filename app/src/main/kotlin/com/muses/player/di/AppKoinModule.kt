package com.muses.player.di

import com.muses.player.core.data.di.databaseModule
import com.muses.player.core.data.repository.repositoryModule
import com.muses.player.core.data.tag.tagModule
import com.muses.player.core.lyrics.di.lyricsModule
import com.muses.player.core.media.playback.playbackModule
import com.muses.player.core.scrape.di.scrapeModule
import com.muses.player.core.webdav.webdavModule
import com.muses.player.feature.library.libraryModule
import com.muses.player.feature.player.playerModule
import com.muses.player.feature.playlist.playlistModule
import com.muses.player.feature.scrape.scrapeFeatureModule
import com.muses.player.feature.sources.sourcesModule
import com.muses.player.navigation.MainViewModel
import com.muses.player.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** app 层 ViewModel 装配（P2a Hilt→Koin：MainViewModel + SettingsViewModel）。 */
val appModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}

/** 全量模块聚合：`MusesApplication.startKoin` 唯一入口（P2a design §3）。 */
val appModules = listOf(
    databaseModule,
    tagModule,
    repositoryModule,
    lyricsModule,
    playbackModule,
    scrapeModule,
    webdavModule,
    libraryModule,
    playerModule,
    playlistModule,
    scrapeFeatureModule,
    sourcesModule,
    appModule,
)

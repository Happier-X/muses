package com.muses.player.feature.shell.di

import com.muses.player.core.lyrics.LyricsMatcher
import com.muses.player.core.model.online.NoOpOnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.navigation.MainViewModel
import com.muses.player.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 应用壳 ViewModel 装配（U22 双端共享：MainViewModel + SettingsViewModel，双端 startKoin 均需装配）。 */
val shellModule = module {
    single { com.muses.player.core.download.DownloadQueueStore(get()) }
    single { com.muses.player.download.DownloadManager(get(), get(), get(), get(), get(), get(),
        getOrNull<OnlineTrackMetadataResolver>() ?: NoOpOnlineTrackMetadataResolver,
        getOrNull<LyricsMatcher>(), com.muses.player.download.createDownloadStorage(get()),
        runningChanged = { com.muses.player.download.keepDownloadsRunning(it) }) }
    viewModel {
        // 在线曲目歌词端口与匹配器可选：宿主未装配时回落空实现（迷你条照常工作）
        MainViewModel(
            get(),
            get(),
            get(),
            get(),
            getOrNull<OnlineTrackMetadataResolver>() ?: NoOpOnlineTrackMetadataResolver,
            getOrNull<LyricsMatcher>(),
        )
    }
    viewModel { SettingsViewModel(get()) }
}

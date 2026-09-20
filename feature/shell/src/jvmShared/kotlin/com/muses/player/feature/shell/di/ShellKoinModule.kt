package com.muses.player.feature.shell.di

import com.muses.player.core.model.online.NoOpOnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import com.muses.player.navigation.MainViewModel
import com.muses.player.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 应用壳 ViewModel 装配（U22 双端共享：MainViewModel + SettingsViewModel，双端 startKoin 均需装配）。 */
val shellModule = module {
    viewModel {
        // 在线曲目歌词端口可选：宿主未装配 :core:lxsdk 时回落空实现
        MainViewModel(
            get(),
            get(),
            get(),
            get(),
            getOrNull<OnlineTrackMetadataResolver>() ?: NoOpOnlineTrackMetadataResolver,
        )
    }
    viewModel { SettingsViewModel(get()) }
}

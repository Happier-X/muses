package com.muses.player.feature.shell.di

import com.muses.player.navigation.MainViewModel
import com.muses.player.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 应用壳 ViewModel 装配（U22 双端共享：MainViewModel + SettingsViewModel，双端 startKoin 均需装配）。 */
val shellModule = module {
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}

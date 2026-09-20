package com.muses.player.feature.home

import com.muses.player.core.playback.PlaybackPort
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 首页 ViewModel 装配（双端同模块）。
 *
 * 依赖一览：榜单聚合服务、AI 推荐服务、曲库画像构建器（均 :core:search / :core:ai 单例）、
 * 设置仓储（AI 配置 + 在线音质）、凭据仓储（AI Key，密文）、脚本仓库（播放前校验）、播放端口。
 */
val homeCoreModule = module {
    viewModel {
        val port: PlaybackPort = get()
        HomeViewModel(
            chartService = get(),
            recommendService = get(),
            profileBuilder = get(),
            settingsRepository = get(),
            credentialsRepository = get(),
            scriptRepository = get(),
            playback = port,
        )
    }
}

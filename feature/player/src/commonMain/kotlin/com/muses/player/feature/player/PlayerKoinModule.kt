package com.muses.player.feature.player

import com.muses.player.core.model.online.NoOpOnlineTrackMetadataResolver
import com.muses.player.core.model.online.OnlineTrackMetadataResolver
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** 播放页 ViewModel 装配（P2a Hilt→Koin）。 */
val playerModule = module {
    viewModel {
        // 在线曲目封面/歌词端口可选：宿主未装配 :core:lxsdk（未装洛雪音源）时回落空实现
        PlayerViewModel(
            get(),
            get(),
            getOrNull<OnlineTrackMetadataResolver>() ?: NoOpOnlineTrackMetadataResolver,
        )
    }
    viewModel { QueueViewModel(get()) }
}

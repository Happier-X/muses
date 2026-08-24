package com.muses.player.core.media.playback

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {

    @Binds
    abstract fun bindPlaybackController(impl: NoOpPlaybackController): PlaybackController
}

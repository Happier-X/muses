package com.muses.player.core.data.tag

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TagModule {
    // AudioTagReader 由 @Inject 构造，无需 @Provides
    // 标签补齐采用播放时懒扫描，不使用后台 Worker（用户决策 2026-08-27）
}

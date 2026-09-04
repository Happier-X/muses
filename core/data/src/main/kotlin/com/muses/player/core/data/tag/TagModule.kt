package com.muses.player.core.data.tag

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * 标签装配（P2a Hilt→Koin：原空 `@Module`，AudioTagReader 靠 `@Inject` 构造）。
 * 标签补齐采用播放时懒扫描，不使用后台 Worker（用户决策 2026-08-27）。
 */
val tagModule = module {
    singleOf(::AudioTagReader)
}

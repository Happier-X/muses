package com.muses.player.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/** 保留实时订阅；手动刷新重新查询数据，并把查询结果提交到同一个状态流。 */
class RefreshableState<T>(
    private val upstream: Flow<T>,
    scope: CoroutineScope,
    initialValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
) {
    private val snapshots = MutableSharedFlow<T>()
    val state = merge(upstream, snapshots)
        .stateIn(scope, started, initialValue)

    suspend fun refresh() {
        snapshots.emit(upstream.first())
    }
}

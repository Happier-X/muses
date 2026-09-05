package com.muses.player.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 桌面导航（S3b）：三屏最小可用（库房/播放/设置）；V3 起增刮削入口。
 */
enum class DesktopDestination {
    LIBRARY,
    PLAYER,
    SOURCES,
    SCRAPE,
    SETTINGS,
}

class DesktopViewModel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _destination = MutableStateFlow(DesktopDestination.LIBRARY)
    val destination: StateFlow<DesktopDestination> = _destination.asStateFlow()

    fun navigate(destination: DesktopDestination) {
        _destination.value = destination
    }
}

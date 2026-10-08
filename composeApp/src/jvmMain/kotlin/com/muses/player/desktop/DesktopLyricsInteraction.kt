package com.muses.player.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 悬浮歌词与主窗口之间的最小通信点：可见性供托盘置灰，设置请求驱动路由跳转。 */
internal object DesktopLyricsInteraction {
    private val mutableVisible = MutableStateFlow(false)
    val visible = mutableVisible.asStateFlow()
    private val mutableSettingsRequest = MutableStateFlow(0L)
    val settingsRequest = mutableSettingsRequest.asStateFlow()
    fun setVisible(value: Boolean) { mutableVisible.value = value }
    fun openSettings() { mutableSettingsRequest.update { it + 1 } }
}

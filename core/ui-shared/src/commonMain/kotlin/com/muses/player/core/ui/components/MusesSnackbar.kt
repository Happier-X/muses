package com.muses.player.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState

/**
 * 全局短提示（miuix Snackbar；替换 PlatformToast 系统 Toast + DesktopToastOverlay 手写浮层）。
 *
 * - 调用：一行非挂起 [MusesSnackbar.show]，随处可调（页面/ViewModel/回调）；
 *   同文案连续触发每次都显示（如连续两次「已加入待刮削队列」）；
 * - 挂载：MusesApp 根 Scaffold 的 `snackbarHost` 槽挂 [MusesSnackbarHostContent] 一次，
 *   定位/边距/动画全走官方，绘制在底栏之上；
 * - 事件总线：SharedFlow 无去重；壳未挂载前攒 8 条，挂载后首帧即消费，不丢提示。
 */
internal val musesSnackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)

object MusesSnackbar {
    /** 显示一条短提示（非挂起，ViewModel/回调里直接调） */
    fun show(message: String) {
        musesSnackbarEvents.tryEmit(message)
    }
}

/**
 * 短提示宿主内容（挂在根 Scaffold `snackbarHost` 槽，一处即可）。
 *
 * @param hostState MusesApp 作用域持有，经 remember 跨重组保持。
 */
@Composable
fun MusesSnackbarHostContent(hostState: SnackbarHostState) {
    LaunchedEffect(hostState) {
        musesSnackbarEvents.collect { message ->
            hostState.showSnackbar(message)
        }
    }
    SnackbarHost(state = hostState)
}

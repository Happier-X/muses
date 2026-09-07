package com.muses.player.core.uishared.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * U2 jvmMain（桌面）实现：桌面无系统 Toast 通道，[PlatformToast.show] 写入内部总线，
 * 由桌面壳（MusesDesktopApp）顶层挂载的 [DesktopToastOverlay] 消费渲染为底部浮层；
 * 壳层未挂载浮层时消息留在总线（下次挂载消费一次），不丢功能语义。
 *
 * 去重说明：连续相同文案必须每次都显示（如连续两次「已加入待刮削队列」），
 * 故用 [MutableSharedFlow]（无去重、每次 emit 都送达）做事件总线；
 * [desktopToastMessage] 保留作「最近一条」状态，供调试/兜底读取。
 */
internal val desktopToastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
internal val desktopToastMessage = MutableStateFlow<String?>(null)

actual object PlatformToast {
    actual fun show(message: String) {
        desktopToastMessage.value = message
        desktopToastEvents.tryEmit(message)
    }
}

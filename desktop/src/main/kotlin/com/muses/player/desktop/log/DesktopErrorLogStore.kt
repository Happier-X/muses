package com.muses.player.desktop.log

import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.desktop.playback.DesktopErrorLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 桌面 ErrorLogStore 适配（W4 桌面装配，任务 09-05-scrape-kmp）。
 *
 * commonMain 的 [ErrorLogStore] 接口（WebDavClient/刮削链埋点依赖）此前桌面无绑定，
 * 已有 [DesktopErrorLog] 为 object（落盘 crash-latest.txt + 内存环形缓冲）但未实现该接口；
 * 本适配器把接口调用转发到既有设施，桌面 WebDAV/刮削链的 WARN/ERROR 与播放日志同处可查。
 */
object DesktopErrorLogStore : ErrorLogStore {

    private val latestSummaryInternal = MutableStateFlow<String?>(DesktopErrorLog.latestSummary())

    override val latestSummary: StateFlow<String?> = latestSummaryInternal

    override fun log(level: ErrorLogStore.Level, tag: String, message: String, throwable: Throwable?) {
        DesktopErrorLog.log(level.name, tag, message, throwable)
        latestSummaryInternal.value = DesktopErrorLog.latestSummary()
    }

    /** 桌面日志全量转储（环形缓冲 dump，与安卓 [ErrorLogStore.dump] 的行格式不必一致，仅调试消费） */
    override suspend fun dump(): String? = DesktopErrorLog.dump().takeIf { it.isNotBlank() }
}

package com.muses.player.core.data.log

import kotlinx.coroutines.flow.StateFlow

/**
 * 错误日志仓库（任务 08-26-settings-log-viewer）。
 *
 * 分级采集 MVP：仅保留 WARN/ERROR 级别 + 未捕获异常（crash），
 * 存放于内存环形缓冲（最近约 500 条）；crash 由 [CrashHandler] 持久化并在下次启动读回。
 */
interface ErrorLogStore {

    /** 日志级别（MVP 仅采集两级） */
    enum class Level { WARN, ERROR }

    /**
     * 记录一条日志。
     * @param tag 来源标签（如 "Playback" / "WebDavScan"）
     * @param message 首行摘要；throwable 非空时其堆栈会附在后续行
     * @param throwable 可选异常，堆栈序列化后一并进入缓冲
     */
    fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)

    /** 最近一条日志的摘要（时间 + 首行消息），缓冲为空时为 null —— 供设置页副标题 */
    val latestSummary: StateFlow<String?>

    /**
     * 格式化全部缓冲为可复制文本（含启动时读回的「上次会话崩溃」段）；
     * 缓冲为空返回 null。不含文件头（版本号等由 UI 层拼装）。
     */
    suspend fun dump(): String?
}

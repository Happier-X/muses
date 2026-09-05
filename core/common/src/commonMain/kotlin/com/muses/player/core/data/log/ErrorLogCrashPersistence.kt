package com.muses.player.core.data.log

/**
 * 崩溃持久化能力（安卓侧 [CrashHandler] 专用，与查询用 [ErrorLogStore] 接口分离）：
 * 序列化当前缓冲 / 将上次会话内容恢复进缓冲头部。
 */
interface ErrorLogCrashPersistence {
    /** 序列化全部缓冲为可持久化的紧凑行格式 */
    fun serialize(): String

    /** 将上次会话持久化内容恢复进缓冲头部；空/非法输入静默忽略 */
    fun restore(serialized: String)
}

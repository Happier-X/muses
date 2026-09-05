package com.muses.player.core.data.log

/**
 * W1 日志时间格式化（RingBufferErrorLogStore 摘要/dump 用）。
 * commonMain 无 java.text.SimpleDateFormat；androidMain/jvmMain 均有 JDK，
 * actual 用同一 pattern 委托 SimpleDateFormat，输出与原 core:data 实现一致。
 */
internal expect fun formatLogTime(timestampMs: Long, pattern: String): String

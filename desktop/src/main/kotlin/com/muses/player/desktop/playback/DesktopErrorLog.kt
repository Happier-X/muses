package com.muses.player.desktop.playback

import com.muses.player.core.data.platform.PlatformDirs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * S2 桌面错误日志（复用 S1 底座 `errorLogDir`，对齐安卓侧 `CrashHandler` 落盘语义）。
 *
 * - 目录：[PlatformDirs.errorLogDir]；
 * - 崩溃/播放失败追加写 `crash-latest.txt`（S3 设置页“反馈”消费：读回后删文件由调用方处理）；
 * - 内存环形缓冲 cap=500（WARN/ERROR），供后续 ErrorLogStore 桌面实现复用；
 * - 全程 try-catch 不抛异常；`CancellationException` 由调用方前置重抛，不进此日志。
 */
object DesktopErrorLog {

    const val CRASH_FILE = "crash-latest.txt"
    private const val CAP = 500

    data class Entry(val level: String, val tag: String, val msg: String, val at: Long)

    private val lock = Any()
    private val ring = ArrayDeque<Entry>(CAP)

    fun crashFile(): File = File(PlatformDirs.errorLogDir(), CRASH_FILE)

    fun log(tag: String, msg: String, e: Throwable? = null) {
        log("ERROR", tag, msg, e)
    }

    fun log(level: String, tag: String, msg: String, e: Throwable?) {
        if (level != "WARN" && level != "ERROR") return
        val entry = Entry(level, tag, msg, System.currentTimeMillis())
        synchronized(lock) {
            if (ring.size >= CAP) ring.removeFirst()
            ring.addLast(entry)
        }
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(entry.at))
            val stack = e?.stackTraceToString()?.let { "\n$it" }.orEmpty()
            crashFile().appendText("[$ts][$level][$tag] $msg$stack\n")
        }
    }

    fun dump(): String = synchronized(lock) {
        ring.joinToString("\n") { "[${it.level}][${it.tag}] ${it.msg}" }
    }

    fun latestSummary(): String? = synchronized(lock) {
        ring.lastOrNull()?.let { "[${it.tag}] ${it.msg}" }
    }

    /** 启动读回后删文件（对齐安卓侧 CrashHandler 语义，由 S3 入口调用）。 */
    fun consumeCrashFile(): String? = runCatching {
        val f = crashFile()
        if (!f.exists()) return null
        val text = f.readText()
        f.delete()
        text.takeIf { it.isNotBlank() }
    }.getOrNull()
}

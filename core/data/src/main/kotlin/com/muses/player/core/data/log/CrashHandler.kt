package com.muses.player.core.data.log

import android.content.Context
import android.util.Log
import java.io.File

/**
 * 未捕获异常处理器（任务 08-26-settings-log-viewer）。
 *
 * - [install] 在 `MusesApplication.onCreate` 调用：先读回上次崩溃（若有），再包装
 *   线程默认 [Thread.UncaughtExceptionHandler]；
 * - 崩溃时把当前缓冲 + 崩溃堆栈写入 `filesDir/error_log/crash-latest.txt`
 *   （阻塞 IO 可接受——进程即将死亡），随后**必须**委托原 handler 保持系统崩溃流程；
 * - 全部逻辑 try-catch 包裹：安装/持久化任何异常都不得把正常启动变成崩溃。
 *
 * W1 KMP 上收：[ErrorLogStore]/[ErrorLogCrashPersistence] 接口已迁 :core:common commonMain
 * （同包名经 api(:core:common) 透传解析）；Context/Log 依赖为安卓专属，本壳留在 core:data。
 */
object CrashHandler {

    private const val TAG = "CrashHandler"
    private const val DIR_NAME = "error_log"
    private const val FILE_NAME = "crash-latest.txt"

    /** 安装 crash handler；幂等安全，内部异常全部吞掉不影响启动 */
    fun install(context: Context, persistence: ErrorLogCrashPersistence) {
        try {
            restorePreviousCrash(context, persistence)

            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    persistCrash(context, persistence, throwable)
                } catch (_: Throwable) {
                    // 持久化失败不能阻断系统崩溃流程
                }
                previous?.uncaughtException(thread, throwable)
                    ?: Runtime.getRuntime().exit(2) // 无前置 handler 时对齐默认终止行为
            }
        } catch (e: Throwable) {
            Log.w(TAG, "crash handler 安装失败（忽略）", e)
        }
    }

    /** 启动读回：上次会话的崩溃日志插入缓冲头部，读完即删 */
    private fun restorePreviousCrash(context: Context, persistence: ErrorLogCrashPersistence) {
        val file = crashFile(context)
        if (!file.exists()) return
        try {
            persistence.restore(file.readText())
        } catch (e: Throwable) {
            Log.w(TAG, "上次崩溃日志读回失败（忽略）", e)
        } finally {
            // 无论解析成败都删除，避免同一份崩溃被重复报告
            runCatching { file.delete() }
        }
    }

    /** 崩溃现场落盘：当前缓冲序列化 + 本次崩溃条目（ERROR/Crash + 完整堆栈） */
    private fun persistCrash(context: Context, persistence: ErrorLogCrashPersistence, throwable: Throwable) {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val crashEntry = "L\t${System.currentTimeMillis()}\t" +
            ErrorLogStore.Level.ERROR.name +
            "\tCrash\t" +
            // 首行摘要 + 堆栈整体转义后写入同一行（换行转义为 \n，保证行格式可解析）
            escape(
                "未捕获异常：" + (throwable.message ?: throwable::class.java.name) +
                    "\n" + throwable.stackTraceToString(),
            )
        File(dir, FILE_NAME).writeText(persistence.serialize() + "\n" + crashEntry)
    }

    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

    internal fun crashFile(context: Context): File =
        File(File(context.filesDir, DIR_NAME), FILE_NAME)
}

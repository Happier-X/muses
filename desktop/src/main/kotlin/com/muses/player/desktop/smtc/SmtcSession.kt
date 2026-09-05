package com.muses.player.desktop.smtc

/**
 * SMTC 会话抽象：真实实现 [SmtcWinRtSession]（模块内可见）走 JNA interop，测试注入 fake。
 * 所有方法实现必须内部消化失败（静默或记日志），不向调用方抛异常。
 */
interface SmtcSession {
    fun updateMetadata(title: String, artist: String?, album: String?)
    fun updatePlaybackStatus(status: SmtcPlaybackStatus)
    fun updateTimeline(positionMs: Long, durationMs: Long)
    fun close()
}

/** 播放状态 → MediaPlaybackStatus native 值（Stopped=2/Playing=3/Paused=4，调研报告 §3.b）。 */
enum class SmtcPlaybackStatus(val nativeValue: Int) {
    STOPPED(2),
    PLAYING(3),
    PAUSED(4),
}

/** 系统媒体按键动作（注入，语义与 [com.muses.player.desktop.tray.DesktopTray] 菜单一致）。 */
data class SmtcActions(
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
)

/** 会话工厂（测试注入 fake；默认走 [SmtcWinRtSession] 真实 interop）。 */
fun interface SmtcSessionFactory {
    fun open(
        hwnd: Long,
        actions: SmtcActions,
        errorLog: (tag: String, msg: String, e: Throwable?) -> Unit,
    ): SmtcSession
}

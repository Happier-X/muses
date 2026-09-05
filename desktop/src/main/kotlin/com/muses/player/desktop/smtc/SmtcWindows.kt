package com.muses.player.desktop.smtc

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Platform
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.ptr.IntByReference

/** 主窗口 HWND 查找：EnumWindows 过滤当前进程 + 可见 + 精确标题（GetForWindow 要求本进程顶层窗口）。 */
internal object SmtcWindows {

    fun findMainWindowHwnd(windowTitle: String): Long? {
        if (!Platform.isWindows()) return null
        val pid = Kernel32.INSTANCE.GetCurrentProcessId()
        var found: Long? = null
        User32.INSTANCE.EnumWindows({ hwnd, _ ->
            val pointer = hwnd.pointer ?: return@EnumWindows true
            val pidRef = IntByReference()
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef)
            if (pidRef.value == pid && User32.INSTANCE.IsWindowVisible(hwnd)) {
                val buf = CharArray(256)
                User32.INSTANCE.GetWindowText(hwnd, buf, buf.size)
                if (Native.toString(buf) == windowTitle) {
                    found = Pointer.nativeValue(pointer)
                    return@EnumWindows false
                }
            }
            true
        }, null)
        return found
    }
}

package com.muses.player.desktop.smtc

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * combase.dll 的 WinRT 基础入口（JNA 5.17 的 Ole32 无 Ro* 封装，见任务调研报告 §3.a）。
 * HSTRING 是指针大小的句柄，只能经 [WindowsCreateString] 创建、[WindowsDeleteString] 释放。
 *
 * 仅在 [open] 路径被调用时加载；非 Windows 平台触碰 INSTANCE 会抛
 * UnsatisfiedLinkError，由调用方（SmtcController）捕获降级。
 */
internal interface WinRtRuntime : StdCallLibrary {

    /** RO_INIT_MULTITHREADED = 1（专用 MTA 线程，避免与 JVM 主线程 STA 组件冲突）。 */
    fun RoInitialize(initType: Int): Int

    fun RoUninitialize()

    fun RoGetActivationFactory(classId: Pointer, iid: Guid.GUID, factoryOut: PointerByReference): Int

    fun WindowsCreateString(sourceString: WString, length: Int, hstringOut: PointerByReference): Int

    fun WindowsDeleteString(hstring: Pointer)

    companion object {
        val INSTANCE: WinRtRuntime =
            Native.load("combase", WinRtRuntime::class.java, W32APIOptions.UNICODE_OPTIONS)
    }
}

/** SMTC interop 链路上任一 HRESULT 失败（仅用于会话建立期，更新期失败静默记日志）。 */
internal class SmtcException(message: String) : Exception(message)

internal const val RO_INIT_MULTITHREADED = 1

/** HRESULT 成功判定（S_OK=0，S_FALSE=1 等非负值均算成功）。 */
internal fun hresultSucceeded(hr: Int): Boolean = hr >= 0

internal fun hresultHex(hr: Int): String = "0x${Integer.toHexString(hr)}"

/** 创建 HSTRING（失败抛 [SmtcException]；调用方负责 [WinRtRuntime.WindowsDeleteString]）。 */
internal fun WinRtRuntime.createHString(value: String): Pointer {
    val out = PointerByReference()
    val hr = WindowsCreateString(WString(value), value.length, out)
    if (!hresultSucceeded(hr)) throw SmtcException("WindowsCreateString 失败 hr=${hresultHex(hr)}")
    return out.value ?: throw SmtcException("WindowsCreateString 返回空 HSTRING")
}

/** RoGetActivationFactory 封装（HSTRING 生命周期内部管理）。 */
internal fun WinRtRuntime.getActivationFactory(classId: String, iid: Guid.GUID): Pointer {
    val hstr = createHString(classId)
    try {
        val out = PointerByReference()
        val hr = RoGetActivationFactory(hstr, iid, out)
        if (!hresultSucceeded(hr)) {
            throw SmtcException("RoGetActivationFactory($classId) 失败 hr=${hresultHex(hr)}")
        }
        return out.value ?: throw SmtcException("RoGetActivationFactory($classId) 返回空 factory")
    } finally {
        WindowsDeleteString(hstr)
    }
}

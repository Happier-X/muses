package com.muses.player.desktop.smtc

import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary

/** SMTC 相关 WinRT 接口 IID 与枚举常量（来源：任务调研报告 §3，官方 winmd 双重核对）。 */
internal object SmtcNative {

    // ── IID ────────────────────────────────────────────────

    val IID_INTEROP: Guid.GUID = guid("DDB0472D-C911-4A1F-86D9-DC3D71A95F5A") // ISystemMediaTransportControlsInterop
    val IID_CONTROLS: Guid.GUID = guid("99FA3FF4-1742-42A6-902E-087D41F965EC") // ISystemMediaTransportControls
    val IID_CONTROLS2: Guid.GUID = guid("EA98D2F6-7F3C-4AF2-A586-72889808EFB1") // ISystemMediaTransportControls2
    val IID_DISPLAY_UPDATER: Guid.GUID = guid("8ABBC53E-FA55-4ECF-AD8E-C984E5DD1550")
    val IID_MUSIC_PROPERTIES: Guid.GUID = guid("6BBF0C59-D0A0-4D26-92A0-F978E1D18E7B") // IMusicDisplayProperties
    val IID_MUSIC_PROPERTIES2: Guid.GUID = guid("00368462-97D3-44B9-B00F-008AFCEFAF18") // IMusicDisplayProperties2
    val IID_ACTIVATION_FACTORY: Guid.GUID = guid("00000035-0000-0000-C000-000000000046") // IActivationFactory
    val IID_BUTTON_PRESSED_HANDLER: Guid.GUID = guid("0557E996-7B23-5BAE-AA81-EA0D671143A4")
    private val IID_AGILE_OBJECT_BYTES: ByteArray = iidBytes("94EA2B94-E9CC-49E0-C0FF-EE64CA8F5B90") // IAgileObject
    private val IID_IUNKNOWN_BYTES: ByteArray = iidBytes("00000000-0000-0000-C000-000000000046")
    private val IID_IINSPECTABLE_BYTES: ByteArray = iidBytes("AF86E2E0-B12D-4C6A-9C5A-D7AA65101E90")

    private val QI_ACCEPTED: List<ByteArray> = listOf(
        IID_IUNKNOWN_BYTES,
        IID_IINSPECTABLE_BYTES,
        IID_AGILE_OBJECT_BYTES,
        IID_BUTTON_PRESSED_HANDLER.toByteArray(),
    )

    // ── activation factory 字符串 ──────────────────────────

    const val CLASS_CONTROLS = "Windows.Media.SystemMediaTransportControls"
    const val CLASS_TIMELINE_PROPERTIES = "Windows.Media.SystemMediaTransportControlsTimelineProperties"

    // ── 枚举（native 值） ──────────────────────────────────

    /** MediaPlaybackType：Music=1。 */
    const val PLAYBACK_TYPE_MUSIC = 1

    /** SystemMediaTransportControlsButton：Play=0/Pause=1/Stop=2/…/Next=6/Previous=7。 */
    const val BUTTON_PLAY = 0
    const val BUTTON_PAUSE = 1
    const val BUTTON_NEXT = 6
    const val BUTTON_PREVIOUS = 7

    /** ISystemMediaTransportControls vtable 槽位（槽 0-5 = IUnknown+IInspectable）。 */
    const val SLOT_PUT_PLAYBACK_STATUS = 7
    const val SLOT_GET_DISPLAY_UPDATER = 8
    const val SLOT_PUT_IS_ENABLED = 11
    const val SLOT_PUT_IS_PLAY_ENABLED = 13
    const val SLOT_PUT_IS_PAUSE_ENABLED = 17
    const val SLOT_PUT_IS_PREVIOUS_ENABLED = 25
    const val SLOT_PUT_IS_NEXT_ENABLED = 27
    const val SLOT_ADD_BUTTON_PRESSED = 32
    const val SLOT_REMOVE_BUTTON_PRESSED = 33

    /** ISystemMediaTransportControls2 vtable 槽位。 */
    const val SLOT2_UPDATE_TIMELINE_PROPERTIES = 12

    /** ISystemMediaTransportControlsDisplayUpdater vtable 槽位。 */
    const val SLOT_PUT_TYPE = 7
    const val SLOT_GET_MUSIC_PROPERTIES = 12
    const val SLOT_UPDATE = 17

    /** IMusicDisplayProperties（AlbumArtist 在 Artist 之前）。 */
    const val SLOT_MUSIC_PUT_TITLE = 7
    const val SLOT_MUSIC_PUT_ALBUM_ARTIST = 9
    const val SLOT_MUSIC_PUT_ARTIST = 11

    /** IMusicDisplayProperties2（QI 后调用）。 */
    const val SLOT_MUSIC2_PUT_ALBUM_TITLE = 7

    /** TimelineProperties 接口槽位（get/put 交替：6/7 StartTime、8/9 EndTime、10/11 MinSeekTime、12/13 MaxSeekTime、14/15 Position）。 */
    const val SLOT_TIMELINE_PUT_END_TIME = 9
    const val SLOT_TIMELINE_PUT_MIN_SEEK_TIME = 11
    const val SLOT_TIMELINE_PUT_MAX_SEEK_TIME = 13
    const val SLOT_TIMELINE_PUT_POSITION = 15

    /** IActivationFactory.ActivateInstance 槽位。 */
    const val SLOT_ACTIVATE_INSTANCE = 6

    /** ISystemMediaTransportControlsButtonPressedEventArgs.get_Button 槽位。 */
    const val SLOT_EVENTARGS_GET_BUTTON = 6

    const val E_NOINTERFACE = -0x7fffbffe // 0x80004002

    /** GUID 字符串（无大括号）→ JNA GUID（可传指针，可 toByteArray）。 */
    private fun guid(value: String): Guid.GUID =
        com.sun.jna.platform.win32.Ole32Util.getGUIDFromString("{$value}")

    private fun iidBytes(value: String): ByteArray = guid(value).toByteArray()

    /** QI 的 riid 与期望 IID 的 16 字节内存比较。 */
    private fun matches(riid: Pointer, expected: ByteArray): Boolean =
        riid.getByteArray(0, 16).contentEquals(expected)

    /** QI 分发：接受 IUnknown/IInspectable/IAgileObject/handler IID，其余 E_NOINTERFACE。 */
    internal fun queryInterfaceAccepts(riid: Pointer): Boolean = QI_ACCEPTED.any { matches(riid, it) }
}

/** 读取 WinRT 对象 vtable 槽位函数（槽 0-2 = IUnknown，3-5 = IInspectable，方法自槽 6 起）。 */
private fun Pointer.vtableFunction(slot: Int): Function =
    Function.getFunction(getPointer(0).getPointer(slot.toLong() * Native.POINTER_SIZE))

/**
 * 以 HRESULT(int) 返回值调用 WinRT 对象的 vtable 方法（x64 单一调用约定）。
 * bool 参数按 WinRT ABI 为 1 字节，x64 寄存器传参下以 int32 传 0/1 兼容。
 */
internal fun Pointer.invokeHr(slot: Int, vararg args: Any?): Int {
    val fn = vtableFunction(slot)
    val arguments = arrayOfNulls<Any>(args.size + 1)
    arguments[0] = this
    args.copyInto(arguments, 1)
    val result = fn.invoke(Int::class.javaPrimitiveType!!, arguments)
    return (result as? Number)?.toInt() ?: 0
}

/** 调用返回接口指针的 get_* 方法（out void**）。 */
internal fun Pointer.invokeGetObject(slot: Int, vararg args: Any?): Pointer {
    val out = PointerByReference()
    val hr = invokeHr(slot, *args, out)
    if (!hresultSucceeded(hr)) throw SmtcException("vtable 槽 $slot 调用失败 hr=${hresultHex(hr)}")
    return out.value ?: throw SmtcException("vtable 槽 $slot 返回空对象")
}

// ── ButtonPressed 事件回调 COM 对象 ────────────────────────

private fun interface ComVoidFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?): Int
}

private fun interface ComQueryInterfaceFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?, riid: Pointer?, out: PointerByReference?): Int
}

private fun interface ComGetIidsFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?, iidCount: IntByReference?, iids: PointerByReference?): Int
}

private fun interface ComGetRuntimeClassNameFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?, className: PointerByReference?): Int
}

private fun interface ComGetTrustLevelFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?, trustLevel: IntByReference?): Int
}

/** Invoke(sender, args)：ButtonPressed 事件入口（args 上取 get_Button）。 */
private fun interface ComInvokeFn : StdCallLibrary.StdCallCallback {
    fun invoke(thisPtr: Pointer?, sender: Pointer?, args: Pointer?): Int
}

/** COM 回调对象的 vtable（IUnknown 3 + IInspectable 3 + Invoke），布局仿 JNA DispatchListener。 */
@Structure.FieldOrder("queryInterface", "addRef", "release", "getIids", "getRuntimeClassName", "getTrustLevel", "invoke")
private class CallbackVTable : Structure() {
    @JvmField var queryInterface: ComQueryInterfaceFn? = null
    @JvmField var addRef: ComVoidFn? = null
    @JvmField var release: ComVoidFn? = null
    @JvmField var getIids: ComGetIidsFn? = null
    @JvmField var getRuntimeClassName: ComGetRuntimeClassNameFn? = null
    @JvmField var getTrustLevel: ComGetTrustLevelFn? = null
    @JvmField var invoke: ComInvokeFn? = null
}

/**
 * ButtonPressed 事件的 COM 回调对象（实现 IUnknown + IInspectable + Invoke）。
 *
 * 引用计数固定返回 1 不真正释放：native 侧 add/remove_ButtonPressed 的 AddRef/Release
 * 为空操作，Java 侧以强引用保活直到会话关闭（调研报告 §3.f 坑 7：回调悬挂会导致 JVM crash）。
 * 回调线程为系统 RPC 线程池，[onButton] 内只做轻量转发，异常不得跨 COM 边界。
 */
internal class SmtcButtonPressedHandler(private val onButton: (button: Int) -> Unit) {

    private val vtable = CallbackVTable().apply {
        queryInterface = ComQueryInterfaceFn { _, riid, out ->
            if (riid != null && out != null && SmtcNative.queryInterfaceAccepts(riid)) {
                out.value = objectPointer
                0 // S_OK
            } else {
                out?.value = null
                SmtcNative.E_NOINTERFACE
            }
        }
        addRef = ComVoidFn { _ -> 1 }
        release = ComVoidFn { _ -> 1 }
        getIids = ComGetIidsFn { _, iidCount, iids ->
            iidCount?.value = 0
            iids?.value = null
            0
        }
        getRuntimeClassName = ComGetRuntimeClassNameFn { _, className ->
            // null HSTRING 合法（等价空串）；调用方一般不读取
            className?.value = null
            0
        }
        getTrustLevel = ComGetTrustLevelFn { _, trustLevel ->
            trustLevel?.value = 0
            0
        }
        invoke = ComInvokeFn { _, _, args ->
            try {
                if (args != null) {
                    val out = IntByReference()
                    val hr = args.invokeHr(SmtcNative.SLOT_EVENTARGS_GET_BUTTON, out)
                    if (hresultSucceeded(hr)) onButton(out.value)
                }
                0
            } catch (t: Throwable) {
                0x80004005.toInt() // E_FAIL：不让异常跨 COM 边界
            }
        }
        write()
    }

    /** COM 对象内存：槽 0 指向 vtable（结构见调研报告 §3.f）。 */
    val objectPointer: Pointer = Memory(Native.POINTER_SIZE.toLong()).apply { setPointer(0, vtable.pointer) }
}

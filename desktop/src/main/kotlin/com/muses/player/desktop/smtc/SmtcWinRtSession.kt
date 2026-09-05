package com.muses.player.desktop.smtc

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * SMTC WinRT 真实会话（纯 JNA interop，调研报告 §3）：
 * - 建立：RoInitialize(MTA) → RoGetActivationFactory(interop) → GetForWindow(hwnd) →
 *   启用 IsEnabled + Play/Pause/Previous/Next 四键 → add_ButtonPressed 事件；
 * - 更新：put_PlaybackStatus（槽 7）、DisplayUpdater + MusicProperties + Update()（槽 17，
 *   不调用则面板不刷新）、ISystemMediaTransportControls2.UpdateTimelineProperties（槽 12，QI 后调用）；
 * - 释放：remove_ButtonPressed + put_IsEnabled(false) + 逐层 Release + RoUninitialize。
 *
 * 所有方法必须在该会话的专用 MTA 线程串行调用（由 SmtcController 的单线程 executor 保证）；
 * 更新类失败全部静默记日志，绝不影响播放主链路。
 */
internal class SmtcWinRtSession private constructor(
    private val runtime: WinRtRuntime,
    private val controls: Pointer,
    private val onTogglePlay: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val errorLog: (tag: String, msg: String, e: Throwable?) -> Unit,
) : SmtcSession {

    companion object {
        private const val TAG = "Smtc"

        private const val SLOT_GET_FOR_WINDOW = 6 // ISystemMediaTransportControlsInterop 唯一方法
        private const val SLOT_QUERY_INTERFACE = 0
        private const val SLOT_RELEASE = 2

        /**
         * 建立会话；任一步失败抛异常（由 Controller 捕获降级为 no-op）。
         * 专用 MTA 线程内调用（RoInitialize 与 JVM 主线程可能存在的 STA 组件隔离，调研报告 §3.a）。
         */
        fun open(
            hwnd: Long,
            actions: SmtcActions,
            errorLog: (tag: String, msg: String, e: Throwable?) -> Unit,
        ): SmtcWinRtSession {
            val runtime = WinRtRuntime.INSTANCE
            val initHr = runtime.RoInitialize(RO_INIT_MULTITHREADED)
            if (initHr < 0) throw SmtcException("RoInitialize 失败 hr=${hresultHex(initHr)}")
            try {
                val interop = runtime.getActivationFactory(SmtcNative.CLASS_CONTROLS, SmtcNative.IID_INTEROP)
                val controlsOut = PointerByReference()
                val hr = interop.invokeHr(SLOT_GET_FOR_WINDOW, hwnd, SmtcNative.IID_CONTROLS, controlsOut)
                if (!hresultSucceeded(hr)) {
                    throw SmtcException("GetForWindow 失败 hr=${hresultHex(hr)}")
                }
                interop.invokeHr(SLOT_RELEASE) // factory 引用用完即还
                val controls = controlsOut.value
                    ?: throw SmtcException("GetForWindow 返回空会话对象")
                return SmtcWinRtSession(
                    runtime = runtime,
                    controls = controls,
                    onTogglePlay = actions.onTogglePlay,
                    onNext = actions.onNext,
                    onPrevious = actions.onPrevious,
                    errorLog = errorLog,
                ).also { it.enable() }
            } catch (t: Throwable) {
                runtime.RoUninitialize()
                throw t
            }
        }
    }

    // ButtonPressed 订阅 token（<0 表示未订阅成功）
    private var buttonToken: Long = -1L

    // 派生接口/子对象缓存（各自持有一次引用，close 时 Release）
    private var handler: SmtcButtonPressedHandler? = null
    private var controls2: Pointer? = null
    private var updater: Pointer? = null
    private var music: Pointer? = null
    private var music2: Pointer? = null
    private var timelineProps: Pointer? = null

    private fun enable() {
        runCatching {
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_ENABLED, 1)
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_PLAY_ENABLED, 1)
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_PAUSE_ENABLED, 1)
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_PREVIOUS_ENABLED, 1)
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_NEXT_ENABLED, 1)
        }.onFailure { e -> errorLog(TAG, "启用 SMTC 按键失败", e) }
        runCatching {
            val h = SmtcButtonPressedHandler { button -> dispatchButton(button) }
            val token = LongByReference()
            val hr = controls.invokeHr(SmtcNative.SLOT_ADD_BUTTON_PRESSED, h.objectPointer, token)
            if (hresultSucceeded(hr)) {
                buttonToken = token.value
                handler = h // 强引用保活：回调悬挂会导致 JVM crash（调研报告 §3.f 坑 7）
            }
        }.onFailure { e -> errorLog(TAG, "订阅 SMTC 按键事件失败", e) }
    }

    /** 系统按键 → 注入动作；面板按当前状态只暴露 Play 或 Pause，toggle 语义等价。 */
    private fun dispatchButton(button: Int) {
        when (button) {
            SmtcNative.BUTTON_PLAY, SmtcNative.BUTTON_PAUSE -> onTogglePlay()
            SmtcNative.BUTTON_NEXT -> onNext()
            SmtcNative.BUTTON_PREVIOUS -> onPrevious()
        }
    }

    override fun updateMetadata(title: String, artist: String?, album: String?) {
        runCatching {
            val u = displayUpdater()
            u.invokeHr(SmtcNative.SLOT_PUT_TYPE, SmtcNative.PLAYBACK_TYPE_MUSIC)
            val m = musicProperties(u)
            m.putHString(SmtcNative.SLOT_MUSIC_PUT_TITLE, title)
            m.putHString(SmtcNative.SLOT_MUSIC_PUT_ARTIST, artist)
            musicProperties2(m)?.putHString(SmtcNative.SLOT_MUSIC2_PUT_ALBUM_TITLE, album)
            u.invokeHr(SmtcNative.SLOT_UPDATE)
        }.onFailure { e -> errorLog(TAG, "更新 SMTC 元数据失败", e) }
    }

    override fun updatePlaybackStatus(status: SmtcPlaybackStatus) {
        runCatching {
            controls.invokeHr(SmtcNative.SLOT_PUT_PLAYBACK_STATUS, status.nativeValue)
        }.onFailure { e -> errorLog(TAG, "更新 SMTC 播放状态失败", e) }
    }

    override fun updateTimeline(positionMs: Long, durationMs: Long) {
        runCatching {
            if (durationMs <= 0L) return // 时长未知（缓冲期）不推进度
            val c2 = controls2() ?: return
            val props = timelineProperties()
            props.putTimelineHundredNs(SmtcNative.SLOT_TIMELINE_PUT_END_TIME, durationMs * 10_000L)
            props.putTimelineHundredNs(SmtcNative.SLOT_TIMELINE_PUT_MIN_SEEK_TIME, 0L)
            props.putTimelineHundredNs(SmtcNative.SLOT_TIMELINE_PUT_MAX_SEEK_TIME, durationMs * 10_000L)
            props.putTimelineHundredNs(
                SmtcNative.SLOT_TIMELINE_PUT_POSITION,
                positionMs.coerceIn(0L, durationMs) * 10_000L,
            )
            c2.invokeHr(SmtcNative.SLOT2_UPDATE_TIMELINE_PROPERTIES, props)
        }.onFailure { e -> errorLog(TAG, "更新 SMTC 时间轴失败", e) }
    }

    override fun close() {
        runCatching {
            if (buttonToken >= 0L) {
                controls.invokeHr(SmtcNative.SLOT_REMOVE_BUTTON_PRESSED, buttonToken)
                buttonToken = -1L
            }
            controls.invokeHr(SmtcNative.SLOT_PUT_IS_ENABLED, 0)
        }.onFailure { e -> errorLog(TAG, "清理 SMTC 会话失败", e) }
        listOfNotNull(controls2, music2, music, updater, timelineProps).forEach { p ->
            runCatching { p.invokeHr(SLOT_RELEASE) }
        }
        runCatching { controls.invokeHr(SLOT_RELEASE) }
        runCatching { runtime.RoUninitialize() }
    }

    // ── 内部：派生接口获取（失败不缓存，下次重试） ──────────

    private fun displayUpdater(): Pointer =
        updater ?: controls.invokeGetObject(SmtcNative.SLOT_GET_DISPLAY_UPDATER).also { updater = it }

    private fun musicProperties(u: Pointer): Pointer =
        music ?: u.invokeGetObject(SmtcNative.SLOT_GET_MUSIC_PROPERTIES).also { music = it }

    private fun musicProperties2(m: Pointer): Pointer? =
        music2 ?: queryInterface(m, SmtcNative.IID_MUSIC_PROPERTIES2)?.also { music2 = it }

    private fun controls2(): Pointer? =
        controls2 ?: queryInterface(controls, SmtcNative.IID_CONTROLS2)?.also { controls2 = it }

    /** TimelineProperties 为 activatable 对象：factory + ActivateInstance（调研报告 §3.g）。 */
    private fun timelineProperties(): Pointer =
        timelineProps ?: run {
            val factory = runtime.getActivationFactory(
                SmtcNative.CLASS_TIMELINE_PROPERTIES,
                SmtcNative.IID_ACTIVATION_FACTORY,
            )
            val instance = factory.invokeGetObject(SmtcNative.SLOT_ACTIVATE_INSTANCE)
            factory.invokeHr(SLOT_RELEASE) // factory 引用用完即还
            instance
        }.also { timelineProps = it }

    private fun queryInterface(target: Pointer, iid: Guid.GUID): Pointer? {
        val out = PointerByReference()
        val hr = target.invokeHr(SLOT_QUERY_INTERFACE, iid, out)
        return if (hresultSucceeded(hr)) out.value else null
    }

    /**
     * put HSTRING；null/空串传 NULL HSTRING（WinRT 侧拷贝为空，清掉上一曲残留），
     * 非空串调用后立即释放句柄。
     */
    private fun Pointer.putHString(slot: Int, value: String?) {
        val h: Pointer? = value?.takeIf { it.isNotEmpty() }?.let { runtime.createHString(it) }
        try {
            invokeHr(slot, h)
        } finally {
            h?.let { runtime.WindowsDeleteString(it) }
        }
    }

    /** put TimeSpan（Int64，100ns 单位）。 */
    private fun Pointer.putTimelineHundredNs(slot: Int, hundredNs: Long) {
        invokeHr(slot, hundredNs)
    }
}

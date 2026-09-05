package com.muses.player.desktop.smtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** 面板元数据（标题/艺术家/专辑；封面首版不做，见任务调研报告 §3.h）。 */
data class SmtcMetadata(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
)

/**
 * Windows SMTC 控制器：任务栏/音量面板媒体浮层的注册、状态推送与按键回流。
 *
 * - 全部 WinRT 调用在专用单线程 daemon executor 上串行执行（RoInitialize(MTA) 所在线程）；
 * - 状态源以 StateFlow 注入，元数据/播放状态即时推送，时间轴按 [timelineThrottleMs] 节流；
 * - 动作以 lambda 注入（onTogglePlay/onNext/onPrevious），本类不感知播放器实现；
 * - 非 Windows / 无主窗口 / WinRT 不可用 / 任一环节失败 → 静默降级为 no-op，不影响播放主链路；
 * - install/uninstall 均幂等。
 */
class SmtcController(
    private val sessionFactory: SmtcSessionFactory = SmtcSessionFactory { hwnd, actions, log ->
        SmtcWinRtSession.open(hwnd, actions, log)
    },
    private val hwndFinder: (windowTitle: String) -> Long? = SmtcWindows::findMainWindowHwnd,
    private val timelineThrottleMs: Long = 1_000L,
    private val hwndRetryCount: Int = 20,
    private val hwndRetryDelayMs: Long = 500L,
    private val errorLog: (tag: String, msg: String, e: Throwable?) -> Unit = { _, _, _ -> },
) {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "muses-smtc").apply { isDaemon = true }
    }
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    private val installed = AtomicBoolean(false)

    @Volatile private var session: SmtcSession? = null

    /**
     * 安装 SMTC；重复调用安全（返回 null）。返回安装 Job：hwnd 查找 + 会话建立完成即结束
     * （状态订阅随会话建立启动，不随 Job 结束）。窗口尚未显示时按轮询参数重试。
     * uninstall 后重复 install 会因调度器关闭被静默拒绝（SMTC 进程级单次安装，见 design）。
     */
    fun install(
        windowTitle: String,
        metadata: StateFlow<SmtcMetadata?>,
        isPlaying: StateFlow<Boolean>,
        positionMs: StateFlow<Long>,
        durationMs: StateFlow<Long>,
        onTogglePlay: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit,
    ): Job? {
        if (!installed.compareAndSet(false, true)) return null
        return try {
            scope.launch {
                val hwnd = findHwnd(windowTitle) ?: run {
                    errorLog(TAG, "未找到主窗口（$windowTitle），SMTC 不可用", null)
                    return@launch
                }
                val actions = SmtcActions(onTogglePlay, onNext, onPrevious)
                val s = runCatching { sessionFactory.open(hwnd, actions, errorLog) }
                    .onFailure { e -> errorLog(TAG, "注册 SMTC 媒体会话失败", e) }
                    .getOrNull() ?: return@launch
                session = s
                // 播放状态：无曲目 → Stopped；有曲目按 isPlaying → Playing/Paused
                launch {
                    combine(metadata, isPlaying) { m, playing ->
                        when {
                            m == null -> SmtcPlaybackStatus.STOPPED
                            playing -> SmtcPlaybackStatus.PLAYING
                            else -> SmtcPlaybackStatus.PAUSED
                        }
                    }.collect { status ->
                        runCatching { s.updatePlaybackStatus(status) }
                            .onFailure { e -> errorLog(TAG, "推送 SMTC 播放状态失败", e) }
                    }
                }
                // 元数据：曲目切换刷新（null 交由状态流回落 Stopped）
                launch {
                    metadata.collect { m ->
                        if (m != null) {
                            runCatching { s.updateMetadata(m.title, m.artist, m.album) }
                                .onFailure { e -> errorLog(TAG, "推送 SMTC 元数据失败", e) }
                        }
                    }
                }
                // 时间轴：节流推送（首版只读进度条，不注册 seek 回调）
                launch {
                    combine(positionMs, durationMs) { pos, dur -> pos to dur }
                        .sample(timelineThrottleMs)
                        .collect { (pos, dur) ->
                            runCatching { s.updateTimeline(pos, dur) }
                                .onFailure { e -> errorLog(TAG, "推送 SMTC 时间轴失败", e) }
                        }
                }
            }
        } catch (e: Throwable) {
            // 调度器已关闭（uninstall 后重复 install）等异常：静默降级为不可用
            errorLog(TAG, "SMTC 安装失败", e)
            null
        }
    }

    /**
     * 卸载并清理会话；未安装/已卸载时安全。退出路径调用（同步短等待，尽力让浮层消失）。
     */
    fun uninstall() {
        if (!installed.compareAndSet(true, false)) return
        val s = session
        session = null
        scope.cancel()
        if (s != null) {
            runCatching {
                executor.submit { s.close() }.get(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }.onFailure { e -> errorLog(TAG, "清理 SMTC 会话未完成", e) }
        }
        executor.shutdown()
    }

    private suspend fun findHwnd(windowTitle: String): Long? {
        repeat(hwndRetryCount) {
            runCatching { hwndFinder(windowTitle) }.getOrNull()?.let { return it }
            delay(hwndRetryDelayMs)
        }
        return null
    }

    private companion object {
        const val TAG = "Smtc"
        const val CLOSE_TIMEOUT_SECONDS = 2L
    }
}

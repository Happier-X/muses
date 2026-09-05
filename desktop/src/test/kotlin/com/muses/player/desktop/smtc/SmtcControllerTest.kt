package com.muses.player.desktop.smtc

import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * SmtcController 纯逻辑单测（fake SmtcSession，不触达 WinRT/JNA）：
 * 状态映射、元数据推送、时间轴节流、异常隔离、uninstall 幂等、无窗口降级。
 */
class SmtcControllerTest {

    private class FakeSession : SmtcSession {
        val metadata = Collections.synchronizedList(mutableListOf<Triple<String, String?, String?>>())
        val statuses = Collections.synchronizedList(mutableListOf<SmtcPlaybackStatus>())
        val timelines = Collections.synchronizedList(mutableListOf<Pair<Long, Long>>())
        @Volatile var failMetadata = false
        @Volatile var closed = false

        override fun updateMetadata(title: String, artist: String?, album: String?) {
            if (failMetadata) throw IllegalStateException("metadata boom")
            metadata += Triple(title, artist, album)
        }

        override fun updatePlaybackStatus(status: SmtcPlaybackStatus) {
            statuses += status
        }

        override fun updateTimeline(positionMs: Long, durationMs: Long) {
            timelines += positionMs to durationMs
        }

        override fun close() {
            closed = true
        }
    }

    private fun controllerOf(
        session: FakeSession?,
        factoryCalls: MutableList<Long> = Collections.synchronizedList(mutableListOf()),
        factoryFails: Boolean = false,
        hwnd: Long? = 42L,
    ): Pair<SmtcController, MutableList<Long>> {
        val controller = SmtcController(
            sessionFactory = SmtcSessionFactory { hwndValue, _, _ ->
                factoryCalls += hwndValue
                when {
                    factoryFails -> throw IllegalStateException("factory boom")
                    session != null -> session
                    else -> throw IllegalStateException("no session")
                }
            },
            hwndFinder = { title -> if (title == "Muses") hwnd else null },
            timelineThrottleMs = 10L,
            hwndRetryCount = 2,
            hwndRetryDelayMs = 1L,
        )
        return controller to factoryCalls
    }

    private fun install(
        controller: SmtcController,
        metadata: StateFlow<SmtcMetadata?>,
        isPlaying: StateFlow<Boolean>,
        positionMs: MutableStateFlow<Long> = MutableStateFlow(0L),
        durationMs: MutableStateFlow<Long> = MutableStateFlow(0L),
    ): Job = controller.install(
        windowTitle = "Muses",
        metadata = metadata,
        isPlaying = isPlaying,
        positionMs = positionMs,
        durationMs = durationMs,
        onTogglePlay = {},
        onNext = {},
        onPrevious = {},
    ) ?: error("install 应成功")

    private fun awaitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertTrue(condition(), "等待条件超时")
    }

    @Test
    fun `有曲目时推送元数据与播放状态映射`() {
        val session = FakeSession()
        val (controller, _) = controllerOf(session)
        val metadata = MutableStateFlow<SmtcMetadata?>(SmtcMetadata("歌名", "歌手", "专辑"))
        val isPlaying = MutableStateFlow(true)
        install(controller, metadata, isPlaying)

        awaitUntil { session.metadata.isNotEmpty() }
        assertEquals(Triple("歌名", "歌手", "专辑"), session.metadata.first())
        awaitUntil { session.statuses.contains(SmtcPlaybackStatus.PLAYING) }

        isPlaying.value = false
        awaitUntil { session.statuses.lastOrNull() == SmtcPlaybackStatus.PAUSED }
        controller.uninstall()
    }

    @Test
    fun `无曲目时状态回落 Stopped 且不推送元数据`() {
        val session = FakeSession()
        val (controller, _) = controllerOf(session)
        install(controller, MutableStateFlow(null), MutableStateFlow(false))

        awaitUntil { session.statuses.contains(SmtcPlaybackStatus.STOPPED) }
        assertTrue(session.metadata.isEmpty())
        controller.uninstall()
    }

    @Test
    fun `元数据更新异常不阻断播放状态推送`() {
        val session = FakeSession()
        val (controller, _) = controllerOf(session)
        val metadata = MutableStateFlow<SmtcMetadata?>(null)
        val isPlaying = MutableStateFlow(false)
        install(controller, metadata, isPlaying)
        awaitUntil { session.statuses.isNotEmpty() }

        session.failMetadata = true
        metadata.value = SmtcMetadata("新歌")
        isPlaying.value = true
        awaitUntil { session.statuses.contains(SmtcPlaybackStatus.PLAYING) }
        controller.uninstall()
    }

    @Test
    fun `时间轴经节流推送`() {
        val session = FakeSession()
        val (controller, _) = controllerOf(session)
        val positionMs = MutableStateFlow(0L)
        val durationMs = MutableStateFlow(0L)
        install(controller, MutableStateFlow(SmtcMetadata("t")), MutableStateFlow(true), positionMs, durationMs)

        positionMs.value = 123L
        durationMs.value = 456L
        awaitUntil { session.timelines.any { it.first == 123L && it.second == 456L } }
        controller.uninstall()
    }

    @Test
    fun `找不到主窗口时不建会话且卸载安全`() {
        val (controller, factoryCalls) = controllerOf(null, hwnd = null)
        runBlocking { install(controller, MutableStateFlow(null), MutableStateFlow(false)).join() }

        controller.uninstall()
        controller.uninstall() // 幂等
        assertTrue(factoryCalls.isEmpty())
    }

    @Test
    fun `会话工厂异常被静默降级`() {
        val (controller, factoryCalls) = controllerOf(null, factoryFails = true)
        runBlocking { install(controller, MutableStateFlow(null), MutableStateFlow(false)).join() }

        assertEquals(1, factoryCalls.size)
        controller.uninstall()
    }

    @Test
    fun `uninstall 关闭会话且幂等`() {
        val session = FakeSession()
        val (controller, _) = controllerOf(session)
        install(controller, MutableStateFlow(null), MutableStateFlow(false))
        awaitUntil { session.statuses.isNotEmpty() }

        controller.uninstall()
        controller.uninstall()
        assertTrue(session.closed)
    }
}

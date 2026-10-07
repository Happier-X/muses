package com.muses.player.core.media.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VolumeBoostSessionTest {
    @Test
    fun `设置先于音频输出到达时等待真实会话`() = runTest {
        val sessions = mutableListOf<Int>()
        val effect = FakeEffect()
        val boost = VolumeBoostSession(this) { id -> sessions += id; effect }
        boost.apply(6)
        assertTrue(sessions.isEmpty())
        assertNull(boost.status.value.error)
        assertNull(boost.status.value.appliedDb)

        boost.bind(42)
        assertEquals(listOf(42), sessions)
        assertEquals(600, effect.targetGainMillibels)
        assertTrue(effect.enabled)
        assertEquals(6, boost.status.value.appliedDb)
        boost.release()
    }

    @Test
    fun `初始化临时失败后无需设置变化即可恢复`() = runTest {
        var attempts = 0
        val effect = FakeEffect()
        val boost = VolumeBoostSession(this) {
            attempts++
            if (attempts == 1) error("效果器暂时不可用")
            effect
        }
        boost.bind(42)
        boost.apply(6)
        assertNotNull(boost.status.value.error)
        advanceUntilIdle()
        assertEquals(2, attempts)
        assertEquals(6, boost.status.value.appliedDb)
        assertNull(boost.status.value.error)
        boost.release()
    }

    @Test
    fun `读回值不符时重建效果器且不会误报成功`() = runTest {
        val rejected = FakeEffect(ignoreGain = true)
        val recovered = FakeEffect()
        var attempts = 0
        val boost = VolumeBoostSession(this) { if (attempts++ == 0) rejected else recovered }
        boost.bind(42)
        boost.apply(9)
        assertNull(boost.status.value.appliedDb)
        assertNotNull(boost.status.value.error)
        assertTrue(rejected.released)
        advanceUntilIdle()
        assertEquals(900, recovered.targetGainMillibels)
        assertEquals(9, boost.status.value.appliedDb)
        boost.release()
    }

    @Test
    fun `会话重建时释放旧效果器并保留最新档位`() = runTest {
        val effects = mutableListOf<FakeEffect>()
        val sessions = mutableListOf<Int>()
        val boost = VolumeBoostSession(this) { id ->
            sessions += id
            FakeEffect().also { effects += it }
        }
        boost.bind(42)
        boost.apply(3)
        boost.apply(9)
        boost.bind(71)
        assertEquals(listOf(42, 71), sessions)
        assertTrue(effects.first().released)
        assertEquals(900, effects.last().targetGainMillibels)
        assertEquals(71, boost.status.value.sessionId)
        boost.release()
    }

    @Test
    fun `永久不支持时重试有限且关闭会取消重试`() = runTest {
        var attempts = 0
        val boost = VolumeBoostSession(this) { attempts++; error("设备不支持") }
        boost.bind(42)
        boost.apply(6)
        advanceUntilIdle()
        assertEquals(4, attempts)
        assertNotNull(boost.status.value.error)

        boost.apply(3)
        assertEquals(5, attempts)
        boost.apply(0)
        advanceUntilIdle()
        assertEquals(5, attempts)
        assertEquals(0, boost.status.value.appliedDb)
        assertNull(boost.status.value.error)
        boost.release()
    }

    @Test
    fun `切换档位取消旧重试并按新档位恢复`() = runTest {
        var attempts = 0
        val effect = FakeEffect()
        val boost = VolumeBoostSession(this) {
            if (attempts++ < 2) error("临时失败")
            effect
        }
        boost.bind(42)
        boost.apply(6)
        runCurrent()
        advanceTimeBy(100)
        boost.apply(3)
        advanceUntilIdle()
        assertEquals(3, attempts)
        assertEquals(300, effect.targetGainMillibels)
        assertEquals(3, boost.status.value.appliedDb)
        boost.release()
    }

    @Test
    fun `恢复播放重新启用被系统关闭的效果器`() = runTest {
        val effect = FakeEffect()
        val boost = VolumeBoostSession(this) { effect }
        boost.bind(42)
        boost.apply(6)
        effect.enabled = false
        boost.refresh()
        assertTrue(effect.enabled)
        assertEquals(6, boost.status.value.appliedDb)
        boost.apply(0)
        assertTrue(effect.released)
        boost.release()
    }

    @Test
    fun `释放取消待执行任务且可以重复调用`() = runTest {
        var attempts = 0
        val boost = VolumeBoostSession(this) { attempts++; error("临时失败") }
        boost.bind(42)
        boost.apply(6)
        boost.release()
        boost.release()
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertEquals(0, boost.status.value.sessionId)
        assertEquals(0, boost.status.value.appliedDb)
    }

    private class FakeEffect(private val ignoreGain: Boolean = false) : VolumeBoostEffect {
        private var gain = 0
        override var targetGainMillibels: Int
            get() = gain
            set(value) { if (!ignoreGain) gain = value }
        override var enabled = false
        var released = false
            private set
        override fun release() {
            released = true
            enabled = false
        }
    }
}

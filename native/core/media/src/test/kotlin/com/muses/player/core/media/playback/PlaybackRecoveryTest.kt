package com.muses.player.core.media.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 规格 = src/features/player/controller.ts advanceToNextRecoveryCandidate + SAFE_PLAYBACK_ERRORS */
class PlaybackRecoveryControllerTest {

    @Test
    fun `沿activeOrder向后查找未尝试候选`() {
        val c = PlaybackRecoveryController()
        c.markAttempted("a") // 失败曲已登记
        val order = listOf("a", "b", "c")
        assertEquals(1, c.selectNextCandidate(order, 0))
    }

    @Test
    fun `回绕一次_从队尾回到队首`() {
        val c = PlaybackRecoveryController()
        // 队列 [a,b,c]，当前播到末尾 c 且失败：c 已 attempted，回绕到 a
        c.markAttempted("c")
        assertEquals(0, c.selectNextCandidate(listOf("a", "b", "c"), 2))
    }

    @Test
    fun `跳过attempted_回绕一圈无候选返回null`() {
        val c = PlaybackRecoveryController()
        listOf("a", "b").forEach(c::markAttempted)
        assertNull(c.selectNextCandidate(listOf("a", "b"), 1))
    }

    @Test
    fun `errorIndex越界时从头开始找`() {
        val c = PlaybackRecoveryController()
        // Web：startIndex=-1，offset=1 → 首个候选即队首
        assertEquals(0, c.selectNextCandidate(listOf("x", "y"), -5))
    }

    @Test
    fun `空队列返回null`() {
        val c = PlaybackRecoveryController()
        assertNull(c.selectNextCandidate(emptyList(), 0))
    }

    @Test
    fun `reset清空attempted`() {
        val c = PlaybackRecoveryController()
        c.markAttempted("a"); c.markAttempted("b")
        c.reset()
        // 清空后从 errorIndex=0 向后：offset=1 → 队列下标 1
        assertEquals(1, c.selectNextCandidate(listOf("a", "b"), 0))
    }
}

class PlaybackErrorCopyTest {

    @Test
    fun `IO_FILE_NOT_FOUND映射文件失效文案`() {
        assertEquals(
            "音频文件不存在或已失效，请重新扫描音源。",
            PlaybackErrorCopy.copyFor(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND),
        )
    }

    @Test
    fun `网络类映射检查网络文案`() {
        for (code in intArrayOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )) {
            assertEquals(
                "播放失败，请检查音频文件或网络连接。",
                PlaybackErrorCopy.copyFor(code),
            )
        }
    }

    @Test
    fun `认证与HTTP状态码映射WebDAV认证失败文案`() {
        for (code in intArrayOf(
            PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )) {
            assertEquals(
                "WebDAV 认证失败，请检查账号或重新添加音源。",
                PlaybackErrorCopy.copyFor(code),
            )
        }
    }

    @Test
    fun `权限类映射本地文件不可访问文案`() {
        assertEquals(
            "本地音频文件不可访问，请重新扫描或重新授权。",
            PlaybackErrorCopy.copyFor(PlaybackException.ERROR_CODE_IO_NO_PERMISSION),
        )
    }

    @Test
    fun `未知错误兜底不泄露内部信息`() {
        assertEquals("播放失败，请稍后重试。", PlaybackErrorCopy.copyFor(PlaybackException.ERROR_CODE_UNSPECIFIED))
        assertEquals("播放失败，请稍后重试。", PlaybackErrorCopy.safeCopy("/data/some/secret/path.mp3 not found"))
    }

    private fun playbackEx(code: Int): PlaybackException =
        PlaybackException("msg", null, code)
}

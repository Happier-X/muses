package com.muses.player.desktop.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S2 安全文案单测：白名单逐字对齐安卓侧 `PlaybackErrorCopy`，未知兜底不泄露内部信息。
 */
class DesktopPlaybackErrorCopyTest {

    @Test
    fun 白名单9条与安卓侧逐字一致() {
        assertEquals(9, DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS.size)
        assertTrue("音频文件不存在或已失效，请重新扫描音源。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("WebDAV 认证失败，请检查账号或重新添加音源。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("播放失败，请检查音频文件或网络连接。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("触发限流，稍后重试" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
    }

    @Test
    fun 非白名单兜底() {
        assertEquals(
            "播放失败，请稍后重试。",
            DesktopPlaybackErrorCopy.safeCopy("uk.co.caprica.vlcj.player.base.MediaPlayerException: boom"),
        )
        assertEquals(
            "触发限流，稍后重试",
            DesktopPlaybackErrorCopy.safeCopy("触发限流，稍后重试"),
        )
        assertEquals("播放失败，请稍后重试。", DesktopPlaybackErrorCopy.safeCopy(null))
    }
}

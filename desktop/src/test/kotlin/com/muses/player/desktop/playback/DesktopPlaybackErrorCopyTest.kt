package com.muses.player.desktop.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S2 安全文案单测：白名单逐字对齐安卓侧 `PlaybackErrorCopy`，未知兜底不泄露内部信息。
 */
class DesktopPlaybackErrorCopyTest {

    @Test
    fun 白名单为安卓九条加桌面专属三条() {
        // 安卓侧 9 条（逐字对齐）+ 桌面专属 3 条：VLC_MISSING、ONLINE_RESOLVER_MISSING、
        // ONLINE_RESOLVE_FAILED。勿按条数与安卓侧硬对齐：安卓侧在线失败回落通用文案，
        // 不定专门常量（见 PlaybackErrorCopy 内注释）。
        assertEquals(12, DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS.size)
        assertTrue("音频文件不存在或已失效，请重新扫描音源。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("WebDAV 认证失败，请检查账号或重新添加音源。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("播放失败，请检查音频文件或网络连接。" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue("触发限流，稍后重试" in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue(DesktopPlaybackErrorCopy.VLC_MISSING in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue(DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
        assertTrue(DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED in DesktopPlaybackErrorCopy.SAFE_PLAYBACK_ERRORS)
    }

    @Test
    fun 在线音源文案不被兜底() {
        // 回归：在线两条文案曾漏在白名单外，经 onSongFailed → safeCopy 被静默换成通用文案，
        // 用户只看到「播放失败，请稍后重试。」，拿不到「检查音源脚本」的指引
        assertEquals(
            DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING,
            DesktopPlaybackErrorCopy.safeCopy(DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING),
        )
        assertEquals(
            DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED,
            DesktopPlaybackErrorCopy.safeCopy(DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED),
        )
    }

    @Test
    fun 在线失败区分未接入与解析失败() {
        // 两种病因两种指引：未接入解析器（构建未启用在线音源）↔ 解析失败（检查音源脚本可用性），
        // 映射错方向会让用户去查错东西
        assertEquals(
            DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING,
            JvmPlayerPort.onlineUnavailableCopyFor(resolverAvailable = false),
        )
        assertEquals(
            DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED,
            JvmPlayerPort.onlineUnavailableCopyFor(resolverAvailable = true),
        )
    }

    @Test
    fun 经safeCopy落地的文案均已登记() {
        // 防漏网：以下常量都会经 JvmPlayerPort 的 safeCopy 流向 UI，漏登记即被兜底成通用文案。
        // 新增此类文案时，除了加进白名单，也把它加到这个列表里
        listOf(
            DesktopPlaybackErrorCopy.VLC_MISSING,
            DesktopPlaybackErrorCopy.FILE_NOT_FOUND,
            DesktopPlaybackErrorCopy.FILE_NO_PERMISSION,
            DesktopPlaybackErrorCopy.NETWORK,
            DesktopPlaybackErrorCopy.AUTH_FAILED,
            DesktopPlaybackErrorCopy.RATE_LIMITED_RETRY,
            DesktopPlaybackErrorCopy.ONLINE_RESOLVER_MISSING,
            DesktopPlaybackErrorCopy.ONLINE_RESOLVE_FAILED,
        ).forEach { copy ->
            assertEquals(copy, DesktopPlaybackErrorCopy.safeCopy(copy), "未登记进 SAFE_PLAYBACK_ERRORS：$copy")
        }
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

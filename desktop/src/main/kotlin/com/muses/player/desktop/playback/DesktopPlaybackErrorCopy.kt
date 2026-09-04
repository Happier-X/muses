package com.muses.player.desktop.playback

/**
 * S2 桌面播放安全文案（复刻 `PlaybackErrorCopy.SAFE_PLAYBACK_ERRORS` 白名单，不依赖 Media3）。
 *
 * commonMain 不收桌面专属 API，`core:media` 为安卓模块桌面不可依赖，
 * 故白名单逐字复刻（9 条 + 兜底 + 限流停止文案），语义冻结对齐安卓侧。
 */
object DesktopPlaybackErrorCopy {

    val SAFE_PLAYBACK_ERRORS = listOf(
        "找不到这首歌对应的 WebDAV 音源，请重新扫描音源。",
        "WebDAV 密码不存在，请重新添加该音源。",
        "WebDAV 播放缺少认证信息。",
        "本地音频文件不可访问，请重新扫描或重新授权。",
        "本地音频文件无访问权限，请重新授权音源目录。",
        "WebDAV 认证失败，请检查账号或重新添加音源。",
        "音频文件不存在或已失效，请重新扫描音源。",
        "播放失败，请检查音频文件或网络连接。",
        "触发限流，稍后重试",
    )

    const val DEFAULT_ERROR = "播放失败，请稍后重试。"

    /** 服务级限流/网关故障：跳歌只会继续撞墙，直接停止等用户手动重试。 */
    const val RATE_LIMITED_ERROR = "服务器请求过于频繁，请稍后再试。"

    const val RATE_LIMITED_RETRY = "触发限流，稍后重试"

    const val FILE_NOT_FOUND = "音频文件不存在或已失效，请重新扫描音源。"
    const val FILE_NO_PERMISSION = "本地音频文件不可访问，请重新扫描或重新授权。"
    const val NETWORK = "播放失败，请检查音频文件或网络连接。"
    const val AUTH_FAILED = "WebDAV 认证失败，请检查账号或重新添加音源。"

    /** 白名单内原样、否则兜底（对齐 `PlaybackErrorCopy.safeCopy`）。 */
    fun safeCopy(message: String?): String =
        if (message != null && message in SAFE_PLAYBACK_ERRORS) message else DEFAULT_ERROR
}

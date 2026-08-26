package com.muses.player.core.media.playback

import androidx.media3.common.PlaybackException

/**
 * 安全错误文案映射（规格书 = src/features/player/controller.ts SAFE_PLAYBACK_ERRORS +
 * setUserSafeError 语义）：
 *
 * - 已知错误类别 → 固定人话文案（白名单），不泄露内部堆栈/路径
 * - 未知错误统一兜底「播放失败，请稍后重试。」
 */
object PlaybackErrorCopy {

    /** Web SAFE_PLAYBACK_ERRORS 白名单原文（语义对应的 ExoPlayer 错误码见 mapFor） */
    val SAFE_PLAYBACK_ERRORS = listOf(
        "找不到这首歌对应的 WebDAV 音源，请重新扫描音源。",
        "WebDAV 密码不存在，请重新添加该音源。",
        "WebDAV 播放缺少认证信息。",
        "本地音频文件不可访问，请重新扫描或重新授权。",
        "本地音频文件无访问权限，请重新授权音源目录。",
        "WebDAV 认证失败，请检查账号或重新添加音源。",
        "音频文件不存在或已失效，请重新扫描音源。",
        "播放失败，请检查音频文件或网络连接。",
    )

    const val DEFAULT_ERROR = "播放失败，请稍后重试。"

    /** 服务级限流/网关故障：跳歌只会继续撞墙，直接停止等用户手动重试 */
    const val RATE_LIMITED_ERROR = "服务器请求过于频繁，请稍后再试。"

    /**
     * PlaybackException errorCode → 白名单文案。
     * 映射关系（对齐 Web 原生插件的错误分类习惯）：
     * - 文件缺失/读取失败类 → 「音频文件不存在或已失效…」
     * - 权限/不可访问类 → 本地文件两条文案
     * - 网络连接类 → 「播放失败，请检查音频文件或网络连接。」
     * - 认证失败类 → 「WebDAV 认证失败…」
     */
    fun copyFor(error: PlaybackException): String = copyFor(error.errorCode)

    /** 纯错误码版本：便于 JVM 单测（构造 PlaybackException 需 SystemClock） */
    fun copyFor(code: Int): String {
        return when (code) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "音频文件不存在或已失效，请重新扫描音源。"
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
            -> "本地音频文件不可访问，请重新扫描或重新授权。"
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            -> "播放失败，请检查音频文件或网络连接。"
            PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            -> "WebDAV 认证失败，请检查账号或重新添加音源。"
            else -> DEFAULT_ERROR
        }
    }

    /** 非异常类失败（如解析失败字符串）的通用安全化：白名单内原样、否则兜底 */
    fun safeCopy(message: String?): String =
        if (message != null && message in SAFE_PLAYBACK_ERRORS) message else DEFAULT_ERROR

    /**
     * 从异常链提取 HTTP 状态码（HttpDataSource.InvalidResponseCodeException.responseCode）。
     * 用于区分「单曲问题（4xx 跳歌恢复）」与「服务整体拒绝（429/5xx 停止重试）」。
     */
    fun httpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }
}

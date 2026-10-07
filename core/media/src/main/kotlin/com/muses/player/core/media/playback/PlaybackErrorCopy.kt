package com.muses.player.core.media.playback

import androidx.media3.common.PlaybackException
import com.muses.player.core.model.SourceType

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
        "触发限流，稍后重试",
        "播放服务器暂时不可用，请稍后重试。",
        "在线音源拒绝播放请求，请重试或更换音源脚本。",
        "在线音源播放链接已失效，请重试或更换音源脚本。",
        "播放服务器拒绝访问，请检查音源或稍后重试。",
        "音频文件不存在或播放链接已失效，请重试。",
        "播放请求失败，请检查音源或稍后重试。",
    )

    const val DEFAULT_ERROR = "播放失败，请稍后重试。"

    /** 服务级限流/网关故障：跳歌只会继续撞墙，直接停止等用户手动重试 */
    const val RATE_LIMITED_ERROR = "服务器请求过于频繁，请稍后再试。"

    /** 08-27-webdav-playback-429：限流可自愈文案，新版提示与白名单一致 */
    const val RATE_LIMITED_RETRY = "触发限流，稍后重试"

    // 注：在线音源（洛雪自定义源脚本）的失败文案已下沉到 OnlineResolvingDataSourceFactory
    // 与搜索页提示（「还没有能解析 XX 的音源脚本」），此处不再保留未使用的常量。

    /**
     * PlaybackException errorCode → 白名单文案。
     * 映射关系（对齐 Web 原生插件的错误分类习惯）：
     * - 文件缺失/读取失败类 → 「音频文件不存在或已失效…」
     * - 权限/不可访问类 → 本地文件两条文案
     * - 网络连接类 → 「播放失败，请检查音频文件或网络连接。」
     * - HTTP 失败按实际音源和状态码分类，只有 WebDAV 的认证错误才提示检查账号。
     */
    fun copyFor(error: PlaybackException): String =
        copyFor(error.errorCode, httpCode = httpResponseCode(error))

    /** 纯错误码版本：便于 JVM 单测（构造 PlaybackException 需 SystemClock） */
    fun copyFor(code: Int, sourceType: SourceType? = null, httpCode: Int? = null): String {
        if (code == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
            code == PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED
        ) {
            return when {
                httpCode == 429 -> RATE_LIMITED_RETRY
                httpCode in 500..599 -> "播放服务器暂时不可用，请稍后重试。"
                sourceType == SourceType.WEBDAV &&
                    (httpCode == 401 || httpCode == 403 ||
                        code == PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED) ->
                    "WebDAV 认证失败，请检查账号或重新添加音源。"
                sourceType == SourceType.ONLINE && (httpCode == 401 || httpCode == 403) ->
                    "在线音源拒绝播放请求，请重试或更换音源脚本。"
                sourceType == SourceType.ONLINE && (httpCode == 404 || httpCode == 410) ->
                    "在线音源播放链接已失效，请重试或更换音源脚本。"
                httpCode == 401 || httpCode == 403 ->
                    "播放服务器拒绝访问，请检查音源或稍后重试。"
                httpCode == 404 || httpCode == 410 ->
                    "音频文件不存在或播放链接已失效，请重试。"
                else -> "播放请求失败，请检查音源或稍后重试。"
            }
        }
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
        return httpResponseError(error)?.responseCode
    }

    /** 只记录域名，不输出可能含签名或凭据的完整播放地址。 */
    fun httpRequestHost(error: PlaybackException): String? =
        httpResponseError(error)?.dataSpec?.uri?.host

    private fun httpResponseError(error: PlaybackException):
        androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                return cause
            }
            cause = cause.cause
        }
        return null
    }
}

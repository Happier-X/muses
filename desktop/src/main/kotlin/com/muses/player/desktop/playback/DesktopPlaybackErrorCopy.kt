package com.muses.player.desktop.playback

/**
 * S2 桌面播放安全文案（复刻 `PlaybackErrorCopy.SAFE_PLAYBACK_ERRORS` 白名单，不依赖 Media3）。
 *
 * commonMain 不收桌面专属 API，`core:media` 为安卓模块桌面不可依赖，
 * 故白名单逐字复刻安卓侧 9 条，语义冻结；**在此之上另有桌面专属 3 条**（安卓侧无对应文案）：
 * `VLC_MISSING`（桌面播放引擎）、`ONLINE_RESOLVER_MISSING` / `ONLINE_RESOLVE_FAILED`（在线音源）。
 *
 * 桌面侧在线音源细化到「未接入解析器」与「解析失败」两种病因；安卓侧同类失败回落通用文案
 * （`PlaybackErrorCopy` 已注明不保留在线常量），故两边白名单不再等长——改动时勿按条数硬对齐。
 *
 * ⚠️ 经 [safeCopy] 落地的文案**必须**在本白名单内，否则会被静默兜底成「播放失败，请稍后重试。」
 * （新增文案时同步更新 `DesktopPlaybackErrorCopyTest` 的断言）。
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
        VLC_MISSING,
        ONLINE_RESOLVER_MISSING,
        ONLINE_RESOLVE_FAILED,
    )

    const val DEFAULT_ERROR = "播放失败，请稍后重试。"

    /** 服务级限流/网关故障：跳歌只会继续撞墙，直接停止等用户手动重试。 */
    const val RATE_LIMITED_ERROR = "服务器请求过于频繁，请稍后再试。"

    const val RATE_LIMITED_RETRY = "触发限流，稍后重试"

    const val FILE_NOT_FOUND = "音频文件不存在或已失效，请重新扫描音源。"
    const val FILE_NO_PERMISSION = "本地音频文件不可访问，请重新扫描或重新授权。"
    const val NETWORK = "播放失败，请检查音频文件或网络连接。"
    const val AUTH_FAILED = "WebDAV 认证失败，请检查账号或重新添加音源。"

    /**
     * VLC 原生库缺失（发行版内置引擎缺失 + 未装 VLC 桌面版 + 未配置 MUSES_VLC_DIR）：三步指引，
     * 从最可能可行的一条（重装带内置引擎的安装包）到进阶配置。
     */
    const val VLC_MISSING =
        "未找到 VLC 播放引擎：请重新安装应用（安装包内含内置引擎），或安装 VLC 桌面版，" +
            "也可用 MUSES_VLC_DIR 指向含 libvlc.dll 的目录。"

    // ── 在线音源（洛雪自定义源脚本）──

    /**
     * 在线音源未启用（DI 未装配 `OnlineTrackResolver`，如构建裁剪或模块缺失）。
     * 注意：「装了应用但没导入任何音源脚本」不归此条——那种情况解析器存在、resolve 失败，
     * 走 [ONLINE_RESOLVE_FAILED]（引用户去检查/导入脚本）。
     * 当前 `DesktopContainer` 总是装配解析器，故本条属防御分支（装配缺失告警）。
     * 经 [safeCopy] 流向 UI，故必须在 [SAFE_PLAYBACK_ERRORS] 内（已入）。
     */
    const val ONLINE_RESOLVER_MISSING = "未启用在线音源，无法播放该曲目。"

    /**
     * 在线曲目直链解析失败（脚本未导入/源不支持/直链过期/曲目引用损坏）。
     * 经 [safeCopy] 流向 UI，故必须在 [SAFE_PLAYBACK_ERRORS] 内（已入）。
     */
    const val ONLINE_RESOLVE_FAILED = "在线音源解析失败，请检查音源脚本是否可用。"

    /** 白名单内原样、否则兜底（对齐 `PlaybackErrorCopy.safeCopy`）。 */
    fun safeCopy(message: String?): String =
        if (message != null && message in SAFE_PLAYBACK_ERRORS) message else DEFAULT_ERROR
}

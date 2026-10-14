package com.muses.player.core.model.online

/** 小范围请求验证音频地址，拒绝解析成功但服务器拒绝访问的直链。 */
fun interface OnlinePlayableUrlProbe {
    suspend fun canOpen(url: String): Boolean

    /** 已知原曲时长时，同时排除可确认的试听或截断片段。 */
    suspend fun canOpen(url: String, expectedDurationMs: Long?): Boolean = canOpen(url)
}

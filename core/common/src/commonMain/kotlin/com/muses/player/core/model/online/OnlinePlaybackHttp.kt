package com.muses.player.core.model.online

/** 在线音频的探测和实际播放使用同一请求标识，避免探测可用、播放却被 CDN 拒绝。 */
object OnlinePlaybackHttp {
    const val USER_AGENT = "LX-Music-Mobile"

    fun requestHeaders(existing: Map<String, String>): Map<String, String> =
        if (existing.keys.any { it.equals("User-Agent", ignoreCase = true) }) existing
        else existing + ("User-Agent" to USER_AGENT)
}

package com.muses.player.core.model.online

/** 小范围请求验证音频地址，拒绝解析成功但服务器拒绝访问的直链。 */
fun interface OnlinePlayableUrlProbe {
    suspend fun canOpen(url: String): Boolean
}

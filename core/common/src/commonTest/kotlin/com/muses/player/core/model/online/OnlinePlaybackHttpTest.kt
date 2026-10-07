package com.muses.player.core.model.online

import kotlin.test.Test
import kotlin.test.assertEquals

class OnlinePlaybackHttpTest {
    @Test
    fun `默认标识与探测一致且保留其他请求头`() {
        assertEquals(mapOf("Range" to "bytes=0-", "User-Agent" to OnlinePlaybackHttp.USER_AGENT),
            OnlinePlaybackHttp.requestHeaders(mapOf("Range" to "bytes=0-")))
    }

    @Test
    fun `尊重已有请求标识且忽略大小写`() {
        val existing = mapOf("user-agent" to "custom", "Referer" to "https://example.com/")
        assertEquals(existing, OnlinePlaybackHttp.requestHeaders(existing))
    }
}

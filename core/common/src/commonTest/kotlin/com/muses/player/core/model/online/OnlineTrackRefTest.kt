package com.muses.player.core.model.online

import com.muses.player.core.model.SourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [OnlineTrackRef] 编解码测试：确保 JSON 与音源 id 能无损往返 */
class OnlineTrackRefTest {

    @Test
    fun `基础往返编码解码`() {
        val ref = OnlineTrackRef(
            platform = "kw",
            musicInfoJson = """{"songmid":"MUSIC_12345","name":"测试歌曲"}""",
            sourceId = "src-1",
            quality = "320k",
        )
        val encoded = ref.encode()
        assertTrue(OnlineTrackRef.isOnlineUri(encoded))

        val decoded = OnlineTrackRef.parse(encoded)
        assertEquals(ref, decoded)
    }

    @Test
    fun `含特殊字符的 JSON 无损往返`() {
        // JSON 里的 / ? # & % 等字符不得破坏 URI 结构
        val trickyJson = """{"url":"http://a.com/b?c=d&e=f#g","pct":"100%","q":"a/b"}"""
        val ref = OnlineTrackRef(
            platform = "tx",
            musicInfoJson = trickyJson,
            sourceId = "src/with:odd chars",
            quality = "flac",
        )
        val decoded = OnlineTrackRef.parse(ref.encode())
        assertEquals(trickyJson, decoded?.musicInfoJson)
        assertEquals("src/with:odd chars", decoded?.sourceId)
        assertEquals("flac", decoded?.quality)
    }

    @Test
    fun `中文与 emoji 无损往返`() {
        val ref = OnlineTrackRef(
            platform = "wy",
            musicInfoJson = """{"name":"周杰伦 - 晴天🎵","artist":"测试"}""",
            sourceId = "源一",
        )
        val decoded = OnlineTrackRef.parse(ref.encode())
        assertEquals(ref.musicInfoJson, decoded?.musicInfoJson)
        assertEquals("源一", decoded?.sourceId)
        assertNull(decoded?.quality)
    }

    @Test
    fun `quality 可省略`() {
        val ref = OnlineTrackRef(platform = "mg", musicInfoJson = "{}", sourceId = "s")
        val decoded = OnlineTrackRef.parse(ref.encode())
        assertNull(decoded?.quality)
        assertEquals("mg", decoded?.platform)
    }

    @Test
    fun `非在线 URI 返回 null`() {
        assertNull(OnlineTrackRef.parse("/storage/emulated/0/Music/a.mp3"))
        assertNull(OnlineTrackRef.parse("https://dav.example.com/a.flac"))
        assertNull(OnlineTrackRef.parse(null))
        assertNull(OnlineTrackRef.parse(""))
        assertFalse(OnlineTrackRef.isOnlineUri("file:///x.mp3"))
    }

    @Test
    fun `损坏的 URI 返回 null 而不抛错`() {
        assertNull(OnlineTrackRef.parse("muslx://"))
        assertNull(OnlineTrackRef.parse("muslx://kw"))
        assertNull(OnlineTrackRef.parse("muslx://kw/"))
        // payload 非法 base64：不应崩
        val result = OnlineTrackRef.parse("muslx://kw/!!!invalid!!!")
        // 宽松实现下可能返回空 JSON 或 null，两者都可接受，关键是不得抛异常
        assertTrue(result == null || result.platform == "kw")
    }

    @Test
    fun `超长 payload 往返正确`() {
        val bigJson = """{"data":"""" + "x".repeat(4000) + """"}"""
        val ref = OnlineTrackRef(platform = "kw", musicInfoJson = bigJson, sourceId = "s1")
        assertEquals(bigJson, OnlineTrackRef.parse(ref.encode())?.musicInfoJson)
    }

    @Test
    fun `编码结果不含会破坏 URI 的字符`() {
        val ref = OnlineTrackRef(
            platform = "kw",
            musicInfoJson = """{"a":"/?#&% =+"}""",
            sourceId = "id with spaces",
            quality = "128k",
        )
        val encoded = ref.encode()
        // payload 段（platform 之后、query 之前）不得含 ? # 空格
        val payload = encoded.removePrefix("muslx://").substringAfter('/').substringBefore('?')
        assertFalse(payload.contains('?'))
        assertFalse(payload.contains('#'))
        assertFalse(payload.contains(' '))
        assertFalse(payload.contains('+'))
        assertFalse(payload.contains('/'))
    }
}

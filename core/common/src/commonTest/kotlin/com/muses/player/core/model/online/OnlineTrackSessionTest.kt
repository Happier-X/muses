package com.muses.player.core.model.online

import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [OnlineTrackSession] 测试：只登记在线曲目 + 回退查询语义 */
class OnlineTrackSessionTest {

    private fun song(id: String, type: SourceType) = Song(
        id = id,
        sourceId = "s",
        path = if (type == SourceType.ONLINE) "muslx://kw/abc" else "/local/$id.mp3",
        title = "标题$id",
        sourceType = type,
    )

    @Test
    fun `只登记在线曲目`() = runTest {
        OnlineTrackSession.clear()
        OnlineTrackSession.remember(
            listOf(
                song("online:kw:1", SourceType.ONLINE),
                song("local:1", SourceType.LOCAL),
                song("dav:1", SourceType.WEBDAV),
            ),
        )
        assertEquals(1, OnlineTrackSession.size())
        assertTrue(OnlineTrackSession.find("online:kw:1") != null)
        assertNull(OnlineTrackSession.find("local:1"))
        assertNull(OnlineTrackSession.find("dav:1"))
        OnlineTrackSession.clear()
    }

    @Test
    fun `未登记返回 null`() = runTest {
        OnlineTrackSession.clear()
        assertNull(OnlineTrackSession.find("不存在"))
    }

    @Test
    fun `批量查询只返回已登记的`() = runTest {
        OnlineTrackSession.clear()
        OnlineTrackSession.remember(listOf(song("a", SourceType.ONLINE)))
        val found = OnlineTrackSession.findAll(listOf("a", "b"))
        assertEquals(setOf("a"), found.keys)
        OnlineTrackSession.clear()
    }

    @Test
    fun `重复登记同一 id 覆盖旧值`() = runTest {
        OnlineTrackSession.clear()
        OnlineTrackSession.remember(listOf(song("dup", SourceType.ONLINE)))
        OnlineTrackSession.remember(
            listOf(song("dup", SourceType.ONLINE).copy(title = "新标题")),
        )
        assertEquals(1, OnlineTrackSession.size())
        assertEquals("新标题", OnlineTrackSession.find("dup")?.title)
        OnlineTrackSession.clear()
    }

    @Test
    fun `clear 清空全部`() = runTest {
        OnlineTrackSession.remember(listOf(song("x", SourceType.ONLINE)))
        OnlineTrackSession.clear()
        assertEquals(0, OnlineTrackSession.size())
    }

    @Test
    fun `observe 立即发出已登记条目`() = runTest {
        OnlineTrackSession.clear()
        OnlineTrackSession.remember(listOf(song("online:kw:9", SourceType.ONLINE)))
        assertEquals("标题online:kw:9", OnlineTrackSession.observe("online:kw:9").first()?.title)
        OnlineTrackSession.clear()
    }

    @Test
    fun `先观察后登记也会收到补发`() = runTest {
        // 消除「播放页先观察当前曲、播放链路后登记」的竞态（StateFlow 推送语义）
        OnlineTrackSession.clear()
        val flow = OnlineTrackSession.observe("late")
        assertNull(flow.first())
        OnlineTrackSession.remember(listOf(song("late", SourceType.ONLINE)))
        assertEquals("标题late", flow.first()?.title)
        OnlineTrackSession.clear()
    }
}

package com.muses.player.core.search

import com.muses.player.core.search.http.SearchHttp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlinePlaylistServiceTest {
    private fun service(body: String, path: String = "/api/playlist/highquality/list"): OnlinePlaylistService = OnlinePlaylistService(
        SearchHttp(HttpClient(MockEngine {
            assertEquals(path, it.url.encodedPath)
            respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })),
    )

    @Test fun `推荐目录使用真实编号封面与歌曲数量且保留平台顺序`() = runTest {
        val items = service("""{"code":200,"result":[
            {"id":2,"name":"民谣","picUrl":"http://cdn.test/folk.jpg","trackCount":164},
            {"id":1,"name":"流行","picUrl":"https://cdn.test/pop.jpg","trackCount":20},
            {"id":2,"name":"重复","picUrl":"https://cdn.test/other.jpg"},
            {"id":3,"name":"缺封面"}
        ]}""", "/api/personalized/playlist").recommended()
        assertEquals(listOf("2", "1"), items.map { it.id })
        assertEquals("https://cdn.test/folk.jpg", items.first().coverUrl)
        assertEquals(164, items.first().trackCount)
        assertEquals("民谣", items.first().title)
    }

    @Test fun `推荐接口失败或结构异常不当成成功空列表`() = runTest {
        for (body in listOf("""{"code":503,"result":[]}""", """{"code":200}""")) {
            assertFailsWith<IllegalStateException> {
                service(body, "/api/personalized/playlist").recommended()
            }
        }
    }

    @Test
    fun `标题封面与歌单编号保持对应且过滤无效条目`() = runTest {
        val items = service("""{"code":200,"playlists":[
            {"id":6666112560,"name":"可爱摇滚","coverImgUrl":"http://p2.music.126.net/cover.jpg",
             "creator":{"nickname":"音乐收藏家"},"trackCount":30},
            {"id":6666112560,"name":"重复","coverImgUrl":"https://example.com/other.jpg"},
            {"id":2,"name":"没有封面"},
            {"id":3,"name":"","coverImgUrl":"https://example.com/empty.jpg"},
            {"id":4,"name":"轻音乐","coverImgUrl":"https://example.com/soft.jpg"}
        ]}""").featured()
        assertEquals(2, items.size)
        assertEquals("6666112560", items[0].id)
        assertEquals("可爱摇滚", items[0].title)
        assertEquals("https://p2.music.126.net/cover.jpg", items[0].coverUrl)
        assertEquals("音乐收藏家", items[0].creator)
        assertEquals(30, items[0].trackCount)
        assertEquals("4", items[1].id)
        assertEquals("https://example.com/soft.jpg", items[1].coverUrl)
    }

    @Test
    fun `接口错误不能被当成空歌单`() = runTest {
        assertFailsWith<IllegalStateException> {
            service("""{"code":503,"playlists":[]}""").featured()
        }
        assertFailsWith<IllegalStateException> {
            service("""{"code":200}""").featured()
        }
    }
}

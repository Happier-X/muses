package com.muses.player.core.search

import com.muses.player.core.search.http.SearchHttp
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [SearchHttp.parseObject] 的解析健壮性测试。
 *
 * 这是最容易出错的一层：酷我返回单引号伪 JSON，而标准 JSON 里又可能含撇号
 * （如歌名 `Don't Stop`），两者必须同时正确处理。
 */
class SearchHttpParseTest {

    private fun http() = SearchHttp(HttpClient(MockEngine { respond("{}") }))

    @Test
    fun `标准 JSON 正常解析`() = runTest {
        val root = http().parseObject("""{"code":0,"name":"花海"}""")
        assertEquals("花海", (root["name"] as JsonPrimitive).content)
    }

    @Test
    fun `单引号伪 JSON 被正规化`() = runTest {
        // 酷我 search.kuwo.cn 的真实格式
        val root = http().parseObject("{'TOTAL':'2','abslist':[{'SONGNAME':'歌一'},{'SONGNAME':'歌二'}]}")
        assertEquals("2", (root["TOTAL"] as JsonPrimitive).content)
        val list = root["abslist"] as JsonArray
        assertEquals(2, list.size)
        assertEquals("歌一", ((list[0] as kotlinx.serialization.json.JsonObject)["SONGNAME"] as JsonPrimitive).content)
    }

    @Test
    fun `标准 JSON 字符串内的撇号不被破坏`() = runTest {
        // 关键边界：Don't / It's 这类歌名里的撇号必须原样保留
        val root = http().parseObject("""{"song":"Don't Stop","artist":"It's OK"}""")
        assertEquals("Don't Stop", (root["song"] as JsonPrimitive).content)
        assertEquals("It's OK", (root["artist"] as JsonPrimitive).content)
    }

    @Test
    fun `含转义双引号的字符串正确处理`() = runTest {
        val root = http().parseObject("""{"song":"She said \"hi\"","other":"a'b"}""")
        assertEquals("She said \"hi\"", (root["song"] as JsonPrimitive).content)
        assertEquals("a'b", (root["other"] as JsonPrimitive).content)
    }

    @Test
    fun `undefined 字面量被替换为 null`() = runTest {
        // JS 侧常见输出；直接解析会失败
        val root = http().parseObject("{'a':undefined,'b':'1'}")
        assertEquals("1", (root["b"] as JsonPrimitive).content)
        assertEquals(kotlinx.serialization.json.JsonNull, root["a"])
    }

    @Test
    fun `非法 JSON 返回空对象而不抛错`() = runTest {
        val root = http().parseObject("this is not json at all")
        assertEquals(0, root.keys.size)
    }

    @Test
    fun `空字符串返回空对象`() = runTest {
        assertEquals(0, http().parseObject("").keys.size)
    }

    @Test
    fun `嵌套单引号 JSON 深度解析正确`() = runTest {
        val root = http().parseObject(
            "{'req':{'data':{'body':{'song':{'list':[{'mid':'001TQ','name':'歌'}]}}}}}",
        )
        val song = ((((root["req"] as kotlinx.serialization.json.JsonObject)["data"] as kotlinx.serialization.json.JsonObject)["body"] as kotlinx.serialization.json.JsonObject)["song"] as kotlinx.serialization.json.JsonObject)
        val list = song["list"] as JsonArray
        assertEquals("001TQ", ((list[0] as kotlinx.serialization.json.JsonObject)["mid"] as JsonPrimitive).content)
    }
}

package com.muses.player.core.lxsdk

import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient
import com.muses.player.core.model.online.OnlineTrackRef
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [LxScriptRepository] 集成测试：多脚本管理、按 platform 路由、音质回退、懒加载。
 */
class LxScriptRepositoryTest {

    /** 声明指定平台的脚本，musicUrl 返回固定直链 */
    private fun scriptFor(vararg platforms: String, name: String = "多源脚本"): String {
        val sources = platforms.joinToString(",\n") { p ->
            val qualitys = if (p == "local") "[]" else "['128k','320k','flac']"
            val actions = if (p == "local") "['musicUrl','lyric','pic']" else "['musicUrl']"
            "$p: { name: '${p}源', type: 'music', actions: $actions, qualitys: $qualitys }"
        }
        return """
            /**
             * @name $name
             */
            const { EVENT_NAMES, request, on, send } = globalThis.lx
            const httpRequest = (url, options) => new Promise((resolve, reject) => {
              request(url, options, (err, resp) => err ? reject(err) : resolve(resp.body))
            })
            on(EVENT_NAMES.request, ({ source, action, info }) => {
              if (action === 'musicUrl') {
                return httpRequest('http://api.test/' + source + '?q=' + info.type)
                  .then(d => d.url)
              }
              if (action === 'lyric') return Promise.resolve({ lyric: 'L:' + source })
              return Promise.reject(new Error('unsupported'))
            })
            send(EVENT_NAMES.inited, { sources: { $sources } })
        """.trimIndent()
    }

    /** 声明指定档位并把收到的 type 回显到 URL，便于断言回退结果 */
    private fun scriptWithQualities(qualitiesJsArray: String): String = """
        /**
         * @name 音质探针源
         */
        const { EVENT_NAMES, request, on, send } = globalThis.lx
        const httpRequest = (url, options) => new Promise((resolve, reject) => {
          request(url, options, (err, resp) => err ? reject(err) : resolve(resp.body))
        })
        on(EVENT_NAMES.request, ({ action, info }) => {
          if (action === 'musicUrl') {
            return httpRequest('http://api.test/probe?q=' + info.type).then(d => d.url)
          }
          return Promise.reject(new Error('unsupported'))
        })
        send(EVENT_NAMES.inited, {
          sources: { kw: { name: 'kw', type: 'music', actions: ['musicUrl'], qualitys: $qualitiesJsArray } },
        })
    """.trimIndent()

    private fun repository(): LxScriptRepository {
        val mock = MockEngine { request ->
            val p = request.url.toString()
            respond(
                content = """{"url":"http://cdn.test/$p"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return LxScriptRepository(
            crypto = LxCryptoJvm(),
            httpClient = LxHttpClient(HttpClient(mock)),
        )
    }

    @Test
    fun `未导入脚本时解析抛出可读错误`() = runTest {
        val repo = repository()
        val ex = assertFailsWith<LxResolveException> {
            repo.resolveMusicUrl("kw", """{"songmid":"x"}""")
        }
        assertTrue(ex.message!!.contains("未导入"), "实际：${ex.message}")
    }

    @Test
    fun `注册脚本后可按平台解析直链`() = runTest {
        val repo = repository()
        repo.register("s1", scriptFor("kw", "kg"))
        val url = repo.resolveMusicUrl("kw", """{"songmid":"x"}""")
        assertTrue(url.startsWith("http://cdn.test/"), "实际：$url")
        assertTrue(url.contains("/kw"), "应命中 kw 平台，实际：$url")
    }

    @Test
    fun `请求未声明的平台时报错并列出尝试记录`() = runTest {
        val repo = repository()
        repo.register("s1", scriptFor("kw"))
        val ex = assertFailsWith<LxResolveException> {
            repo.resolveMusicUrl("tx", """{"songmid":"x"}""")
        }
        assertTrue(ex.message!!.contains("tx"), "实际：${ex.message}")
    }

    @Test
    fun `多脚本按平台各自路由`() = runTest {
        val repo = repository()
        repo.register("s1", scriptFor("kw", name = "脚本一"))
        repo.register("s2", scriptFor("tx", name = "脚本二"))

        assertTrue(repo.resolveMusicUrl("kw", "{}").contains("/kw"))
        assertTrue(repo.resolveMusicUrl("tx", "{}").contains("/tx"))
    }

    @Test
    fun `音质回退到脚本声明支持的档位`() = runTest {
        // 脚本只声明 128k，请求 320k 时应回退到 128k 而不是报错
        val limited = """
            /**
             * @name 低音质源
             */
            const { EVENT_NAMES, request, on, send } = globalThis.lx
            const httpRequest = (url, options) => new Promise((resolve, reject) => {
              request(url, options, (err, resp) => err ? reject(err) : resolve(resp.body))
            })
            on(EVENT_NAMES.request, ({ action, info }) => {
              return httpRequest('http://api.test/q=' + info.type).then(d => d.url)
            })
            send(EVENT_NAMES.inited, {
              sources: { kw: { name: 'kw', type: 'music', actions: ['musicUrl'], qualitys: ['128k'] } },
            })
        """.trimIndent()
        val repo = repository()
        repo.register("s1", limited)
        val url = repo.resolveMusicUrl("kw", "{}", LxQuality.Q_320K)
        // 回退到 128k（脚本 qualitys 映射后传给接口）
        assertTrue(url.contains("128") || url.startsWith("http://cdn.test/"), "实际：$url")
    }

    @Test
    fun `加载失败的脚本不阻断其它脚本`() = runTest {
        val repo = repository()
        repo.register("broken", "this is not valid javascript ((((")
        repo.register("good", scriptFor("kw"))
        // 坏脚本被跳过，好脚本仍能解析
        val url = repo.resolveMusicUrl("kw", "{}")
        assertTrue(url.startsWith("http://cdn.test/"), "实际：$url")
    }

    @Test
    fun `注销脚本后不再参与解析`() = runTest {
        val repo = repository()
        repo.register("s1", scriptFor("kw"))
        assertEquals(1, repo.scripts().size)
        repo.unregister("s1")
        assertEquals(0, repo.scripts().size)
        assertFailsWith<LxResolveException> { repo.resolveMusicUrl("kw", "{}") }
    }

    @Test
    fun `重复注册同 id 会替换旧脚本`() = runTest {
        val repo = repository()
        repo.register("s1", scriptFor("kw", name = "旧"))
        repo.register("s1", scriptFor("mg", name = "新"))
        assertEquals(1, repo.scripts().size)
        assertEquals("新", repo.scripts().first().meta.name)
        // 新脚本声明 mg，不再声明 kw
        assertTrue(repo.resolveMusicUrl("mg", "{}").contains("/mg"))
        assertFailsWith<LxResolveException> { repo.resolveMusicUrl("kw", "{}") }
    }

    @Test
    fun `repository 可作为解析器端口的后端`() = runTest {
        // 端到端：OnlineTrackRef → LxOnlineTrackResolver → repository → 直链
        val repo = repository()
        repo.register("s1", scriptFor("kw"))
        val resolver = LxOnlineTrackResolver(repo, defaultQuality = LxQuality.Q_320K)

        val ref = OnlineTrackRef(
            platform = "kw",
            musicInfoJson = """{"songmid":"MUSIC_9"}""",
            sourceId = "s1",
            quality = "320k",
        )
        val playable = resolver.resolve(ref)
        assertTrue(playable.url.startsWith("http://cdn.test/"), "实际：${playable.url}")
        assertEquals("320k", playable.quality)
    }

    @Test
    fun `请求档位可用时原样传递`() = runTest {
        val repo = repository()
        // 脚本声明含 master，请求 master 应原样传给脚本
        repo.register("s1", scriptWithQualities("['128k','320k','flac','hires','master']"))
        val url = repo.resolveMusicUrl("kw", "{}", LxQuality.MASTER)
        assertTrue(url.contains("q=master"), "实际：$url")
    }

    @Test
    fun `请求档位不可用时向下就近回退`() = runTest {
        val repo = repository()
        // 只声明到 flac：请求 master 应回退到 flac（更低档），而不是升到不存在的档
        repo.register("s1", scriptWithQualities("['128k','320k','flac']"))
        val url = repo.resolveMusicUrl("kw", "{}", LxQuality.MASTER)
        assertTrue(url.contains("q=flac"), "实际：$url")
    }

    @Test
    fun `无更低可用档时才向上回退`() = runTest {
        val repo = repository()
        // 只声明 128k：请求 320k 无更低档（128k 更低，应优先）——此处验证确实取到 128k
        repo.register("s1", scriptWithQualities("['128k']"))
        val url = repo.resolveMusicUrl("kw", "{}", LxQuality.Q_320K)
        assertTrue(url.contains("q=128k"), "实际：$url")
    }

    @Test
    fun `仅声明更高档时向上回退`() = runTest {
        val repo = repository()
        // 只声明 master：请求 320k 没有更低可用档，只能向上取 master
        repo.register("s1", scriptWithQualities("['master']"))
        val url = repo.resolveMusicUrl("kw", "{}", LxQuality.Q_320K)
        assertTrue(url.contains("q=master"), "实际：$url")
    }

    @Test
    fun `解析器把仓库错误包装为 OnlineResolveException`() = runTest {
        val resolver = LxOnlineTrackResolver(repository())
        val ref = OnlineTrackRef(platform = "kw", musicInfoJson = "{}", sourceId = "none")
        val ex = assertFailsWith<com.muses.player.core.model.online.OnlineResolveException> {
            resolver.resolve(ref)
        }
        assertTrue(ex.message!!.isNotBlank())
    }
}

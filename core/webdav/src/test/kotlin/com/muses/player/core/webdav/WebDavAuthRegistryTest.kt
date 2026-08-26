package com.muses.player.core.webdav

import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * [WebDavAuthRegistry] 单测：fake 仓库注入，覆盖最长前缀匹配 / 无匹配 /
 * refresh 增量生效 / user 为 null 的 Basic 编码。
 */
class WebDavAuthRegistryTest {

    private class FakeSourceRepository : SourceRepository {
        val sources = MutableStateFlow<List<Source>>(emptyList())

        override fun observeSources(): Flow<List<Source>> = sources
        override suspend fun getSource(id: String): Source? = sources.value.find { it.id == id }
        override suspend fun upsert(source: Source) {
            sources.value = sources.value.filterNot { it.id == source.id } + source
        }

        override suspend fun deleteById(id: String) {
            sources.value = sources.value.filterNot { it.id == id }
        }
    }

    private class FakeCredentialsRepository : CredentialsRepository {
        val passwords = mutableMapOf<String, String>()

        override suspend fun savePassword(sourceId: String, password: String) {
            passwords[sourceId] = password
        }

        override suspend fun getPassword(sourceId: String): String? = passwords[sourceId]
        override suspend fun clearPassword(sourceId: String) {
            passwords.remove(sourceId)
        }
    }

    private lateinit var sourceRepository: FakeSourceRepository
    private lateinit var credentialsRepository: FakeCredentialsRepository
    private lateinit var registry: WebDavAuthRegistry

    @Before
    fun setUp() {
        sourceRepository = FakeSourceRepository()
        credentialsRepository = FakeCredentialsRepository()
        registry = WebDavAuthRegistry(sourceRepository, credentialsRepository)
    }

    private fun webdavSource(
        id: String,
        url: String,
        username: String? = "alice",
    ): Source = Source(
        id = id,
        name = id,
        type = SourceType.WEBDAV,
        url = url,
        username = username,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun decodeBasic(header: String): Pair<String, String> {
        val encoded = header.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        val index = decoded.indexOf(':')
        return decoded.substring(0, index) to decoded.substring(index + 1)
    }

    @Test
    fun longest_prefix_match_wins() {
        sourceRepository.sources.value = listOf(
            webdavSource("root", "http://nas.local"),
            webdavSource("sub", "http://nas.local/dav/music"),
        )
        credentialsRepository.passwords["root"] = "pass-root"
        credentialsRepository.passwords["sub"] = "pass-sub"

        // 请求 URL scheme/host 大小写不敏感，应命中更长前缀的 sub 源
        val header = kotlinx.coroutines.runBlocking {
            registry.authorizationHeader("http://NAS.local/dav/music/artist/song.mp3")
        }

        assertEquals("Basic", header?.substringBefore(' '))
        assertEquals("alice" to "pass-sub", decodeBasic(header!!))
    }

    @Test
    fun returns_null_when_no_registered_source_matches_url() {
        sourceRepository.sources.value = listOf(webdavSource("a", "http://other.host/dav"))
        credentialsRepository.passwords["a"] = "pass"

        val header = kotlinx.coroutines.runBlocking {
            registry.authorizationHeader("http://nas.local/music/song.mp3")
        }
        assertNull(header)
    }

    @Test
    fun refresh_picks_up_newly_added_source_without_restart() {
        // 首次查询触发懒加载，此时表为空 → 无匹配
        assertNull(
            kotlinx.coroutines.runBlocking {
                registry.authorizationHeader("http://nas.local/dav/song.mp3")
            },
        )

        sourceRepository.upsertSync(webdavSource("new", "http://nas.local/dav"))
        credentialsRepository.savePasswordSync("new", "pass-new")

        // refresh 后新源立即生效（无需重建 Registry）
        kotlinx.coroutines.runBlocking { registry.refresh() }
        val header = kotlinx.coroutines.runBlocking {
            registry.authorizationHeader("http://nas.local/dav/song.mp3")
        }
        assertEquals("alice" to "pass-new", decodeBasic(header!!))
    }

    @Test
    fun null_username_encodes_empty_user_in_basic_header() {
        sourceRepository.sources.value = listOf(webdavSource("anon", "http://nas.local/", username = null))
        credentialsRepository.passwords["anon"] = "secret"

        val header = kotlinx.coroutines.runBlocking {
            registry.authorizationHeader("http://nas.local/song.mp3")
        }
        assertEquals("" to "secret", decodeBasic(header!!))
    }

    @Test
    fun prefix_match_respects_slash_boundary() {
        sourceRepository.sources.value = listOf(webdavSource("dav", "http://nas.local/dav"))
        credentialsRepository.passwords["dav"] = "pass"

        // /davious 与 /dav 不是同一前缀（'/' 边界对齐），不应误注入凭据
        val header = kotlinx.coroutines.runBlocking {
            registry.authorizationHeader("http://nas.local/davious/song.mp3")
        }
        assertNull(header)
    }

    /** 同步便捷方法：测试内直接操作 fake 状态，避免每处都写 runBlocking */
    private fun FakeSourceRepository.upsertSync(source: Source) {
        sources.value = sources.value.filterNot { it.id == source.id } + source
    }

    private fun FakeCredentialsRepository.savePasswordSync(sourceId: String, password: String) {
        passwords[sourceId] = password
    }
}

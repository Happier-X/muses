package com.muses.player.core.media.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavAudioCache
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WebDavLibraryScanner 单测（fake 注入 WebDavClient / WebDavAudioCache / CredentialsRepository）。
 * 覆盖：扩展名过滤、readTags=false 零下载、读标签失败降级、密码缺失抛错。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebDavLibraryScannerTest {

    private lateinit var context: Context
    private lateinit var client: FakeWebDavClient
    private lateinit var cache: FakeAudioCache
    private lateinit var credentials: FakeCredentialsRepository
    private lateinit var scanner: WebDavLibraryScanner

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        client = FakeWebDavClient()
        cache = FakeAudioCache()
        credentials = FakeCredentialsRepository(password = "secret")
        scanner = WebDavLibraryScanner(context, client, cache, credentials)
    }

    private fun webDavSource() = Source(
        id = "src-1",
        name = "NAS",
        type = SourceType.WEBDAV,
        url = "http://nas.local:5005",
        path = "/music",
        username = "admin",
        createdAt = 0L,
        updatedAt = 0L,
    )

    // ── ① 发现阶段扩展名过滤 ──────────────────────────

    @Test
    fun scan_filters_non_audio_files_and_recurses_directories() = runTest {
        client.dirs["http://nas.local:5005/music"] = listOf(
            file("01 - 开场.mp3"),
            dir("sub"),
            file("cover.jpg"), // 非音频应被过滤
            file("notes.txt"), // 非音频应被过滤
        )
        client.dirs["http://nas.local:5005/music/sub"] = listOf(
            file("track.flac", parent = "/music/sub"),
            file("ignored.exe", parent = "/music/sub"),
        )

        val songs = scanner.scan(webDavSource(), readTags = false)

        assertEquals(listOf("01 - 开场", "track"), songs.map { it.title })
        assertEquals(
            listOf(
                "http://nas.local:5005/music/01%20-%20开场.mp3",
                "http://nas.local:5005/music/sub/track.flac",
            ),
            songs.map { it.path },
        )
        assertTrue(songs.all { it.sourceType == SourceType.WEBDAV })
        assertTrue(songs.all { it.sourceId == "src-1" })
        assertTrue(songs.all { it.tagsVersion == LocalLibraryScanner.TAGS_VERSION })
        // 稳定 ID 与 LocalLibraryScanner.stableSongId 同源
        assertEquals(
            LocalLibraryScanner.stableSongId("src-1", songs[0].path),
            songs[0].id,
        )
        // 进度终态
        val progress = scanner.scanProgress.value
        assertTrue(progress.finished)
        assertEquals(2, progress.total)
    }

    // ── ② readTags=false 零下载 ──────────────────────────

    @Test
    fun scan_readTagsFalse_never_downloads() = runTest {
        client.dirs["http://nas.local:5005/music"] = listOf(file("a.mp3"), dir("sub"))
        client.dirs["http://nas.local:5005/music/sub"] = listOf(file("b.ogg", parent = "/music/sub"))

        val songs = scanner.scan(webDavSource(), readTags = false)

        assertEquals(2, songs.size)
        assertEquals("readTags=false 不应触发任何下载", 0, client.downloadCalls.size)
        assertEquals(0, client.getStringCalls.size)
        assertTrue(cache.putCalls.isEmpty())
        // 零标签快速建库
        assertNull(songs[0].lyrics)
        assertNull(songs[0].coverUri)
        assertEquals(0L, songs[0].durationMs)
    }

    // ── ③ 读标签失败降级为文件名，不中断整体 ──────────────

    @Test
    fun scan_tagReadFailure_degrades_to_filename_and_continues() = runTest {
        client.dirs["http://nas.local:5005/music"] = listOf(file("bad.mp3"), file("good.mp3"))
        client.downloads["http://nas.local:5005/music/bad.mp3"] =
            writeGarbageFile("not-an-audio-file") // jaudiotagger 解析必失败
        client.downloads["http://nas.local:5005/music/good.mp3"] =
            writeGarbageFile("still-not-audio")

        val songs = scanner.scan(webDavSource(), readTags = true)

        // 两首都产出（降级为文件名），扫描未中断
        assertEquals(listOf("bad", "good"), songs.map { it.title })
        assertEquals(2, client.downloadCalls.size)
        // 下载过的文件顺手 putToCache 预热播放 LRU（eTag 从发现阶段带下来）
        assertEquals(setOf("http://nas.local:5005/music/bad.mp3", "http://nas.local:5005/music/good.mp3"), cache.putCalls.keys)
        assertEquals("\"etag-bad\"", cache.putCalls["http://nas.local:5005/music/bad.mp3"])
    }

    /** 缓存命中时不重复下载 */
    @Test
    fun scan_uses_cached_file_without_redownload() = runTest {
        val url = "http://nas.local:5005/music/hit.mp3"
        client.dirs["http://nas.local:5005/music"] = listOf(file("hit.mp3"))
        cache.files[url] = writeGarbageFile("cached-bytes") // 内容非法 → 走降级，但验证零下载

        val songs = scanner.scan(webDavSource(), readTags = true)

        assertEquals(1, songs.size)
        assertEquals(0, client.downloadCalls.size)
    }

    // ── ④ 密码缺失明确报错 ──────────────────────────

    @Test
    fun scan_throws_when_password_missing() = runTest {
        credentials.password = null
        client.dirs["http://nas.local:5005/music"] = emptyList()

        val error = runCatching { scanner.scan(webDavSource(), readTags = false) }
            .exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is IllegalStateException)
        assertEquals(
            "WebDAV 密码不存在，请重新添加该音源。",
            error?.message,
        )
        // 失败后进度置终态，UI 可离开「正在查找文件」态
        assertTrue(scanner.scanProgress.value.finished)
    }

    // ── fakes ──────────────────────────────

    /** 父目录默认根 /music；子目录条目须显式传 parent 保证 URL 正确 */
    private fun file(name: String, parent: String = "/music") =
        WebDavItem(name = name, url = "http://nas.local:5005$parent/$name".replace(" ", "%20"), isDirectory = false, eTag = "\"etag-${name.take(3)}\"")
    private fun dir(name: String, parent: String = "/music") =
        WebDavItem(name = name, url = "http://nas.local:5005$parent/$name", isDirectory = true)

    private fun writeGarbageFile(content: String): File =
        File.createTempFile("scan-test-", ".mp3").apply {
            writeText(content)
            deleteOnExit()
        }

    /** 目录树内存版 WebDAV 客户端；记录下载/sidecar 请求供断言 */
    private class FakeWebDavClient : WebDavClient {
        val dirs = mutableMapOf<String, List<WebDavItem>>()

        /** url → 本地伪音频文件（内容为垃圾字节，保证 TagReader 解析失败走降级路径） */
        val downloads = mutableMapOf<String, File>()
        val downloadCalls = mutableListOf<String>()
        val getStringCalls = mutableListOf<String>()
        val sidecarLyrics = mutableMapOf<String, String>()

        override fun authenticate(username: String, password: String) = Unit
        override suspend fun probe(baseUrl: String): Boolean = true

        override suspend fun list(url: String): List<WebDavItem> =
            dirs[url.trimEnd('/')] ?: throw AssertionError("意外列目录: $url")

        override suspend fun get(url: String, dest: File): File {
            downloadCalls.add(url)
            val source = downloads[url] ?: throw java.io.IOException("模拟下载失败")
            dest.parentFile?.mkdirs()
            source.copyTo(dest, overwrite = true)
            return dest
        }

        override suspend fun put(url: String, source: File) = Unit
        override suspend fun delete(url: String) = Unit
        override suspend fun move(source: String, dest: String) = Unit

        override suspend fun getString(url: String): String? {
            getStringCalls.add(url)
            return sidecarLyrics[url]
        }
    }

    /** 内存版音频缓存（记录 putToCache 的 eTag 透传） */
    private class FakeAudioCache : WebDavAudioCache {
        val files = mutableMapOf<String, File>()
        val putCalls = linkedMapOf<String, String?>()

        override fun getCachedFile(url: String): File? = files[url]
        override fun putToCache(url: String, file: File, eTag: String?, lastModified: String?) {
            putCalls[url] = eTag
            files[url] = file
        }
    }

    private class FakeCredentialsRepository(var password: String?) : CredentialsRepository {
        val saved = mutableMapOf<String, String>()
        override suspend fun savePassword(sourceId: String, password: String) {
            saved[sourceId] = password
        }

        override suspend fun getPassword(sourceId: String): String? = password
        override suspend fun clearPassword(sourceId: String) {
            password = null
        }
    }
}

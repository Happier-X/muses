package com.muses.player.core.media.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavAudioCache
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WebDavLibraryScanner 单测（fake 注入 WebDavClient / CredentialsRepository）。
 * 扫描器为纯发现+文件名建库（标签由播放懒扫描负责），覆盖：
 * 扩展名过滤+递归 / 零下载 / 密码缺失抛错且进度置终态 / 文件名建库 tagsVersion=0。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebDavLibraryScannerTest {

    private lateinit var context: Context
    private lateinit var client: FakeWebDavClient
    private lateinit var credentials: FakeCredentialsRepository
    private lateinit var scanner: WebDavLibraryScanner

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        client = FakeWebDavClient()
        credentials = FakeCredentialsRepository(password = "secret")
        scanner = WebDavLibraryScanner(client, credentials, FakeErrorLogStore())
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

    // ── ① 发现阶段扩展名过滤 + 递归 ──────────────────────

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

        val songs = scanner.scan(webDavSource())

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
        // 文件名建库：tagsVersion=0（待播放懒扫描），无时长无封面
        assertTrue(songs.all { it.tagsVersion == WebDavLibraryScanner.FILENAME_TAGS_VERSION })
        assertTrue(songs.all { it.durationMs == 0L })
        assertTrue(songs.all { it.coverUri == null })
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

    // ── ② 扫描零下载（标签移交播放懒扫描） ──────────────────────

    @Test
    fun scan_never_downloads() = runTest {
        client.dirs["http://nas.local:5005/music"] = listOf(file("a.mp3"), dir("sub"))
        client.dirs["http://nas.local:5005/music/sub"] = listOf(file("b.ogg", parent = "/music/sub"))

        val songs = scanner.scan(webDavSource())

        assertEquals(2, songs.size)
        assertEquals("扫描不应触发任何下载", 0, client.downloadCalls.size)
        assertEquals(0, client.getStringCalls.size)
    }

    // ── ③ sidecar .lrc URL 构造 ──────────────────────────

    @Test
    fun buildSidecarLyricsUrl_replaces_extension() {
        val audio = "http://nas.local:5005/music/%E5%8D%81%E5%B9%B4.mp3"
        assertEquals(
            "http://nas.local:5005/music/%E5%8D%81%E5%B9%B4.lrc",
            scanner.buildSidecarLyricsUrl(audio),
        )
        // 无扩展名文件：追加 .lrc
        assertEquals(
            "http://nas.local:5005/music/track.lrc",
            scanner.buildSidecarLyricsUrl("http://nas.local:5005/music/track"),
        )
    }

    // ── ④ 密码缺失明确报错 ──────────────────────────

    @Test
    fun scan_throws_when_password_missing() = runTest {
        credentials.password = null
        client.dirs["http://nas.local:5005/music"] = emptyList()

        val error = runCatching { scanner.scan(webDavSource()) }
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

    // ── fakes ──────────────────────────────────────────────

    /** 父目录默认根 /music；子目录条目须显式传 parent 保证 URL 正确 */
    private fun file(name: String, parent: String = "/music") =
        WebDavItem(name = name, url = "http://nas.local:5005$parent/$name".replace(" ", "%20"), isDirectory = false, eTag = "\"etag-${name.take(3)}\"")

    private fun dir(name: String, parent: String = "/music") =
        WebDavItem(name = name, url = "http://nas.local:5005$parent/$name", isDirectory = true)

    /** 目录树内存版 WebDAV 客户端；记录下载/sidecar 请求供断言 */
    private class FakeWebDavClient : WebDavClient {
        val dirs = mutableMapOf<String, List<WebDavItem>>()
        val downloadCalls = mutableListOf<String>()
        val getStringCalls = mutableListOf<String>()

        override fun authenticate(username: String, password: String) = Unit
        override suspend fun probe(baseUrl: String): Boolean = true

        override suspend fun list(url: String): List<WebDavItem> =
            dirs[url.trimEnd('/')] ?: throw AssertionError("意外列目录: $url")

        override suspend fun get(url: String, dest: java.io.File): java.io.File {
            downloadCalls.add(url)
            throw AssertionError("扫描器不应下载: $url")
        }

        override suspend fun put(url: String, source: java.io.File) = Unit
        override suspend fun delete(url: String) = Unit
        override suspend fun move(source: String, dest: String) = Unit

        override suspend fun getString(url: String): String? {
            getStringCalls.add(url)
            return null
        }
    }

    /** 内存版错误日志（埋点不影响断言；密码缺失用例可校验 error 留痕） */
    private class FakeErrorLogStore : ErrorLogStore {
        val entries = mutableListOf<String>()
        override fun log(level: ErrorLogStore.Level, tag: String, message: String, throwable: Throwable?) {
            entries.add("$tag/$message")
        }

        override val latestSummary = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        override suspend fun dump(): String? = null
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

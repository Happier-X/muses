package com.muses.player.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.muses.player.core.data.repository.*
import com.muses.player.core.lxsdk.LxScriptMeta
import com.muses.player.core.lxsdk.store.*
import com.muses.player.core.model.*
import com.muses.player.core.model.playback.RecentPlayEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class WebDavSyncTest {
    private class Cloud : java.io.Closeable {
        val documents = linkedMapOf<String, String>()
        val server = MockWebServer()
        var failPut = false
        var onPut: (() -> Unit)? = null
        var puts = 0
        init {
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val name = request.path!!.substringAfterLast('/')
                    return when (request.method) {
                        "MKCOL" -> MockResponse().setResponseCode(405)
                        "PROPFIND" -> MockResponse().setResponseCode(207).setBody(
                            "<d:multistatus xmlns:d=\"DAV:\">" + documents.keys.joinToString("") {
                                "<d:response><d:href>/dav/MusesSync/$it</d:href></d:response>"
                            } + "</d:multistatus>")
                        "GET" -> documents[name]?.let { MockResponse().setBody(it).setHeader("ETag", "\"${it.hashCode()}\"") }
                            ?: MockResponse().setResponseCode(404)
                        "PUT" -> {
                            puts++
                            if (failPut) MockResponse().setResponseCode(500) else {
                                val old = documents[name]
                                if ((request.getHeader("If-None-Match") == "*" && old != null) ||
                                    (request.getHeader("If-Match") != null && request.getHeader("If-Match") != "\"${old?.hashCode()}\""))
                                    MockResponse().setResponseCode(412)
                                else {
                                    documents[name] = request.body.readUtf8()
                                    onPut?.invoke()
                                    MockResponse().setResponseCode(201)
                                }
                            }
                        }
                        else -> MockResponse().setResponseCode(400)
                    }
                }
            }
            server.start()
        }
        override fun close() = server.close()
    }

    private class Device(cloud: Cloud) : java.io.Closeable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val root = Files.createTempDirectory("muses-sync-test").toFile()
        val store = PreferenceDataStoreFactory.create(scope = scope) { File(root, "sync.preferences_pb") }
        val settings = DataStoreSettingsRepository(store)
        val history = RecentPlaysRepository(store)
        val stats = PlayStatsRepository(store)
        val sourceData = mutableMapOf<String, Source>()
        val songData = mutableMapOf<String, Song>()
        val scriptData = mutableMapOf<String, LxStoredScript>()
        val credentialData = mutableMapOf("ai" to "api-secret")
        val credentials = object : CredentialsRepository {
            override suspend fun savePassword(sourceId: String, password: String) { credentialData[sourceId] = password }
            override suspend fun getPassword(sourceId: String) = credentialData[sourceId]
            override suspend fun clearPassword(sourceId: String) = error("同步不能清理凭证")
        }
        val sources = object : SourceRepository {
            override fun observeSources() = flowOf(sourceData.values.toList())
            override suspend fun getSource(id: String) = sourceData[id]
            override suspend fun upsert(source: Source) { sourceData[source.id] = source }
            override suspend fun deleteById(id: String) = error("同步不能删除音源")
        }
        val songs = object : SongRepository {
            override fun observeSongs() = flowOf(songData.values.toList())
            override suspend fun getSong(id: String) = songData[id]
            override suspend fun upsert(song: Song) { songData[song.id] = song }
            override suspend fun rebuildDerivedIndexes() = Unit
            override suspend fun replaceSourceSongs(sourceId: String, songs: List<Song>): ScanMergeResult = error("同步不能重扫曲库")
            override suspend fun deleteSourceSongs(sourceId: String) = error("同步不能删除曲库")
        }
        val scripts = object : LxScriptStore {
            override fun list() = scriptData.values.toList()
            override fun get(id: String) = scriptData[id]
            override fun save(id: String, source: String, enabled: Boolean, sourceUrl: String?): LxStoredScript {
                return LxStoredScript(id, "脚本", source, LxScriptMeta(), System.currentTimeMillis(), enabled, sourceUrl)
                    .also { scriptData[id] = it }
            }
            override fun setEnabled(id: String, enabled: Boolean): Boolean = error("测试未使用")
            override fun delete(id: String): Boolean = error("同步不能删除脚本")
        }
        val manager = WebDavSyncManager(store, credentials, sources, songs, history, stats, scripts)
        val config = WebDavSyncConfig(cloud.server.url("/dav/").toString(), "account", "MusesSync")
        suspend fun configure() = manager.save(config, "sync-secret")
        override fun close() { scope.cancel() }
    }

    @Test fun twoDevicesMergeAndRepeatedSyncDoesNotDoubleStats() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
            a.configure(); b.configure()
            a.settings.setOnlinePreferredQuality("flac")
            a.stats.recordPlay("online:wy:1", "歌曲", "歌手", null, "2026-10-08")
            a.stats.addListenMs(1000, "2026-10-08")
            a.history.record(RecentPlayEntry("online:wy:1", "歌曲", "歌手", playedAt = System.currentTimeMillis()))
            a.manager.synchronize()
            assertTrue(a.manager.status.value.startsWith("同步完成"))
            b.stats.recordPlay("online:wy:2", "另一首", "歌手", null, "2026-10-08")
            b.stats.addListenMs(2000, "2026-10-08")
            b.manager.synchronize()
            assertEquals("flac", b.settings.onlinePreferredQuality.first())
            assertEquals(2, b.stats.load().totalPlayCount)
            assertEquals(3000, b.stats.load().totalListenMs)
            assertEquals(1, b.stats.loadLocalContribution().totalPlayCount)
            a.manager.synchronize(); b.manager.synchronize(); a.manager.synchronize()
            assertEquals(2, a.stats.load().totalPlayCount)
            assertEquals(3000, a.stats.load().totalListenMs)
            assertEquals(1, b.history.load().size)
            delay(5)
            b.settings.setOnlinePreferredQuality("128k"); b.manager.synchronize(); a.manager.synchronize()
            assertEquals("128k", a.settings.onlinePreferredQuality.first())
        } } }
    }

    @Test fun failureDoesNotApplyRemoteSettingsOrMarkSuccess() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
            a.configure(); b.configure()
            a.settings.setOnlinePreferredQuality("flac"); a.manager.synchronize()
            cloud.failPut = true
            b.manager.synchronize()
            assertEquals("320k", b.settings.onlinePreferredQuality.first())
            assertEquals(0, b.manager.lastSync.first())
            assertTrue(b.manager.status.value.startsWith("同步失败"))
        } } }
    }

    @Test fun secretsDevicePathsAndBuiltinScriptsAreNotPublished() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a ->
            a.configure()
            a.settings.setDownloadDeviceDirectory("C:/private/music")
            a.settings.setAudioFocusEnabled(true)
            a.history.record(RecentPlayEntry("local-song", "本地歌曲", "歌手", "file:///private/cover.jpg", System.currentTimeMillis()))
            a.sourceData["bad"] = Source("bad", "不能同步", SourceType.WEBDAV, "https://music.example.com/?token=source-secret", createdAt = 0, updatedAt = 0)
            a.sourceData["local"] = Source("local", "本机", SourceType.LOCAL, path = "C:/private/music", createdAt = 0, updatedAt = 0)
            a.scriptData["builtin"] = LxStoredScript("builtin", "内置", "builtin-code", LxScriptMeta(), 0, isBuiltin = true)
            a.manager.synchronize()
            val body = cloud.documents.values.single()
            listOf("sync-secret", "api-secret", "source-secret", "C:/private", "file:///private", "audio_focus_enabled", "builtin-code").forEach {
                assertFalse(body.contains(it), it)
            }
            assertTrue(a.manager.status.value.startsWith("同步完成"))
        } }
    }

    @Test fun sameServerDifferentMusicAccountsStaySeparateAndScriptsMerge() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
            a.configure(); b.configure()
            a.sourceData["a"] = Source("a", "甲账号", SourceType.WEBDAV, "https://music.example.com/dav/", username = "alice", createdAt = 0, updatedAt = 0)
            b.sourceData["b"] = Source("b", "乙账号", SourceType.WEBDAV, "https://music.example.com/dav/", username = "bob", createdAt = 0, updatedAt = 0)
            a.scripts.save("user_script", "/* @name 测试 */", false, null)
            a.manager.synchronize(); b.manager.synchronize()
            assertEquals(setOf("alice", "bob"), b.sourceData.values.map { it.username }.toSet())
            assertEquals(false, b.scriptData["user_script"]?.enabled)
        } } }
    }

    @Test fun sourceEditDuringUploadIsPreserved() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a ->
            a.configure()
            val source = Source("a", "原名称", SourceType.WEBDAV, "https://music.example.com/dav/", username = "alice", createdAt = 0, updatedAt = 0)
            a.sourceData["a"] = source
            cloud.onPut = { a.sourceData["a"] = source.copy(name = "刚改的名称", updatedAt = System.currentTimeMillis()) }
            a.manager.synchronize()
            assertEquals("刚改的名称", a.sourceData["a"]?.name)
        } }
    }

    @Test fun maliciousTimestampIsRejectedBeforePublishing() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
            a.configure(); b.configure()
            a.settings.setOnlinePreferredQuality("flac"); a.manager.synchronize()
            val name = cloud.documents.keys.single()
            val snapshot = Json.decodeFromString<SyncSnapshot>(cloud.documents.getValue(name))
            cloud.documents[name] = Json.encodeToString(snapshot.copy(preferences = mapOf(
                "online_preferred_quality" to SyncValue(JsonPrimitive("flac"), Long.MAX_VALUE, snapshot.deviceId))))
            val puts = cloud.puts
            b.manager.synchronize()
            assertEquals(puts, cloud.puts)
            assertEquals("320k", b.settings.onlinePreferredQuality.first())
        } } }
    }

    @Test fun transportRejectsCredentialUrlsTraversalAndFiltersCoverSecrets() {
        val transport = WebDavSyncTransport()
        assertFails { transport.root(WebDavSyncConfig("https://user:password@example.com/dav/", "user")) }
        assertFails { transport.root(WebDavSyncConfig("https://example.com/dav/", "user", "../Music")) }
        assertEquals("https://example.com/dav/MusesSync/", transport.root(WebDavSyncConfig("https://example.com/dav/", "user")).toString())
        assertNull(publicCover("https://example.com/cover?token=secret"))
    }

    @Test fun statsOverflowCannotBecomeNegative() {
        val contribution = PlayStats(totalListenMs = Long.MAX_VALUE, totalPlayCount = Int.MAX_VALUE)
        val result = mergePlayStats(listOf(contribution, contribution))
        assertEquals(Long.MAX_VALUE, result.totalListenMs)
        assertEquals(Int.MAX_VALUE, result.totalPlayCount)
    }

    @Test fun eachDisabledCategorySkipsUploadAndReceiving() = runBlocking {
        for (content in SyncContent.entries) {
            Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
                a.configure(); b.configure()
                a.settings.setOnlinePreferredQuality("flac")
                a.sourceData["a"] = Source("a", "远端音源", SourceType.WEBDAV, "https://music.example.com/dav/",
                    username = "alice", createdAt = 0, updatedAt = 0)
                a.scripts.save("remote_script", "/* @name 远端脚本 */", true, null)
                a.songData["online:wy:1"] = Song("online:wy:1", "remote_script", "muslx://test", "历史歌曲", sourceType = SourceType.ONLINE)
                a.history.record(RecentPlayEntry("online:wy:1", "历史歌曲", "歌手", playedAt = System.currentTimeMillis()))
                a.stats.recordPlay("online:wy:2", "统计歌曲", "歌手", null, "2026-10-08")
                a.manager.synchronize()
                val first = cloud.documents.keys.single()
                b.manager.setContentEnabled(content, false)
                b.manager.synchronize()
                assertTrue(b.manager.status.value.startsWith("同步完成"), content.name)
                val uploaded = Json.decodeFromString<SyncSnapshot>(cloud.documents.entries.single { it.key != first }.value)
                when (content) {
                    SyncContent.PREFERENCES -> {
                        assertEquals("320k", b.settings.onlinePreferredQuality.first())
                        assertTrue(uploaded.preferences.isEmpty())
                    }
                    SyncContent.SOURCES -> {
                        assertTrue(b.sourceData.isEmpty()); assertTrue(uploaded.sources.isEmpty())
                        assertTrue(b.scriptData.isEmpty()); assertTrue(uploaded.scripts.isEmpty())
                    }
                    SyncContent.HISTORY -> {
                        assertTrue(b.history.load().isEmpty()); assertTrue(uploaded.history.isEmpty())
                        assertFalse(b.songData.containsKey("online:wy:1"))
                    }
                    SyncContent.STATS -> { assertEquals(0, b.stats.load().totalPlayCount); assertEquals(PlayStats.Empty, uploaded.stats) }
                }
            } } }
        }
    }

    @Test fun disabledCategoriesPreserveEarlierCloudValuesAndReenableMerges() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a -> Device(cloud).use { b ->
            a.configure(); b.configure()
            a.settings.setOnlinePreferredQuality("flac")
            a.stats.recordPlay("online:wy:1", "歌曲", "歌手", null, "2026-10-08")
            a.manager.synchronize()
            val name = cloud.documents.keys.single()
            val old = Json.decodeFromString<SyncSnapshot>(cloud.documents.getValue(name))
            a.manager.setContentEnabled(SyncContent.PREFERENCES, false)
            a.manager.setContentEnabled(SyncContent.STATS, false)
            a.settings.setOnlinePreferredQuality("128k")
            a.stats.recordPlay("online:wy:1", "歌曲", "歌手", null, "2026-10-08")
            b.stats.recordPlay("online:wy:2", "另一首", "歌手", null, "2026-10-08")
            b.manager.synchronize(); a.manager.synchronize()
            val frozen = Json.decodeFromString<SyncSnapshot>(cloud.documents.getValue(name))
            assertEquals(old.preferences, frozen.preferences)
            assertEquals(old.stats, frozen.stats)
            assertEquals("128k", a.settings.onlinePreferredQuality.first())
            assertEquals(2, a.stats.load().totalPlayCount)
            assertFalse(a.manager.selection.first().preferences)
            a.manager.setContentEnabled(SyncContent.PREFERENCES, true)
            a.manager.setContentEnabled(SyncContent.STATS, true)
            a.manager.synchronize(); b.manager.synchronize(); a.manager.synchronize()
            assertEquals("128k", b.settings.onlinePreferredQuality.first())
            assertEquals(3, a.stats.load().totalPlayCount)
            assertEquals(3, b.stats.load().totalPlayCount)
        } } }
    }

    @Test fun allCategoriesDisabledDoesNotContactCloudAndDoesNotNeedCredentials() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a ->
            SyncContent.entries.forEach { a.manager.setContentEnabled(it, false) }
            assertFalse(a.manager.selection.first().anyEnabled)
            a.manager.synchronize()
            assertEquals(0, cloud.server.requestCount)
            assertEquals("请先选择要同步的内容", a.manager.status.value)
        } }
    }

    @Test fun legacySourceSelectionsDoNotEnablePreviouslyDisabledContent() = runBlocking {
        Cloud().use { cloud -> Device(cloud).use { a ->
            val key = stringPreferencesKey("webdav_sync_selection")
            for ((sources, scripts) in listOf(true to false, false to true, false to false, true to true)) {
                a.store.edit { it[key] = "{\"sources\":$sources,\"scripts\":$scripts}" }
                assertEquals(sources && scripts, a.manager.selection.first().sources)
            }
            a.manager.setContentEnabled(SyncContent.SOURCES, true)
            assertTrue(a.manager.selection.first().sources)
            assertFalse(a.store.data.first()[key]!!.contains("scripts"))
        } }
    }
}

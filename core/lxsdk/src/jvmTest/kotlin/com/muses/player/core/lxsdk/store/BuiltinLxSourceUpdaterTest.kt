package com.muses.player.core.lxsdk.store

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.muses.player.core.data.repository.DataStoreSettingsRepository
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.lxsdk.crypto.LxCryptoJvm
import com.muses.player.core.lxsdk.http.LxHttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class BuiltinLxSourceUpdaterTest {
    private fun script(version: String) = """
        /**
         * @name 稳定版音源 v$version
         * @version $version
         */
        lx.on('request', () => Promise.resolve('https://cdn.test/audio.mp3'));
        lx.send('inited', {sources: {wy: {actions: ['musicUrl'], qualitys: ['320k']}}});
    """.trimIndent()

    private inner class Fixture : AutoCloseable {
        val root = Files.createTempDirectory("muses-weekly-test").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope,
            produceFile = { File(root, "settings.preferences_pb") }))
        val store = FileLxScriptStore({ File(root, "scripts") }, {
            listOf(BundledLxScript("builtin-stable", script("1.0.3"), "https://example.test/old.js"))
        })
        val repository = LxScriptRepository(LxCryptoJvm(), storedScriptsProvider = {
            val enabled = settings.builtinLxSourcesEnabled.first()
            store.list().filter { it.enabled && (enabled || !it.isBuiltin) }.map { it.id to it.source }
        })
        var now = 1_000_000L
        var requests = 0
        var version = "2.0.0"
        var fail = false
        val updater = BuiltinLxSourceUpdater(store, repository, settings, LxHttpClient(),
            stateFile = { File(root, "update.json") }, clock = { now }, fetch = { url ->
                requests++
                if (fail) error("网络不可用")
                when {
                    url.endsWith("/contents") -> """[{"name":"V260907","type":"dir"},{"name":"V261001","type":"dir"}]"""
                    url.endsWith("/V261001") -> """[{"name":"stable.js","type":"file","download_url":"https://raw.githubusercontent.com/guoyue2010/lxmusic-/main/V261001/stable.js"}]"""
                    else -> script(version)
                }
            })
        override fun close() { repository.close(); scope.cancel(); root.deleteRecursively() }
    }

    @Test fun `每周检查最新目录并保留独立禁用及自导入脚本`() = runBlocking {
        Fixture().use { f ->
            f.store.list()
            f.store.setEnabled("builtin-stable", false)
            f.store.save("custom", "用户脚本")
            assertTrue(f.updater.checkIfDue())
            assertEquals("2.0.0", f.store.get("builtin-stable")!!.meta.version)
            assertFalse(f.store.get("builtin-stable")!!.enabled)
            assertEquals("用户脚本", f.store.get("custom")!!.source)
            val count = f.requests
            f.now += BuiltinLxSourceUpdater.WEEK_MS - 1
            assertTrue(f.updater.checkIfDue())
            assertEquals(count, f.requests)
            f.now++
            f.version = "3.0.0"
            assertTrue(f.updater.checkIfDue())
            assertEquals("3.0.0", f.store.get("builtin-stable")!!.meta.version)
        }
    }

    @Test fun `关闭内置开关只排除内置脚本且停止更新`() = runBlocking {
        Fixture().use { f ->
            f.store.list()
            f.store.save("custom", "用户脚本")
            f.updater.setEnabled(false)
            assertEquals(listOf("custom"), f.repository.scripts().map { it.scriptId })
            assertTrue(f.updater.checkIfDue())
            assertEquals(0, f.requests)
            f.updater.setEnabled(true)
            assertEquals(2, f.repository.scripts().size)
        }
    }

    @Test fun `已编辑或删除的脚本不会被更新覆盖`() = runBlocking {
        Fixture().use { f ->
            f.store.list()
            f.store.save("builtin-stable", "用户修改", false)
            f.updater.checkIfDue()
            assertEquals("用户修改", f.store.get("builtin-stable")!!.source)
            f.store.delete("builtin-stable")
            f.now += BuiltinLxSourceUpdater.WEEK_MS
            f.updater.checkIfDue()
            assertNull(f.store.get("builtin-stable"))
        }
    }

    @Test fun `网络失败保留旧版并在一天后重试`() = runBlocking {
        Fixture().use { f ->
            f.fail = true
            assertFalse(f.updater.checkIfDue())
            assertEquals("1.0.3", f.store.get("builtin-stable")!!.meta.version)
            val count = f.requests
            assertFalse(f.updater.checkIfDue())
            assertEquals(count, f.requests)
            f.now += 24 * 60 * 60 * 1_000L
            f.fail = false
            assertTrue(f.updater.checkIfDue())
            assertEquals("2.0.0", f.store.get("builtin-stable")!!.meta.version)
        }
    }

    @Test fun `更新仍关闭卡密通道`() = runBlocking {
        Fixture().use { f ->
            val source = f.updater.sanitize("builtin-xigua", "var HYW_ENABLE = true;\nvar HYW_CARD_KEY = 'secret';")
            assertTrue(source.contains("HYW_ENABLE = false"))
            assertFalse(source.contains("secret"))
        }
    }
}

package com.muses.player.core.lxsdk.store

import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.lxsdk.LxScriptMetaParser
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.lxsdk.http.LxHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

/** Android 后台任务和桌面运行期间共享的每周音源更新器。 */
class BuiltinLxSourceUpdater(
    private val store: FileLxScriptStore,
    private val repository: LxScriptRepository,
    private val settings: SettingsRepository,
    private val http: LxHttpClient,
    private val stateFile: () -> File = { File(PlatformDirs.appDataDir(), "builtin-lx-update.json") },
    private val clock: () -> Long = System::currentTimeMillis,
    private val fetch: (suspend (String) -> String)? = null,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow("每周自动检查更新")
    val status = _status.asStateFlow()

    companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1_000
        private const val API = "https://api.github.com/repos/guoyue2010/lxmusic-/contents"
    }

    suspend fun setEnabled(enabled: Boolean) {
        settings.setBuiltinLxSourcesEnabled(enabled)
        repository.refreshFromStore()
    }

    suspend fun runPeriodicChecks() {
        settings.builtinLxSourcesEnabled.distinctUntilChanged().collectLatest { enabled ->
            repository.refreshFromStore()
            if (enabled) while (true) {
                checkIfDue()
                delay(60 * 60 * 1_000L)
            }
        }
    }

    /** 失败保留旧脚本，24 小时后重试；正常检查至少间隔七天。 */
    suspend fun checkIfDue(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!settings.builtinLxSourcesEnabled.first()) return@withLock true
            if (store.list().none { it.isBuiltin }) return@withLock true
            val previous = runCatching { Json.parseToJsonElement(stateFile().readText()).jsonObject }.getOrNull()
            val success = previous?.get("success")?.jsonPrimitive?.longOrNull ?: 0L
            val attempt = previous?.get("attempt")?.jsonPrimitive?.longOrNull ?: 0L
            val now = clock()
            if (success > 0 && now - success in 0 until WEEK_MS) return@withLock true
            if (attempt > success && now - attempt in 0 until 24 * 60 * 60 * 1_000L) return@withLock false
            _status.value = "正在检查内置音源更新…"
            try {
                val directories = Json.parseToJsonElement(get(API)).jsonArray
                val latest = directories.mapNotNull { entry ->
                    val item = entry.jsonObject
                    item["name"]?.jsonPrimitive?.content?.takeIf {
                        item["type"]?.jsonPrimitive?.content == "dir" && Regex("V\\d{6}").matches(it)
                    }
                }.maxOrNull() ?: error("未找到音源版本目录")
                val entries = Json.parseToJsonElement(get("$API/$latest")).jsonArray
                val known = BundledLxScripts.load().associateBy { identity(LxScriptMetaParser.parse(it.source).name.orEmpty()) }
                var updated = 0
                var matched = 0
                var failed = false
                for (entry in entries) {
                    if (!settings.builtinLxSourcesEnabled.first()) return@withLock true
                    val item = entry.jsonObject
                    if (item["type"]?.jsonPrimitive?.content != "file" ||
                        item["name"]?.jsonPrimitive?.content?.endsWith(".js") != true) continue
                    val url = item["download_url"]?.jsonPrimitive?.contentOrNull ?: continue
                    // 固定可信仓库地址，不能由清单重定向至任意来源。
                    if (!url.startsWith("https://raw.githubusercontent.com/guoyue2010/lxmusic-/")) continue
                    try {
                        val raw = get(url)
                        require(raw.length in 1..1_000_000) { "音源脚本大小异常" }
                        val bundled = known[identity(LxScriptMetaParser.parse(raw).name.orEmpty())] ?: continue
                        val current = store.get(bundled.id) ?: continue
                        matched++
                        val source = sanitize(bundled.id, raw)
                        if (current.source == source) continue
                        require(repository.validate(source).sources.isNotEmpty()) { "脚本未声明可用音源" }
                        if (!settings.builtinLxSourcesEnabled.first()) return@withLock true
                        if (store.updateBuiltin(current.id, current.source, source, url)) updated++
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { failed = true }
                }
                require(matched > 0) { "最新目录未找到已内置的免费音源" }
                repository.refreshFromStore()
                persist(now, if (failed) success else now)
                _status.value = if (failed) "部分音源更新失败，保留旧版，稍后自动重试" else "已检查 $latest，更新 $updated 个音源 · 每周自动更新"
                !failed
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                persist(now, success)
                _status.value = "更新检查失败，保留现有音源，稍后自动重试"
                false
            }
        }
    }

    private suspend fun get(url: String): String = fetch?.invoke(url) ?: run {
        val response = Json.parseToJsonElement(http.requestResponseJson(url,
            """{"timeout":20000,"headers":{"User-Agent":"Muses","Accept":"application/vnd.github+json"}}""")).jsonObject
        require(response["statusCode"]?.jsonPrimitive?.intOrNull == 200) { "更新服务不可用" }
        response.getValue("body").jsonPrimitive.content
    }

    private fun persist(attempt: Long, success: Long) {
        val file = stateFile()
        file.parentFile?.mkdirs()
        file.writeText(buildJsonObject { put("attempt", attempt); put("success", success) }.toString())
    }

    private fun identity(name: String): String = name.lowercase()
        .replace(Regex("(?i)\\s*v?\\d+(?:\\.\\d+)*"), "")
        .replace(Regex("[^\\p{L}\\p{N}]"), "")

    internal fun sanitize(id: String, source: String): String {
        if (id != "builtin-xigua") {
            require(!source.contains("卡密")) { "音源需要卡密，保留旧版" }
            return source
        }
        require(source.contains("var HYW_ENABLE =") && source.contains("var HYW_CARD_KEY =")) { "卡密通道配置已变化，保留旧版" }
        return source.lineSequence().joinToString("\n") { line ->
            when {
                line.trimStart().startsWith("var HYW_ENABLE =") -> "var HYW_ENABLE = false;"
                line.trimStart().startsWith("var HYW_CARD_KEY =") -> "var HYW_CARD_KEY = '';"
                else -> line
            }
        }
    }
}

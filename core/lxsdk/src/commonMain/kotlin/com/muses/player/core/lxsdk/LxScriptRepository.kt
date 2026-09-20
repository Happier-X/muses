package com.muses.player.core.lxsdk

import com.muses.player.core.lxsdk.http.LxHttpClient
import com.muses.player.core.lxsdk.crypto.LxCrypto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 已加载的音源脚本条目：脚本源码 + 元信息 + 运行中的引擎。
 *
 * 一个脚本可声明多个源（kw/kg/tx/wy/mg/local），故按 platform 建索引。
 */
class LoadedLxScript(
    val scriptId: String,
    val source: String,
) {
    var engine: LxScriptEngine? = null
        private set

    /** 加载失败时的错误信息（供 UI 展示「脚本不可用」） */
    var loadError: String? = null
        private set

    /** 加载成功后的源声明 */
    val sources: Map<String, LxSourceDeclaration>
        get() = engine?.sources ?: emptyMap()

    val meta: LxScriptMeta
        get() = engine?.meta ?: LxScriptMetaParser.parse(source)

    suspend fun ensureLoaded(
        crypto: LxCrypto,
        httpClient: LxHttpClient,
        requestTimeoutMs: Long,
    ): Boolean {
        if (engine != null) return true
        return try {
            val e = LxScriptEngine(
                scriptSource = source,
                crypto = crypto,
                httpClient = httpClient,
                requestTimeoutMs = requestTimeoutMs,
            )
            e.load()
            engine = e
            loadError = null
            true
        } catch (e: Exception) {
            loadError = e.message ?: "脚本加载失败"
            false
        }
    }

    fun close() {
        runCatching { engine?.close() }
        engine = null
    }
}

/**
 * 在线音源脚本仓库：管理多个洛雪脚本，按 platform 路由解析请求。
 *
 * 职责：
 * - 持有已导入脚本列表（由上层持久化，仓库只关心运行态）；
 * - 懒加载脚本引擎（首次用到某平台时才加载对应脚本，避免启动即拉起全部 QuickJS runtime）；
 * - 按 platform 选择可用脚本并解析直链。
 *
 * 线程模型：脚本列表与索引变更经 [mutex] 串行化；引擎内部的并发由 [LxScriptEngine] 自身保证。
 */
class LxScriptRepository(
    private val crypto: LxCrypto,
    private val httpClient: LxHttpClient = LxHttpClient(),
    private val requestTimeoutMs: Long = 15_000L,
    /**
     * 已导入脚本的来源（懒加载）。
     *
     * 首次真正用到仓库时才从此处拉取并登记，而**不是**在构造时 eager 读取——
     * 因为 Android 的 `PlatformDirs` 依赖 `initPlatformDirs(context)` 先执行，
     * 构造期读取会在 Koin 解析链早期抛错（实测崩溃）。
     *
     * @return (scriptId, 脚本源码) 列表；仅返回已启用的脚本
     */
    private val storedScriptsProvider: (suspend () -> List<Pair<String, String>>)? = null,
) {

    private val mutex = Mutex()

    /** 懒同步互斥（与 [mutex] 分开，避免重入死锁：同步过程内部会调 register） */
    private val syncMutex = Mutex()
    private var synced = false

    private val scripts = mutableListOf<LoadedLxScript>()

    /**
     * 首次调用时把存储中的脚本登记进仓库（幂等）。
     * 注意：必须在**获取 [mutex] 之前**调用，否则与 [register] 重入死锁。
     */
    private suspend fun ensureSynced() {
        if (synced) return
        syncMutex.withLock {
            if (synced) return
            val provider = storedScriptsProvider
            if (provider != null) {
                runCatching { provider() }.getOrDefault(emptyList()).forEach { (id, source) ->
                    register(id, source)
                }
            }
            synced = true
        }
    }

    /** 当前已注册脚本快照（供 UI 展示） */
    suspend fun scripts(): List<LoadedLxScript> {
        ensureSynced()
        return mutex.withLock { scripts.toList() }
    }

    /**
     * 触发全部脚本加载（供 UI 展示源列表与加载错误）。
     * 加载失败的脚本不抛错，错误记录在各自的 [LoadedLxScript.loadError] 上。
     */
    suspend fun loadAll(): List<LoadedLxScript> {
        ensureSynced()
        val snapshot = mutex.withLock { scripts.toList() }
        snapshot.forEach { it.ensureLoaded(crypto, httpClient, requestTimeoutMs) }
        return snapshot
    }

    /**
     * 校验脚本源码（导入前预检）：试加载并返回源声明。
     * 临时引擎用完即关，不影响已注册脚本。
     *
     * 用于 UI 在用户粘贴脚本时立即反馈「能否初始化」「能提供哪些源」，
     * 避免导入一个跑不起来的脚本。
     */
    suspend fun validate(source: String): LxScriptDescriptor {
        val engine = LxScriptEngine(
            scriptSource = source,
            crypto = crypto,
            httpClient = httpClient,
            requestTimeoutMs = requestTimeoutMs,
        )
        return try {
            engine.load()
        } finally {
            engine.close()
        }
    }

    /**
     * 当前可用于某平台的源声明（需已加载）。
     * 供 UI 提示「该平台当前由哪个脚本提供」；未加载时返回空。
     */
    suspend fun declarationsFor(platform: String): List<Pair<String, LxSourceDeclaration>> {
        ensureSynced()
        return mutex.withLock { scripts.toList() }
            .mapNotNull { script ->
                val decl = script.sources[platform] ?: return@mapNotNull null
                (script.meta.name ?: script.scriptId) to decl
            }
    }

    /** 注册（或替换）脚本。不立即加载——首次请求该脚本的源时才加载。 */
    suspend fun register(scriptId: String, source: String) = mutex.withLock {
        scripts.filter { it.scriptId == scriptId }.forEach { it.close() }
        scripts.removeAll { it.scriptId == scriptId }
        scripts.add(LoadedLxScript(scriptId = scriptId, source = source))
    }

    /** 注销脚本并释放其引擎 */
    suspend fun unregister(scriptId: String) = mutex.withLock {
        scripts.filter { it.scriptId == scriptId }.forEach { it.close() }
        scripts.removeAll { it.scriptId == scriptId }
        Unit
    }

    suspend fun clear() = mutex.withLock {
        scripts.forEach { it.close() }
        scripts.clear()
    }

    /**
     * 解析直链。
     *
     * 路由策略：按 [platform] 找到**第一个**声明了该源且支持 `musicUrl` 的脚本。
     * 找不到可用脚本时抛 [LxResolveException]（上层转成用户可读文案）。
     */
    suspend fun resolveMusicUrl(
        platform: String,
        musicInfoJson: String,
        quality: LxQuality? = null,
    ): String {
        ensureSynced()
        val candidates = mutex.withLock { scripts.toList() }
        if (candidates.isEmpty()) {
            throw LxResolveException("未导入任何音源脚本。")
        }

        val failures = mutableListOf<String>()
        for (script in candidates) {
            // 懒加载：加载失败的脚本跳过并记录（不阻断其它脚本）
            val loaded = script.ensureLoaded(crypto, httpClient, requestTimeoutMs)
            if (!loaded) {
                failures += "${script.meta.name ?: script.scriptId}: ${script.loadError}"
                continue
            }
            val engine = script.engine ?: continue
            val declared = engine.sources?.get(platform) ?: continue
            if (!declared.supports(LxAction.MUSIC_URL)) continue

            val effectiveQuality = pickQuality(declared, quality)
            try {
                return engine.getMusicUrl(platform, effectiveQuality, musicInfoJson).url
            } catch (e: Exception) {
                failures += "${script.meta.name ?: script.scriptId}: ${e.message}"
            }
        }

        throw LxResolveException(
            buildString {
                append("无法为源 '$platform' 解析播放地址。")
                if (failures.isNotEmpty()) {
                    append("已尝试：")
                    append(failures.joinToString("；"))
                }
            },
        )
    }

    /**
     * 解析歌词（脚本 `lyric` 动作）。
     *
     * 路由策略同 [resolveMusicUrl]：按 [platform] 找到第一个声明了该动作的脚本；
     * 找不到可用脚本或脚本报错时抛 [LxResolveException]（上层转用户可读文案或静默忽略）。
     */
    suspend fun resolveLyric(platform: String, musicInfoJson: String): LxLyric =
        routeToEngine(platform, LxAction.LYRIC, "歌词") { engine ->
            engine.getLyric(platform, musicInfoJson)
        }

    /**
     * 解析封面地址（脚本 `pic` 动作）。
     *
     * 失败语义同 [resolveLyric]：抛 [LxResolveException]。
     */
    suspend fun resolveCover(platform: String, musicInfoJson: String): String =
        routeToEngine(platform, LxAction.PIC, "封面") { engine ->
            engine.getPic(platform, musicInfoJson).url
        }

    /**
     * 按 [platform] + [action] 路由到第一个可用引擎并执行 [call]。
     *
     * 与 [resolveMusicUrl] 的差异：不做音质选择（lyric/pic 与音质无关），
     * 但沿用同一套「懒加载 → 声明校验 → 逐脚本尝试 → 汇总失败原因」口径。
     */
    private suspend fun <T> routeToEngine(
        platform: String,
        action: LxAction,
        what: String,
        call: suspend (LxScriptEngine) -> T,
    ): T {
        ensureSynced()
        val candidates = mutex.withLock { scripts.toList() }
        if (candidates.isEmpty()) {
            throw LxResolveException("未导入任何音源脚本。")
        }

        val failures = mutableListOf<String>()
        for (script in candidates) {
            val loaded = script.ensureLoaded(crypto, httpClient, requestTimeoutMs)
            if (!loaded) {
                failures += "${script.meta.name ?: script.scriptId}: ${script.loadError}"
                continue
            }
            val engine = script.engine ?: continue
            val declared = engine.sources?.get(platform) ?: continue
            if (!declared.supports(action)) continue
            try {
                return call(engine)
            } catch (e: Exception) {
                failures += "${script.meta.name ?: script.scriptId}: ${e.message}"
            }
        }

        throw LxResolveException(
            buildString {
                append("无法为源 '$platform' 获取$what。")
                if (failures.isNotEmpty()) {
                    append("已尝试：")
                    append(failures.joinToString("；"))
                }
            },
        )
    }

    /**
     * 音质选择：请求档位可用则用之；否则**就近回退**。
     *
     * 回退方向规则：优先**向下**（更低档位体积更小、兼容性更好，不会让用户在移动网络上
     * 意外被切到上百 MB 的母带文件）；只有当没有更低可用档时才**向上**取最低可用档。
     *
     * 返回 null 表示脚本对该源未声明任何音质（上层按 `type=null` 传递，
     * 由脚本自身的 `mapQuality` 兜底）。
     */
    private fun pickQuality(
        declared: LxSourceDeclaration,
        requested: LxQuality?,
    ): LxQuality? {
        val start = requested ?: LxQuality.DEFAULT
        if (declared.supports(start)) return start
        val available = LxQuality.ordered().filter { declared.supports(it) }
        if (available.isEmpty()) return null
        return available.lastOrNull { it.rank < start.rank }
            ?: available.firstOrNull { it.rank > start.rank }
            ?: available.first()
    }

    fun close() {
        scripts.forEach { it.close() }
        scripts.clear()
    }
}

/** 解析失败（上层转用户可读文案，不直接透出） */
class LxResolveException(message: String, cause: Throwable? = null) : Exception(message, cause)

package com.muses.player.core.lxsdk

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.define
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import com.muses.player.core.lxsdk.crypto.LxCrypto
import com.muses.player.core.lxsdk.http.LxHttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.abs

/**
 * 洛雪自定义音源脚本引擎。
 *
 * 一个实例 = 一份脚本 = 一个长生命周期 QuickJS runtime（spike 结论：DSL 形式每次新建
 * runtime 会丢 handler，必须持有实例并跨请求复用）。
 *
 * 生命周期：
 * ```
 * val engine = LxScriptEngine(scriptSource)
 * val descriptor = engine.load()      // 加载 + inited，拿到源声明
 * val url = engine.getMusicUrl("kw", LxQuality.Q_320K, """{"songid":123}""")
 * engine.close()
 * ```
 *
 * 线程模型：QuickJS 实例**不可并发访问**（同一 runtime 内多次 evaluate 会相互干扰），
 * 故所有 evaluate 经 [evalMutex] 串行化。HTTP 请求在锁外等待，不阻塞其它调用。
 */
class LxScriptEngine(
    private val scriptSource: String,
    private val crypto: LxCrypto,
    private val httpClient: LxHttpClient = LxHttpClient(),
    private val json: Json = LxJson,
    /** 单次请求等待脚本返回的超时（脚本可能挂死，必须兜底） */
    private val requestTimeoutMs: Long = 15_000L,
    /**
     * 脚本 console 输出回调（level = log/info/warn/error/debug）。
     * 洛雪脚本大量用 console.log 排错，透出日志是排查「脚本静默失败」的关键。
     */
    private val onScriptLog: ((level: String, message: String) -> Unit)? = null,
) {

    private var js: QuickJs? = null
    private var descriptor: LxScriptDescriptor? = null
    private var closed = false

    /** QuickJS 实例互斥：同 runtime 不可并发 evaluate */
    private val evalMutex = Mutex()

    /** 并发请求的结果槽位（key = 槽位名） */
    private val pendingResults = mutableMapOf<String, CompletableDeferred<String?>>()
    private val pendingResultsLock = Mutex()

    /** 脚本元信息（解析自源码头部注释；加载前后均可读） */
    val meta: LxScriptMeta by lazy { LxScriptMetaParser.parse(scriptSource) }

    /** 加载后的源声明；未加载时为 null */
    val sources: Map<String, LxSourceDeclaration>? get() = descriptor?.sources

    /**
     * 加载并初始化脚本。
     *
     * @throws LxException.ScriptInitFailed 脚本语法错误，或未在初始化期发送 `inited` 事件
     */
    suspend fun load(): LxScriptDescriptor {
        checkNotClosed()
        if (descriptor != null) return descriptor!!

        val instance = QuickJs.create(Dispatchers.Default)
        js = instance
        try {
            installBindings(instance)
            instance.evaluate<Any?>(LxBridge.BOOTSTRAP)
            instance.evaluate<Any?>(LxBridge.currentScriptInfoScript(metaToJson()))
            // 脚本本体为全局作用域执行（洛雪规范）
            instance.evaluate<Any?>(scriptSource, filename = "lx-source.js")
        } catch (e: LxException) {
            runCatching { instance.close() }
            js = null
            throw e
        } catch (e: Exception) {
            runCatching { instance.close() }
            js = null
            throw LxException.ScriptInitFailed("脚本执行失败：${e.message}", e)
        }

        // inited 在脚本同步执行期应已送达；未送达即初始化失败
        val inited = descriptor
            ?: run {
                runCatching { instance.close() }
                js = null
                throw LxException.ScriptInitFailed(
                    "脚本未发送 inited 事件（初始化失败）。请确认脚本完整且为洛雪自定义源格式。",
                )
            }
        return inited
    }

    // ── 对外请求 API ──

    /**
     * 获取歌曲直链。
     * @param platform 源 key（kw/kg/tx/wy/mg/local）
     * @param quality 音质（`local` 源传 null）
     * @param musicInfoJson 音乐信息对象 JSON（含 songmid/songid/hash 等各源自有字段）
     */
    suspend fun getMusicUrl(
        platform: String,
        quality: LxQuality?,
        musicInfoJson: String,
    ): LxMusicUrl {
        val info = buildJsonObject {
            put("type", quality?.key?.let { JsonPrimitive(it) } ?: JsonNull)
            put("musicInfo", parseMusicInfo(musicInfoJson))
        }
        val result = request("musicUrl", platform, info, resultKeyHint = "musicUrl")
        val url = extractUrl(result)
            ?: throw LxException.ScriptRuntimeError("musicUrl 未返回有效地址（脚本返回空）")
        return LxMusicUrl(url = url, quality = quality)
    }

    /** 获取歌词（local 源支持；其它源按脚本声明） */
    suspend fun getLyric(platform: String, musicInfoJson: String): LxLyric {
        val info = buildJsonObject { put("musicInfo", parseMusicInfo(musicInfoJson)) }
        val result = request("lyric", platform, info, resultKeyHint = "lyric")?.jsonObject
            ?: throw LxException.ScriptRuntimeError("lyric 未返回有效结果")
        fun field(key: String): String? = (result[key] as? JsonPrimitive)?.contentOrNull
        return LxLyric(
            lyric = field("lyric"),
            tlyric = field("tlyric"),
            rlyric = field("rlyric"),
            lxlyric = field("lxlyric"),
        )
    }

    /** 获取封面地址（local 源支持；其它源按脚本声明） */
    suspend fun getPic(platform: String, musicInfoJson: String): LxPic {
        val info = buildJsonObject { put("musicInfo", parseMusicInfo(musicInfoJson)) }
        val result = request("pic", platform, info, resultKeyHint = "pic")
        val url = extractUrl(result)
            ?: throw LxException.ScriptRuntimeError("pic 未返回有效地址")
        return LxPic(url)
    }

    fun close() {
        if (closed) return
        closed = true
        runCatching { js?.close() }
        js = null
    }

    // ── 内部实现 ──

    /**
     * 统一的 request 调用：校验声明 → 序列化 payload → JS 侧 await → 等待回填。
     *
     * 关键（spike §4.3）：`evaluate()` 不会 await 脚本的 async IIFE，
     * 故用「JS 侧完成后回调 binding + Kotlin CompletableDeferred」取回结果。
     */
    private suspend fun request(
        action: String,
        platform: String,
        info: JsonObject,
        resultKeyHint: String,
    ): kotlinx.serialization.json.JsonElement? {
        checkNotClosed()
        val desc = descriptor ?: throw LxException.ScriptInitFailed("脚本尚未加载，请先调用 load()")

        val declared = desc.sources[platform]
            ?: throw LxException.UnsupportedRequest("脚本未声明源 '$platform'")
        if (!declared.actions.contains(action)) {
            throw LxException.UnsupportedRequest("源 '$platform' 不支持 action '$action'")
        }

        val payload = buildJsonObject {
            put("source", platform)
            put("action", action)
            put("info", info)
        }.toString()

        // 并发安全的唯一槽位：同一实例可同时处理多个请求
        val slotKey = "$resultKeyHint-${nextSlotId()}"
        val deferred = CompletableDeferred<String?>()
        pendingResultsLock.withLock { pendingResults[slotKey] = deferred }

        try {
            val instance = js ?: throw LxException.Closed()
            evalMutex.withLock {
                instance.evaluate<Any?>(LxBridge.requestScript(payload, slotKey))
            }
            // 超时兜底：脚本挂死时不能无限等待
            val raw = try {
                withTimeout(requestTimeoutMs) { deferred.await() }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw LxException.Timeout(
                    "$action 请求超时（${requestTimeoutMs}ms），源 '$platform' 无响应",
                )
            }
            if (raw.isNullOrBlank()) return null
            return runCatching { json.parseToJsonElement(raw) }.getOrNull()
        } finally {
            pendingResultsLock.withLock { pendingResults.remove(slotKey) }
        }
    }

    /** 安装 `__native` 原子能力对象（Kotlin → JS 全部经 JSON 字符串传递，见 spike §4.2） */
    private fun installBindings(instance: QuickJs) {
        instance.define("__native") {
            // 脚本 → 宿主事件（主要用途：inited）
            function("send") { args ->
                val eventName = args.getOrNull(0) as? String
                val payloadJson = args.getOrNull(1) as? String
                if (eventName == "inited") {
                    descriptor = parseInited(payloadJson)
                }
                null
            }

            // 宿主 HTTP：asyncFunction 返回 JS Promise，脚本侧可 await / 走回调
            // （binding 传参一律为 JSON/字符串，见 spike §4.2）
            asyncFunction("http") { args ->
                httpClient.request(args.getOrNull(0) as? String ?: "", args.getOrNull(1) as? String)
            }

            // 结果回填通道
            function("resolve") { args ->
                val key = args.getOrNull(0) as? String
                val value = args.getOrNull(1) as? String
                pendingResults[key]?.complete(value)
                null
            }
            function("reject") { args ->
                val key = args.getOrNull(0) as? String
                val message = args.getOrNull(1) as? String ?: "脚本执行失败"
                pendingResults[key]?.completeExceptionally(LxException.ScriptRuntimeError(message))
                null
            }

            // 脚本 console 输出（bootstrap 的 console.log 落到此处）
            function("log") { args ->
                val level = args.getOrNull(0) as? String ?: "log"
                val message = args.getOrNull(1) as? String ?: ""
                onScriptLog?.invoke(level, message)
                null
            }

            // ── crypto / buffer / zlib ──
            function("md5") { args -> crypto.md5(args.getOrNull(0) as? String ?: "") }
            function("randomBytes") { args -> crypto.randomBytes((args.getOrNull(0) as? Number)?.toInt() ?: 0) }
            function("aesEncrypt") { args ->
                crypto.aesEncrypt(
                    (args.getOrNull(0) as? String)?.toByteArray(Charsets.UTF_8) ?: ByteArray(0),
                    args.getOrNull(1) as? String ?: "aes-128-cbc",
                    args.getOrNull(2) as? String ?: "",
                    args.getOrNull(3) as? String,
                )
            }
            function("rsaEncrypt") { args ->
                crypto.rsaEncrypt(
                    (args.getOrNull(0) as? String)?.toByteArray(Charsets.UTF_8) ?: ByteArray(0),
                    args.getOrNull(1) as? String ?: "",
                )
            }
            function("bufferFrom") { args ->
                crypto.bufferFrom(args.getOrNull(0) as? String ?: "", args.getOrNull(1) as? String ?: "utf8")
                    .toString(Charsets.ISO_8859_1)
            }
            function("bufferToString") { args ->
                crypto.bufferToString(
                    (args.getOrNull(0) as? String)?.toByteArray(Charsets.ISO_8859_1) ?: ByteArray(0),
                    args.getOrNull(1) as? String ?: "utf8",
                )
            }
            // zlib：bootstrap 的 utils.zlib.inflate/deflate 依赖这两个绑定
            // （此前缺失 —— 脚本一旦调用即 TypeError，且被脚本 try/catch 吞掉难察觉）
            function("inflate") { args ->
                (args.getOrNull(0) as? String)?.toByteArray(Charsets.ISO_8859_1)
                    ?.let { crypto.inflate(it) }
                    ?.toString(Charsets.ISO_8859_1)
            }
            function("deflate") { args ->
                (args.getOrNull(0) as? String)?.toByteArray(Charsets.ISO_8859_1)
                    ?.let { crypto.deflate(it) }
                    ?.toString(Charsets.ISO_8859_1)
            }
        }
    }

    /** 解析 inited 事件负载为领域模型 */
    private fun parseInited(payloadJson: String?): LxScriptDescriptor {
        if (payloadJson.isNullOrBlank()) return LxScriptDescriptor(meta = meta)
        val root = runCatching { json.parseToJsonElement(payloadJson).jsonObject }.getOrNull()
            ?: return LxScriptDescriptor(meta = meta)
        val sourcesObj = root["sources"] as? JsonObject ?: return LxScriptDescriptor(meta = meta)
        val sources = sourcesObj.mapNotNull { (key, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            fun str(k: String) = (obj[k] as? JsonPrimitive)?.contentOrNull
            fun list(k: String) = (obj[k] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: emptyList()
            key to LxSourceDeclaration(
                platform = key,
                name = str("name"),
                type = str("type") ?: "music",
                actions = list("actions"),
                qualitys = list("qualitys"),
            )
        }.toMap()
        return LxScriptDescriptor(meta = meta, sources = sources)
    }

    private fun parseMusicInfo(musicInfoJson: String): JsonObject =
        runCatching { json.parseToJsonElement(musicInfoJson).jsonObject }.getOrElse {
            throw LxException.UnsupportedRequest("musicInfo 不是合法 JSON 对象：$musicInfoJson")
        }

    /** 元信息 → JSON 字符串（注入 lx.currentScriptInfo） */
    private fun metaToJson(): String = json.encodeToString(LxScriptMeta.serializer(), meta)

    private fun checkNotClosed() {
        if (closed) throw LxException.Closed()
    }

    /** 直链提取：兼容「纯字符串」与「{ url: "..." } 对象」两种脚本返回风格 */
    private fun extractUrl(element: kotlinx.serialization.json.JsonElement?): String? = when (element) {
        null -> null
        is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonObject -> (element["url"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        else -> null
    }

    private var slotCounter = 0
    private val slotLock = Any()
    private fun nextSlotId(): Int = synchronized(slotLock) { abs(++slotCounter) }
}

/** 洛雪源返回结构不稳定，统一用宽松 Json 配置 */
internal val LxJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

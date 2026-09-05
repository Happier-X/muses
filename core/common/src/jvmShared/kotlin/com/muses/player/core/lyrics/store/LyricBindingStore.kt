package com.muses.player.core.lyrics.store

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class BoundLyricSource { AmlL, Provider }

data class LyricBinding(
    val source: BoundLyricSource,
    val provider: String? = null,
    val resourceValue: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
) {
    fun stableKey(): String = listOf(source.name, provider.orEmpty(), resourceValue).joinToString(":")
}

/**
 * 歌词绑定持久化（任务 09-05-lyrics-kmp X1：去安卓化，原 Android SharedPreferences 版改路径注入 +
 * kotlinx.serialization）。
 *
 * 存储布局：[directory] 下单文件 [StoreFileName]，内容为 `{ "<stableKey>": <binding JSON>, ... }`。
 * 单文件承载全表的原因：stableKey 含 `:` 等字符，不能安全映射为文件名（Windows/桌面端兼容）。
 *
 * binding JSON 与旧 org.json 实现逐字段冻结（由 LyricBindingStoreTest 样本锁定）：字段顺序
 * source/provider/resourceValue/title/artist/durationMs、紧凑输出、provider 为 null 时写 ""、
 * 读取缺省字段按旧 opt 语义回退（provider→null、title/artist→""、durationMs→0）、损坏数据整体返回 null。
 *
 * 调用方注入目录：安卓侧传 filesDir 子目录，桌面侧传等价数据目录。
 */
class LyricBindingStore(private val directory: File) {

    fun read(key: String): LyricBinding? {
        val value = loadEntries()[key] ?: return null
        return runCatching { parseBinding(value) }.getOrNull()
    }

    fun write(key: String, binding: LyricBinding) {
        val entries = LinkedHashMap<String, JsonElement>(loadEntries())
        entries[key] = toJsonElement(binding)
        persist(JsonObject(entries))
    }

    fun clear() {
        persist(JsonObject(emptyMap()))
    }

    private fun parseBinding(value: JsonElement): LyricBinding {
        val obj = value.jsonObject
        return LyricBinding(
            source = BoundLyricSource.valueOf(obj.getValue("source").jsonPrimitive.content),
            provider = obj.optString("provider").takeIf(String::isNotBlank),
            resourceValue = obj.getValue("resourceValue").jsonPrimitive.content,
            title = obj.optString("title"),
            artist = obj.optString("artist"),
            durationMs = obj.optLong("durationMs"),
        )
    }

    private fun toJsonElement(binding: LyricBinding): JsonObject = buildJsonObject {
        put("source", binding.source.name)
        put("provider", binding.provider ?: "")
        put("resourceValue", binding.resourceValue)
        put("title", binding.title)
        put("artist", binding.artist)
        put("durationMs", binding.durationMs)
    }

    private fun loadEntries(): Map<String, JsonElement> {
        val file = storeFile()
        if (!file.exists()) return emptyMap()
        return runCatching { Json.parseToJsonElement(file.readText()).jsonObject }
            .getOrDefault(JsonObject(emptyMap()))
    }

    private fun persist(entries: JsonObject) {
        check(directory.isDirectory || directory.mkdirs()) { "无法创建歌词绑定存储目录：$directory" }
        storeFile().writeText(entries.toString(), Charsets.UTF_8)
    }

    private fun storeFile(): File = File(directory, StoreFileName)

    /** 对齐旧 org.json 的 optString：缺 key/非 primitive 值回退 ""，显式 null 不误读为字面量 "null"。 */
    private fun JsonObject.optString(name: String): String =
        (this[name] as? JsonPrimitive)?.contentOrNull ?: ""

    /** 对齐旧 org.json 的 optLong：缺 key/不可解析回退 0。 */
    private fun JsonObject.optLong(name: String): Long =
        (this[name] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L

    private companion object {
        const val StoreFileName = "muses_lyric_bindings.json"
    }
}

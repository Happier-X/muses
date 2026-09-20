package com.muses.player.core.search.provider

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * 各平台响应结构的宽松取值工具。
 *
 * 逆向接口字段名大小写/类型不稳定（有时字符串有时数字），
 * 故统一经这些扩展安全取用，取不到返回 null 而不抛错。
 */

/** 取字符串（数字也转字符串）；缺失/非原始值返回 null */
internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

/** 取数字；字符串数字也可（部分接口数字以字符串返回） */
internal fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.let { raw ->
        raw.toDoubleOrNull()?.toLong()
    }

internal fun JsonObject.int(key: String): Int? = long(key)?.toInt()

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

/** 按路径逐层取对象：`path("req","data","body")` */
internal fun JsonObject.path(vararg keys: String): JsonObject? {
    var current: JsonObject = this
    for ((index, k) in keys.withIndex()) {
        val next = current[k] as? JsonObject ?: return null
        if (index == keys.lastIndex) return next
        current = next
    }
    return current
}

/** 取字符串数组里的首个非空值（如 singer/artists 数组取名字） */
internal fun JsonArray.firstStr(key: String): String? =
    this.asSequence()
        .mapNotNull { (it as? JsonObject)?.str(key) }
        .firstOrNull { it.isNotBlank() }

/** 拼多个歌手名 */
internal fun JsonArray.joinStr(key: String): String? =
    this.mapNotNull { (it as? JsonObject)?.str(key) }
        .filter { it.isNotBlank() }
        .takeIf { it.isNotEmpty() }
        ?.joinToString("/")

/**
 * 构造音乐信息 JSON（给音源脚本消费）。
 *
 * **同一标识冗余写入多个常见字段名**：自定义源脚本由不同作者编写，
 * 读取字段不统一（songmid / id / hash / mid / copyrightId…）。
 * 冗余可显著提升跨脚本兼容性，代价仅是 JSON 略大。
 *
 * @param ids 标识键值对（如 `"songmid" to "474678847"`），会展开为全部键
 */
internal fun buildMusicInfo(
    vararg ids: Pair<String, String?>,
    name: String? = null,
    singer: String? = null,
    album: String? = null,
    durationSec: Long? = null,
    /** 平台特有字段（如酷狗各音质 hash、albumId），原样写入 */
    extra: Map<String, String> = emptyMap(),
): String {
    // 取首个有效标识值，冗余写入所有常见字段名
    val primary = ids.firstOrNull { !it.second.isNullOrBlank() }?.second
    val aliasKeys = listOf(
        "songmid", "songId", "song_id", "id", "mid", "hash",
        "fileHash", "FileHash", "copyrightId", "copyright_id", "rid", "audioId",
    )
    return buildJsonObject {
        // 显式传入的键（保留原始拼写）
        ids.forEach { (k, v) -> v?.takeIf { it.isNotBlank() }?.let { put(k, it) } }
        // 冗余别名：仅补未显式给出的键
        if (!primary.isNullOrBlank()) {
            aliasKeys.filter { alias -> ids.none { it.first == alias } }
                .forEach { alias -> put(alias, primary) }
        }
        name?.let { put("name", it); put("songName", it); put("title", it) }
        singer?.let { put("singer", it); put("artist", it); put("artists", it) }
        album?.let { put("album", it); put("albumName", it) }
        durationSec?.let { put("interval", it); put("duration", it) }
        extra.forEach { (k, v) -> if (v.isNotBlank()) put(k, v) }
    }.toString()
}

/** 平台展示名到 key 的统一（用于结果模型） */
internal const val PLATFORM_KW = "kw"
internal const val PLATFORM_KG = "kg"
internal const val PLATFORM_TX = "tx"
internal const val PLATFORM_WY = "wy"
internal const val PLATFORM_MG = "mg"

/** 空元素便捷判断 */
internal fun JsonElement?.isMissing(): Boolean = this == null
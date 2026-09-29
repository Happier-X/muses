package com.muses.player.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private const val WEB_DAV_PATHS_PREFIX = "muses:webdav-paths:v1:"

/** 兼容旧版单目录字段，以带版本前缀的 JSON 数组保存同账号下的多个目录。 */
fun encodeWebDavSourcePaths(paths: List<String>): String? {
    val normalized = paths.map(String::trim).filter(String::isNotEmpty).distinct()
    if (normalized.isEmpty()) return null
    if (normalized.size == 1) return normalized.single()
    val json = buildJsonArray { normalized.forEach { add(JsonPrimitive(it)) } }
    return WEB_DAV_PATHS_PREFIX + Json.encodeToString(JsonElement.serializer(), json)
}

/** 旧数据仍按单目录读取；遇到无效的多目录载荷时安全退回单目录。 */
fun decodeWebDavSourcePaths(path: String?): List<String> {
    if (path.isNullOrBlank()) return emptyList()
    if (!path.startsWith(WEB_DAV_PATHS_PREFIX)) return listOf(path)
    return runCatching {
        Json.parseToJsonElement(path.removePrefix(WEB_DAV_PATHS_PREFIX))
            .jsonArray
            .map { it.jsonPrimitive.content }
            .filter(String::isNotBlank)
    }.getOrElse { listOf(path) }
}

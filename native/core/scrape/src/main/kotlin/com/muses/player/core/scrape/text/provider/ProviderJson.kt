package com.muses.player.core.scrape.text.provider

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * provider 响应松散解析辅助（响应结构不稳定，不强类型映射）。
 * 所有取值在结构不符时返回 null，与 Web 可选链 + Array.isArray 防御一致。
 */
internal fun JsonElement?.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content

internal fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

/** 按路径逐层下钻对象字段 */
internal fun JsonObject.at(key: String): JsonElement? = this[key]

/** 逐层安全取对象：obj.data?.song 形式 */
internal fun JsonElement?.path(vararg keys: String): JsonElement? {
    var current = this
    for (key in keys) {
        current = current.asObjectOrNull()?.get(key) ?: return null
    }
    return current
}

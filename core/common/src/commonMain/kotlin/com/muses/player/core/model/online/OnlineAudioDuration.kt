package com.muses.player.core.model.online

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** LX 元数据中的时长单位为秒，也兼容分秒/时分秒文本。 */
fun OnlineTrackRef.expectedDurationMs(): Long? = runCatching {
    val info = Json.parseToJsonElement(musicInfoJson).jsonObject
    listOf("interval", "duration").firstNotNullOfOrNull { key ->
        val value = info[key]?.jsonPrimitive ?: return@firstNotNullOfOrNull null
        value.longOrNull?.takeIf { it > 0 } ?: value.contentOrNull?.split(':')?.takeIf { it.size in 2..3 }
            ?.map { it.toLongOrNull() }?.takeIf { parts ->
                parts.all { it != null && it in 0..999 } && parts.drop(1).all { it!! < 60 }
            }?.fold(0L) { total, part -> total * 60 + part!! }?.takeIf { it > 0 }
    }?.takeIf { it <= Long.MAX_VALUE / 1000 }?.times(1000)
}.getOrNull()

/** 只拒绝明确明显截短的音频，保留原生短曲、时长小幅差异和未知时长。 */
fun isTruncatedOnlineAudio(actualMs: Long?, expectedMs: Long?): Boolean =
    actualMs != null && actualMs > 0 && expectedMs != null && expectedMs >= 90_000 &&
        actualMs + 20_000 < expectedMs && actualMs < expectedMs * 0.75

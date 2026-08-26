package com.muses.player.core.lyrics.provider

import com.muses.player.core.lyrics.crypto.WyCrypto
import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.model.lyrics.OnlineLyricsFormat
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 网易云歌词（规格书 = src/features/lyrics/providers/wy.ts）：
 * 优先 eapi 拿 yrc，否则公开 API 行级 LRC；tlyric 为 timed LRC 译文。
 */

private val WY_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://music.163.com/",
    "Accept" to "application/json,text/plain,*/*",
)

private val WY_EAPI_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36",
    "origin" to "https://music.163.com",
    "Referer" to "https://music.163.com/",
    "Content-Type" to "application/x-www-form-urlencoded",
    "Accept" to "*/*",
)

internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonElement?.asObjArray(): List<JsonObject> =
    (this as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

/** 判断是否为 yrc 逐字体（wy.ts isYrcBody + qrc.ts looksLikeWordLevelBracket） */
private fun looksLikeWordLevelBracket(text: String): Boolean {
    val line = text.split(Regex("\\r?\\n")).firstOrNull { l ->
        l.trim().isNotEmpty() && !l.trim().startsWith("{")
    } ?: return false
    return Regex("^\\[\\d+,\\d+\\]").containsMatchIn(line.trim())
}

private fun isYrcBody(text: String): Boolean {
    if (text.isBlank()) return false
    if (Regex("\\(\\d+,\\d+,\\d+\\)").containsMatchIn(text) && looksLikeWordLevelBracket(text)) return true
    return looksLikeWordLevelBracket(text) && text.contains("(")
}

/** 从歌词响应挑 yrc 优先、lrc 兜底，并携带校验过的译文（wy.ts pickFromBody） */
private fun pickFromBody(body: JsonObject): OnlineLyricsProviderHit? {
    val translationText = body.obj("tlyric")?.str("lyric")?.trim()
    fun <T : OnlineLyricsProviderHit> withTrans(hit: T): T =
        if (!translationText.isNullOrEmpty() && Regex("\\[\\d+:\\d+").containsMatchIn(translationText)) {
            @Suppress("UNCHECKED_CAST")
            hit.copy(translationText = translationText) as T
        } else {
            hit
        }

    val yrc = body.obj("yrc")?.str("lyric")?.trim()
    if (!yrc.isNullOrEmpty() && isYrcBody(yrc)) {
        return withTrans(OnlineLyricsProviderHit(text = yrc, format = OnlineLyricsFormat.YRC))
    }
    val lrc = body.obj("lrc")?.str("lyric")?.trim()
    if (!lrc.isNullOrEmpty() && Regex("\\[\\d+:\\d+").containsMatchIn(lrc)) {
        return withTrans(OnlineLyricsProviderHit(text = lrc, format = OnlineLyricsFormat.LRC))
    }
    return null
}

private fun JsonElement?.path(vararg keys: String): String? {
    var cur: JsonElement? = this
    for (key in keys) {
        cur = (cur as? JsonObject)?.get(key) ?: return null
    }
    return (cur as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
}

private suspend fun searchWySongId(http: LyricsHttp, query: OnlineLyricsQuery): Long? {
    val keyword = buildKeyword(query)
    if (keyword.isEmpty()) return null
    val url = "https://music.163.com/api/search/get/web?s=${java.net.URLEncoder.encode(keyword, "UTF-8")}" +
        "&type=1&offset=0&total=true&limit=10"
    val body = try {
        http.getJson(url, WY_HEADERS) as? JsonObject
    } catch (_: Exception) {
        return null
    } ?: return null
    val songs = body.obj("result")?.obj("songs")?.get("songs")?.asObjArray().orEmpty()
    data class WySong(val id: Long, override val title: String?, override val artist: String?, override val album: String?) : ScoreableHit
    val list = songs.mapNotNull { s ->
        val idPrimitive = s["id"] as? JsonPrimitive ?: return@mapNotNull null
        val id = idPrimitive.content.toDoubleOrNull()?.toLong() ?: return@mapNotNull null
        if (id <= 0) return@mapNotNull null
        WySong(
            id = id,
            title = s.str("name"),
            artist = s.get("artists")?.asObjArray().orEmpty()
                .mapNotNull { it.str("name")?.takeIf(String::isNotEmpty) }.joinToString(" "),
            album = s.obj("album")?.str("name"),
        )
    }
    return pickBest(list, query)?.id
}

private suspend fun fetchWyEapiLyric(http: LyricsHttp, id: Long): JsonObject? {
    val apiPath = "/api/song/lyric/v1"
    // -1：请求翻译/原文/逐字；0 时部分环境不返回 tlyric
    val payload = """{"id":$id,"cp":false,"tv":-1,"lv":-1,"rv":-1,"kv":-1,"yv":1,"ytv":1,"yrv":-1}"""
    return try {
        val params = WyCrypto.buildEapiParams(apiPath, payload)
        http.postJson(
            "https://interface3.music.163.com/eapi/song/lyric/v1",
            "params=$params",
            WY_EAPI_HEADERS,
        ) as? JsonObject
    } catch (_: Exception) {
        null
    }
}

private suspend fun fetchWyPublicLyric(http: LyricsHttp, id: Long): JsonObject? = try {
    val url = "https://music.163.com/api/song/lyric?id=$id&lv=-1&kv=-1&tv=-1&rv=-1&yv=1"
    http.getJson(url, WY_HEADERS) as? JsonObject
} catch (_: Exception) {
    null
}

/** wy 搜索主流程（wy.ts searchWyLyrics）；失败返回 null 由链上下一源承接 */
suspend fun searchWyLyrics(http: LyricsHttp, query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
    val id = searchWySongId(http, query) ?: return null

    fetchWyEapiLyric(http, id)?.let { body ->
        pickFromBody(body)?.let { return it }
    }
    fetchWyPublicLyric(http, id)?.let { body ->
        pickFromBody(body)?.let { return it }
    }
    return null
}

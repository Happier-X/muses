package com.muses.player.core.lyrics.provider

import com.muses.player.core.lyrics.http.LyricsHttp
import com.muses.player.core.lyrics.provider.qrc.QrcDecoder
import com.muses.player.core.model.lyrics.OnlineLyricsFormat
import com.muses.player.core.model.lyrics.OnlineLyricsProviderHit
import com.muses.player.core.model.lyrics.OnlineLyricsQuery
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull

/**
 * QQ 歌词 provider（规格书 = src/features/lyrics/providers/tx.ts）：
 * 优先 GetPlayLyricInfo 加密 QRC（QrcDecoder），失败降级 fcg_query_lyric_new 行级 LRC。
 */

private val TX_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
    "Referer" to "https://y.qq.com/",
    "Accept" to "application/json,text/plain,*/*",
)

private val TX_DESKTOP_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36",
    "Referer" to "https://y.qq.com",
    "Content-Type" to "application/json",
    "Accept" to "application/json,text/plain,*/*",
)

/** tx.ts decodeMaybeBase64：已是 LRC 明文原样返回；否则尝试 base64 解码 */
private fun decodeMaybeBase64(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.contains('[') && Regex("\\[\\d+:\\d+").containsMatchIn(trimmed)) return trimmed
    return runCatching {
        String(java.util.Base64.getDecoder().decode(trimmed), Charsets.UTF_8)
    }.getOrDefault(trimmed)
}

private data class TxHit(
    val mid: String,
    val songId: Long?,
    override val title: String?,
    override val artist: String?,
    override val album: String?,
) : ScoreableHit

private suspend fun searchTxHit(http: LyricsHttp, query: OnlineLyricsQuery): TxHit? {
    val keyword = buildKeyword(query)
    if (keyword.isEmpty()) return null
    val url = "https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp?g_tk=5381&uin=0&format=json" +
        "&inCharset=utf-8&outCharset=utf-8&notice=0&platform=h5&needNewCode=1" +
        "&w=${enc(keyword)}&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0" +
        "&perpage=10&n=10&p=1&remoteplace=txt.mqq.all"

    val body = try {
        http.getJson(url, TX_HEADERS) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }
    val items = body.obj("data")?.obj("song")?.obj("list")?.get("list").asObjArray()
    val list = items.mapNotNull { item ->
        val mid = item.str("songmid")?.trim()?.takeUnless { it.isEmpty() } ?: return@mapNotNull null
        val rawId = (item["songid"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        val songId = rawId?.toDoubleOrNull()?.takeIf { it > 0 }?.toLong()
            ?: rawId?.takeIf { Regex("^\\d+$").containsMatchIn(it) }?.toLongOrNull()
        TxHit(
            mid = mid,
            songId = songId,
            title = item.str("songname"),
            artist = item.get("singer").asObjArray()
                .mapNotNull { it.str("name")?.takeIf(String::isNotEmpty) }.joinToString(" "),
            album = item.str("albumname"),
        )
    }.filter { it.mid.isNotEmpty() }
    return pickBest(list, query)
}

/** tx.ts fetchTxQrc：GetPlayLyricInfo 加密 QRC → QrcDecoder */
private suspend fun fetchTxQrc(http: LyricsHttp, songId: Long): String? {
    val payload = """
        {"comm":{"ct":"19","cv":"1859","uin":"0"},"req":{"method":"GetPlayLyricInfo",
        "module":"music.musichallSong.PlayLyricInfo","param":{"format":"json","crypt":1,"ct":19,
        "cv":1873,"interval":0,"lrc_t":0,"qrc":1,"qrc_t":0,"roma":1,"roma_t":0,"songID":$songId,
        "trans":1,"trans_t":0,"type":-1}}}
    """.trimIndent().replace(Regex("\\s"), "")

    return try {
        val body = http.postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", payload, TX_DESKTOP_HEADERS) as? JsonObject
            ?: return null
        if (body.str("code") != "0") return null
        val req = body.obj("req") ?: return null
        if (req.str("code") != "0") return null
        val hex = req.obj("data")?.str("lyric")?.trim()?.takeUnless { it.isEmpty() } ?: return null
        QrcDecoder.decryptToPlain(hex)
    } catch (_: Exception) {
        null
    }
}

/** tx.ts fetchTxLrcByMid：fcg_query_lyric_new 行级 LRC（JSONP 剥壳 + base64 容错） */
private suspend fun fetchTxLrcByMid(http: LyricsHttp, songmid: String): String? {
    val url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=${enc(songmid)}" +
        "&format=json&nobase64=1&g_tk=5381&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0&platform=yqq.json&needNewCode=0"

    val raw = try {
        http.getText(url, TX_HEADERS)
    } catch (_: Exception) {
        return null
    }

    // 剥 JSONP 外壳：callback( ... ); → ...
    val jsonText = raw.replace(Regex("^\\s*[a-zA-Z0-9_]*\\("), "").replace(Regex("\\)\\s*;?\\s*$"), "")
    val body = try {
        Json.parseToJsonElement(jsonText) as? JsonObject ?: return null
    } catch (_: Exception) {
        return null
    }

    // 若意外拿到明文 qrc
    val qrc = body.str("qrc")
    if (!qrc.isNullOrEmpty() && qrc.contains("(") && qrc.contains("[")) {
        return qrc
    }

    val lyric = decodeMaybeBase64(body.str("lyric").orEmpty())
    if (lyric.isBlank() || !Regex("\\[\\d+:\\d+").containsMatchIn(lyric)) return null
    return lyric
}

/** tx 搜索主流程（tx.ts searchTxLyrics）；失败返回 null 由链上下一源承接 */
suspend fun searchTxLyrics(http: LyricsHttp, query: OnlineLyricsQuery): OnlineLyricsProviderHit? {
    val hit = searchTxHit(http, query) ?: return null

    if (hit.songId != null) {
        fetchTxQrc(http, hit.songId)?.let { qrc ->
            return OnlineLyricsProviderHit(text = qrc, format = OnlineLyricsFormat.QRC)
        }
    }

    val lrc = fetchTxLrcByMid(http, hit.mid) ?: return null
    // 明文 qrc 误入 LRC 接口时
    if (Regex("^\\[\\d+,\\d+\\]", setOf(RegexOption.MULTILINE)).containsMatchIn(lrc) &&
        Regex("\\(\\d+,\\d+\\)").containsMatchIn(lrc)
    ) {
        return OnlineLyricsProviderHit(text = lrc, format = OnlineLyricsFormat.QRC)
    }
    return OnlineLyricsProviderHit(text = lrc, format = OnlineLyricsFormat.LRC)
}

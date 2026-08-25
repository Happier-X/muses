package com.muses.player.core.scrape.cover.provider

import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.normalizeText
import com.muses.player.core.scrape.text.provider.asArrayOrNull
import com.muses.player.core.scrape.text.provider.asObjectOrNull
import com.muses.player.core.scrape.text.provider.asStringOrNull
import com.muses.player.core.scrape.text.provider.path
import com.muses.player.core.scrape.text.provider.urlEncode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// 规格书 = src/features/cover/providers/wy.ts：
// 网易云封面，公开搜索 + song/detail 取 album.picUrl；
// 不用 weapi/eapi 加密；独立实现。

private const val WY_SEARCH = "https://music.163.com/api/search/get/web"
private const val WY_DETAIL = "https://music.163.com/api/song/detail/"

private val WY_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://music.163.com/",
    "Accept" to "application/json,text/plain,*/*",
)

private const val MAX_DETAIL_TRIES = 3

/** wy.ts normalizeCoverUrl：http 升 https */
internal fun normalizeWyCoverUrl(raw: String?): String? {
    val trimmed = raw?.trim()
    if (trimmed.isNullOrEmpty() || !Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
        return null
    }
    return if (trimmed.startsWith("http://")) {
        "https://" + trimmed.substring("http://".length)
    } else {
        trimmed
    }
}

/** wy.ts：typeof item.id === 'number' && item.id > 0 */
private fun wySongId(item: JsonObject): Long? {
    val primitive = item["id"] as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val id = primitive.content.toDoubleOrNull() ?: return null
    return if (id > 0) id.toLong() else null
}

/** wy.ts artistNames：artists[].name 过滤空后空格连接 */
private fun wyArtistNames(item: JsonObject): String =
    item["artists"].asArrayOrNull().orEmpty()
        .mapNotNull { it.asObjectOrNull()?.get("name").asStringOrNull()?.trim()?.ifEmpty { null } }
        .joinToString(" ")

/** wy.ts scoreItem：title=10 / artist=6 / album=3 / 有效歌曲 id +1 */
private fun scoreItem(item: JsonObject, q: CoverScoreInput): Int {
    val title = normalizeText(item["name"].asStringOrNull())
    val artist = normalizeText(wyArtistNames(item))
    val album = normalizeText(item.path("album", "name").asStringOrNull())
    var score = scoreRelated(title, artist, album, q.qTitle, q.qArtist, q.qAlbum)
    if ((wySongId(item) ?: 0L) > 0L) {
        score += 1
    }
    return score
}

/** wy.ts fetchDetailPicUrl：song/detail ids=[id]（JSON.stringify 后整体 encode） */
private suspend fun fetchDetailPicUrl(http: ScrapeHttp, songId: Long): String? {
    // JS JSON.stringify([songId])：songId 为数字 → "[12345]"（无引号）
    val idsJson = "[$songId]"
    val url = "$WY_DETAIL?ids=${urlEncode(idsJson)}"
    val body = http.getJson(url, WY_HEADERS)
    val pic = body.path("songs").asArrayOrNull()?.firstOrNull()
        ?.asObjectOrNull()?.path("album", "picUrl").asStringOrNull()
    return normalizeWyCoverUrl(pic)
}

class WyCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.WY

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val keyword = buildCoverKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // wy.ts：公开搜索接口全参数串原样保留
        val searchUrl =
            "$WY_SEARCH?s=${urlEncode(keyword)}&type=1&offset=0&total=true&limit=10"

        val body = http.getJson(searchUrl, WY_HEADERS)
        val list = body.path("result", "songs").asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull() }
        val withId = list.filter { item -> wySongId(item) != null }
        if (withId.isEmpty()) {
            return null
        }

        val q = CoverScoreInput(query)
        val ranked = withId.sortedByDescending { scoreItem(it, q) }
        for (item in ranked.take(MAX_DETAIL_TRIES)) {
            try {
                // wySongId 已在 withId 过滤保证非空
                val pic = fetchDetailPicUrl(http, wySongId(item)!!)
                if (pic != null) {
                    return pic
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 单条详情失败则试下一条
            }
        }
        return null
    }
}

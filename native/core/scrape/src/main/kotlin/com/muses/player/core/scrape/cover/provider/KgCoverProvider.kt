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
import kotlinx.serialization.json.JsonObject

// 规格书 = src/features/cover/providers/kg.ts：
// 酷狗封面，songsearch.kugou.com 公开搜索接口返回的 Image 字段；
// 独立实现，不拷贝 GPL 项目源码。

private const val KG_SEARCH = "https://songsearch.kugou.com/song_search_v2"

private val KG_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://www.kugou.com/",
    "Accept" to "application/json,text/plain,*/*",
)

/** kg.ts KG_COVER_SIZE */
private const val KG_COVER_SIZE = "480"

/** kg.ts normalizeCoverUrl：{size} → 480，http 升 https */
internal fun normalizeKgCoverUrl(raw: String?): String? {
    val trimmed = raw?.trim()
    if (trimmed.isNullOrEmpty() || !Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(trimmed)) {
        return null
    }
    val withSize = if (trimmed.contains("{size}")) trimmed.replace("{size}", KG_COVER_SIZE) else trimmed
    return if (withSize.startsWith("http://")) {
        "https://" + withSize.substring("http://".length)
    } else {
        withSize
    }
}

/** kg.ts scoreItem：title（SongName 优先，回退 OriSongName）=10 / artist=6 / album=3 / 有可规范化封面 +1 */
private fun scoreItem(item: JsonObject, q: CoverScoreInput): Int {
    val title = normalizeText(
        item["SongName"].asStringOrNull()?.takeIf { it.isNotEmpty() }
            ?: item["OriSongName"].asStringOrNull(),
    )
    val artist = normalizeText(item["SingerName"].asStringOrNull())
    val album = normalizeText(item["AlbumName"].asStringOrNull())
    var score = scoreRelated(title, artist, album, q.qTitle, q.qArtist, q.qAlbum)
    if (normalizeKgCoverUrl(item["Image"].asStringOrNull()) != null) {
        score += 1
    }
    return score
}

class KgCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.KG

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val keyword = buildCoverKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // kg.ts：搜索接口全参数串原样保留
        val url =
            "$KG_SEARCH?keyword=${urlEncode(keyword)}" +
                "&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter" +
                "&filter=2&iscorrection=1&privilege_filter=0&area_code=1"

        val body = http.getJson(url, KG_HEADERS)
        val list = body.path("data", "lists").asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull() }
        val withCover = list.filter { item -> normalizeKgCoverUrl(item["Image"].asStringOrNull()) != null }
        if (withCover.isEmpty()) {
            return null
        }

        val q = CoverScoreInput(query)
        val ranked = withCover.sortedByDescending { scoreItem(it, q) }
        return normalizeKgCoverUrl(ranked.first()["Image"].asStringOrNull())
    }
}

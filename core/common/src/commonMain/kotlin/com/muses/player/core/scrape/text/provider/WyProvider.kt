package com.muses.player.core.scrape.text.provider

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.buildKeyword
import com.muses.player.core.scrape.text.pickBestHit

// 规格书 = src/features/metadata/providers/wy.ts

private const val WY_SEARCH = "https://music.163.com/api/search/get/web"

// wy.ts WY_HEADERS：UA / Referer 原样保留
private val WY_HEADERS = mapOf(
    "User-Agent" to
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://music.163.com/",
    "Accept" to "application/json,text/plain,*/*",
)

class WyProvider(private val http: ScrapeHttp) : TextMetaProvider {

    override val id: OnlineTextSource = OnlineTextSource.WY

    override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
        val keyword = buildKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // wy.ts URL：参数原样保留（type=1 歌曲搜索）
        val url = "$WY_SEARCH?s=${urlEncode(keyword)}&type=1&offset=0&total=true&limit=10"
        val body = http.getJson(url, WY_HEADERS)
        val list = body.path("result", "songs").asArrayOrNull()

        val hits = (list ?: emptyList()).mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            // artists[].name 以空格连接（wy.ts：filter(Boolean).join(' ')）
            val artist = item["artists"].asArrayOrNull()
                ?.mapNotNull { it.asObjectOrNull()?.get("name").asStringOrNull()?.trim()?.ifEmpty { null } }
                ?.joinToString(" ")
            TextMetaHit(
                title = item["name"].asStringOrNull()?.trim()?.ifEmpty { null },
                artist = artist?.ifEmpty { null },
                album = item.path("album", "name").asStringOrNull()?.trim()?.ifEmpty { null },
                source = OnlineTextSource.WY,
            )
        }.filter { it.artist != null || it.album != null }

        return pickBestHit(hits, query)
    }
}

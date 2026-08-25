package com.muses.player.core.scrape.text.provider

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.buildKeyword
import com.muses.player.core.scrape.text.pickBestHit

// 规格书 = src/features/metadata/providers/kg.ts

private const val KG_SEARCH = "https://songsearch.kugou.com/song_search_v2"

// kg.ts KG_HEADERS：UA / Referer 原样保留
private val KG_HEADERS = mapOf(
    "User-Agent" to
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://www.kugou.com/",
    "Accept" to "application/json,text/plain,*/*",
)

class KgProvider(private val http: ScrapeHttp) : TextMetaProvider {

    override val id: OnlineTextSource = OnlineTextSource.KG

    override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
        val keyword = buildKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // kg.ts URL：全参数串原样保留（clientver 为空串参数）
        val url =
            "$KG_SEARCH?keyword=${urlEncode(keyword)}" +
                "&page=1&pagesize=10&userid=0&clientver=&platform=WebFilter" +
                "&filter=2&iscorrection=1&privilege_filter=0&area_code=1"

        val body = http.getJson(url, KG_HEADERS)
        val list = body.path("data", "lists").asArrayOrNull()

        val hits = (list ?: emptyList()).mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            // kg.ts：title 取 SongName，缺省回退 OriSongName（|| 语义）
            val title = (item["SongName"].asStringOrNull()?.ifEmpty { null }
                ?: item["OriSongName"].asStringOrNull())?.trim()?.ifEmpty { null }
            TextMetaHit(
                title = title,
                artist = item["SingerName"].asStringOrNull()?.trim()?.ifEmpty { null },
                album = item["AlbumName"].asStringOrNull()?.trim()?.ifEmpty { null },
                source = OnlineTextSource.KG,
            )
        }.filter { it.artist != null || it.album != null }

        return pickBestHit(hits, query)
    }
}

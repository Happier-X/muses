package com.muses.player.core.scrape.text.provider

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.buildKeyword
import com.muses.player.core.scrape.text.pickBestHit

// 规格书 = src/features/metadata/providers/mg.ts

private const val MIGU_SEARCH = "https://m.music.migu.cn/migu/remoting/scr_search_tag"

// mg.ts MIGU_HEADERS：UA / Referer 原样保留
private val MIGU_HEADERS = mapOf(
    "User-Agent" to
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://m.music.migu.cn/",
    "Accept" to "application/json,text/plain,*/*",
)

class MgProvider(private val http: ScrapeHttp) : TextMetaProvider {

    override val id: OnlineTextSource = OnlineTextSource.MG

    override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
        val keyword = buildKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // mg.ts URL：参数原样保留（type=2 歌曲搜索）
        val url = "$MIGU_SEARCH?rows=10&type=2&keyword=${urlEncode(keyword)}&pgc=1"
        val body = http.getJson(url, MIGU_HEADERS)
        val list = body.path("musics").asArrayOrNull()

        val hits = (list ?: emptyList()).mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            TextMetaHit(
                title = item["songName"].asStringOrNull()?.trim()?.ifEmpty { null },
                artist = item["singerName"].asStringOrNull()?.trim()?.ifEmpty { null },
                album = item["albumName"].asStringOrNull()?.trim()?.ifEmpty { null },
                source = OnlineTextSource.MG,
            )
        }.filter { it.artist != null || it.album != null }

        return pickBestHit(hits, query)
    }
}

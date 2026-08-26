package com.muses.player.core.scrape.text.provider

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.buildKeyword
import com.muses.player.core.scrape.text.pickBestHit

// 规格书 = src/features/metadata/providers/tx.ts

private const val TX_SEARCH = "https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp"

// tx.ts TX_HEADERS：UA / Referer 原样保留
private val TX_HEADERS = mapOf(
    "User-Agent" to
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
    "Referer" to "https://y.qq.com/",
    "Accept" to "application/json,text/plain,*/*",
)

class TxProvider(private val http: ScrapeHttp) : TextMetaProvider {

    override val id: OnlineTextSource = OnlineTextSource.TX

    override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
        val keyword = buildKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // tx.ts URL：全参数串原样保留
        val url =
            "$TX_SEARCH?g_tk=5381&uin=0&format=json&inCharset=utf-8&outCharset=utf-8" +
                "&notice=0&platform=h5&needNewCode=1&w=${urlEncode(keyword)}" +
                "&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0" +
                "&perpage=10&n=10&p=1&remoteplace=txt.mqq.all"

        val body = http.getJson(url, TX_HEADERS)
        val list = body.path("data", "song", "list").asArrayOrNull()

        val hits = (list ?: emptyList()).mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            // singer[].name 以空格连接（tx.ts：filter(Boolean).join(' ')）
            val artist = item["singer"].asArrayOrNull()
                ?.mapNotNull { it.asObjectOrNull()?.get("name").asStringOrNull()?.trim()?.ifEmpty { null } }
                ?.joinToString(" ")
            TextMetaHit(
                title = item["songname"].asStringOrNull()?.trim()?.ifEmpty { null },
                artist = artist?.ifEmpty { null },
                album = item["albumname"].asStringOrNull()?.trim()?.ifEmpty { null },
                source = OnlineTextSource.TX,
            )
        }.filter { it.artist != null || it.album != null }

        return pickBestHit(hits, query)
    }
}

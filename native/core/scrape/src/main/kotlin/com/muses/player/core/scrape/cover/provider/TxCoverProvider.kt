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

// 规格书 = src/features/cover/providers/tx.ts：
// QQ 音乐封面，移动端 search_for_qq_cp 取 albummid，拼 y.gtimg.cn 封面；
// 不用桌面签名搜索；独立实现。

private const val TX_SEARCH = "https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp"

private val TX_HEADERS = mapOf(
    "User-Agent" to
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
    "Referer" to "https://y.qq.com/",
    "Accept" to "application/json,text/plain,*/*",
)

/** tx.ts albumCoverUrl：mid 空 / '空' / '0' 视为无封面 */
internal fun txAlbumCoverUrl(albummid: String?): String? {
    val mid = albummid?.trim()
    if (mid.isNullOrEmpty() || mid == "空" || mid == "0") {
        return null
    }
    return "https://y.gtimg.cn/music/photo_new/T002R500x500M000$mid.jpg"
}

/** tx.ts singerNames：singer[].name 过滤空后空格连接 */
private fun txSingerNames(item: JsonObject): String =
    item["singer"].asArrayOrNull().orEmpty()
        .mapNotNull { it.asObjectOrNull()?.get("name").asStringOrNull()?.trim()?.ifEmpty { null } }
        .joinToString(" ")

/** tx.ts scoreItem：title=10 / artist=6 / album=3 / 有可拼封面 +1 */
private fun scoreItem(item: JsonObject, query: OnlineCoverQuery, q: CoverScoreInput): Int {
    val title = normalizeText(item["songname"].asStringOrNull())
    val artist = normalizeText(txSingerNames(item))
    val album = normalizeText(item["albumname"].asStringOrNull())
    var score = scoreRelated(title, artist, album, q.qTitle, q.qArtist, q.qAlbum)
    if (txAlbumCoverUrl(item["albummid"].asStringOrNull()) != null) {
        score += 1
    }
    return score
}

class TxCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.TX

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val keyword = buildCoverKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // tx.ts：search_for_qq_cp 全参数串原样保留
        val url =
            "$TX_SEARCH?g_tk=5381&uin=0&format=json&inCharset=utf-8&outCharset=utf-8" +
                "&notice=0&platform=h5&needNewCode=1&w=${urlEncode(keyword)}" +
                "&zhidaqu=1&catZhida=1&t=0&flag=1&ie=utf-8&sem=1&aggr=0" +
                "&perpage=10&n=10&p=1&remoteplace=txt.mqq.all"

        val body = http.getJson(url, TX_HEADERS)
        val list = body.path("data", "song", "list").asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull() }
        val withCover = list.filter { item -> txAlbumCoverUrl(item["albummid"].asStringOrNull()) != null }
        if (withCover.isEmpty()) {
            return null
        }

        val q = CoverScoreInput(query)
        val ranked = withCover.sortedByDescending { scoreItem(it, query, q) }
        return txAlbumCoverUrl(ranked.first()["albummid"].asStringOrNull())
    }
}

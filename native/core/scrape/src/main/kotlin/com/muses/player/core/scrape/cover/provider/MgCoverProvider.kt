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

// 规格书 = src/features/cover/providers/mg.ts：
// 咪咕封面，m.music.migu.cn 移动搜索接口返回的 cover 字段；
// 不依赖 jadeite 签名链路，也不拷贝 GPL 项目源码。

private const val MIGU_SEARCH = "https://m.music.migu.cn/migu/remoting/scr_search_tag"

private val MIGU_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
    "Referer" to "https://m.music.migu.cn/",
    "Accept" to "application/json,text/plain,*/*",
)

/** mg.ts normalizeCoverUrl：http 升 https */
internal fun normalizeMgCoverUrl(raw: String?): String? {
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

/** mg.ts scoreItem：title=10 / artist=6 / album=3 / 有可规范化封面 +1 */
private fun scoreItem(item: JsonObject, q: CoverScoreInput): Int {
    val title = normalizeText(item["songName"].asStringOrNull())
    val artist = normalizeText(item["singerName"].asStringOrNull())
    val album = normalizeText(item["albumName"].asStringOrNull())
    var score = scoreRelated(title, artist, album, q.qTitle, q.qArtist, q.qAlbum)
    if (normalizeMgCoverUrl(item["cover"].asStringOrNull()) != null) {
        score += 1
    }
    return score
}

class MgCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.MG

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val keyword = buildCoverKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // mg.ts：移动搜索接口全参数串原样保留
        val url =
            "$MIGU_SEARCH?rows=10&type=2&keyword=${urlEncode(keyword)}&pgc=1"

        val body = http.getJson(url, MIGU_HEADERS)
        val list = body.path("musics").asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull() }
        val withCover = list.filter { item -> normalizeMgCoverUrl(item["cover"].asStringOrNull()) != null }
        if (withCover.isEmpty()) {
            return null
        }

        val q = CoverScoreInput(query)
        val ranked = withCover.sortedByDescending { scoreItem(it, q) }
        return normalizeMgCoverUrl(ranked.first()["cover"].asStringOrNull())
    }
}

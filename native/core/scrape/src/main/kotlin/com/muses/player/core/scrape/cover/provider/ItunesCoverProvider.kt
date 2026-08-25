package com.muses.player.core.scrape.cover.provider

import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.normalizeText
import com.muses.player.core.scrape.text.provider.asArrayOrNull
import com.muses.player.core.scrape.text.provider.asObjectOrNull
import com.muses.player.core.scrape.text.provider.path
import com.muses.player.core.scrape.text.provider.urlEncode
import com.muses.player.core.scrape.text.provider.asStringOrNull
import kotlinx.serialization.json.JsonObject

// 规格书 = src/features/cover/providers/itunes.ts

private val ITUNES_HEADERS = mapOf(
    "Accept" to "application/json",
)

/** itunes.ts enlargeArtworkUrl：artworkUrl100 形如 .../100x100bb.jpg → 放大到 600 */
internal fun enlargeItunesArtworkUrl(url: String): String =
    // itunes.ts 正则带前导斜杠：/\/\d+x\d+([a-z]*)\./i（替换含斜杠，避免双斜杠）
    Regex("""/\d+x\d+([a-z]*)\.""", RegexOption.IGNORE_CASE)
        // JS replace 非全局：仅替换第一处
        .replaceFirst(url, "/600x600$1.")

/** itunes.ts scoreResult：title=10 / artist=6 / album=3 / 有 artworkUrl100 +1 */
private fun scoreResult(item: JsonObject, q: CoverScoreInput): Int {
    val title = normalizeText(item["trackName"].asStringOrNull())
    val artist = normalizeText(item["artistName"].asStringOrNull())
    val album = normalizeText(item["collectionName"].asStringOrNull())
    var score = scoreRelated(title, artist, album, q.qTitle, q.qArtist, q.qAlbum)
    if (!item["artworkUrl100"].asStringOrNull().isNullOrEmpty()) {
        score += 1
    }
    return score
}

class ItunesCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.ITUNES

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val term = buildCoverKeyword(query)
        if (term.isEmpty()) {
            return null
        }

        // itunes.ts：搜索接口全参数串原样保留
        val url =
            "https://itunes.apple.com/search?term=${urlEncode(term)}" +
                "&entity=song&limit=5"

        val body = http.getJson(url, ITUNES_HEADERS)

        // itunes.ts：results 过滤有 artworkUrl100 的条目（Boolean(空串) 为假）
        val results = body.path("results").asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull() }
            .filter { !it["artworkUrl100"].asStringOrNull().isNullOrEmpty() }
        if (results.isEmpty()) {
            return null
        }

        val q = CoverScoreInput(query)
        val ranked = results.sortedByDescending { scoreResult(it, q) }
        val best = ranked.first()
        val bestArtwork = best["artworkUrl100"].asStringOrNull()
        if (bestArtwork.isNullOrEmpty() || scoreResult(best, q) < 10) {
            // 至少要求标题有一定相关性；全无关时放弃，交给回退源
            if (bestArtwork.isNullOrEmpty()) {
                return null
            }
            // 若标题完全对不上，仍允许最高分结果（宽松）仅当唯一结果
            if (scoreResult(best, q) < 1 && results.size > 1) {
                return null
            }
        }

        return enlargeItunesArtworkUrl(bestArtwork!!)
    }
}

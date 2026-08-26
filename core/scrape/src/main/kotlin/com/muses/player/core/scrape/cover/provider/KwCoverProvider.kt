package com.muses.player.core.scrape.cover.provider

import com.muses.player.core.scrape.cover.OnlineCoverQuery
import com.muses.player.core.scrape.cover.OnlineCoverSource
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.provider.asArrayOrNull
import com.muses.player.core.scrape.text.provider.asObjectOrNull
import com.muses.player.core.scrape.text.provider.asStringOrNull
import com.muses.player.core.scrape.text.provider.buildKwSearchUrl
import com.muses.player.core.scrape.text.provider.urlEncode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// 规格书 = src/features/cover/providers/kw.ts：
// 移植自 any-listen-extension-online-metadata（Apache-2.0）的 musicSearch + getPic 最小链路，
// 宿主 request 替换为 ScrapeHttp.getText。

private const val UA_MOZILLA = "Mozilla/5.0"

private val MUSICRID_PREFIX = Regex("^MUSIC_", RegexOption.IGNORE_CASE)

/** kw.ts extractMusicId：MUSICRID 去 ^MUSIC_ 前缀，空串视为无 id */
private fun extractMusicId(musicrid: String?): String? {
    val rid = musicrid?.trim()
    if (rid.isNullOrEmpty()) {
        return null
    }
    return rid.replace(MUSICRID_PREFIX, "").ifEmpty { null }
}

private val HTTP_URL_START = Regex("^https?://", RegexOption.IGNORE_CASE)

/** kw.ts normalizePicUrl：kwcdn → kuwo.cn 并升 https；其余 http 升 https */
internal fun normalizeKwPicUrl(body: String): String? {
    val trimmed = body.trim()
    if (!HTTP_URL_START.containsMatchIn(trimmed)) {
        return null
    }
    var url = trimmed
    // 扩展侧：kwcdn → kuwo.cn 并升 https
    url =
        if (url.startsWith("http://") && url.contains(".kwcdn.kuwo.cn")) {
            url.replace(".kwcdn.kuwo.cn", ".kuwo.cn").replaceFirst("http://", "https://")
        } else if (url.startsWith("http://")) {
            url.replaceFirst("http://", "https://")
        } else {
            url
        }
    return url
}

class KwCoverProvider(private val http: ScrapeHttp) : CoverProvider {

    override val id: OnlineCoverSource = OnlineCoverSource.KW

    override suspend fun searchCoverUrl(query: OnlineCoverQuery): String? {
        val keyword = buildCoverKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // kw.ts：httpGetText + UA Mozilla/5.0（搜索 URL 与文本链 kw 同串，直接复用）
        val searchRaw = http.getText(
            buildKwSearchUrl(keyword),
            mapOf(
                "User-Agent" to UA_MOZILLA,
                "Accept" to "application/json,text/plain,*/*",
            ),
        )

        // kw.ts parseKwSearch：JSON 解析失败 catch 后返回空列表
        val list: List<JsonObject> = try {
            Json.parseToJsonElement(searchRaw).asObjectOrNull()
                ?.get("abslist")?.asArrayOrNull()
                ?.mapNotNull { it.asObjectOrNull() }
                ?: emptyList()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

        // kw.ts：list.find(extractMusicId 有值) 的第一个 id
        val musicId = list.firstNotNullOfOrNull { item -> extractMusicId(item["MUSICRID"].asStringOrNull()) }
            ?: return null

        // kw.ts getPic：artistpicserver pic.web 全参数串原样保留
        val picRaw = http.getText(
            "https://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=500&size=500&rid=" +
                urlEncode(musicId),
            mapOf(
                "User-Agent" to UA_MOZILLA,
                "Accept" to "text/plain,*/*",
            ),
        )
        return normalizeKwPicUrl(picRaw)
    }
}

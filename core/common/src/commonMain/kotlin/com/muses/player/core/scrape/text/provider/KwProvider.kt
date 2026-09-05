package com.muses.player.core.scrape.text.provider

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.text.TextMetaProvider
import com.muses.player.core.scrape.text.buildKeyword
import com.muses.player.core.scrape.text.pickBestHit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

// 规格书 = src/features/metadata/providers/kw.ts

private const val UA_MOZILLA = "Mozilla/5.0"

/** kw 搜索 URL（kw.ts buildSearchUrl：全参数串原样保留） */
internal fun buildKwSearchUrl(keyword: String): String =
    "https://search.kuwo.cn/r.s?client=kt&all=${urlEncode(keyword)}" +
        "&pn=0&rn=10&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1" +
        "&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012" +
        "&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"

/**
 * JS encodeURIComponent 对齐：空格转 %20（URLEncoder 会转成 +，需替换）。
 * W2 上收：commonMain 无 java.net.URLEncoder，下沉为 expect/actual
 * （android/jvm 两端 actual 均为 `URLEncoder.encode(value, "UTF-8")` + `+→%20`，
 * 用 charset 名重载以兼容 minSdk 26（Charset 重载需 API 33），行为冻结）。
 * public 可见性：core:scrape 留守的写回链（SongFileWriters）仍复用此函数（spec 契约）。
 */
expect fun urlEncode(value: String): String

class KwProvider(private val http: ScrapeHttp) : TextMetaProvider {

    override val id: OnlineTextSource = OnlineTextSource.KW

    override suspend fun search(query: OnlineTextQuery): TextMetaHit? {
        val keyword = buildKeyword(query)
        if (keyword.isEmpty()) {
            return null
        }

        // kw.ts：httpGetText + UA Mozilla/5.0
        val raw = http.getText(
            buildKwSearchUrl(keyword),
            mapOf(
                "User-Agent" to UA_MOZILLA,
                "Accept" to "application/json,text/plain,*/*",
            ),
        )

        // kw.ts：JSON.parse 失败 catch 后返回 null（其余源由 matcher 归为 network）
        val body: JsonObject = try {
            Json.parseToJsonElement(raw).jsonObject
        } catch (_: Exception) {
            return null
        }
        val list = body["abslist"].asArrayOrNull()

        val hits = (list ?: emptyList()).mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            TextMetaHit(
                title = item["SONGNAME"].asStringOrNull()?.trim()?.ifEmpty { null },
                artist = item["ARTIST"].asStringOrNull()?.trim()?.ifEmpty { null },
                album = item["ALBUM"].asStringOrNull()?.trim()?.ifEmpty { null },
                source = OnlineTextSource.KW,
            )
        }.filter { it.artist != null || it.album != null }

        return pickBestHit(hits, query)
    }
}

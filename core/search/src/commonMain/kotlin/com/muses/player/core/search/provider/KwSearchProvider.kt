package com.muses.player.core.search.provider

import com.muses.player.core.search.OnlineSearchException
import com.muses.player.core.search.OnlineSearchPage
import com.muses.player.core.search.OnlineSearchProvider
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.http.SearchHttp
import kotlinx.coroutines.CancellationException

/**
 * 酷我音乐搜索。
 *
 * 接口：`https://search.kuwo.cn/r.s`（GET，**无需签名**，匿名可用）
 * 参数：`all`=关键词、`pn`=页码(**0-based**)、`rn`=每页数、`ft`=music、`rformat`=json、`pcjson`=1
 *
 * 响应：`abslist[]`，标识字段 `MUSICRID`（形如 `MUSIC_474678847`，取数字部分给脚本）。
 *
 * 注意：该接口返回**单引号伪 JSON**，由 [SearchHttp.parseObject] 统一兜底转换。
 */
class KwSearchProvider(
    private val http: SearchHttp = SearchHttp(),
) : OnlineSearchProvider {

    override val platform: String = PLATFORM_KW
    override val displayName: String = "酷我音乐"

    override suspend fun search(keyword: String, page: Int, pageSize: Int): OnlineSearchPage {
        // 酷我页码从 0 开始
        val pn = (page - 1).coerceAtLeast(0)
        val url = buildString {
            append("https://search.kuwo.cn/r.s?")
            append("pn=").append(pn)
            append("&rn=").append(pageSize)
            append("&all=").append(urlEncode(keyword))
            append("&ft=music&newsearch=1&alflac=1&itemset=web_2013&client=kt&cluster=0")
            append("&vermerge=1&rformat=json&encoding=utf8&show_copyright_off=1&pcmp4=1")
            append("&ver=mbox&plat=pc&vipver=MUSIC_9.1.1.2_BCS2&newver=1&issubtitle=1&pcjson=1")
        }
        val text = try {
            http.getText(url, referer = "https://www.kuwo.cn/")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineSearchException(platform, "酷我搜索请求失败：${e.message}", e)
        }

        val root = http.parseObject(text)
        val list = root.arr("abslist")
            ?: throw OnlineSearchException(platform, "酷我搜索返回结构异常")
        val total = root.long("TOTAL")
        val results = list.mapNotNull { element ->
            val item = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            val rid = item.str("MUSICRID")?.substringAfterLast('_')?.takeIf { it.isNotBlank() }
                ?: item.str("DC_TARGETID")
                ?: return@mapNotNull null
            val name = item.str("SONGNAME") ?: return@mapNotNull null
            val artist = item.str("ARTIST")
            val album = item.str("ALBUM")
            val durationSec = item.long("DURATION")
            OnlineSearchResult(
                platform = platform,
                songId = rid,
                name = name,
                artist = artist,
                album = album,
                durationMs = durationSec?.times(1000),
                coverUrl = item.str("web_albumpic_short")?.let { path ->
                    if (path.startsWith("http")) path else "https://img1.kuwo.cn/star/albumcover/$path"
                },
                musicInfoJson = buildMusicInfo(
                    "rid" to rid,
                    "songmid" to rid,
                    name = name,
                    singer = artist,
                    album = album,
                    durationSec = durationSec,
                ),
            )
        }
        val hasMore = when {
            total != null && total > 0 -> pn * pageSize + results.size < total
            else -> results.size >= pageSize
        }
        return OnlineSearchPage(platform, keyword, page, results, hasMore)
    }
}

/** 极简 URL 编码（commonMain 无 java.net；仅编码非保留字符之外的部分） */
internal fun urlEncode(value: String): String = buildString {
    for (b in value.toByteArray(Charsets.UTF_8)) {
        val c = b.toInt().toChar()
        if (c.isLetterOrDigit() && b.toInt() in 0..127 || c in "-_.~") {
            append(c)
        } else {
            append('%')
            append(HEX_UPPER[(b.toInt() shr 4) and 0x0F])
            append(HEX_UPPER[b.toInt() and 0x0F])
        }
    }
}

private const val HEX_UPPER = "0123456789ABCDEF"
package com.muses.player.core.search.provider

import com.muses.player.core.search.OnlineChart
import com.muses.player.core.search.OnlineChartException
import com.muses.player.core.search.OnlineChartPage
import com.muses.player.core.search.OnlineChartProvider
import com.muses.player.core.search.OnlineSearchResult
import com.muses.player.core.search.http.SearchHttp
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/**
 * 酷狗音乐排行榜。
 *
 * 接口（移动端开放接口，无需签名/登录）：
 * - 榜单列表：`mobilecdnbj.kugou.com/api/v3/rank/list`（`showtype=2` 拿榜单分类）→ `data.info[]`
 * - 榜单歌曲：`mobilecdnbj.kugou.com/api/v3/rank/song`（`rankid` + `page`/`pagesize`）→ `data.info[]`
 *
 * 实测：榜单 56 个（`rankid` 如 8888=TOP500、85897=国潮音乐榜）。
 * 歌曲标识取 `hash`（酷狗源脚本通用字段），并冗余写入 `album_id`/`audio_id` 供脚本取封面/鉴权。
 * 注意：该接口**不返回专辑名**（无 `album_name` 字段），故 `album` 为 null。
 */
class KgChartProvider(
    private val http: SearchHttp = SearchHttp(),
) : OnlineChartProvider {

    override val platform: String = PLATFORM_KG
    override val displayName: String = "酷狗音乐"

    private val host = "http://mobilecdnbj.kugou.com/api/v3"

    override suspend fun charts(): List<OnlineChart> {
        val url = "$host/rank/list?version=9108&plat=0&showtype=2&parentid=0&apiver=2&pagesize=100"
        val text = try {
            http.getText(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineChartException(platform, "酷狗榜单列表请求失败：${e.message}", e)
        }

        val root = http.parseObject(text)
        val list = root.obj("data")?.arr("info")
            ?: throw OnlineChartException(platform, "酷狗榜单列表结构异常")

        return list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val rankId = item.long("rankid") ?: return@mapNotNull null
            val name = item.str("rankname") ?: return@mapNotNull null
            OnlineChart(
                platform = platform,
                chartId = rankId.toString(),
                name = name,
                // 酷狗图片 URL 带 {size} 占位符，列表卡用小图
                coverUrl = item.str("banner_9")?.replace("{size}", "240"),
                updateInfo = item.str("update_frequency"),
            )
        }.distinctBy { it.chartId }
    }

    override suspend fun chartSongs(chartId: String, page: Int, pageSize: Int): OnlineChartPage {
        val rankId = chartId.toLongOrNull()
            ?: throw OnlineChartException(platform, "酷狗榜单标识异常：$chartId")
        val safePage = page.coerceAtLeast(1)
        val url = "$host/rank/song?version=9108&plat=0&pagesize=$pageSize" +
            "&page=$safePage&rankid=$rankId"

        val text = try {
            http.getText(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineChartException(platform, "酷狗榜单歌曲请求失败：${e.message}", e)
        }

        val root = http.parseObject(text)
        val data = root.obj("data")
            ?: throw OnlineChartException(platform, "酷狗榜单歌曲结构异常")
        val list = data.arr("info")
            ?: return OnlineChartPage(platform, chartId, page, emptyList(), false)

        val results = list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val hash = item.str("hash") ?: return@mapNotNull null
            val name = item.str("songname") ?: return@mapNotNull null
            // 歌手在 authors[]（单数 singername 在部分榜单返回中存在，兜底取）
            val singer = item.arr("authors")?.joinStr("author_name") ?: item.str("singername")
            val albumId = item.str("album_id")
            val audioId = item.long("audio_id")
            // duration 单位为秒
            val durationSec = item.long("duration")

            OnlineSearchResult(
                platform = platform,
                songId = hash,
                name = name,
                artist = singer,
                album = null,
                durationMs = durationSec?.times(1000),
                coverUrl = item.str("album_sizable_cover")?.replace("{size}", "240"),
                musicInfoJson = buildMusicInfo(
                    "hash" to hash,
                    name = name,
                    singer = singer,
                    durationSec = durationSec,
                    extra = buildMap {
                        albumId?.let { put("album_id", it); put("albumId", it) }
                        audioId?.let { put("audio_id", it.toString()); put("audioId", it.toString()) }
                        item.str("sqhash")?.let { put("sqhash", it) }
                        item.str("320hash")?.let { put("320hash", it) }
                    },
                ),
            )
        }
        // total 可用时以它为准（更准），否则按「返回满页」推断
        val total = data.long("total")
        val hasMore = total?.let { safePage.toLong() * pageSize < it } ?: (results.size >= pageSize)
        return OnlineChartPage(platform, chartId, page, results, hasMore)
    }
}

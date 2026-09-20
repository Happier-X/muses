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
 * 网易云音乐排行榜。
 *
 * 接口（网页版老接口，匿名可用）：
 * - 榜单列表：`music.163.com/api/toplist` → `list[]`
 * - 榜单歌曲：`music.163.com/api/playlist/detail?id=<榜单id>` → `result.tracks[]`
 *
 * 网易的「榜单」本质是官方歌单（榜单 id 即歌单 id，如 19723756=飙升榜），
 * 故详情走歌单接口；该接口**一次返回全量**（实测 500+ 首），
 * 因此分页在本地切片完成，无需向平台请求第二页。
 *
 * 字段兼容：详情同时存在新旧两套命名（旧 `artists`/`album`/`duration`，新 `ar`/`al`/`dt`），
 * 两套都兜，避免平台灰度切换时解析失败。
 */
class WyChartProvider(
    private val http: SearchHttp = SearchHttp(),
) : OnlineChartProvider {

    override val platform: String = PLATFORM_WY
    override val displayName: String = "网易云音乐"

    override suspend fun charts(): List<OnlineChart> {
        val text = try {
            http.getText("https://music.163.com/api/toplist", referer = "https://music.163.com/")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineChartException(platform, "网易榜单列表请求失败：${e.message}", e)
        }

        val root = http.parseObject(text)
        val list = root.arr("list")
            ?: throw OnlineChartException(platform, "网易榜单列表结构异常")

        return list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.long("id") ?: return@mapNotNull null
            val name = item.str("name") ?: return@mapNotNull null
            OnlineChart(
                platform = platform,
                chartId = id.toString(),
                name = name,
                coverUrl = item.str("coverImgUrl"),
                updateInfo = item.str("updateFrequency"),
            )
        }.distinctBy { it.chartId }
    }

    override suspend fun chartSongs(chartId: String, page: Int, pageSize: Int): OnlineChartPage {
        val id = chartId.toLongOrNull()
            ?: throw OnlineChartException(platform, "网易榜单标识异常：$chartId")

        val text = try {
            http.getText(
                "https://music.163.com/api/playlist/detail?id=$id",
                referer = "https://music.163.com/",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw OnlineChartException(platform, "网易榜单歌曲请求失败：${e.message}", e)
        }

        val root = http.parseObject(text)
        val tracks = root.obj("result")?.arr("tracks")
            ?: return OnlineChartPage(platform, chartId, page, emptyList(), false)

        val all = tracks.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val songId = item.long("id") ?: return@mapNotNull null
            val name = item.str("name") ?: return@mapNotNull null
            // 旧字段 artists[] / album{}，新字段 ar[] / al{}
            val singer = item.arr("artists")?.joinStr("name") ?: item.arr("ar")?.joinStr("name")
            val albumObj = item.obj("album") ?: item.obj("al")
            val album = albumObj?.str("name")?.takeIf { it.isNotBlank() }
            // 时长：旧 duration / 新 dt，均为毫秒
            val durationMs = item.long("duration") ?: item.long("dt")
            val cover = albumObj?.str("picUrl")
            val idText = songId.toString()

            OnlineSearchResult(
                platform = platform,
                songId = idText,
                name = name,
                artist = singer,
                album = album,
                durationMs = durationMs,
                coverUrl = cover,
                musicInfoJson = buildMusicInfo(
                    "id" to idText,
                    name = name,
                    singer = singer,
                    album = album,
                    durationSec = durationMs?.div(1000),
                    extra = buildMap {
                        albumObj?.long("id")?.let { put("albumId", it.toString()) }
                    },
                ),
            )
        }

        // 平台一次给全量：本地切片
        val safePage = page.coerceAtLeast(1)
        val from = (safePage - 1) * pageSize
        if (from >= all.size) {
            return OnlineChartPage(platform, chartId, page, emptyList(), false)
        }
        val slice = all.subList(from, minOf(from + pageSize, all.size))
        return OnlineChartPage(
            platform = platform,
            chartId = chartId,
            page = page,
            results = slice,
            hasMore = from + pageSize < all.size,
        )
    }
}

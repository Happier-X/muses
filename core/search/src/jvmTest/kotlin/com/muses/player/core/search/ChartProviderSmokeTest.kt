package com.muses.player.core.search

import com.muses.player.core.search.provider.KgChartProvider
import com.muses.player.core.search.provider.QqChartProvider
import com.muses.player.core.search.provider.WyChartProvider
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * 榜单 provider 联网冒烟（**默认跳过**，避免 CI 依赖外网逆向接口）。
 *
 * 跑法：`MUSES_ONLINE_SMOKE=1 ./gradlew :core:search:jvmTest`
 *
 * 为什么需要它：三个平台接口是逆向接口，字段结构随时可能变；
 * 单测只能覆盖解析逻辑（喂 mock JSON），真实结构漂移只有联网才能发现。
 * 该测试断言「能拉到非空榜单 + 能拉到非空歌曲 + musicInfo 含播放所需标识」。
 */
class ChartProviderSmokeTest {

    private fun enabled(): Boolean = System.getenv("MUSES_ONLINE_SMOKE") == "1"

    @Test
    fun QQ音乐榜单可拉取并有标识() = runBlocking {
        if (!enabled()) return@runBlocking println("SKIP ChartProviderSmokeTest：未设置 MUSES_ONLINE_SMOKE=1")
        val provider = QqChartProvider()
        val charts = provider.charts()
        assertTrue(charts.isNotEmpty(), "QQ 榜单列表为空")
        println("[smoke] QQ 榜单 ${charts.size} 个，首个=${charts.first().name}(${charts.first().chartId})")

        val page = provider.chartSongs(charts.first().chartId, page = 1, pageSize = 10)
        assertTrue(page.results.isNotEmpty(), "QQ 榜单歌曲为空")
        val first = page.results.first()
        assertTrue(first.musicInfoJson.contains("songmid"), "QQ musicInfo 缺 songmid：${first.musicInfoJson}")
        // 封面回归：QQ 两个接口都不直接给封面 URL，靠 album.mid 拼（改坏了首页/搜索就会大面积落占位）
        val withCover = page.results.count { !it.coverUrl.isNullOrBlank() }
        println("[smoke] QQ 歌曲 ${page.results.size} 首，首曲=${first.name} - ${first.artist}，有封面 $withCover/${page.results.size}，示例=${first.coverUrl}")
        assertTrue(withCover >= page.results.size / 2, "QQ 榜单封面拼接失败（仅 $withCover/${page.results.size}）")
    }

    @Test
    fun 酷狗榜单可拉取并有标识() = runBlocking {
        if (!enabled()) return@runBlocking println("SKIP ChartProviderSmokeTest：未设置 MUSES_ONLINE_SMOKE=1")
        val provider = KgChartProvider()
        val charts = provider.charts()
        assertTrue(charts.isNotEmpty(), "酷狗榜单列表为空")
        println("[smoke] 酷狗榜单 ${charts.size} 个，首个=${charts.first().name}(${charts.first().chartId})")

        val page = provider.chartSongs(charts.first().chartId, page = 1, pageSize = 10)
        assertTrue(page.results.isNotEmpty(), "酷狗榜单歌曲为空")
        val first = page.results.first()
        assertTrue(first.musicInfoJson.contains("hash"), "酷狗 musicInfo 缺 hash：${first.musicInfoJson}")
        println("[smoke] 酷狗歌曲 ${page.results.size} 首，首曲=${first.name} - ${first.artist}")
    }

    @Test
    fun 网易榜单可拉取并有标识() = runBlocking {
        if (!enabled()) return@runBlocking println("SKIP ChartProviderSmokeTest：未设置 MUSES_ONLINE_SMOKE=1")
        val provider = WyChartProvider()
        val charts = provider.charts()
        assertTrue(charts.isNotEmpty(), "网易榜单列表为空")
        println("[smoke] 网易榜单 ${charts.size} 个，首个=${charts.first().name}(${charts.first().chartId})")

        val page = provider.chartSongs(charts.first().chartId, page = 1, pageSize = 10)
        assertTrue(page.results.isNotEmpty(), "网易榜单歌曲为空")
        val first = page.results.first()
        assertTrue(first.musicInfoJson.contains("\"id\""), "网易 musicInfo 缺 id：${first.musicInfoJson}")
        println("[smoke] 网易歌曲 ${page.results.size} 首，首曲=${first.name} - ${first.artist}")
    }

    @Test
    fun QQ搜索封面可拼接() = runBlocking {
        if (!enabled()) return@runBlocking println("SKIP ChartProviderSmokeTest：未设置 MUSES_ONLINE_SMOKE=1")
        val page = com.muses.player.core.search.provider.TxSearchProvider().search("周杰伦", page = 1, pageSize = 10)
        val withCover = page.results.count { !it.coverUrl.isNullOrBlank() }
        println("[smoke] QQ 搜索 ${page.results.size} 首，有封面 $withCover，示例=${page.results.firstOrNull()?.coverUrl}")
        assertTrue(withCover >= page.results.size / 2, "QQ 搜索封面拼接失败（仅 $withCover/${page.results.size}）")
    }

    @Test
    fun 酷狗与网易搜索封面() = runBlocking {
        if (!enabled()) return@runBlocking println("SKIP ChartProviderSmokeTest：未设置 MUSES_ONLINE_SMOKE=1")
        val kg = com.muses.player.core.search.provider.KgSearchProvider().search("周杰伦", page = 1, pageSize = 10)
        val wy = com.muses.player.core.search.provider.WySearchProvider().search("周杰伦", page = 1, pageSize = 10)
        println("[smoke] 酷狗搜索有封面 ${kg.results.count { !it.coverUrl.isNullOrBlank() }}/${kg.results.size}，示例=${kg.results.firstOrNull()?.coverUrl}")
        println("[smoke] 网易搜索有封面 ${wy.results.count { !it.coverUrl.isNullOrBlank() }}/${wy.results.size}，示例=${wy.results.firstOrNull()?.coverUrl}")
        assertTrue(kg.results.any { !it.coverUrl.isNullOrBlank() }, "酷狗搜索封面缺失")
    }
}

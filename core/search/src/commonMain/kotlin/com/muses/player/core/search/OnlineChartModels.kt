package com.muses.player.core.search

/**
 * 在线排行榜（各平台官方接口直连，供首页「排行榜」区块消费）。
 *
 * 为什么独立于搜索：[OnlineSearchProvider] 只覆盖「关键词 → 歌曲」一跳，
 * 而榜单是「平台榜单列表 → 榜单歌曲」两跳，参数与分页语义都不同。
 * 但产出统一为 [OnlineSearchResult]（含 `musicInfoJson`），
 * 使榜单歌曲与搜索结果**共用同一条播放链路**（洛雪脚本解析直链 → 会话登记入队）。
 */
data class OnlineChart(
    val platform: String,
    /** 平台内榜单标识：QQ `topId` / 酷狗 `rankid` / 网易 歌单 id（榜单即特殊歌单） */
    val chartId: String,
    val name: String,
    /** 榜单封面（平台提供时；首页横向卡片用） */
    val coverUrl: String? = null,
    /** 更新信息（如「每天更新」「飙升榜 第263天」），平台提供时展示 */
    val updateInfo: String? = null,
)

/** 榜单歌曲页（结果模型与搜索同构，播放链路零适配） */
data class OnlineChartPage(
    val platform: String,
    val chartId: String,
    /** 1-based 页码 */
    val page: Int,
    val results: List<OnlineSearchResult>,
    /** 是否还有下一页（平台未提供时为 null，调用方按「返回条数 == pageSize」推断） */
    val hasMore: Boolean?,
)

/**
 * 单平台榜单 provider。
 *
 * 实现须自行处理平台差异（榜单列表结构、分页口径、字段映射）；
 * 失败一律抛 [OnlineChartException]，由聚合层做「部分失败不阻断」。
 */
interface OnlineChartProvider {
    /** 平台 key（kw/kg/tx/wy/mg），与洛雪源、搜索 provider 一致 */
    val platform: String

    /** 平台展示名（如「QQ音乐」） */
    val displayName: String

    /**
     * 平台内置榜单列表。
     *
     * 语义上是一次性全量（各平台榜单数量有限：QQ 约 20、酷狗约 56、网易约 30），
     * 不做分页——首页只需展示前若干个 + 可切换。
     */
    suspend fun charts(): List<OnlineChart>

    /**
     * 榜单歌曲。
     * @param page 1-based 页码
     */
    suspend fun chartSongs(chartId: String, page: Int, pageSize: Int): OnlineChartPage
}

/** 榜单失败（网络/解析/平台拒绝）；聚合层转成用户可读提示 */
class OnlineChartException(
    val platform: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

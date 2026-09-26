package com.muses.player.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/** 单日听歌统计（key 为设备本地日，ISO `yyyy-MM-dd`，由调用方按平台时区算好传入） */
data class DailyPlayStat(
    /** 当日实际播放时长（毫秒，不含暂停时段） */
    val listenMs: Long = 0L,
    /** 当日播放次数（每首歌开始播放计一次） */
    val playCount: Int = 0,
)

/** 单曲累计统计（高频歌曲榜；元数据为入库时快照，曲目从曲库删除后仍可展示） */
data class SongPlayStat(
    val songId: String,
    val title: String,
    val subtitle: String,
    val coverUri: String? = null,
    val playCount: Int = 0,
)

/** 某月汇总（统计页「本月概览」口径） */
data class MonthlyListenStats(
    val listenMs: Long = 0L,
    val playCount: Int = 0,
    /** 听歌天数：当月有播放记录的天数 */
    val listeningDays: Int = 0,
)

/**
 * 听歌统计快照（统计页全量口径）。
 *
 * [totalListenMs]/[totalPlayCount] 为独立累计值，不随 [days] 明细裁剪而丢失；
 * [days]/[songs] 是可裁剪明细（上限见 [PlayStatsRepository.MAX_DAYS]/[PlayStatsRepository.MAX_SONGS]）。
 */
data class PlayStats(
    val days: Map<String, DailyPlayStat> = emptyMap(),
    val songs: Map<String, SongPlayStat> = emptyMap(),
    val totalListenMs: Long = 0L,
    val totalPlayCount: Int = 0,
) {
    /** 某日统计（无记录时返回全 0，调用方无需判空） */
    fun day(day: String): DailyPlayStat = days[day] ?: DailyPlayStat()

    /** 某月逐日统计（yearMonth 形如 `2026-09`；key = 月内日号 1..31） */
    fun monthDays(yearMonth: String): Map<Int, DailyPlayStat> = days
        .filterKeys { it.startsWith(yearMonth + "-") }
        .mapNotNull { (day, stat) -> day.takeLast(2).toIntOrNull()?.let { it to stat } }
        .toMap()

    /** 某月汇总（yearMonth 形如 `2026-09`） */
    fun month(yearMonth: String): MonthlyListenStats {
        val stats = days.filterKeys { it.startsWith(yearMonth + "-") }.values
        return MonthlyListenStats(
            listenMs = stats.sumOf { it.listenMs },
            playCount = stats.sumOf { it.playCount },
            listeningDays = stats.count { it.playCount > 0 || it.listenMs > 0 },
        )
    }

    /** 高频歌曲（播放次数降序；并列按标题稳定排序） */
    fun topSongs(limit: Int = 10): List<SongPlayStat> = songs.values
        .sortedWith(compareByDescending<SongPlayStat> { it.playCount }.thenBy { it.title })
        .take(limit)

    /** 最早有记录的月份（`yyyy-MM`，无记录为 null）——统计页月份切换到左边界 */
    val earliestMonth: String? = days.keys.minOrNull()?.take(7)

    companion object {
        val Empty = PlayStats()
    }
}

/**
 * 听歌统计仓库（统计页数据源）。
 *
 * 与 [RecentPlaysRepository]（最近 50 条展示用、同曲去重）不同，本仓库面向**累计口径**：
 * 按日累计听歌时长/播放次数、按曲累计播放次数，供统计页的热力图、本月概览、
 * 累计数据与高频歌曲榜消费。
 *
 * 存储沿用 Preferences DataStore 单 key JSON 快照（对齐 recent_plays 写法）；
 * 时间口径由调用方给：day 为本地日字符串、时长为毫秒增量（commonMain 无时区/日历 API）。
 */
class PlayStatsRepository constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        /** 统计快照 key（与 recent_plays / playback_state 同 DataStore，key 各自独立） */
        private val KEY = stringPreferencesKey("play_stats")
        private const val SNAPSHOT_VERSION = 1

        /** 逐日明细上限（约三年）：超出按日期裁掉最旧的，累计值不受影响 */
        const val MAX_DAYS = 1100

        /** 单曲明细上限：超出只保留播放次数最高的一批（榜单取前十，足够） */
        const val MAX_SONGS = 300
    }

    /** 读改写串行化：播放服务与统计埋点可能并发写入 */
    private val mutex = Mutex()

    private val _updated = MutableStateFlow(0L)

    /** 写入信号（值单调递增，需要主动刷新的观察者据此重读） */
    val updated: StateFlow<Long> = _updated

    /** 加载统计快照 */
    suspend fun load(): PlayStats = decode(dataStore.data.first()[KEY])

    /** 响应式读取（统计页消费） */
    fun observe(): Flow<PlayStats> = dataStore.data.map { decode(it[KEY]) }

    /**
     * 登记一次播放（歌曲开始播放时调用）：当日次数 +1、该曲累计次数 +1。
     *
     * @param coverUri 封面快照（展示用，可为 null）
     * @param day 设备本地日（`yyyy-MM-dd`）
     */
    suspend fun recordPlay(
        songId: String,
        title: String,
        subtitle: String,
        coverUri: String?,
        day: String,
    ) {
        mutex.withLock {
            val current = decode(dataStore.data.first()[KEY])
            write(
                current.copy(
                    days = trimDays(withDay(current, day) { it.copy(playCount = it.playCount + 1) }),
                    songs = trimSongs(
                        current.songs + (songId to SongPlayStat(
                            songId = songId,
                            title = title,
                            subtitle = subtitle,
                            coverUri = coverUri,
                            playCount = (current.songs[songId]?.playCount ?: 0) + 1,
                        )),
                    ),
                    totalPlayCount = current.totalPlayCount + 1,
                ),
            )
        }
    }

    /**
     * 累加听歌时长（播放中的墙上时间增量，暂停时段不计入）。
     *
     * @param ms 本次结算的毫秒增量（<= 0 直接忽略，不做无谓写盘）
     * @param day 结算时刻所属的设备本地日（`yyyy-MM-dd`）
     */
    suspend fun addListenMs(ms: Long, day: String) {
        if (ms <= 0L) return
        mutex.withLock {
            val current = decode(dataStore.data.first()[KEY])
            write(
                current.copy(
                    days = trimDays(withDay(current, day) { it.copy(listenMs = it.listenMs + ms) }),
                    totalListenMs = current.totalListenMs + ms,
                ),
            )
        }
    }

    /** 清空统计（设置/调试用） */
    suspend fun clear() {
        mutex.withLock { write(PlayStats.Empty) }
    }

    private fun withDay(
        stats: PlayStats,
        day: String,
        transform: (DailyPlayStat) -> DailyPlayStat,
    ): Map<String, DailyPlayStat> = stats.days + (day to transform(stats.day(day)))

    private fun trimDays(days: Map<String, DailyPlayStat>): Map<String, DailyPlayStat> {
        if (days.size <= MAX_DAYS) return days
        // ISO 日字符串字典序 = 时间序，保留最新的 MAX_DAYS 天
        return days.keys.sorted().takeLast(MAX_DAYS).associateWith { days.getValue(it) }
    }

    private fun trimSongs(songs: Map<String, SongPlayStat>): Map<String, SongPlayStat> {
        if (songs.size <= MAX_SONGS) return songs
        return songs.values
            .sortedByDescending { it.playCount }
            .take(MAX_SONGS)
            .associateBy { it.songId }
    }

    private suspend fun write(stats: PlayStats) {
        val body = buildJsonObject {
            put("version", JsonPrimitive(SNAPSHOT_VERSION.toString()))
            put("totalListenMs", JsonPrimitive(stats.totalListenMs.toString()))
            put("totalPlayCount", JsonPrimitive(stats.totalPlayCount.toString()))
            put("days", buildJsonObject {
                for ((day, stat) in stats.days) {
                    put(day, buildJsonObject {
                        put("listenMs", JsonPrimitive(stat.listenMs.toString()))
                        put("playCount", JsonPrimitive(stat.playCount.toString()))
                    })
                }
            })
            put("songs", buildJsonObject {
                for (song in stats.songs.values) {
                    put(song.songId, buildJsonObject {
                        put("title", JsonPrimitive(song.title))
                        put("subtitle", JsonPrimitive(song.subtitle))
                        song.coverUri?.let { put("coverUri", JsonPrimitive(it)) }
                        put("playCount", JsonPrimitive(song.playCount.toString()))
                    })
                }
            })
        }
        dataStore.edit { prefs ->
            prefs[KEY] = Json.encodeToString(JsonObject.serializer(), body)
        }
        _updated.value += 1
    }

    private fun decode(raw: String?): PlayStats = runCatching {
        if (raw.isNullOrEmpty()) return PlayStats.Empty
        val root = Json.parseToJsonElement(raw).jsonObject
        PlayStats(
            days = (root["days"] as? JsonObject)
                ?.mapValues { (_, value) ->
                    val obj = value as? JsonObject ?: return@mapValues DailyPlayStat()
                    DailyPlayStat(listenMs = obj.long("listenMs"), playCount = obj.int("playCount"))
                }
                .orEmpty(),
            songs = buildMap {
                val obj = root["songs"] as? JsonObject ?: return@buildMap
                for ((id, value) in obj) {
                    val entry = value as? JsonObject ?: continue
                    val title = entry.str("title") ?: continue
                    put(
                        id,
                        SongPlayStat(
                            songId = id,
                            title = title,
                            subtitle = entry.str("subtitle").orEmpty(),
                            coverUri = entry.str("coverUri"),
                            playCount = entry.int("playCount"),
                        ),
                    )
                }
            },
            totalListenMs = root.long("totalListenMs"),
            totalPlayCount = root.int("totalPlayCount"),
        )
    }.getOrDefault(PlayStats.Empty)
}

private fun JsonObject.long(key: String): Long =
    (this[key] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L

private fun JsonObject.int(key: String): Int = long(key).toInt()

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content


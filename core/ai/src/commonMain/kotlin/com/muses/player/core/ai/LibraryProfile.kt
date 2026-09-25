package com.muses.player.core.ai

import com.muses.player.core.data.dao.SongDao
import kotlin.random.Random

/**
 * 曲库画像（喂给 LLM 的**唯一**数据面）。
 *
 * 隐私口径（用户已知情选择「统计 + 抽样都发」）：
 * - 只含**聚合统计 + 抽样歌名/歌手**；
 * - **不含**文件路径、音源地址、账号、凭据、歌词正文；
 * - 抽样量有上限（默认 60 首），避免把整个曲库外发。
 */
data class LibraryProfile(
    val totalSongs: Int,
    /** 来源类型展示名 → 曲目数（如「本地」800 /「WebDAV」434） */
    val sourceBreakdown: List<Pair<String, Int>>,
    /** 高频歌手 → 曲目数（降序） */
    val topArtists: List<Pair<String, Int>>,
    /** 高频专辑 → 曲目数（降序） */
    val topAlbums: List<Pair<String, Int>>,
    /** 抽样曲目（`歌名 - 歌手`），供 AI 感知具体口味 */
    val sampleTracks: List<String>,
    /** 本地与 WebDAV 全量歌曲，仅供本地过滤，不进入提示词。 */
    val ownedSongs: Map<String, Set<String>> = emptyMap(),
) {
    val isEmpty: Boolean get() = totalSongs == 0

    /** 渲染为 prompt 片段（此处是外发内容的**唯一定义点**，改这里即改隐私面） */
    fun toPromptText(): String = buildString {
        appendLine("【曲库统计】")
        appendLine("总曲目：$totalSongs 首")
        if (sourceBreakdown.isNotEmpty()) {
            appendLine("来源分布：" + sourceBreakdown.joinToString("，") { "${it.first} ${it.second} 首" })
        }
        if (topArtists.isNotEmpty()) {
            appendLine("高频歌手：" + topArtists.joinToString("，") { "${it.first}(${it.second})" })
        }
        if (topAlbums.isNotEmpty()) {
            appendLine("高频专辑：" + topAlbums.joinToString("，") { "${it.first}(${it.second})" })
        }
        if (sampleTracks.isNotEmpty()) {
            appendLine()
            appendLine("【曲库抽样曲目】")
            sampleTracks.forEach { appendLine("- $it") }
        }
    }.trimEnd()
}

/**
 * 曲库画像构建：从 [SongDao] 读全量曲目后在内存聚合。
 *
 * 为什么在内存聚合而不是 SQL `GROUP BY`：曲库量级为千级（本机实测 1k~1w），
 * 一次 `getAll()` 的成本远低于为画像新增一组 DAO 查询与索引；
 * 且后续若要做「风格/年代推断」也只需改本类，不动数据库。
 */
class LibraryProfileBuilder(
    private val songDao: SongDao,
    /** 抽样随机源（测试注入固定 seed 以获得确定性） */
    private val random: Random = Random.Default,
) {

    suspend fun build(
        artistLimit: Int = 30,
        albumLimit: Int = 12,
        sampleSize: Int = 60,
    ): LibraryProfile {
        val songs = runCatching { songDao.getAll() }.getOrDefault(emptyList())
        if (songs.isEmpty()) {
            return LibraryProfile(0, emptyList(), emptyList(), emptyList(), emptyList())
        }

        // 歌手/专辑按出现次数降序；空值与「未知」类占位不参与（避免污染画像）
        val artists = songs.asSequence()
            .mapNotNull { it.artist?.trim()?.takeIf { name -> name.isNotEmpty() && !name.isUnknownArtist() } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(artistLimit)
            .map { it.key to it.value }

        val albums = songs.asSequence()
            .mapNotNull { it.albumTitle?.trim()?.takeIf { name -> name.isNotEmpty() } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(albumLimit)
            .map { it.key to it.value }

        val breakdown = songs.asSequence()
            .map { it.sourceType.toDisplaySource() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        // 抽样：随机取若干首（每次刷新推荐抽样不同 → 推荐结果自然有变化，但不是无依据乱推）
        val sampleTracks = songs.shuffled(random)
            .asSequence()
            .mapNotNull { song ->
                val title = song.title.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val artist = song.artist?.trim()?.takeIf { it.isNotEmpty() }
                if (artist == null) title else "$title - $artist"
            }
            .distinct()
            .take(sampleSize)
            .toList()

        return LibraryProfile(
            totalSongs = songs.size,
            sourceBreakdown = breakdown,
            topArtists = artists,
            topAlbums = albums,
            sampleTracks = sampleTracks,
            ownedSongs = songs.asSequence()
                .filter { it.sourceType == "LOCAL" || it.sourceType == "WEBDAV" }
                .groupBy { it.title.normalizeForMatch() }
                .filterKeys { it.isNotEmpty() }
                .mapValues { (_, entries) -> entries.map { it.artist.normalizeForMatch() }.toSet() },
        )
    }
}

/** 来源类型展示名（`SourceType.name` 落库，未知值原样保留以免丢信息） */
private fun String.toDisplaySource(): String = when (this) {
    "LOCAL" -> "本地"
    "WEBDAV" -> "WebDAV"
    "ONLINE" -> "在线"
    else -> this
}

/** 常见「无歌手」占位值：参与画像只会让 AI 推荐失焦 */
private fun String.isUnknownArtist(): Boolean {
    val normalized = trim().lowercase()
    return normalized in UNKNOWN_ARTISTS
}

private val UNKNOWN_ARTISTS = setOf(
    "unknown", "unknown artist", "未知艺术家", "未知歌手", "佚名", "群星", "various artists", "<unknown>",
)

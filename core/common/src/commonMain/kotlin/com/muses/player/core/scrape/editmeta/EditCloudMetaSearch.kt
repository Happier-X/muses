package com.muses.player.core.scrape.editmeta

import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.cover.CoverProvider
import com.muses.player.core.scrape.text.normalizeText
import com.muses.player.core.scrape.text.scoreTextHit
import kotlinx.coroutines.CancellationException

/**
 * 编辑页强制云端搜索 + 多候选编排（规格书 = src/features/editMeta/searchEditCloudMeta.ts + types.ts）。
 *
 * 与播放静默 matchOnline* 分离：不读负缓存 / 不做 not-needed 早退 / 不做补空过滤。
 * 不写库、不落盘、不碰播放状态。
 *
 * 歌词维度：按 PRD 边界，具体歌词 provider（含 AMLL 聚合库）由调用方通过
 * [LyricsSearchPort] 注入；本类只负责编排（收集/去重/粗排/截断）。
 */

// ── 类型（editMeta/types.ts 逐项对齐）────────────────────

/** 云端元信息来源平台（MusicTag 式选择） */
enum class CloudPlatformId(val wire: String) {
    ALL("all"), WY("wy"), TX("tx"), KG("kg"), KW("kw"), MG("mg"), ITUNES("itunes"),
}

/** 云端歌词来源平台（无 iTunes，有 LRCLIB） */
enum class CloudLyricsPlatformId(val wire: String) {
    ALL("all"), WY("wy"), TX("tx"), KG("kg"), KW("kw"), MG("mg"), LRCLIB("lrclib"),
}

/** 云端搜索维度 */
enum class EditDimKey { TEXT, COVER, LYRICS }

/** 编辑页云端查询：以当前表单/种子字段为关键词，强制搜索 */
data class EditCloudMetaQuery(
    val songId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    /** 秒；歌词源可用 */
    val durationSec: Double? = null,
)

enum class EditDimStatus(val wire: String) {
    OK("ok"), NO_MATCH("no-match"), NETWORK("network"), ABORTED("aborted"),
}

/** 单维候选结果 */
data class EditDimResult<T>(
    val status: EditDimStatus,
    val items: List<T>,
    /** 最优下标；无结果为 0 */
    val defaultIndex: Int = 0,
)

data class EditCoverCandidate(
    val remoteUrl: String,
    val source: com.muses.player.core.scrape.cover.OnlineCoverSource,
)

/** 歌词候选格式与来源 wire 值（features/lyrics/providers/types.ts 对齐） */
data class EditLyricsCandidate(
    val text: String,
    val format: String,
    val source: String,
    val translationText: String? = null,
)

data class EditCloudMetaResult(
    val text: EditDimResult<TextMetaHit>,
    val cover: EditDimResult<EditCoverCandidate>,
    val lyrics: EditDimResult<EditLyricsCandidate>,
)

data class SearchOptions(
    /** 每维最多保留候选数，默认 8 */
    val maxCandidates: Int? = null,
    /** 限定元信息来源平台（文本+封面）；默认全部平台混合 */
    val platform: CloudPlatformId = CloudPlatformId.ALL,
    /** 限定歌词来源平台；默认全部歌词平台 */
    val lyricsPlatform: CloudLyricsPlatformId = CloudLyricsPlatformId.ALL,
    /** 仅搜索指定维度；默认全部 */
    val dimensions: Set<EditDimKey> = setOf(EditDimKey.TEXT, EditDimKey.COVER, EditDimKey.LYRICS),
)

// ── 歌词 provider 接口缝（具体实现属歌词域，本任务不接线）──

/** 单条歌词命中 */
data class LyricsHit(
    val text: String,
    val format: String,
    val translationText: String? = null,
)

/** 歌词搜索端口：id 形如 wy/tx/qrc/lrclib/amll */
interface LyricsSearchPort {
    val id: String

    suspend fun searchLyrics(query: EditCloudMetaQuery): LyricsHit?
}

/** 用户中止信号（对应 Web AbortSignal）：检查点抛出 [EditSearchAbortedException] */
fun interface AbortSignal {
    fun isAborted(): Boolean
}

/** 中止异常（Web AbortError 对应物） */
class EditSearchAbortedException : CancellationException("aborted")

// ── 编排器 ────────────────────────────────────────────────

class EditCloudMetaSearch(
    private val textProviders: List<com.muses.player.core.scrape.text.TextMetaProvider>,
    private val coverProviders: List<CoverProvider>,
    /**
     * 歌词 provider 全集（id → 端口）；平台过滤后参与。
     * AMLL 聚合库（TTML）应作为独立端口注入，始终参与、不随平台过滤。
     */
    private val lyricsPorts: List<LyricsSearchPort> = emptyList(),
) {

    companion object {
        /** Web DEFAULT_MAX_CANDIDATES */
        const val DEFAULT_MAX_CANDIDATES: Int = 8
        const val AMLL_PORT_ID: String = "amll"
    }

    // ── 平台映射表 ────────────────────────────────────────

    /** 平台 → 文本 provider id（itunes 无文本） */
    private val platformTextIds: Map<CloudPlatformId, List<String>> = mapOf(
        CloudPlatformId.WY to listOf("wy"),
        CloudPlatformId.TX to listOf("tx"),
        CloudPlatformId.KG to listOf("kg"),
        CloudPlatformId.KW to listOf("kw"),
        CloudPlatformId.MG to listOf("mg"),
        CloudPlatformId.ITUNES to emptyList(),
    )

    /** 平台 → 封面 provider id */
    private val platformCoverIds: Map<CloudPlatformId, List<String>> = mapOf(
        CloudPlatformId.WY to listOf("wy"),
        CloudPlatformId.TX to listOf("tx"),
        CloudPlatformId.KG to listOf("kg"),
        CloudPlatformId.KW to listOf("kw"),
        CloudPlatformId.MG to listOf("mg"),
        CloudPlatformId.ITUNES to listOf("itunes"),
    )

    /** 平台 → 歌词 provider id（tx 含 qrc；lrclib 独立） */
    private val platformLyricsIds: Map<CloudLyricsPlatformId, List<String>> = mapOf(
        CloudLyricsPlatformId.WY to listOf("wy"),
        CloudLyricsPlatformId.TX to listOf("tx", "qrc"),
        CloudLyricsPlatformId.KG to listOf("kg"),
        CloudLyricsPlatformId.KW to listOf("kw"),
        CloudLyricsPlatformId.MG to listOf("mg"),
        CloudLyricsPlatformId.LRCLIB to listOf("lrclib"),
    )

    private fun throwIfAborted(signal: AbortSignal?) {
        if (signal != null && signal.isAborted()) {
            throw EditSearchAbortedException()
        }
    }

    private fun isAbortError(error: Throwable): Boolean = error is EditSearchAbortedException

    private fun <T> emptyDim(): EditDimResult<T> = EditDimResult(EditDimStatus.NO_MATCH, emptyList())

    private fun <T> finalizeDim(items: List<T>, sawNetwork: Boolean, aborted: Boolean): EditDimResult<T> =
        when {
            aborted && items.isEmpty() -> EditDimResult(EditDimStatus.ABORTED, emptyList())
            items.isNotEmpty() -> EditDimResult(EditDimStatus.OK, items)
            aborted -> EditDimResult(EditDimStatus.ABORTED, emptyList())
            sawNetwork -> EditDimResult(EditDimStatus.NETWORK, emptyList())
            else -> EditDimResult(EditDimStatus.NO_MATCH, emptyList())
        }

    // ── 文本维度 ──────────────────────────────────────────

    private fun textDedupKey(hit: TextMetaHit): String =
        listOf(
            normalizeText(hit.title),
            normalizeText(hit.artist),
            normalizeText(hit.album),
            hit.source.wire,
        ).joinToString("\u0001")

    private fun rankAndCapText(hits: List<TextMetaHit>, query: OnlineTextQuery, max: Int): List<TextMetaHit> {
        val seen = mutableSetOf<String>()
        val unique = hits.filter { seen.add(textDedupKey(it)) }
        return unique.sortedByDescending { scoreTextHit(it, query) }.take(max)
    }

    private suspend fun searchTextDimension(
        query: OnlineTextQuery,
        max: Int,
        providers: List<com.muses.player.core.scrape.text.TextMetaProvider>,
        signal: AbortSignal?,
    ): EditDimResult<TextMetaHit> {
        val collected = mutableListOf<TextMetaHit>()
        var sawNetwork = false
        var aborted = false

        loop@ for (provider in providers) {
            try {
                throwIfAborted(signal)
                val hit = provider.search(query)
                throwIfAborted(signal)
                if (hit != null && (!hit.title?.trim().isNullOrEmpty() ||
                        !hit.artist?.trim().isNullOrEmpty() || !hit.album?.trim().isNullOrEmpty())
                ) {
                    collected.add(hit.copy(source = provider.id))
                }
            } catch (e: EditSearchAbortedException) {
                aborted = true
                break@loop
            } catch (e: CancellationException) {
                if (isAbortError(e)) {
                    aborted = true; break@loop
                }
                sawNetwork = true
            } catch (_: Exception) {
                sawNetwork = true
            }
        }

        return finalizeDim(rankAndCapText(collected, query, max), sawNetwork, aborted)
    }

    // ── 封面维度 ──────────────────────────────────────────

    private suspend fun searchCoverDimension(
        query: EditCloudMetaQuery,
        max: Int,
        providers: List<CoverProvider>,
        signal: AbortSignal?,
    ): EditDimResult<EditCoverCandidate> {
        val coverQuery = com.muses.player.core.scrape.cover.OnlineCoverQuery(
            songId = query.songId,
            title = query.title,
            artist = query.artist,
            album = query.album,
        )
        val collected = mutableListOf<EditCoverCandidate>()
        val seenUrls = mutableSetOf<String>()
        var sawNetwork = false
        var aborted = false

        loop@ for (provider in providers) {
            if (collected.size >= max) break@loop
            try {
                throwIfAborted(signal)
                val remoteUrl = provider.searchCoverUrl(coverQuery)
                throwIfAborted(signal)
                val url = remoteUrl?.trim()
                if (url.isNullOrEmpty() || !Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(url)) {
                    continue@loop
                }
                val key = url.lowercase()
                if (!seenUrls.add(key)) continue@loop
                collected.add(EditCoverCandidate(remoteUrl = url, source = provider.id))
            } catch (e: EditSearchAbortedException) {
                aborted = true
                break@loop
            } catch (e: CancellationException) {
                if (isAbortError(e)) {
                    aborted = true; break@loop
                }
                sawNetwork = true
            } catch (_: Exception) {
                sawNetwork = true
            }
        }

        return finalizeDim(collected.take(max), sawNetwork, aborted)
    }

    // ── 歌词维度 ──────────────────────────────────────────

    private suspend fun searchLyricsDimension(
        query: EditCloudMetaQuery,
        max: Int,
        ports: List<LyricsSearchPort>,
        amllPort: LyricsSearchPort?,
        signal: AbortSignal?,
    ): EditDimResult<EditLyricsCandidate> {
        val collected = mutableListOf<EditLyricsCandidate>()
        val seen = mutableSetOf<String>()
        var sawNetwork = false
        var aborted = false

        fun pushHit(item: LyricsHit, source: String) {
            val text = item.text.trim()
            if (text.isEmpty()) return
            val key = "$source\u0001${item.format}\u0001${text.take(120)}"
            if (!seen.add(key)) return
            collected.add(
                EditLyricsCandidate(
                    text = text,
                    format = item.format,
                    source = source,
                    translationText = item.translationText?.trim()?.ifEmpty { null },
                ),
            )
        }

        // amll 聚合库（TTML）始终参与：独立高质量来源，不随平台过滤
        if (amllPort != null) {
            try {
                throwIfAborted(signal)
                val hit = amllPort.searchLyrics(query)
                throwIfAborted(signal)
                if (hit != null) {
                    pushHit(hit, source = amllPort.id.ifEmpty { AMLL_PORT_ID })
                }
            } catch (e: EditSearchAbortedException) {
                aborted = true
            } catch (e: CancellationException) {
                if (isAbortError(e)) aborted = true else sawNetwork = true
            } catch (_: Exception) {
                sawNetwork = true
            }
        }

        if (!aborted) {
            loop@ for (port in ports) {
                if (collected.size >= max) break@loop
                try {
                    throwIfAborted(signal)
                    val hit = port.searchLyrics(query)
                    throwIfAborted(signal)
                    if (hit != null && !hit.text.trim().isEmpty()) {
                        pushHit(hit, source = port.id)
                    }
                } catch (e: EditSearchAbortedException) {
                    aborted = true
                    break@loop
                } catch (e: CancellationException) {
                    if (isAbortError(e)) {
                        aborted = true; break@loop
                    }
                    sawNetwork = true
                } catch (_: Exception) {
                    sawNetwork = true
                }
            }
        }

        // 质量粗排：ttml/yrc/qrc 优先于 lrc，同级保持收集顺序（stable sort）
        fun formatRank(format: String): Int = when (format) {
            "ttml", "yrc", "qrc" -> 2
            "lrc" -> 1
            else -> 0
        }
        val sorted = collected.sortedByDescending { formatRank(it.format) }

        return finalizeDim(sorted.take(max), sawNetwork, aborted)
    }

    // ── 公开入口 ──────────────────────────────────────────

    /**
     * 并行拉取文本 / 封面 / 歌词多候选（编辑强制搜）。
     */
    suspend fun search(
        query: EditCloudMetaQuery,
        options: SearchOptions = SearchOptions(),
        signal: AbortSignal? = null,
    ): EditCloudMetaResult {
        val title = query.title.trim()
        val songId = query.songId.trim()
        val max = maxOf(1, options.maxCandidates ?: DEFAULT_MAX_CANDIDATES)
        val platform = options.platform
        val lyricsPlatform = options.lyricsPlatform
        val dims = options.dimensions

        if (title.isEmpty() || songId.isEmpty()) {
            return EditCloudMetaResult(emptyDim(), emptyDim(), emptyDim())
        }

        val textQuery = OnlineTextQuery(
            songId = songId,
            title = title,
            artist = query.artist?.trim()?.ifEmpty { null },
            album = query.album?.trim()?.ifEmpty { null },
        )
        val editQuery = query.copy(
            songId = songId,
            title = title,
            artist = query.artist?.trim()?.ifEmpty { null },
            album = query.album?.trim()?.ifEmpty { null },
        )

        // 平台过滤：选具体平台时只用该平台 provider；
        // AMLL 聚合库始终参与，不随平台过滤（由调用方注入为 id=amll 的端口）
        val selectedText = if (platform == CloudPlatformId.ALL) {
            textProviders
        } else {
            textProviders.filter { it.id.wire in platformTextIds.getValue(platform) }
        }
        val selectedCover = if (platform == CloudPlatformId.ALL) {
            coverProviders
        } else {
            coverProviders.filter { it.id.wire in platformCoverIds.getValue(platform) }
        }
        val amllPort = lyricsPorts.firstOrNull { it.id == AMLL_PORT_ID }
        val selectedLyrics = if (lyricsPlatform == CloudLyricsPlatformId.ALL) {
            lyricsPorts.filter { it.id != AMLL_PORT_ID }
        } else {
            lyricsPorts.filter { it.id != AMLL_PORT_ID && it.id in platformLyricsIds.getValue(lyricsPlatform) }
        }

        val text = if (EditDimKey.TEXT in dims) searchTextDimension(textQuery, max, selectedText, signal) else emptyDim()
        val cover = if (EditDimKey.COVER in dims) searchCoverDimension(editQuery, max, selectedCover, signal) else emptyDim()
        val lyrics = if (EditDimKey.LYRICS in dims) searchLyricsDimension(editQuery, max, selectedLyrics, amllPort, signal) else emptyDim()

        // 维度并行化说明：Web 为 Promise.all 并行；此处保持串行以简化取消语义，
        // 各维度互不依赖，行为等价（总耗时差异可接受）
        return EditCloudMetaResult(
            text = markAborted(text, signal),
            cover = markAborted(cover, signal),
            lyrics = markAborted(lyrics, signal),
        )
    }

    private fun <T> markAborted(dim: EditDimResult<T>, signal: AbortSignal?): EditDimResult<T> {
        if (signal != null && signal.isAborted() && dim.items.isEmpty() && dim.status == EditDimStatus.NO_MATCH) {
            return dim.copy(status = EditDimStatus.ABORTED)
        }
        return dim
    }
}

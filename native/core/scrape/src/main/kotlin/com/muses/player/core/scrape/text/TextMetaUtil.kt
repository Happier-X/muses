package com.muses.player.core.scrape.text

import com.muses.player.core.model.scrape.MetaSources
import com.muses.player.core.model.scrape.OnlineTextQuery
import com.muses.player.core.model.scrape.TextMetaHit
import java.text.Normalizer

// 规格书 = src/features/metadata/util.ts（逐函数翻译）
// normalize 逻辑 = src/features/lyrics/normalize.ts（core 层尚无对应实现，此处一并翻译）

/** util.ts isBlank */
fun isBlank(value: String?): Boolean = value?.trim().isNullOrEmpty()

/**
 * normalize.ts normalizeText：
 * - NFKC 全角半角统一
 * - 小写
 * - 去掉常见括号与装饰符号
 * - 去掉 live/remix 等常见后缀词
 * - 折叠空白
 *
 * 字符类与后缀词正则逐字符对照 Web 源码（含 0x22/0x27 引号、0x5c 反斜杠）。
 */
private val DECOR_CHARS =
    Regex("""[「」『』【】\[\]()（）{}<>《》"'']""")
private val SUFFIX_WORDS =
    Regex("""\b(live|remix|remaster(?:ed)?|ver\.?|version|acoustic|instrumental|off\s*vocal|karaoke|edit|mix)\b""", RegexOption.IGNORE_CASE)
private val PUNCT_CHARS =
    Regex("""[-–—_:|/\\·•~,，.。!！?？+]+""")
private val WHITESPACE_CHARS =
    Regex("[\u3000\\s]+")

internal fun normalizeText(input: String?): String {
    if (input.isNullOrEmpty()) return ""
    return input
        .let { Normalizer.normalize(it, Normalizer.Form.NFKC) }
        .lowercase()
        .replace(DECOR_CHARS, " ")
        .replace(SUFFIX_WORDS, " ")
        .replace(PUNCT_CHARS, " ")
        .replace(WHITESPACE_CHARS, " ")
        .trim()
}

// audio.ts getFileNameFromPath：按 / 与 \ 切分取最后一段
internal fun getFileNameFromPath(path: String): String =
    path.split('/', '\\').lastOrNull { it.isNotEmpty() } ?: path

// audio.ts getTitleFromPath：去扩展名（extensionIndex > 0 才去，避免隐藏文件误伤）
internal fun getTitleFromPath(path: String): String {
    val fileName = getFileNameFromPath(path)
    val extensionIndex = fileName.lastIndexOf('.')
    return if (extensionIndex > 0) fileName.take(extensionIndex) else fileName
}

/**
 * 弱 title：与去扩展名文件名 normalize 后相等（扫描无内嵌标题时的兜底形态）。
 * path 无效或基名为空时不视为弱，避免误覆盖。（util.ts isWeakTitle）
 */
fun isWeakTitle(title: String?, path: String?): Boolean {
    val t = title?.trim()
    val p = path?.trim()
    if (t.isNullOrEmpty() || p.isNullOrEmpty()) {
        return false
    }
    val base = getTitleFromPath(p).trim()
    if (base.isEmpty()) {
        return false
    }
    return normalizeText(t) == normalizeText(base)
}

/** 标题相关：normalize 后相等或互相包含（util.ts titlesRelated） */
fun titlesRelated(a: String?, b: String?): Boolean {
    val na = normalizeText(a)
    val nb = normalizeText(b)
    if (na.isEmpty() || nb.isEmpty()) {
        return false
    }
    return na == nb || na.contains(nb) || nb.contains(na)
}

/**
 * needsOnlineTextMeta 的入参形状（util.ts OnlineTextNeedQuery）：
 * Pick<OnlineTextQuery,...> + 用户手改保护 + metaSources + duration。
 */
data class OnlineTextNeedQuery(
    val title: String?,
    val artist: String? = null,
    val album: String? = null,
    val path: String? = null,
    /** 可选：用户手改保护；全保护时早退 */
    val userEditedFields: List<String> = emptyList(),
    /**
     * 可选：现字段来源标记（child4 R4-2）。
     * 当被补字段来源已是 cloud（上次低质量补缺）时，要求齐备 duration+artist 才再补，
     * 避免低质量循环重写。
     */
    val metaSources: MetaSources? = null,
    /** 查询时长（秒）；可选，参与 cloud 来源字段的再补约束（child4 R4-2） */
    val durationSec: Double? = null,
)

/** OnlineTextQuery → OnlineTextNeedQuery（title 在 query 中非空保证） */
fun OnlineTextQuery.toNeedQuery(userEditedFields: List<String> = emptyList()): OnlineTextNeedQuery =
    OnlineTextNeedQuery(
        title = title,
        artist = artist,
        album = album,
        path = path,
        userEditedFields = userEditedFields,
        metaSources = metaSources,
        durationSec = durationSec,
    )

/**
 * artist/album 空，或 title 为弱标签时需要匹配；手改字段不参与缺口判定。
 * （util.ts needsOnlineTextMeta，含 child4 R4-2 cloud 来源再补约束）
 */
fun needsOnlineTextMeta(query: OnlineTextNeedQuery): Boolean {
    val protectedFields = query.userEditedFields
    val titleProtected = "title" in protectedFields
    val artistProtected = "artist" in protectedFields
    val albumProtected = "album" in protectedFields
    // 三字段均手改：在线文本补缺无意义
    if (titleProtected && artistProtected && albumProtected) {
        return false
    }
    val needArtist = !artistProtected && isBlank(query.artist)
    val needAlbum = !albumProtected && isBlank(query.album)
    val needWeakTitle = !titleProtected && isWeakTitle(query.title, query.path)
    // child4 R4-2：weak title 来源已是 cloud（上次低质量补缺）时，
    // 要求齐备 duration+artist 才再补，避免低质量循环重写
    // （JS !query.duration 对 undefined/null/0 均成立，此处对齐为 null 或 0.0）
    val titleFromCloud = query.metaSources?.title == com.muses.player.core.model.scrape.MetaFieldSource.CLOUD
    val durationMissing = query.durationSec == null || query.durationSec == 0.0
    val cloudTitleBlocked = needWeakTitle && titleFromCloud && (durationMissing || isBlank(query.artist))
    return needArtist || needAlbum || (needWeakTitle && !cloudTitleBlocked)
}

/** util.ts buildKeyword：title/artist/album 过滤空白后以空格连接 */
fun buildKeyword(query: OnlineTextQuery): String =
    listOfNotNull(
        query.title?.takeIf { it.isNotBlank() },
        query.artist?.takeIf { it.isNotBlank() },
        query.album?.takeIf { it.isNotBlank() },
    ).joinToString(" ").trim()

/**
 * 命中是否对当前缺口有用：补空 artist/album，或弱 title 的相关 title。
 * （util.ts hitFillsMissing）
 */
fun hitFillsMissing(hit: TextMetaHit, query: OnlineTextNeedQuery): Boolean {
    val protectedFields = query.userEditedFields
    val needArtist = "artist" !in protectedFields && isBlank(query.artist)
    val needAlbum = "album" !in protectedFields && isBlank(query.album)
    if (needArtist && !hit.artist?.trim().isNullOrEmpty()) {
        return true
    }
    if (needAlbum && !hit.album?.trim().isNullOrEmpty()) {
        return true
    }
    if (
        "title" !in protectedFields &&
        isWeakTitle(query.title, query.path) &&
        !hit.title?.trim().isNullOrEmpty() &&
        titlesRelated(hit.title, query.title)
    ) {
        return true
    }
    return false
}

/** 合并输入形状（util.ts MergeBase） */
data class TextMetaMergeInput(
    val title: String,
    val path: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val userEditedFields: List<String> = emptyList(),
)

/**
 * 合并结果：next 为合并后的新值；changed 标记是否有实际变化。
 * （util.ts mergeTextMetaFillEmpty 返回 { next, changed }）
 */
data class TextMetaMergeOutcome(val next: TextMetaMergeInput, val changed: Boolean)

/**
 * 合并：弱 title + 相关 hit.title 可改 title；artist/album 仅补空。
 * 手改字段双保险：merge 前 strip（upsert 也会保护）。（util.ts mergeTextMetaFillEmpty）
 */
fun mergeTextMetaFillEmpty(latest: TextMetaMergeInput, hit: TextMetaHit): TextMetaMergeOutcome {
    val protectedFields = latest.userEditedFields
    val titleProtected = "title" in protectedFields
    val artistProtected = "artist" in protectedFields
    val albumProtected = "album" in protectedFields

    val weak = !titleProtected && isWeakTitle(latest.title, latest.path)
    val hitTitle = hit.title?.trim()
    val canWriteTitle = weak && !hitTitle.isNullOrEmpty() && titlesRelated(hitTitle, latest.title)

    val nextTitle = if (canWriteTitle) hitTitle!! else latest.title
    // JS `hit.artist?.trim() || latest.artist`：trim 后为空串也回退 latest，用 normalizeTrimmed 对齐
    val nextArtist =
        if (artistProtected || !isBlank(latest.artist)) latest.artist else normalizeTrimmed(hit.artist, latest.artist)
    val nextAlbum =
        if (albumProtected || !isBlank(latest.album)) latest.album else normalizeTrimmed(hit.album, latest.album)

    val changed =
        nextTitle != latest.title ||
            (nextArtist ?: "") != (latest.artist ?: "") ||
            (nextAlbum ?: "") != (latest.album ?: "")

    return TextMetaMergeOutcome(
        next = latest.copy(title = nextTitle, artist = nextArtist, album = nextAlbum),
        changed = changed,
    )
}

// 归一化「trim 后为空串视为无值」（JS `x?.trim() || fallback` 语义）
internal fun normalizeTrimmed(value: String?, fallback: String?): String? =
    value?.trim()?.ifEmpty { null } ?: fallback

/** 单条命中的打分（util.ts scoreTextHit）：title=10 / artist=6 / album=3 / 有 artist+1 / 有 album+1 */
internal fun scoreTextHit(hit: TextMetaHit, query: OnlineTextQuery): Int {
    val title = normalizeText(hit.title)
    val artist = normalizeText(hit.artist)
    val album = normalizeText(hit.album)
    val qTitle = normalizeText(query.title)
    val qArtist = normalizeText(query.artist)
    val qAlbum = normalizeText(query.album)

    var score = 0
    fun related(a: String, b: String): Boolean = a == b || a.contains(b) || b.contains(a)
    if (title.isNotEmpty() && qTitle.isNotEmpty() && related(title, qTitle)) score += 10
    if (artist.isNotEmpty() && qArtist.isNotEmpty() && related(artist, qArtist)) score += 6
    if (album.isNotEmpty() && qAlbum.isNotEmpty() && related(album, qAlbum)) score += 3
    if (!hit.artist?.trim().isNullOrEmpty()) score += 1
    if (!hit.album?.trim().isNullOrEmpty()) score += 1
    return score
}

/** 排序取最高分命中（util.ts pickBestHit；稳定排序，同分保持原序与 Web sort 一致） */
fun pickBestHit(hits: List<TextMetaHit>, query: OnlineTextQuery): TextMetaHit? {
    if (hits.isEmpty()) {
        return null
    }
    return hits.sortedByDescending { scoreTextHit(it, query) }.firstOrNull()
}

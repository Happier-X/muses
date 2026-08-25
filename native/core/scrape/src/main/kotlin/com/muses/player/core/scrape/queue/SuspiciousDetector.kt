package com.muses.player.core.scrape.queue

import com.muses.player.core.model.Song
import com.muses.player.core.scrape.text.isBlank
import com.muses.player.core.scrape.text.isWeakTitle

/**
 * 可疑歌曲判定函数（规格书 = src/features/scrape/suspicious.ts，规则逐条对齐）。
 *
 * 命中任一规则即视为可疑，可批量入待刮削队列：
 * - artist/album/cover/lyrics 缺失
 * - lyricsSource 为 'scrape'（写回未入文件）或历史遗留 'online'
 * - title/artist/album 来源为 'scrape' 或 'cloud'（未写入文件的值值得重刮）
 * - 弱 title + 其他弱信号（缺 cover 或缺 lyrics）
 *
 * 纯判定函数集合，用于单测与未来 UI 共用；不写库。
 */

/** 可疑判定的可配置项 */
data class SuspiciousSongOptions(
    /** 是否纳入「来源 cloud/scrape」字段（默认 true） */
    val includeCloudSources: Boolean = true,
)

/** 是否存在 scrape/cloud 来源字段（刮削写回未入文件 / 历史在线补缺） */
private fun hasUnfiledSource(song: Song): Boolean {
    val sources = song.metaSources ?: return false
    fun isUnfiled(src: com.muses.player.core.model.scrape.MetaFieldSource?): Boolean =
        src == com.muses.player.core.model.scrape.MetaFieldSource.SCRAPE ||
            src == com.muses.player.core.model.scrape.MetaFieldSource.CLOUD
    return isUnfiled(sources.title) || isUnfiled(sources.artist) || isUnfiled(sources.album)
}

/**
 * 判定单曲是否为「可疑/需要刮削」。
 * 命中即返回 true；多规则同时命中亦视为可疑（去重）。
 */
fun isSuspiciousSongForScrape(song: Song, options: SuspiciousSongOptions = SuspiciousSongOptions()): Boolean {
    val includeCloud = options.includeCloudSources
    val weakTitle = isWeakTitle(song.title, song.path)

    // 1. artist/album/cover/lyrics 任一缺失 → 可疑
    if (isBlank(song.artist)) return true
    if (isBlank(song.album)) return true
    if (song.coverUri == null) return true
    if (song.lyrics?.trim().isNullOrEmpty()) return true
    // 2. 弱 title（=文件名占位）本身不是问题，需配合其他弱信号才计入（避免库中常态告警）
    // 3. 歌词来源 scrape（写回失败仅库内展示）或历史遗留 online → 低可信，值得重刮
    if (includeCloud) {
        val lyricsSourceWire = song.lyricsSource?.wire
        if (lyricsSourceWire == "scrape" || lyricsSourceWire == "online") {
            return true
        }
        // 4. scrape/cloud 补缺字段（任意字段命中即视作可改进）
        if (hasUnfiledSource(song)) {
            return true
        }
    }
    // 5. 弱 title + 其他弱信号（缺 cover 或缺 lyrics）才视为可疑
    if (weakTitle && (song.coverUri == null || song.lyrics?.trim().isNullOrEmpty())) {
        return true
    }
    return false
}

/** 批量筛选：返回新列表（不修改原列表） */
fun pickSuspiciousSongs(songs: List<Song>, options: SuspiciousSongOptions = SuspiciousSongOptions()): List<Song> =
    songs.filter { isSuspiciousSongForScrape(it, options) }

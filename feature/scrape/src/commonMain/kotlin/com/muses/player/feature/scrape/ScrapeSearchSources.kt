package com.muses.player.feature.scrape

import com.muses.player.core.scrape.editmeta.CloudLyricsPlatformId
import com.muses.player.core.scrape.editmeta.CloudPlatformId
import com.muses.player.core.scrape.editmeta.SearchOptions

/** 来源选择先作为草稿，只有重新搜索才成为当前候选的来源快照。 */
data class ScrapeSearchSources(
    val metadata: CloudPlatformId = CloudPlatformId.ALL,
    val lyrics: CloudLyricsPlatformId = CloudLyricsPlatformId.ALL,
) {
    fun searchOptions() = SearchOptions(platform = metadata, lyricsPlatform = lyrics)
}

internal fun metadataSourceLabel(source: CloudPlatformId): String = when (source) {
    CloudPlatformId.ALL -> "全部来源"
    CloudPlatformId.ITUNES -> "iTunes"
    CloudPlatformId.LRCAPI -> "LrcApi（仅封面）"
    else -> scrapeSourceLabel(source.wire)
}

internal fun lyricsSourceLabel(source: CloudLyricsPlatformId): String =
    if (source == CloudLyricsPlatformId.ALL) "全部来源" else scrapeSourceLabel(source.wire)

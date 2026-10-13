package com.muses.player.download

import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.libraryQualityLabel
import com.muses.player.core.lxsdk.LxQuality
import com.muses.player.core.model.sameSong

data class DownloadLibraryDuplicate(val id: String, val song: Song, val quality: String, val existing: List<Song>) {
    fun message(sourceNames: Map<String, String>): String = buildString {
        appendLine("曲库中已存在「${song.title}」：")
        existing.take(5).forEach { old ->
            val extension = old.path.substringBefore('?').substringBefore('#').substringAfterLast('.', "")
                .uppercase().takeIf { it in setOf("MP3", "FLAC", "WAV", "M4A", "AAC", "OGG", "OPUS", "APE", "WMA", "DSF", "DFF") }
            val quality = listOfNotNull(extension, old.libraryQualityLabel).distinct().joinToString(" · ").ifBlank { "音质未确认" }
            appendLine("$quality · ${sourceNames[old.sourceId] ?: if (old.sourceType == SourceType.WEBDAV) "WebDAV" else "本地曲库"}")
        }
        if (existing.size > 5) appendLine("另有 ${existing.size - 5} 个版本")
        val label = LxQuality.fromKey(quality)?.let { if (it == LxQuality.FLAC) "FLAC 无损" else it.label } ?: quality
        appendLine("\n本次选择：$label（实际音质以下载文件核验为准）。")
        append("如果保存到其他位置，原文件仍会保留，曲库中可能出现重复歌曲。仍要继续下载吗？")
    }
}

/** 跨来源按歌名及歌手匹配，保留现场、翻唱等版本文字，避免只凭同名误报。 */
internal fun matchingLibrarySongs(incoming: Song, library: List<Song>): List<Song> {
    return library.filter { it.sourceType != SourceType.ONLINE && sameSong(incoming, it) }
}

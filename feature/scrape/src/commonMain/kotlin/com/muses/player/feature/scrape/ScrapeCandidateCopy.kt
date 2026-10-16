package com.muses.player.feature.scrape

import com.muses.player.core.scrape.editmeta.EditLyricsCandidate
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.model.NeteaseLyricParser
import com.muses.player.core.lyrics.parser.KugouKrcLyricsParser
import com.muses.player.core.lyrics.parser.LrcLyricsParser
import com.muses.player.core.lyrics.parser.QQMusicQrcLyricsParser
import com.muses.player.core.lyrics.parser.TtmlLyricsParser

internal fun candidateDurationLabel(seconds: Double?): String? {
    if (seconds == null || !seconds.isFinite() || seconds <= 0 || seconds > 86_400) return null
    val total = kotlin.math.round(seconds).toInt()
    return "时长 ${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

internal data class LyricsCandidatePreview(val kind: String, val text: String?) {
    val snippet: String get() = text?.lineSequence()?.filter(String::isNotBlank)?.take(3)
        ?.joinToString("\n") { if (it.length > 100) it.take(100) + "…" else it }
        ?: "暂无法生成歌词预览"
}

/** 仅生成展示文本，不修改候选及其译文；由界面在后台调用。 */
internal fun lyricsCandidatePreview(candidate: EditLyricsCandidate): LyricsCandidatePreview {
    val raw = candidate.text
    if (raw.length > 256 * 1024) return LyricsCandidatePreview("歌词较长", null)
    // 本入口不允许 XML 文档声明实体或引用外部文档。
    if (Regex("<!\\s*(?:DOCTYPE|ENTITY)\\b", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
        return LyricsCandidatePreview("格式待核对", null)
    }
    val format = candidate.format.lowercase()
    val xml = raw.trimStart().startsWith('<')
    if (xml && format == "qrc" && !Regex("LyricContent=\".*?\"",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).containsMatchIn(raw)) {
        return LyricsCandidatePreview("格式待核对", null)
    }
    val document = runCatching {
        when {
            format == "ttml" || (xml && format != "qrc") -> TtmlLyricsParser.parse(raw)
            format == "krc" || Regex("<\\d+,\\d+,\\d+>").containsMatchIn(raw) -> KugouKrcLyricsParser.parse(raw)
            format == "yrc" -> LyricsDocument(NeteaseLyricParser.parseYrc(raw))
            format == "qrc" -> QQMusicQrcLyricsParser.parse(raw)
            else -> LrcLyricsParser.parse(raw)
        }
    }.getOrNull()
    val lines = document?.lines.orEmpty().filter { it.text.isNotBlank() }
    if (lines.isNotEmpty()) {
        return LyricsCandidatePreview(if (lines.any { it.syllables.isNotEmpty() }) "逐字歌词" else "逐行歌词",
            lines.joinToString("\n") { it.text })
    }
    if (xml || format in setOf("ttml", "qrc", "yrc", "krc")) return LyricsCandidatePreview("格式待核对", null)
    val plain = raw.lineSequence().map(String::trim).filter(String::isNotBlank)
        .filterNot { Regex("^\\[(?:ar|ti|al|by|offset|re|ve):.*]$", RegexOption.IGNORE_CASE).matches(it) }.joinToString("\n")
    return if (plain.isBlank()) LyricsCandidatePreview("暂无正文", null) else LyricsCandidatePreview("纯文本歌词", plain)
}

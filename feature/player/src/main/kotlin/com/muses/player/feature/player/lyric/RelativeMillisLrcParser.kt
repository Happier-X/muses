package com.muses.player.feature.player.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser
import com.mocharealm.accompanist.lyrics.core.utils.LrcMetadataHelper
import kotlin.math.abs

/**
 * 「行级 [mm:ss.xx] + 词级 <相对毫秒,时长>」逐词 LRC 解析器。
 *
 * 背景：部分在线歌词源/导出工具会产出如下混合格式：
 * ```
 * [00:14.30]<0,430>为<430,190>何<620,660>我<1280,200>总...
 * ```
 * 词级标记是逗号分隔的毫秒二元组（部分变体带第三位保留位 `<0,60,0>`，即 KRC 风格三元组）。
 * lyrics-core 0.4.7 的 [EnhancedLrcParser][com.mocharealm.accompanist.lyrics.core.parser.EnhancedLrcParser]
 * 会因 canParse 命中（行级 `[mm:ss.xx]`）而接管，但其词级时间戳只认 `mm:ss.xx`
 * （`isTimestamp` 用 `\d+([:.]\d+)+` 判定，逗号不通过），导致逐词标记被整行丢弃、
 * 原始标记文本泄漏到 UI（卡拉OK 渐变失效）。
 *
 * 本解析器插入 AutoParser 链中 EnhancedLrcParser 之前，专管该格式：
 * - 词级 `<start,dur[,reserved]>text`，start 相对行级时间戳（若首词 start >= 行时间则按绝对毫秒处理）；
 * - 无词级标记的普通行退化为 [SyncedLine]；
 * - 与上游一致：±150ms 内内容不同的相邻行配对为 translation。
 */
object RelativeMillisLrcParser : ILyricsParser {

	/** 行级时间戳：[mm:ss]、[mm:ss.xx]、[mm:ss.xxx] */
	private val lineTimestampRegex = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")

	/** 词级标记：逗号毫秒二元组 `<start,dur>`，或 KRC 风格三元组 `<start,dur,reserved>` */
	private val wordTagRegex = Regex("""<(\d+),(\d+)(?:,\d+)?>""")

	/** 行首 leading tag（复刻上游 EnhancedLrcParser 的 leading 标签扫描） */
	private val tagRegex = Regex("""\[(.*?)]""")

	private val timestampPattern = Regex("""\d+([:.]\d+)+""")

	private fun isTimestamp(s: String): Boolean = timestampPattern.matches(s.trim())

	/**
	 * `mm:ss[.x[xx[xxx]]]` → 毫秒（对齐上游 TimeUtils.parseAsTime 语义：
	 * `.1`→100ms、`.12`→120ms、`.123`→123ms）。
	 * 上游该函数为 internal，跨 module 不可见，故此处自实现。
	 */
	private fun parseLineTimestampMs(tag: String): Int? {
		val parts = tag.trim().split(':')
		if (parts.isEmpty() || parts.size > 3) return null
		var totalMs = 0L
		parts.forEachIndexed { index, part ->
			val unitMs = when (parts.size) {
				3 -> longArrayOf(3_600_000L, 60_000L, 1_000L)[index]
				2 -> longArrayOf(60_000L, 1_000L)[index]
				else -> longArrayOf(1_000L)[index]
			}
			val dot = part.indexOf('.')
			val whole = if (dot == -1) part else part.substring(0, dot)
			val wholeValue = whole.toIntOrNull() ?: return null
			totalMs += wholeValue * unitMs
			if (dot != -1) {
				val frac = part.substring(dot + 1)
				if (frac.isEmpty() || frac.length > 3 || frac.any { !it.isDigit() }) return null
				var fracMs = frac.toInt()
				repeat(3 - frac.length) { fracMs *= 10 }
				totalMs += fracMs
			}
		}
		return totalMs.toInt().takeIf { it >= 0 }
	}

	override fun canParse(content: String): Boolean {
		if (!content.contains(lineTimestampRegex)) return false
		// 仅当内容中确实出现「逗号词级标记」才接管；标准 enhanced LRC（<mm:ss.xx>）仍交给上游
		return wordTagRegex.containsMatchIn(content)
	}

	override fun parse(lines: List<String>): SyncedLyrics {
		val lyricsLines = LrcMetadataHelper.removeAttributes(lines).filter { it.isNotBlank() }
		val rawData = lyricsLines.flatMap { parseLine(it) }.combineWithTranslation()

		val attributes = LrcMetadataHelper.parse(lines)
		return SyncedLyrics(
			lines = rawData,
			title = attributes.title ?: "",
			artists = emptyList(),
		)
	}

	private fun parseLine(string: String): List<ISyncedLine> {
		val matches = tagRegex.findAll(string).toList()
		if (matches.isEmpty()) return emptyList()

		var lastEnd = 0
		val leadingTags = mutableListOf<MatchResult>()
		for (match in matches) {
			val prefix = string.substring(lastEnd, match.range.first)
			if (prefix.isBlank()) {
				leadingTags.add(match)
				lastEnd = match.range.last + 1
			} else break
		}
		if (leadingTags.isEmpty()) return emptyList()

		val content = if (lastEnd < string.length) string.substring(lastEnd).trim() else ""
		val timestamps = leadingTags.mapNotNull { m ->
			val tag = m.groupValues[1].trim()
			if (isTimestamp(tag)) parseLineTimestampMs(tag) else null
		}
		if (timestamps.isEmpty() || content.isBlank()) return emptyList()

		val syllables = parseSyllables(content, timestamps.first())
		if (syllables.isEmpty()) {
			// 无有效词级标记：退化为整行 SyncedLine（不保留原始标记文本）
			return timestamps.map { SyncedLine(content = content, translation = null, start = it, end = it) }
		}

		return listOf(
			KaraokeLine.MainKaraokeLine(
				syllables = syllables,
				translation = null,
				alignment = KaraokeAlignment.Unspecified,
				start = syllables.first().start,
				end = syllables.last().end,
			)
		)
	}

	/**
	 * 解析 `<start,dur[,reserved]>text` 序列。
	 * 首词 start 小于行时间戳 → 视为相对行首的毫秒偏移，统一加行偏移；否则视为绝对毫秒。
	 */
	private fun parseSyllables(content: String, lineStartMs: Int): List<KaraokeSyllable> {
		if (content.isBlank()) return emptyList()

		data class RawSyllable(val content: String, val relStart: Int, val duration: Int)

		val raws = mutableListOf<RawSyllable>()
		val matches = wordTagRegex.findAll(content).toList()
		matches.forEachIndexed { index, match ->
			val textStart = match.range.last + 1
			val textEnd = matches.getOrNull(index + 1)?.range?.first ?: content.length
			val text = content.substring(textStart, textEnd)
			raws.add(
				RawSyllable(
					content = text,
					relStart = match.groupValues[1].toInt(),
					duration = match.groupValues[2].toInt(),
				)
			)
		}
		if (raws.isEmpty()) return emptyList()

		val isRelative = raws.first().relStart < lineStartMs
		val syllables = raws.mapNotNull { raw ->
			val text = raw.content
			if (text.isBlank()) return@mapNotNull null
			val start = if (isRelative) lineStartMs + raw.relStart else raw.relStart
			val end = start + raw.duration.coerceAtLeast(0)
			KaraokeSyllable(content = text, start = start, end = end, phonetic = "")
		}
		return syllables.ifEmpty { emptyList() }
	}

	/** ±150ms 内内容不同的相邻行配对为 translation（对齐上游 EnhancedLrcParser.combineRawWithTranslation） */
	private fun List<ISyncedLine>.combineWithTranslation(): List<ISyncedLine> {
		val list = ArrayList<ISyncedLine>()
		val used = mutableSetOf<Int>()
		for (i in indices) {
			if (i in used) continue
			val line = this[i]
			val content = line.contentText()
			var paired = false
			for (j in i + 1 until size) {
				if (j in used) continue
				val next = this[j]
				val compatible = (line::class == next::class) || next is SyncedLine
				if (compatible && abs(line.start - next.start) <= 150) {
					val nextContent = next.contentText()
					if (nextContent != content && content.isNotEmpty()) {
						list.add(line.withTranslation(nextContent))
						used += i
						used += j
						paired = true
						break
					}
				}
			}
			if (!paired) {
				list.add(line)
				used += i
			}
		}
		return list
	}

	private fun ISyncedLine.contentText(): String = when (this) {
		is KaraokeLine -> syllables.joinToString("") { it.content }.trim()
		is SyncedLine -> content.trim()
		else -> ""
	}

	private fun ISyncedLine.withTranslation(translation: String): ISyncedLine = when (this) {
		is KaraokeLine.MainKaraokeLine -> copy(translation = translation)
		is SyncedLine -> copy(translation = translation)
		else -> this
	}
}

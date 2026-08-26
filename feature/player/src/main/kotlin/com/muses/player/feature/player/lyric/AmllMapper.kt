package com.muses.player.feature.player.lyric

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine

/** AMLL LyricWord（字段名与 @applemusic-like-lyrics/core 0.5.2 LyricLine.words[] 完全一致） */
data class AmllWord(
	val startTime: Int,
	val endTime: Int,
	val word: String,
)

/** AMLL LyricLine */
data class AmllLyricLine(
	val words: List<AmllWord>,
	val startTime: Int,
	val endTime: Int,
	val translatedLyric: String,
	val romanLyric: String,
	val isBG: Boolean,
	val isDuet: Boolean,
)

/** window.updateLyrics(payload) 的载荷结构（见 native/frontend/amll-web/src/main.ts） */
data class AmllPayload(
	val lines: List<AmllLyricLine>,
	val coverUrl: String?,
	val songId: String,
)

/**
 * lyrics-core 解析结果 → AMLL LyricLine[] 映射 + JSON 序列化。
 *
 * - KaraokeLine（TTML/YRC/KRC 逐词）：syllables → words；translation → translatedLyric；phonetic → romanLyric
 * - Accompaniment 行 → isBG = true
 * - SyncedLine（LRC 整行）：整行折叠为单 word；自带 translation 直挂（0.4.7 EnhancedLrcParser 已做同时间戳配对）
 * - 播完钳制由 Kotlin 侧发送 min(positionMs, lastLine.end) 完成，见 PlayerViewModel
 */
object AmllMapper {

	fun toAmllLines(synced: SyncedLyrics): List<AmllLyricLine> =
		synced.lines.map { it.toAmllLine() }

	private fun ISyncedLine.toAmllLine(): AmllLyricLine = when (this) {
		is KaraokeLine -> AmllLyricLine(
			words = syllables.map { syllable ->
				AmllWord(startTime = syllable.start, endTime = syllable.end, word = syllable.content)
			},
			startTime = start,
			endTime = end,
			translatedLyric = translation.orEmpty(),
			romanLyric = phonetic.orEmpty(),
			isBG = this is KaraokeLine.AccompanimentKaraokeLine,
			isDuet = false,
		)

		else -> {
			// SyncedLine：LRC 整行，单 word 覆盖全行时间窗
			val line = this as? SyncedLine
			val content = line?.content ?: ""
			AmllLyricLine(
				words = listOf(AmllWord(startTime = start, endTime = end, word = content)),
				startTime = start,
				endTime = end,
				translatedLyric = line?.translation.orEmpty(),
				romanLyric = "",
				isBG = false,
				isDuet = false,
			)
		}
	}

	// ---------- JSON 序列化（手写，避免引入 kotlinx-serialization） ----------

	fun toJson(payload: AmllPayload): String = buildString {
		append('{')
		append("\"lines\":[")
		payload.lines.forEachIndexed { index, line ->
			if (index > 0) append(',')
			appendLineJson(line)
		}
		append("],\"coverUrl\":")
		append(payload.coverUrl?.let(::quote) ?: "null")
		append(",\"songId\":")
		append(quote(payload.songId))
		append('}')
	}

	private fun StringBuilder.appendLineJson(line: AmllLyricLine) {
		append('{')
		append("\"words\":[")
		line.words.forEachIndexed { i, w ->
			if (i > 0) append(',')
			append("{\"startTime\":").append(w.startTime)
			append(",\"endTime\":").append(w.endTime)
			append(",\"word\":").append(quote(w.word))
			append('}')
		}
		append("],\"startTime\":").append(line.startTime)
		append(",\"endTime\":").append(line.endTime)
		append(",\"translatedLyric\":").append(quote(line.translatedLyric))
		append(",\"romanLyric\":").append(quote(line.romanLyric))
		append(",\"isBG\":").append(line.isBG)
		append(",\"isDuet\":").append(line.isDuet)
		append('}')
	}

	internal fun quote(value: String): String = buildString(value.length + 2) {
		append('"')
		for (ch in value) {
			when (ch) {
				'\\' -> append("\\\\")
				'"' -> append("\\\"")
				'\n' -> append("\\n")
				'\r' -> append("\\r")
				'\t' -> append("\\t")
				'\b' -> append("\\b")
				'\u000C' -> append("\\f")
				else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
			}
		}
		append('"')
	}
}

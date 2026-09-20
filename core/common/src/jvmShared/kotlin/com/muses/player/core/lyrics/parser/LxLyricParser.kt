package com.muses.player.core.lyrics.parser

import com.muses.player.core.lyrics.model.LyricLine
import com.muses.player.core.lyrics.model.LyricQuality
import com.muses.player.core.lyrics.model.LyricSyllable
import com.muses.player.core.lyrics.model.LyricSource
import com.muses.player.core.lyrics.model.LyricTimingKind
import com.muses.player.core.lyrics.model.LyricsDocument
import com.muses.player.core.lyrics.model.NeteaseLyricParser
import com.muses.player.feature.player.lyric.LyricsParser

/**
 * 洛雪自定义音源（在线音源）歌词解析：脚本 `lyric` 动作的四字段 → [LyricsDocument]。
 *
 * ## 输入形态（洛雪 `lyric` 动作约定）
 * ```
 * lyric   : [00:12.340]这是一行歌词            ← 行级 LRC
 * tlyric  : [00:12.340]this is a line          ← 翻译（行级 LRC）
 * rlyric  : [00:12.340]zhe shi yi hang         ← 罗马音（行级 LRC）
 * lxlyric : [00:12.340]<12340,120>这<12460,180>是…  ← 逐字（`<startMs,durationMs>` 前缀）
 * ```
 *
 * ## 逐字时间的两种基准（实测各源不一致，必须容错）
 * - 酷我/酷狗系：`<startMs,durationMs>` 的 start 是**歌曲绝对毫秒**（与行头时间同基准）；
 * - 咪咕系：start 是**相对本行行首**的毫秒（首字常为 `<0,…>`）。
 *
 * 判定规则：首个字的时间若**早于行头时间超过 [RELATIVE_BASE_TOLERANCE_MS]**，
 * 判为「相对行首」并整体平移行头时间；否则按绝对值处理。
 * 该规则对 `行首时间 < 容差` 的开头行可能保守判为绝对，误差上限即容差（200ms），可接受。
 *
 * ## 兜底
 * - `lxlyric` 缺失但 `lyric` 里仍残留 `<start,duration>` 标记时，同样走逐字解析（去标记后取文本）；
 * - 逐字解析不出内容时回退行级 LRC；行级回退**关闭伪逐字**（同 [LrcLyricsParser] 口径，
 *   避免把普通 LRC 抬成昂贵的伪逐字渲染）；
 * - 翻译/罗马音按时间对齐（复用 [NeteaseLyricParser.alignSecondary] 的 1.5s 容差窗口）。
 *
 * 不写库：结果只用于播放页内存态展示（在线曲目不落库）。
 */
object LxLyricParser {

    /** 行头 `[mm:ss.xxx]`（兼容 `[mm:ss:xxx]`；lxlyric 实际每行单标签，取首个即可） */
    private val lineHeader = Regex("^\\[(\\d{1,3}):(\\d{1,3})(?:[.:](\\d{1,3}))?]")

    /** 逐字标记 `<start,duration>`（第三段为酷狗原始偏移，忽略） */
    private val wordTiming = Regex("<(-?\\d+),(-?\\d+)(?:,-?\\d+)?>")

    /** 逐字时间基准判定容差（ms） */
    private const val RELATIVE_BASE_TOLERANCE_MS = 200L

    /**
     * 解析洛雪四字段歌词。
     *
     * @return 无任何有效歌词时返回 null
     */
    fun parse(
        lyric: String? = null,
        tlyric: String? = null,
        rlyric: String? = null,
        lxlyric: String? = null,
    ): LyricsDocument? {
        // 主歌词：优先逐字（lxlyric，或 lyric 里残留逐字标记），退化为行级 LRC
        val wordSource = lxlyric?.takeIf { it.isNotBlank() }
            ?: lyric?.takeIf { it.isNotBlank() && wordTiming.containsMatchIn(it) }
        val wordLines = wordSource?.let(::parseWordLines).orEmpty()

        val lineLines = if (wordLines.isEmpty()) {
            lyric?.takeIf { it.isNotBlank() }?.let { source ->
                NeteaseLyricParser.parseLrc(wordTiming.replace(source, ""))
            }.orEmpty()
        } else {
            emptyList()
        }

        val primary = wordLines.ifEmpty { lineLines }
        if (primary.isEmpty()) {
            // 第三方脚本常直接返回平台原始歌词（酷狗 KRC / QQ QRC / 网易 YRC / TTML），
            // 不是洛雪形态：交给项目通用解析链兜底（同一份解析语义，避免各源各写一套）
            return parsePlatformRaw(lxlyric?.takeIf { it.isNotBlank() } ?: lyric?.takeIf { it.isNotBlank() })
        }

        val translations = alignAnnotations(primary, tlyric)
        val romanizations = alignAnnotations(primary, rlyric)
        val lines = primary.mapIndexed { index, line ->
            line.copy(
                translation = annotationText(line, translations.getOrNull(index)),
                romanization = annotationText(line, romanizations.getOrNull(index)),
                romanizationSyllables = romanizations.getOrNull(index)?.syllables.orEmpty(),
            )
        }

        return if (wordLines.isNotEmpty()) {
            LyricsDocument(
                lines = lines,
                source = LyricSource.LxMusic,
                quality = LyricQuality.WordSynchronized,
            )
        } else {
            LyricsDocument(
                lines = lines,
                source = LyricSource.LxMusic,
                quality = LyricQuality.LineSynchronized,
                pseudoTimingAllowed = false,
            )
        }
    }

    /** 平台原始形态判别：酷狗 KRC 用 `<a,b[,c]>`，QQ QRC / 网易 YRC 用 `(a,b[,c])` */
    private val krcWordTiming = Regex("<\\d+,\\d+(?:,\\d+)?>")
    private val parenWordTiming = Regex("\\(\\d+,\\d+(?:,\\d+)?\\)")

    /**
     * 平台原始格式兜底：按逐字 timing 的括号形态先定源，再交通用解析链。
     *
     * 为何不直接 `LyricsParser.parseDocument`：通用链的 YRC 分支会**抢在 KRC 之前**
     * 误食 `<a,b,c>` 正文（两者行头 `[start,duration]` 同形），产出带 timing 标记的脏文本。
     * 故先按形态分流，再给 TTML/LRC 留通用链兜底。
     *
     * 只改写来源标记为 [LyricSource.LxMusic]（歌词确实来自洛雪脚本），其余字段原样保留，
     * 以保证时间轴/音节/译文对齐口径与其它入口完全一致。
     */
    private fun parsePlatformRaw(raw: String?): LyricsDocument? {
        if (raw.isNullOrBlank()) return null
        val document = when {
            krcWordTiming.containsMatchIn(raw) -> KugouKrcLyricsParser.parse(raw)
            parenWordTiming.containsMatchIn(raw) ->
                runCatching { QQMusicQrcLyricsParser.parse(raw) }.getOrNull()
            else -> LyricsParser.parseDocument(raw)
        } ?: return null
        if (document.lines.isEmpty()) return null
        // 质量标注统一：有逐字音节的源（KRC/QRC/YRC）抬到 WordSynchronized；
        // 其余（含纯 LRC）按行级承载，避免下游因上游默认值（Fallback）而漏开逐字渲染
        val quality = when {
            document.lines.any { it.syllables.isNotEmpty() } -> LyricQuality.WordSynchronized
            document.quality == LyricQuality.Authored -> LyricQuality.Authored
            else -> LyricQuality.LineSynchronized
        }
        return document.copy(source = LyricSource.LxMusic, quality = quality)
    }

    /** 逐字行解析：`[mm:ss.xxx]<start,duration>字<…>` → [LyricLine]（syllables 为 Precise） */
    private fun parseWordLines(source: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        for (raw in source.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val header = lineHeader.find(line) ?: continue
            val lineStartMs = headerTimeMs(header) ?: continue
            val content = line.substring(header.range.last + 1)
            val matches = wordTiming.findAll(content).toList()

            if (matches.isEmpty()) {
                val text = content.trim()
                if (text.isBlank()) continue
                result += LyricLine(
                    timeMs = lineStartMs,
                    timingKind = LyricTimingKind.LineSynchronized,
                    text = text,
                )
                continue
            }

            // 相对基准判定：首字时间显著早于行头 → 该源的 `<start>` 相对行首
            val firstRawStart = matches.first().groupValues[1].toLongOrNull() ?: lineStartMs
            val baseMs = if (firstRawStart < lineStartMs - RELATIVE_BASE_TOLERANCE_MS) lineStartMs else 0L

            val syllables = buildList {
                var prevEndMs = Long.MIN_VALUE
                for ((index, match) in matches.withIndex()) {
                    val rawStart = match.groupValues[1].toLongOrNull() ?: continue
                    val duration = match.groupValues[2].toLongOrNull()?.coerceAtLeast(1L) ?: continue
                    val textStart = match.range.last + 1
                    val textEnd = matches.getOrNull(index + 1)?.range?.first ?: content.length
                    if (textEnd < textStart) continue
                    val syllableText = content.substring(textStart, textEnd)
                    if (syllableText.isEmpty()) continue
                    // 单调修正：个别源的逐字时间会回退，钳到前一个字结束，避免渲染器倒挂
                    val startMs = (rawStart + baseMs).coerceAtLeast(prevEndMs.takeIf { it > Long.MIN_VALUE } ?: Long.MIN_VALUE)
                    val endMs = (startMs + duration).coerceAtLeast(startMs + 1L)
                    prevEndMs = endMs
                    add(LyricSyllable(text = syllableText, startTimeMs = startMs, endTimeMs = endMs))
                }
            }
            if (syllables.isEmpty()) continue

            val text = syllables.joinToString("") { it.text }.trim()
            if (text.isBlank()) continue
            val endMs = syllables.maxOf { it.endTimeMs }
            result += LyricLine(
                timeMs = lineStartMs,
                durationMs = (endMs - lineStartMs).coerceAtLeast(1L),
                text = text,
                syllables = syllables,
                timingKind = LyricTimingKind.Precise,
            )
        }
        return result.sortedBy { it.timeMs }
    }

    /** 注释（翻译/罗马音）对齐：剥掉逐字标记后按时间对齐到主歌词 */
    private fun alignAnnotations(primary: List<LyricLine>, raw: String?): List<LyricLine?> {
        val source = raw?.takeIf { it.isNotBlank() } ?: return List(primary.size) { null }
        val candidates = NeteaseLyricParser.parseLrc(wordTiming.replace(source, ""))
        if (candidates.isEmpty()) return List(primary.size) { null }
        return NeteaseLyricParser.alignSecondary(primary, candidates)
    }

    /** 注释文本：与主行同文（或空白）时视为无注释 */
    private fun annotationText(target: LyricLine, candidate: LyricLine?): String? {
        val text = candidate?.text?.trim().orEmpty()
        return text.takeIf { it.isNotBlank() && it != target.text.trim() }
    }

    /** `[mm:ss.xxx]` → 毫秒（小数位不足 3 位时右补零，`[00:01.5]` == 500ms） */
    private fun headerTimeMs(match: MatchResult): Long? {
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val fraction = match.groupValues[3]
        val ms = if (fraction.isEmpty()) 0L else fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return minutes * 60_000L + seconds * 1_000L + ms
    }
}

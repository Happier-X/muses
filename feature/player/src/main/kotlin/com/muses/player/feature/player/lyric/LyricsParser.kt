package com.muses.player.feature.player.lyric

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser

/**
 * 歌词解析封装（M2 阶段 1）。
 *
 * 仅解析不渲染：lyrics-core 0.4.7 的 [AutoParser] 自动识别 TTML / LRC / YRC / KRC / Lyricify Syllable。
 * API 事实（相对 spike 验证的 0.4.4 无变化）：`AutoParser()` 无参构造、`parse(String) -> SyncedLyrics`。
 * 注意：0.4.7 无 Android target，以 JVM 变体参与构建；其 TTML 解析为自实现（无 javax.xml 依赖），真机可用。
 */
object LyricsParser {

	private val parser = AutoParser()

	/**
	 * 解析歌词原文；失败/空结果一律返回 null 不抛异常（调用方降级为无歌词空态，背景照常渲染）。
	 * 注意：0.4.7 对不可识别文本不抛异常而是返回空行集，此处归一化为 null。
	 */
	fun parse(raw: String?): SyncedLyrics? {
		if (raw.isNullOrBlank()) return null
		return try {
			parser.parse(raw).takeIf { it.lines.isNotEmpty() }
		} catch (_: Exception) {
			null
		}
	}
}

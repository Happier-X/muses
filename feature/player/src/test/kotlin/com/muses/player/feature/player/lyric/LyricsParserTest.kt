package com.muses.player.feature.player.lyric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

	private fun resource(name: String): String =
		javaClass.classLoader!!.getResourceAsStream("lyrics/$name")!!
			.readBytes().decodeToString()

	@Test
	fun `parse TTML sample1 returns synced lyrics with word-level lines`() {
		val parsed = LyricsParser.parse(resource("sample1.ttml"))
		assertNotNull(parsed)
		parsed!!
		assertTrue("expected multiple lines, got ${parsed.lines.size}", parsed.lines.size > 3)
		val first = parsed.lines.first()
		assertTrue("first line start must be >= 0, got ${first.start}", first.start >= 0)
		assertTrue(first.end > first.start)
	}

	@Test
	fun `parse TTML sample2 returns synced lyrics`() {
		val parsed = LyricsParser.parse(resource("sample2.ttml"))
		assertNotNull(parsed)
		assertTrue(parsed!!.lines.isNotEmpty())
	}

	@Test
	fun `parse bilingual LRC returns line-per-entry with translations`() {
		val parsed = LyricsParser.parse(resource("bilingual.lrc"))
		assertNotNull(parsed)
		parsed!!
		assertTrue("expected >=5 lines, got ${parsed.lines.size}", parsed.lines.size >= 5)
		// 0.4.7 EnhancedLrcParser 会把同时间戳双行配对为 SyncedLine(content, translation)
		val translated = parsed.lines.count { (it as? com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine)?.translation?.isNotBlank() == true }
		assertTrue("expected translated lines, got $translated", translated >= 5)
	}

	@Test
	fun `parse invalid inputs return null without throwing`() {
		assertNull(LyricsParser.parse(null))
		assertNull(LyricsParser.parse(""))
		assertNull(LyricsParser.parse("   \n\t "))
		assertNull(LyricsParser.parse("这不是歌词的随机文本 just random words without timestamps"))
		assertNull(LyricsParser.parse("\u0000\u0001\u0002binary garbage"))
	}
}

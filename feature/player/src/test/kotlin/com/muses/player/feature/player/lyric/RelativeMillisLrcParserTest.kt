package com.muses.player.feature.player.lyric

import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「行级 [mm:ss.xx] + 词级 <相对毫秒,时长>」逐词 LRC 解析测试。
 * 样例结构取自真机数据库中「宋岳庭 - 为何我」的实际歌词（字面已脱敏为相近文本）。
 */
class RelativeMillisLrcParserTest {

	@Test
	fun `canParse accepts comma millis word tags`() {
		val content = """
			[ti:测试]
			[00:14.30]<0,430>为<430,190>何<620,660>我
			[00:18.02]<0,260>失<260,260>眠
		""".trimIndent()
		assertTrue(RelativeMillisLrcParser.canParse(content))
	}

	@Test
	fun `canParse rejects standard enhanced lrc and plain lrc`() {
		assertFalse(RelativeMillisLrcParser.canParse("[00:12.34]<00:12.34>Hel<00:12.60>lo"))
		assertFalse(RelativeMillisLrcParser.canParse("[00:12.34]普通整行歌词"))
	}

	@Test
	fun `parse relative millis syllables are shifted by line timestamp`() {
		val content = """
			[00:14.30]<0,430>为<430,190>何<620,660>我<1280,200>总<1480,210>是
		""".trimIndent()
		val parsed = RelativeMillisLrcParser.parse(content.split("\n"))
		assertEquals(1, parsed.lines.size)
		val line = parsed.lines.single() as KaraokeLine
		assertEquals(5, line.syllables.size)

		val first = line.syllables[0]
		assertEquals("为", first.content)
		// 行 14.30s = 14300ms；词相对偏移 0 → 绝对 14300，时长 430
		assertEquals(14300, first.start)
		assertEquals(14730, first.end)

		val second = line.syllables[1]
		assertEquals("何", second.content)
		assertEquals(14300 + 430, second.start)

		// 行级 start/end 与首末词对齐
		assertEquals(14300, line.start)
		assertEquals(14300 + 1480 + 210, line.end)
	}

	@Test
	fun `parse krc style triple tag words still resolve`() {
		// 三元组 <start,dur,reserved> 变体（KRC 风格词级，但行级是 mm:ss.xx）
		val content = "[01:02.50]<0,60,0>覆<60,60,0>灭"
		val parsed = RelativeMillisLrcParser.parse(content.split("\n"))
		val line = parsed.lines.single() as KaraokeLine
		assertEquals(2, line.syllables.size)
		// 01:02.50 → 62500ms（parseAsTime 按分:秒.厘秒折算）
		assertEquals(62500, line.syllables[0].start)
		assertEquals(62560, line.syllables[1].start)
	}

	@Test
	fun `parse plain line without word tags degrades to synced line`() {
		val content = """
			[00:14.30]<0,430>为何我
			[00:14.35]失眠的晚上 谁烦扰着我
		""".trimIndent()
		val parsed = RelativeMillisLrcParser.parse(content.split("\n"))
		// 逐词行 + 相邻 ±150ms 纯文本行 → 配对为 translation
		val karaoke = parsed.lines.filterIsInstance<KaraokeLine>().single()
		assertEquals("失眠的晚上 谁烦扰着我", karaoke.translation)
		assertTrue(parsed.lines.none { it is SyncedLine && it.translation == null && it.content.contains("<") })
	}

	@Test
	fun `parse never leaks raw word tags into rendered content`() {
		val content = "[00:14.30]<0,430>为<430,190>何<620,660>我"
		val parsed = RelativeMillisLrcParser.parse(content.split("\n"))
		val line = parsed.lines.single()
		val rendered = when (line) {
			is KaraokeLine -> line.syllables.joinToString("") { it.content }
			is SyncedLine -> line.content
			else -> ""
		}
		assertFalse("原始标记不得泄漏: $rendered", rendered.contains("<") || rendered.contains(","))
	}
}

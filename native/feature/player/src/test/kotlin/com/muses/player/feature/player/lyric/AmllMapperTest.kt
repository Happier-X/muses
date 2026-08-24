package com.muses.player.feature.player.lyric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmllMapperTest {

	@Test
	fun `map karaoke line produces word-level entries`() {
		val synced = LyricsParser.parse(
			AmllMapperTest::class.java.classLoader!!.getResourceAsStream("lyrics/sample1.ttml")!!
				.readBytes().decodeToString(),
		)!!
		val lines = AmllMapper.toAmllLines(synced)
		assertEquals(synced.lines.size, lines.size)

		val first = lines.first()
		assertTrue("karaoke line must map to word-level entries", first.words.size > 1)
		assertTrue(first.endTime >= first.startTime)
		assertTrue(first.words.all { it.word.isNotEmpty() })
		assertTrue("word times must stay within line window", first.words.all { it.startTime >= first.startTime - 1 && it.endTime <= first.endTime + 1 })
	}

	@Test
	fun `map LRC line folds to single word with translation attached`() {
		val synced = LyricsParser.parse(
			AmllMapperTest::class.java.classLoader!!.getResourceAsStream("lyrics/bilingual.lrc")!!
				.readBytes().decodeToString(),
		)!!
		val lines = AmllMapper.toAmllLines(synced)
		assertTrue(lines.isNotEmpty())

		val translated = lines.filter { it.translatedLyric.isNotBlank() }
		assertTrue("expected translations mapped, got ${lines.map { it.translatedLyric }}", translated.isNotEmpty())
		translated.forEach { line ->
			assertEquals(1, line.words.size)
			assertEquals(line.words.first().startTime, line.startTime)
			assertEquals(line.words.first().endTime, line.endTime)
		}
		// 主行应为非 Han（样本原文为英文）
		translated.forEach { line ->
			assertFalse(Regex("""[\u4e00-\u9fff]""").containsMatchIn(line.words.first().word))
		}
	}

	@Test
	fun `toJson emits field names matching AMLL core contract and escapes properly`() {
		val payload = AmllPayload(
			lines = listOf(
				AmllLyricLine(
					words = listOf(AmllWord(0, 1000, "Hello \"world\"\nline2")),
					startTime = 0,
					endTime = 1000,
					translatedLyric = "你好",
					romanLyric = "",
					isBG = false,
					isDuet = true,
				),
			),
			coverUrl = "https://appassets.androidplatform.net/assets/amll/cover.jpg",
			songId = "song-1",
		)
		val json = AmllMapper.toJson(payload)

		assertTrue(json.contains("\"words\":"))
		assertTrue(json.contains("\"startTime\":"))
		assertTrue(json.contains("\"endTime\":"))
		assertTrue(json.contains("\"word\":"))
		assertTrue(json.contains("\"translatedLyric\":\"你好\""))
		assertTrue(json.contains("\"romanLyric\":"))
		assertTrue(json.contains("\"isBG\":false"))
		assertTrue(json.contains("\"isDuet\":true"))
		assertTrue(json.contains("\"coverUrl\":\"https://appassets.androidplatform.net/assets/amll/cover.jpg\""))
		assertTrue(json.contains("\"songId\":\"song-1\""))
		// 引号与换行必须被转义（否则 JSON 非法）
		assertTrue(json.contains("\\\"world\\\""))
		assertTrue(json.contains("\\n"))
	}

	@Test
	fun `toJson with null cover emits null literal`() {
		val json = AmllMapper.toJson(AmllPayload(emptyList(), coverUrl = null, songId = "s"))
		assertTrue(json.contains("\"coverUrl\":null"))
		assertTrue(json.contains("\"lines\":[]"))
	}
}

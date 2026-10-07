package com.muses.player.core.ai

import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.OnlineTextSource
import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.scrape.editmeta.EditLyricsCandidate
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiScrapeMatchTest {
    private val input = AiScrapeInput(
        Song("song", "private-source", "/private/music/secret.mp3", "晴天", "周杰伦", durationSec = 269),
        "晴天", "周杰伦", "",
        listOf(
            TextMetaHit("晴天", "周杰伦", "叶惠美", OnlineTextSource.KW),
            TextMetaHit("晴天（现场版）", "周杰伦", "演唱会", OnlineTextSource.WY),
        ),
        listOf(EditLyricsCandidate("[00:01.00]故事的小黄花", "lrc", "wy")),
    )

    @Test
    fun `只接受已有候选且有判断依据的推荐`() {
        val result = parseAiScrapeDecision(
            """{"text":{"candidateId":"text-0","confidence":"high","reason":"原歌手与专辑版本一致","title":"伪造标题"},"lyrics":{"candidateId":"lyrics-0","confidence":"high","reason":"歌词片段对应歌曲"}}""", input,
        )
        assertEquals(0, result.text.index)
        assertEquals(0, result.lyrics.index)
        assertEquals("原歌手与专辑版本一致", result.text.reason)
    }

    @Test
    fun `不确定的推荐保留原选择`() {
        val result = parseAiScrapeDecision("""{"text":{"candidateId":"text-1","confidence":"uncertain","reason":"无法判断录音版本"}}""", input)
        assertNull(result.text.index)
        assertNull(result.lyrics.index)
        assertTrue(result.text.reason.contains("保留原选择"))
    }

    @Test
    fun `负数越界跨维度和错误类型均不接受`() {
        listOf("\"text--1\"", "\"text-2\"", "\"text-0.5\"", "\"lyrics-0\"", "0", "null", "true", "{}", "[\"text-0\"]").forEach { id ->
            val result = parseAiScrapeDecision("""{"text":{"candidateId":$id,"confidence":"high","reason":"模拟理由"}}""", input)
            assertNull(result.text.index, id)
        }
    }

    @Test
    fun `没有理由或没有置信判断不自动选择`() {
        val noReason = parseAiScrapeDecision("""{"text":{"candidateId":"text-0","confidence":"high","reason":" "}}""", input)
        val noConfidence = parseAiScrapeDecision("""{"text":{"candidateId":"text-0","reason":"模拟理由"}}""", input)
        assertNull(noReason.text.index)
        assertNull(noConfidence.text.index)
    }

    @Test
    fun `允许代码围栏但错误JSON给出失败`() {
        val parsed = parseAiScrapeDecision("```json\n{\"text\":{\"candidateId\":\"text-0\",\"confidence\":\"high\",\"reason\":\"匹配一致\"}}\n```", input)
        assertEquals(0, parsed.text.index)
        listOf("不是候选结果", "[]", "{}", "{\"text\":[]}").forEach {
            assertFailsWith<AiException> { parseAiScrapeDecision(it, input) }
        }
    }

    @Test
    fun `空候选不能产生选择`() {
        val empty = input.copy(textCandidates = listOf(TextMetaHit(source = OnlineTextSource.KW)), lyricsCandidates = listOf(EditLyricsCandidate(" ", "lrc", "wy")))
        val result = parseAiScrapeDecision("""{"text":{"candidateId":"text-0","confidence":"high","reason":"模拟理由"},"lyrics":{"candidateId":"lyrics-0","confidence":"high","reason":"模拟理由"}}""", empty)
        assertNull(result.text.index)
        assertNull(result.lyrics.index)
    }

    @Test
    fun `提示只包含比较所需的数据并限制歌词长度`() {
        val prompt = buildAiScrapePrompt(input.copy(
            song = input.song.copy(lyrics = "开头" + "中".repeat(5000) + "结尾"),
            lyricsCandidates = List(20) { EditLyricsCandidate("[00:01]开头" + "中".repeat(5000) + "[04:29]结尾", "lrc", "wy") },
        ))
        assertFalse(prompt.contains("/private/music/secret.mp3"))
        assertFalse(prompt.contains("private-source"))
        assertTrue(prompt.contains("[04:29]结尾"))
        assertTrue(prompt.contains("lyrics-11"))
        assertFalse(prompt.contains("lyrics-12"))
        assertTrue(prompt.length < 25_000)
    }

    @Test
    fun `未发送的候选不能由模型选择`() {
        val many = input.copy(textCandidates = List(14) { input.textCandidates.first() })
        val result = parseAiScrapeDecision("""{"text":{"candidateId":"text-12","confidence":"high","reason":"模拟理由"}}""", many)
        assertNull(result.text.index)
    }

    @Test
    fun `请求使用低温度和已有配置并解析结果`() = runTest {
        var sentBody = ""
        val engine = MockEngine { request ->
            assertEquals("Bearer fake-key", request.headers[HttpHeaders.Authorization])
            sentBody = (request.body as TextContent).text
            val response = buildJsonObject {
                put("choices", buildJsonArray {
                    add(buildJsonObject {
                        put("message", buildJsonObject {
                            put("content", """{"text":{"candidateId":"text-0","confidence":"high","reason":"原歌手与版本一致"}}""")
                        })
                    })
                })
            }
            respond(response.toString(), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine)
        try {
            val service = AiScrapeMatchService(AiChatClient(client))
            val result = service.match(input, AiRecommendConfig("https://example.com/v1", "configured-model", "fake-key"))
            assertEquals(0, result.text.index)
            assertTrue(sentBody.contains("\"temperature\":0.1"))
            assertTrue(sentBody.contains("configured-model"))
            assertFalse(sentBody.contains("fake-key"))
            assertFalse(sentBody.contains("/private/music/secret.mp3"))
        } finally { client.close() }
    }
}

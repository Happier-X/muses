package com.muses.player.core.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AI 推荐解析与归一化单测。
 *
 * 为什么重点测这里：LLM 输出**不可控**（各家模型都可能加 markdown、改字段名、多说话），
 * 解析是全链路里最容易悄悄退化的环节——退化表现是「首页一直说没匹配到」，
 * 而不是抛错，靠肉眼很难发现。
 */
class AiRecommendParsingTest {

    @Test
    fun 解析标准JSON数组() {
        val content = """
            [
              {"name":"晴天","artist":"周杰伦","reason":"同歌手经典"},
              {"name":"海阔天空","artist":"Beyond","reason":"同年代摇滚"}
            ]
        """.trimIndent()
        val result = parseSuggestions(content)
        assertEquals(2, result.size)
        assertEquals("晴天", result[0].name)
        assertEquals("周杰伦", result[0].artist)
        assertEquals("同歌手经典", result[0].reason)
    }

    @Test
    fun 解析markdown围栏包裹的数组() {
        val content = """
            好的，以下是推荐：
            ```json
            [{"name":"稻香","artist":"周杰伦"}]
            ```
            希望对你有帮助。
        """.trimIndent()
        val result = parseSuggestions(content)
        assertEquals(1, result.size)
        assertEquals("稻香", result[0].name)
        assertEquals("周杰伦", result[0].artist)
    }

    @Test
    fun 解析字符串数组形态() {
        // 部分模型会退化成 ["歌名 - 歌手", ...]
        val content = """["晴天 - 周杰伦", "Yesterday"]"""
        val result = parseSuggestions(content)
        assertEquals(2, result.size)
        assertEquals("晴天", result[0].name)
        assertEquals("周杰伦", result[0].artist)
        assertEquals("Yesterday", result[1].name)
        assertEquals(null, result[1].artist)
    }

    @Test
    fun 解析字段别名与缺字段() {
        // title/song + singer 是常见别名；缺 artist 也要能收（匹配时按歌名精确即可）
        val content = """[{"title":"告白气球","singer":"周杰伦"},{"song":"小幸运"}]"""
        val result = parseSuggestions(content)
        assertEquals(2, result.size)
        assertEquals("告白气球", result[0].name)
        assertEquals("周杰伦", result[0].artist)
        assertEquals("小幸运", result[1].name)
    }

    @Test
    fun 无法解析时返回空列表不抛错() {
        assertEquals(emptyList(), parseSuggestions("抱歉，我无法推荐歌曲。"))
        assertEquals(emptyList(), parseSuggestions(""))
    }

    @Test
    fun 归一化忽略大小写括号标点与空白() {
        assertEquals("晴天", "晴天 (Live)".normalizeForMatch())
        assertEquals("晴天", "晴天（重制版）".normalizeForMatch())
        assertEquals("yesterday", "Yesterday".normalizeForMatch())
        // 标点全部剔除（採号也不留，保证 "Don't Stop" 与 "Dont Stop" 视为同一首）
        assertEquals("dontstop", "Don't Stop".normalizeForMatch())
        assertEquals("", null.normalizeForMatch())
    }

    @Test
    fun 去重按歌名加歌手归一化() {
        val list = listOf(
            AiSongSuggestion("晴天", "周杰伦"),
            AiSongSuggestion("晴天 (Live)", "周杰伦"),
            AiSongSuggestion("晴天", "周杰伦"),
            AiSongSuggestion("稻香", "周杰伦"),
        )
        assertEquals(2, list.deduplicate().size)
    }

    @Test
    fun 配置解析预设默认值与可用性() {
        // 留空 → 用预设默认
        val presetOnly = AiRecommendConfig(providerKey = "deepseek", apiKey = "sk-test")
        assertEquals("https://api.deepseek.com/v1", presetOnly.resolvedBaseUrl)
        assertEquals("deepseek-chat", presetOnly.resolvedModel)
        assertTrue(presetOnly.isUsable)

        // 自定义覆盖预设（含尾斜杠要去掉，避免拼出 //chat/completions）
        val overridden = AiRecommendConfig(
            providerKey = "deepseek",
            baseUrl = "https://my-relay.example.com/v1/",
            model = "my-model",
            apiKey = "sk-test",
        )
        assertEquals("https://my-relay.example.com/v1", overridden.resolvedBaseUrl)
        assertEquals("my-model", overridden.resolvedModel)

        // 自定义服务商但没填地址 → 不可用
        val customEmpty = AiRecommendConfig(providerKey = "custom", apiKey = "sk-test")
        assertFalse(customEmpty.isUsable)

        // 缺 Key → 不可用
        assertFalse(AiRecommendConfig(providerKey = "deepseek").isUsable)

        // 未知服务商回落默认预设（不至于因配置串损坏而崩）
        assertEquals(AiProviderPreset.DEEPSEEK, AiProviderPreset.fromKey("not-exist"))
    }

    @Test
    fun 画像prompt不含路径与凭据() {
        val profile = LibraryProfile(
            totalSongs = 2,
            sourceBreakdown = listOf("本地" to 1, "WebDAV" to 1),
            topArtists = listOf("周杰伦" to 2),
            topAlbums = listOf("叶惠美" to 1),
            sampleTracks = listOf("晴天 - 周杰伦", "稻香 - 周杰伦"),
        )
        val text = profile.toPromptText()

        assertTrue(text.contains("总曲目：2 首"))
        assertTrue(text.contains("本地 1 首"))
        assertTrue(text.contains("周杰伦"))
        assertTrue(text.contains("晴天 - 周杰伦"))
        // 外发面必须守住：路径/来源地址不出现在 prompt 里
        assertFalse(text.contains("/storage/"))
        assertFalse(text.contains("http://"))
        assertFalse(text.contains("https://"))
        assertFalse(text.contains("smb://"))
    }
}

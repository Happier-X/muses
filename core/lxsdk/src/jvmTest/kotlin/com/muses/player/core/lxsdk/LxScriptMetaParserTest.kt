package com.muses.player.core.lxsdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [LxScriptMetaParser] 单元测试：对齐洛雪「自定义源信息」规范 */
class LxScriptMetaParserTest {

    @Test
    fun `解析标准双星注释块`() {
        val src = """
            /**
             * @name 测试音乐源
             * @description 我只是一个测试音乐源哦
             * @version 1.0.0
             * @author xxx
             * @homepage http://xxx
             */
            const a = 1;
        """.trimIndent()
        val meta = LxScriptMetaParser.parse(src)
        assertEquals("测试音乐源", meta.name)
        assertEquals("我只是一个测试音乐源哦", meta.description)
        assertEquals("1.0.0", meta.version)
        assertEquals("xxx", meta.author)
        assertEquals("http://xxx", meta.homepage)
    }

    @Test
    fun `解析尖叹号注释块`() {
        // 洛雪官方仓库常见 `/*! ... @preserve */` 形态
        val src = """
            /*!
             * @name 六音音源
             * @description v1.2.1
             * @version v1.2.1
             * @author 六音
             * @homepage www.sixyin.com
             * @preserve
             */
            const x = 1;
        """.trimIndent()
        val meta = LxScriptMetaParser.parse(src)
        assertEquals("六音音源", meta.name)
        assertEquals("v1.2.1", meta.version)
        assertEquals("六音", meta.author)
    }

    @Test
    fun `可选字段缺失时返回 null`() {
        val src = """
            /**
             * @name 最小源
             */
            const x = 1;
        """.trimIndent()
        val meta = LxScriptMetaParser.parse(src)
        assertEquals("最小源", meta.name)
        assertNull(meta.description)
        assertNull(meta.version)
        assertNull(meta.author)
    }

    @Test
    fun `无注释块时返回全空`() {
        val meta = LxScriptMetaParser.parse("const a = 1;")
        assertNull(meta.name)
        assertNull(meta.version)
    }

    @Test
    fun `只解析首个注释块不误抓后续内容`() {
        // 脚本正文里含 @name 字样的字符串，不应被当成元信息
        val src = """
            /**
             * @name 真源
             */
            const fake = "@name 假源";
            /* @name 另一个假源 */
        """.trimIndent()
        val meta = LxScriptMetaParser.parse(src)
        assertEquals("真源", meta.name)
    }

    @Test
    fun `单行块注释也可解析`() {
        val meta = LxScriptMetaParser.parse("/* @name 单行源 */\nconst a=1;")
        assertEquals("单行源", meta.name)
    }

    @Test
    fun `逐行解析不会把正文里的标签误当元信息`() {
        // 正文中若有形如 `@author` 的行（非元信息区），不应污染解析结果
        val src = """
            /**
             * @name 正牌源
             * @author 真作者
             */
            const s = 'path/@author 假作者';
        """.trimIndent()
        val meta = LxScriptMetaParser.parse(src)
        assertEquals("正牌源", meta.name)
        assertEquals("真作者", meta.author)
    }
}

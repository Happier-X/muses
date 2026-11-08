package com.muses.player.feature.shell.platform

import kotlin.test.Test
import kotlin.test.assertEquals

/** 更新日志 markdown → 弹窗纯文本（去掉标题井号/列表符号/分隔线/行内标记）。 */
class PlainNotesTest {

    @Test
    fun 标题与列表转成纯文本() {
        val markdown = """
            ## 新增
            - 刮削来源扩充：新增 MusicBrainz
            - **审核草稿**跨启动恢复
            ---
            ### 修复
            * `歌词`提前展示
        """.trimIndent()

        assertEquals(
            "新增\n· 刮削来源扩充：新增 MusicBrainz\n· 审核草稿跨启动恢复\n修复\n· 歌词提前展示",
            plainNotes(markdown),
        )
    }

    @Test
    fun 空正文返回空串() {
        assertEquals("", plainNotes(""))
        assertEquals("", plainNotes("\n\n---\n"))
    }
}

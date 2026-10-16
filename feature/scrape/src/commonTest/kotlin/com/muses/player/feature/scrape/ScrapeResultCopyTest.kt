package com.muses.player.feature.scrape

import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.WritebackResult
import com.muses.player.core.model.scrape.WritebackStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrapeResultCopyTest {
    private fun result(
        songId: String,
        status: WritebackStatus,
        coverSkipped: Boolean = false,
        error: String? = null,
        message: String? = null,
    ) = WritebackResult(
        songId = songId,
        status = status,
        fileResult = FileWriteResult(
            ok = status != WritebackStatus.FAILED,
            code = if (status == WritebackStatus.FAILED) "write_failed" else null,
            message = message,
        ),
        libraryUpdated = status == WritebackStatus.SUCCESS,
        error = error,
        coverSkipped = coverSkipped,
    )

    @Test
    fun `单曲成功文案区分封面是否写入`() {
        assertEquals("文件标签已保存，曲库已更新", singleWritebackSuccessMessage(false))
        assertEquals("文字标签已保存，封面未写入文件，可稍后重新刮削封面", singleWritebackSuccessMessage(true))
    }

    @Test
    fun `批量汇总默认只统计三态结果`() {
        val lines = buildScrapeResultSummary(
            listOf(
                result("s1", WritebackStatus.SUCCESS),
                result("s2", WritebackStatus.SUCCESS),
                result("s3", WritebackStatus.FILE_FAILED),
                result("s4", WritebackStatus.FAILED),
            ),
        )
        assertEquals(listOf("成功 2 · 文件未保存 1 · 失败 1"), lines)
    }

    @Test
    fun `批量汇总只统计成功且封面未写入的歌曲`() {
        val lines = buildScrapeResultSummary(
            listOf(
                result("s1", WritebackStatus.SUCCESS, coverSkipped = true),
                result("s2", WritebackStatus.SUCCESS),
                result("s3", WritebackStatus.FILE_FAILED, coverSkipped = true),
            ),
        )
        assertEquals(
            listOf(
                "成功 2 · 文件未保存 1 · 失败 0",
                "其中 1 首封面未写入文件，文字信息已保存，可稍后重新刮削封面",
            ),
            lines,
        )
    }

    @Test
    fun `结果行详情在成功时仅于封面未写入给出说明`() {
        assertNull(writebackRowDetail(result("s1", WritebackStatus.SUCCESS)))
        assertEquals(
            "封面未写入文件，文字信息已保存，可稍后重新刮削封面",
            writebackRowDetail(result("s2", WritebackStatus.SUCCESS, coverSkipped = true)),
        )
        assertEquals("指定文案", writebackRowDetail(result("s3", WritebackStatus.FAILED, error = "指定文案")))
        assertEquals(
            "文件写入失败",
            writebackRowDetail(result("s4", WritebackStatus.FILE_FAILED, message = "文件写入失败")),
        )
        assertEquals("请检查文件写入权限或网络连接", writebackRowDetail(result("s5", WritebackStatus.FAILED)))
    }

    @Test
    fun `恢复入口文案区分未确认条目`() {
        assertEquals(
            "上次写回 2 首可恢复标签",
            pendingUndoSummaryText(ScrapeUndoInfo("j-1", songCount = 2, unconfirmedCount = 0)),
        )
        assertEquals(
            "上次写回 3 首可恢复，其中 1 首写入状态未确认，恢复前请先核对文件",
            pendingUndoSummaryText(ScrapeUndoInfo("j-2", songCount = 3, unconfirmedCount = 1)),
        )
    }
}

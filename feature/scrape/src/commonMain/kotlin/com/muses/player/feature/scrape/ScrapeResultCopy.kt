package com.muses.player.feature.scrape

import com.muses.player.core.model.scrape.WritebackResult
import com.muses.player.core.model.scrape.WritebackStatus

/**
 * 写回结果文案：区分「全部写入」与「文字已保存但封面未写入文件」。
 *
 * 封面可能因为地址失效、响应不是图片、下载失败或超出大小上限而没有写入文件；
 * 此时文字标签仍然保存成功，界面上必须说明差异，不能只显示成功。
 */

/** 单曲保存成功文案：封面未写入文件时给出可操作提示。 */
internal fun singleWritebackSuccessMessage(coverSkipped: Boolean): String = if (coverSkipped) {
    "文字标签已保存，封面未写入文件，可稍后重新刮削封面"
} else {
    "文件标签已保存，曲库已更新"
}

/** 批量结果汇总：第一行统计三类结果，封面有未写入时追加一行说明。 */
internal fun buildScrapeResultSummary(results: List<WritebackResult>): List<String> {
    val success = results.count { it.status == WritebackStatus.SUCCESS }
    val partial = results.count { it.status == WritebackStatus.FILE_FAILED }
    val failed = results.count { it.status == WritebackStatus.FAILED }
    val coverSkipped = results.count { it.status == WritebackStatus.SUCCESS && it.coverSkipped }
    return buildList {
        add("成功 $success · 文件未保存 $partial · 失败 $failed")
        if (coverSkipped > 0) {
            add("其中 $coverSkipped 首封面未写入文件，文字信息已保存，可稍后重新刮削封面")
        }
    }
}

/** 结果行详情：成功但封面未写入时提示差异；其余状态沿用既有失败原因。 */
internal fun writebackRowDetail(result: WritebackResult): String? = when {
    result.status == WritebackStatus.SUCCESS && result.coverSkipped ->
        "封面未写入文件，文字信息已保存，可稍后重新刮削封面"
    result.status == WritebackStatus.SUCCESS -> null
    else -> result.error ?: result.fileResult.message ?: "请检查文件写入权限或网络连接"
}

/** 队列页恢复入口文案：未确认写入的条目需要先核对文件。 */
internal fun pendingUndoSummaryText(info: ScrapeUndoInfo): String =
    if (info.unconfirmedCount > 0) {
        "上次写回 ${info.songCount} 首可恢复，其中 ${info.unconfirmedCount} 首写入状态未确认，恢复前请先核对文件"
    } else {
        "上次写回 ${info.songCount} 首可恢复标签"
    }

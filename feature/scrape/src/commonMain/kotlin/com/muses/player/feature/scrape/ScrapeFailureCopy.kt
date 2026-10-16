package com.muses.player.feature.scrape

import com.muses.player.core.scrape.editmeta.EditCloudMetaResult

/** 同类失败合并显示；只使用结构化原因，不展示响应正文或鉴权信息。 */
internal fun describeScrapeFailures(result: EditCloudMetaResult): List<String> {
    val dimensions = listOf(result.text, result.cover, result.lyrics)
    val failures = dimensions.flatMap { it.sourceFailures }.distinct()
    val details = failures.groupBy { it.kind to it.httpStatus }.map { (reason, sources) ->
        val names = sources.map { scrapeSourceLabel(it.source) }.distinct().joinToString("、")
        "$names：${reason.first.message}" + (reason.second?.let { "（HTTP $it）" } ?: "")
    }
    val unknown = dimensions.flatMap { it.failedSources }.distinct().filter { source -> failures.none { it.source == source } }
    return details + if (unknown.isEmpty()) emptyList() else
        listOf("${unknown.joinToString("、") { scrapeSourceLabel(it) }}：请求失败，请稍后重试")
}

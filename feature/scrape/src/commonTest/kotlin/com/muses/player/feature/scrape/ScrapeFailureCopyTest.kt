package com.muses.player.feature.scrape

import com.muses.player.core.model.scrape.TextMetaHit
import com.muses.player.core.network.ProviderFailureKind
import com.muses.player.core.scrape.editmeta.EditCloudMetaResult
import com.muses.player.core.scrape.editmeta.EditDimResult
import com.muses.player.core.scrape.editmeta.EditDimStatus
import com.muses.player.core.scrape.editmeta.EditSourceFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrapeFailureCopyTest {
    @Test fun `多个维度相同失败合并并显示具体状态`() {
        val failure = EditSourceFailure("wy", ProviderFailureKind.RATE_LIMITED, 429)
        val text = EditDimResult<TextMetaHit>(EditDimStatus.NETWORK, emptyList(),
            failedSources = listOf("wy"), sourceFailures = listOf(failure))
        val result = EditCloudMetaResult(text,
            EditDimResult(EditDimStatus.NETWORK, emptyList(), failedSources = listOf("wy"), sourceFailures = listOf(failure)),
            EditDimResult(EditDimStatus.NO_MATCH, emptyList()))
        val copy = describeScrapeFailures(result)
        assertEquals(1, copy.size)
        assertTrue("请求过于频繁" in copy.single())
        assertTrue("HTTP 429" in copy.single())
    }

    @Test fun `兼容旧失败来源且无结果不显示网络错误`() {
        val empty = EditCloudMetaResult(EditDimResult(EditDimStatus.NO_MATCH, emptyList()),
            EditDimResult(EditDimStatus.NO_MATCH, emptyList()), EditDimResult(EditDimStatus.NO_MATCH, emptyList()))
        assertTrue(describeScrapeFailures(empty).isEmpty())
        val legacy = empty.copy(text = empty.text.copy(failedSources = listOf("kw")))
        assertTrue("请求失败" in describeScrapeFailures(legacy).single())
    }
}

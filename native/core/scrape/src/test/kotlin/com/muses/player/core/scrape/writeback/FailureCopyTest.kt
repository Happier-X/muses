package com.muses.player.core.scrape.writeback

import org.junit.Assert.assertEquals
import org.junit.Test

/** 规格 = src/features/scrape/failure-copy.ts 逐条规则 */
class FailureCopyTest {

    @Test
    fun `已知code映射原因分组`() {
        assertEquals(WritebackFailureCategory.AUTH, classifyWritebackFailure("no_password"))
        assertEquals(WritebackFailureCategory.AUTH, classifyWritebackFailure("missingCredentials"))
        assertEquals(WritebackFailureCategory.NETWORK, classifyWritebackFailure("download_failed"))
        assertEquals(WritebackFailureCategory.UPLOAD, classifyWritebackFailure("put_failed"))
    }

    @Test
    fun `未知或空code归other`() {
        assertEquals(WritebackFailureCategory.OTHER, classifyWritebackFailure(null))
        assertEquals(WritebackFailureCategory.OTHER, classifyWritebackFailure("write_failed"))
        assertEquals(WritebackFailureCategory.OTHER, classifyWritebackFailure("empty_file"))
        assertEquals(WritebackFailureCategory.OTHER, classifyWritebackFailure("unknown"))
    }

    @Test
    fun `download_failed优先透传message`() {
        val described = describeWritebackFailure(
            WritebackFailureInput(fileResultCode = "download_failed", fileResultMessage = "http 507", error = "err"),
        )
        assertEquals("http 507", described)
    }

    @Test
    fun `download_failed无message用固定文案`() {
        val described = describeWritebackFailure(
            WritebackFailureInput(fileResultCode = "download_failed", fileResultMessage = null),
        )
        assertEquals("下载 WebDAV 音频失败，请检查网络后重试", described)
    }

    @Test
    fun `其他已知code用固定文案`() {
        assertEquals(
            "WebDAV 地址缺失，请到音源设置补全后重试",
            describeWritebackFailure(WritebackFailureInput(fileResultCode = "missingUrl", fileResultMessage = "ignored")),
        )
    }

    @Test
    fun `未知code兜底message再error再默认`() {
        assertEquals(
            "native diag",
            describeWritebackFailure(WritebackFailureInput(fileResultCode = "AudioMeta-9", fileResultMessage = "native diag")),
        )
        assertEquals(
            "fallback err",
            describeWritebackFailure(WritebackFailureInput(fileResultCode = "AudioMeta-9", fileResultMessage = null, error = "fallback err")),
        )
        assertEquals(
            "写回失败",
            describeWritebackFailure(WritebackFailureInput(fileResultCode = null, fileResultMessage = null, error = null)),
        )
    }
}

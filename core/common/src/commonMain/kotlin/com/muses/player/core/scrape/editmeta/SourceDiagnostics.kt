package com.muses.player.core.scrape.editmeta

/** 不保存请求地址、原始响应或凭据。未验证的封面地址不能报告成功。 */
data class SourceDiagnosticTarget(val source: String, val dimension: EditDimKey)
enum class SourceDiagnosticStatus { OK, NO_MATCH, FAILED, UNVERIFIED }
data class SourceDiagnosticResult(
    val target: SourceDiagnosticTarget,
    val status: SourceDiagnosticStatus,
    val elapsedMs: Long,
    val candidateCount: Int = 0,
    val failure: EditSourceFailure? = null,
    val imageChecked: Boolean = false,
)

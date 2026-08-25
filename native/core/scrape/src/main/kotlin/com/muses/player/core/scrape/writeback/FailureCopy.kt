package com.muses.player.core.scrape.writeback

/**
 * 刮削写回失败文案映射（规格书 = src/features/scrape/failure-copy.ts，逐条翻译）。
 *
 * code → 人话文案与原因分组，供刮削结果态展示/汇总归类；
 * 未知 code 兜底显示原始 message，保证新增错误码不被吞掉。
 */

/** 失败原因分组：网络 / 认证（凭据、配置）/ 上传 / 其他 */
enum class WritebackFailureCategory {
    NETWORK,
    AUTH,
    UPLOAD,
    OTHER,
}

/** describeWritebackFailure 的入参形态 */
data class WritebackFailureInput(
    val fileResultCode: String?,
    val fileResultMessage: String?,
    val error: String? = null,
)

/** code → 原因分组映射表（failure-copy.ts CODE_CATEGORY） */
private val CODE_CATEGORY: Map<String, WritebackFailureCategory> = mapOf(
    "no_password" to WritebackFailureCategory.AUTH,
    "missingCredentials" to WritebackFailureCategory.AUTH,
    "download_failed" to WritebackFailureCategory.NETWORK,
    "put_failed" to WritebackFailureCategory.UPLOAD,
)

/** 按已知 code 返回固定人话文案；未知 code 返回 null 走兜底（failure-copy.ts FIXED_COPY） */
private val FIXED_COPY: Map<String, String> = mapOf(
    "no_password" to "WebDAV 密码缺失，请到音源设置补全后重试",
    "missingCredentials" to "WebDAV 密码缺失，请到音源设置补全后重试",
    "missingUrl" to "WebDAV 地址缺失，请到音源设置补全后重试",
    "download_failed" to "下载 WebDAV 音频失败，请检查网络后重试",
)

/**
 * 失败码 → 原因分组：
 * 网络问题=download_failed；认证问题=no_password/missingCredentials；
 * 上传失败=put_failed；其余（empty_file/write_failed/not_implemented/
 * 原生诊断码/unknown）归 OTHER。
 */
fun classifyWritebackFailure(code: String?): WritebackFailureCategory =
    code?.let { CODE_CATEGORY[it] } ?: WritebackFailureCategory.OTHER

/**
 * 写回结果 → 行详情人话文案：
 * - download_failed：优先透传含 HTTP 码/超时原因的 message，为空才用固定文案
 * - 已知 code 映射固定文案
 * - 未知 code 兜底 message || error || 「写回失败」
 */
fun describeWritebackFailure(input: WritebackFailureInput): String {
    val (code, message, error) = input
    if (code == "download_failed") {
        return message ?: FIXED_COPY.getValue("download_failed")
    }
    val fixed = code?.let { FIXED_COPY[it] }
    if (fixed != null) {
        return fixed
    }
    return message ?: error ?: "写回失败"
}

package com.muses.player.core.ai

import io.ktor.http.Url

/** 配置格式错误，尚未向服务端发送请求。 */
class AiServiceUrlException(message: String) : AiException(message)

/** 清理粘贴地址时带入的空白及不可见格式字符，不猜测或替换服务器地址。 */
fun normalizeAiBaseUrl(value: String): String = value
    .filterNot { it == '\r' || it == '\n' || it == '\t' || it == '\u200B' || it == '\uFEFF' }
    .trim()
    .trimEnd('/')

/** 在发送请求前校验地址，避免把 Ktor 的解析异常直接展示给用户。 */
fun validateAiBaseUrl(value: String): String {
    val normalized = normalizeAiBaseUrl(value)
    val hasScheme = normalized.startsWith("https://", ignoreCase = true) ||
        normalized.startsWith("http://", ignoreCase = true)
    val authority = normalized.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#')
    val parsed = if (hasScheme && authority.isNotBlank() && normalized.none { it.isWhitespace() }) {
        runCatching { Url(normalized) }.getOrNull()
    } else null
    if (parsed == null || parsed.host.isBlank()) {
        throw AiServiceUrlException("服务地址无效，请检查 http:// 或 https:// 前是否有多余字符")
    }
    if ('?' in normalized || '#' in normalized || '@' in authority) {
        throw AiServiceUrlException("服务地址无效，请使用不含参数或登录信息的服务基址")
    }
    return normalized
}

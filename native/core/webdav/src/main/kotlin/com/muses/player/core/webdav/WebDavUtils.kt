package com.muses.player.core.webdav

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * WebDAV 路径工具函数 —— 一比一翻译自 src/features/sources/webdav.ts。
 *
 * 翻译规则（design.md §1）：
 * - 函数签名/行为/边界条件逐条对照 TypeScript 实现；
 * - 命名保持 Kotlin 驼峰惯例，但语义与 Web 层一一对应。
 */

/** 确保路径以 / 开头 */
private fun ensureLeadingSlash(path: String): String {
    if (path.isBlank()) return "/"
    return if (path.startsWith("/")) path else "/$path"
}

/**
 * 标准化 WebDAV 路径 —— normalizeWebDavPath
 * - 去除首尾空白 + 多余斜杠 + 尾部斜杠（根路径除外）
 */
fun normalizeWebDavPath(path: String): String {
    val normalized = ensureLeadingSlash(path.trim()).replace(Regex("/+"), "/")
    return if (normalized.length > 1 && normalized.endsWith("/")) {
        normalized.dropLast(1)
    } else {
        normalized
    }
}

/**
 * 获取父目录路径 —— getParentWebDavPath
 * - 根路径 "/" 返回 null
 */
fun getParentWebDavPath(path: String): String? {
    val normalized = normalizeWebDavPath(path)
    if (normalized == "/") return null
    val parent = normalized.substring(0, normalized.lastIndexOf('/'))
    return parent.ifEmpty { "/" }
}

/**
 * 安全解码 URI 组件
 */
private fun safeDecode(value: String): String {
    return try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }
}

/**
 * 获取 WebDAV 路径的显示名称 —— getWebDavDisplayName
 * - 根路径 "/" 返回 "/"
 * - 其他路径返回最后一段的解码名
 */
fun getWebDavDisplayName(path: String): String {
    val normalized = normalizeWebDavPath(path)
    if (normalized == "/") return "/"
    val segments = normalized.split("/").filter { it.isNotEmpty() }
    val lastSegment = segments.lastOrNull() ?: normalized
    return safeDecode(lastSegment)
}

/**
 * 编码路径段 —— encodePath
 * - 根路径保持 "/"
 * - 其他路径每段独立 URLEncode
 */
private fun encodePath(path: String): String {
    val normalized = normalizeWebDavPath(path)
    if (normalized == "/") return "/"
    val encoded = normalized
        .split("/")
        .filter { it.isNotEmpty() }
        .joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
    return "/$encoded"
}

/**
 * 构建完整的 WebDAV URL —— buildWebDavUrl
 * - serverUrl 去除尾部斜杠 + 编码后的 path
 */
fun buildWebDavUrl(serverUrl: String, path: String): String {
    val trimmedServer = serverUrl.trim().trimEnd('/')
    val encodedPath = encodePath(path)
    return "$trimmedServer$encodedPath"
}

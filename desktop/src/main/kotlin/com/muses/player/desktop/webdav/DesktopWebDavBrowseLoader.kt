package com.muses.player.desktop.webdav

/**
 * 桌面 WebDAV 目录浏览加载器（浏览页共用化的桌面业务侧）。
 *
 * W4 收敛（任务 09-05-scrape-kmp R2）：PROPFIND 请求/解析/429 退避统一收敛至
 * [DesktopWebDavClient]（与安卓 `KtorWebDavClient` 同契约），本类只保留：
 * - 浏览页专用过滤（仅目录 + 名称排序）；
 * - 路径工具（normalize/parent/buildUrl）与 `core:webdav.WebDavUtils` 同语义的桌面本地实现
 *   （不依赖 `:core:webdav`——安卓库形态，桌面 JVM 无法消费）。
 */
object DesktopWebDavBrowseLoader {

    /** 桌面浏览条目（调用方映射为共用 `WebDavBrowseItem`）。 */
    data class Entry(
        val name: String,
        val url: String,
        val isDirectory: Boolean,
        val contentLength: Long = 0L,
        val lastModified: String? = null,
    )

    /** 共享客户端：浏览与刮削/播放链共享同一限流桶与 HttpClient。 */
    private val sharedClient: DesktopWebDavClient by lazy { DesktopWebDavClient() }

    /** PROPFIND depth 1 列出目录内容（仅返回目录项，调用方无需二次过滤）。 */
    suspend fun listDirectories(
        serverUrl: String,
        path: String,
        username: String,
        password: String,
    ): List<Entry> {
        sharedClient.authenticate(username = username, password = password)
        val url = buildUrl(serverUrl, path)
        return sharedClient.list(url)
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
            .map { item ->
                Entry(
                    name = item.name,
                    url = item.url,
                    isDirectory = item.isDirectory,
                    contentLength = item.contentLength,
                    lastModified = item.lastModified,
                )
            }
    }

    /** 与 `core:webdav.buildWebDavUrl` 同语义（server 去尾斜杠 + 编码 path）。 */
    fun buildUrl(serverUrl: String, path: String): String {
        val trimmedServer = serverUrl.trim().trimEnd('/')
        return "$trimmedServer${encodePath(normalizePath(path))}"
    }

    /** 与 `core:webdav.normalizeWebDavPath` 同语义。 */
    fun normalizePath(path: String): String {
        val raw = if (path.isBlank()) "/" else path.trim()
        val withLeading = if (withLeading(raw)) raw else "/$raw"
        val collapsed = withLeading.replace(Regex("/+"), "/")
        return if (collapsed.length > 1 && collapsed.endsWith("/")) collapsed.dropLast(1) else collapsed
    }

    private fun withLeading(raw: String) = raw.startsWith("/")

    /** 与 `core:webdav.getParentWebDavPath` 同语义（根返回 null）。 */
    fun parentPath(path: String): String? {
        val normalized = normalizePath(path)
        if (normalized == "/") return null
        val parent = normalized.substring(0, normalized.lastIndexOf('/'))
        return parent.ifEmpty { "/" }
    }

    private fun encodePath(path: String): String {
        if (path == "/") return "/"
        val encoded = path.split("/").filter { it.isNotEmpty() }
            .joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        return "/$encoded"
    }
}

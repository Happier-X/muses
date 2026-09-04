package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.getParentWebDavPath
import com.muses.player.core.webdav.normalizeWebDavPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** WebDAV 目录浏览状态 */
data class WebDavBrowseState(
    val currentPath: String = "/",
    val directories: List<WebDavDirectoryItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class WebDavBrowseViewModel constructor(
    private val webDavClient: WebDavClient,
) : ViewModel() {

    private val _browseState = MutableStateFlow(WebDavBrowseState())
    val browseState: StateFlow<WebDavBrowseState> = _browseState.asStateFlow()

    private var mode: String = "multiple"
    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""

    val parentPath: String?
        get() = getParentWebDavPath(_browseState.value.currentPath)

    /** 初始化（仅首次） */
    fun init(mode: String, initialPath: String, serverUrl: String, username: String, password: String) {
        if (this.serverUrl.isNotEmpty()) return // 已初始化

        this.mode = mode
        this.serverUrl = serverUrl
        this.username = username
        this.password = password

        val normalizedPath = normalizeWebDavPath(initialPath)
        _browseState.value = WebDavBrowseState(currentPath = normalizedPath)

        loadDirectories(normalizedPath)
    }

    /** 加载目录内容 */
    private fun loadDirectories(path: String) {
        val currentState = _browseState.value
        _browseState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                webDavClient.authenticate(username, password)
                val url = buildWebDavUrl(serverUrl, path)
                val items = webDavClient.list(url)

                // 过滤出目录（isDirectory = true）
                val directories = items
                    .filter { it.isDirectory }
                    .map { item ->
                        // 从 URL 中提取路径
                        val itemPath = extractPathFromUrl(item.url, serverUrl)
                        WebDavDirectoryItem(
                            basename = item.name,
                            path = normalizeWebDavPath(itemPath),
                        )
                    }
                    .sortedBy { it.basename.lowercase() }

                _browseState.value = _browseState.value.copy(
                    currentPath = normalizeWebDavPath(path),
                    directories = directories,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _browseState.value = _browseState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "读取 WebDAV 目录失败。",
                )
            }
        }
    }

    /** 返回上级目录 */
    fun goToParent() {
        val currentPath = _browseState.value.currentPath
        val parent = getParentWebDavPath(currentPath) ?: return
        loadDirectories(parent)
    }

    /** 进入子目录 */
    fun openDirectory(path: String) {
        loadDirectories(path)
    }

    /** 切换选择状态（多选模式） */
    fun toggleSelection(path: String) {
        val currentState = _browseState.value
        val newSelected = currentState.selectedPaths.toMutableSet()
        if (newSelected.contains(path)) {
            newSelected.remove(path)
        } else {
            newSelected.add(path)
        }
        _browseState.value = currentState.copy(selectedPaths = newSelected)
    }

    /** 清空选择 */
    fun clearSelection() {
        _browseState.value = _browseState.value.copy(selectedPaths = emptySet())
    }

    /** 关闭错误对话框 */
    fun dismissError() {
        _browseState.value = _browseState.value.copy(errorMessage = null)
    }

    /** 从完整 URL 中提取路径部分 */
    private fun extractPathFromUrl(fullUrl: String, baseUrl: String): String {
        return try {
            val baseUri = java.net.URI(baseUrl)
            val fullUri = java.net.URI(fullUrl)
            val basePath = normalizeWebDavPath(baseUri.path)
            val fullPath = normalizeWebDavPath(fullUri.path)

            if (fullPath.startsWith(basePath)) {
                val relativePath = fullPath.removePrefix(basePath)
                if (relativePath.isEmpty()) "/" else relativePath
            } else {
                fullPath
            }
        } catch (_: Exception) {
            fullUrl
        }
    }

    private fun buildWebDavUrl(serverUrl: String, path: String): String {
        val trimmedServer = serverUrl.trim().trimEnd('/')
        val normalizedPath = normalizeWebDavPath(path)
        return "$trimmedServer$normalizedPath"
    }
}

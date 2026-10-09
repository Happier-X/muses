package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.normalizeWebDavPath
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    // 初始化一次性门闩：并发与重组重复调用安全；不同参数视为切账号重建
    private val initialized = java.util.concurrent.atomic.AtomicBoolean(false)
    private var loadJob: Job? = null
    private var loadSeq = 0L

    /** 初始化（幂等：同参数重复调用直接返回，不同参数视为切账号重建） */
    fun init(mode: String, initialPath: String, serverUrl: String, username: String, password: String, selectedPaths: List<String> = emptyList()) {
        val normalizedPath = normalizeWebDavPath(initialPath)
        if (!initialized.compareAndSet(false, true)) {
            if (this.mode == mode && this.serverUrl == serverUrl && this.username == username && this.password == password &&
                _browseState.value.currentPath == normalizedPath
            ) return
        }
        this.mode = mode
        this.serverUrl = serverUrl
        this.username = username
        this.password = password

        val initialSelection = selectedPaths
            .map(::normalizeWebDavPath)
            .toSet()
        _browseState.value = WebDavBrowseState(
            currentPath = normalizedPath,
            selectedPaths = initialSelection,
        )

        loadDirectories(normalizedPath)
    }

    /** 加载目录内容（取消上一次未完成的加载，避免竞态覆盖） */
    private fun loadDirectories(path: String) {
        val currentState = _browseState.value
        _browseState.value = currentState.copy(isLoading = true, errorMessage = null)

        loadJob?.cancel()
        val requestSeq = ++loadSeq
        loadJob = viewModelScope.launch {
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

                if (requestSeq != loadSeq) return@launch
                _browseState.value = _browseState.value.copy(
                    currentPath = normalizeWebDavPath(path),
                    directories = directories,
                    isLoading = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestSeq != loadSeq) return@launch
                _browseState.value = _browseState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "读取 WebDAV 目录失败。",
                )
            }
        }
    }

    /** 重读当前目录，保留路径和用户已勾选的目录。 */
    fun refresh() {
        if (initialized.get() && !_browseState.value.isLoading) {
            loadDirectories(_browseState.value.currentPath)
        }
    }

    /** 从面包屑跳转到指定层级。 */
    fun navigateTo(path: String) {
        val normalizedPath = normalizeWebDavPath(path)
        if (normalizedPath == _browseState.value.currentPath) return
        if (mode == "single") clearSelection()
        loadDirectories(normalizedPath)
    }

    /** 进入子目录 */
    fun openDirectory(path: String) {
        if (mode == "single") clearSelection()
        loadDirectories(path)
    }

    /** 多选切换；单选时将当前目录设为唯一选中项。 */
    fun toggleSelection(path: String) {
        val currentState = _browseState.value
        if (mode == "single") {
            _browseState.value = currentState.copy(selectedPaths = setOf(path))
            return
        }
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

    /** 面板关闭时丢弃未确认选择，取消读取并释放本次连接信息。 */
    fun endSession() {
        ++loadSeq
        loadJob?.cancel()
        loadJob = null
        serverUrl = ""
        username = ""
        password = ""
        initialized.set(false)
        _browseState.value = WebDavBrowseState()
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

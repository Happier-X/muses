package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.decodeWebDavSourcePaths
import com.muses.player.core.model.encodeWebDavSourcePaths
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.getWebDavDisplayName
import com.muses.player.core.webdav.normalizeWebDavPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** WebDAV 表单状态 */
data class WebDavFormState(
    // 表单字段
    val name: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val path: String = "",
    val selectedPaths: List<String> = emptyList(),
    // 验证错误
    val nameError: String? = null,
    val serverUrlError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val pathError: String? = null,
    // 状态
    val isVerifying: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // 编辑模式
    val editingSourceId: String? = null,
    val editingSource: Source? = null,
)

@OptIn(ExperimentalUuidApi::class)
class WebDavFormViewModel constructor(
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
    private val webDavClient: WebDavClient,
) : ViewModel() {

    private val _formState = MutableStateFlow(WebDavFormState())
    val formState: StateFlow<WebDavFormState> = _formState.asStateFlow()

    /** 初始化编辑模式：加载现有音源数据 */
    fun initEditMode(sourceId: String) {
        val current = _formState.value
        if (current.editingSourceId == sourceId) return // 已初始化

        viewModelScope.launch {
            val source = sourceRepository.getSource(sourceId)?.takeIf { it.type == SourceType.WEBDAV }
            if (source == null) {
                _formState.value = current.copy(
                    errorMessage = "找不到要编辑的音源。",
                )
                return@launch
            }

            val password = runCatching { credentialsRepository.getPassword(sourceId) }.getOrNull().orEmpty()

            _formState.value = current.copy(
                editingSourceId = sourceId,
                editingSource = source,
                name = source.name,
                serverUrl = source.url ?: "",
                username = source.username ?: "",
                password = password,
                path = decodeWebDavSourcePaths(source.path).joinToString("、"),
                selectedPaths = decodeWebDavSourcePaths(source.path),
            )
        }
    }

    fun updateName(value: String) {
        _formState.value = _formState.value.copy(name = value, nameError = null)
    }

    fun updateServerUrl(value: String) {
        _formState.value = _formState.value.copy(serverUrl = value, serverUrlError = null)
    }

    fun updateUsername(value: String) {
        _formState.value = _formState.value.copy(username = value, usernameError = null)
    }

    fun updatePassword(value: String) {
        _formState.value = _formState.value.copy(password = value, passwordError = null)
    }

    fun dismissError() {
        _formState.value = _formState.value.copy(errorMessage = null)
    }

    fun dismissSuccess() {
        _formState.value = _formState.value.copy(successMessage = null)
    }

    /**
     * 消费目录浏览页带回的结果（表单页观察到 holder 有新值时调用，take 语义）：
     * - 新增与编辑模式都只回填所选目录，提交统一由表单按钮触发。
     * 对照 SourceWebDavPage.vue 的 consumeBrowseResult。
     */
    fun consumeBrowseResult() {
        val browsed = WebDavBrowseResultHolder.take() ?: return
        selectBrowsePaths(browsed.paths)
    }

    /** 底部面板确认后只回填目录，保存音源仍由表单按钮触发。 */
    fun selectBrowsePaths(paths: List<String>) {
        val state = _formState.value
        // 多个所选目录共同构成同一个 WebDAV 音源的目录集合。
        if (paths.isNotEmpty()) {
            _formState.value = state.copy(
                path = paths.joinToString("、"),
                selectedPaths = paths.map(::normalizeWebDavPath).distinct(),
                pathError = null,
            )
        }
    }

    /** 新增模式提交：验证连接与所有选中目录后创建音源。 */
    fun submitAdd() {
        val state = _formState.value
        var hasError = false
        if (state.serverUrl.isBlank()) {
            _formState.value = _formState.value.copy(serverUrlError = "请填写服务器地址")
            hasError = true
        }
        if (state.username.isBlank()) {
            _formState.value = _formState.value.copy(usernameError = "请填写用户名")
            hasError = true
        }
        if (state.password.isBlank()) {
            _formState.value = _formState.value.copy(passwordError = "请填写密码")
            hasError = true
        }
        if (state.selectedPaths.isEmpty()) {
            _formState.value = _formState.value.copy(pathError = "请选择目录")
            hasError = true
        }
        if (hasError || state.isVerifying || state.isSubmitting) return

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSubmitting = true, errorMessage = null)
            try {
                val serverUrl = state.serverUrl.trim()
                val username = state.username.trim()
                val paths = state.selectedPaths.map(::normalizeWebDavPath).distinct()
                webDavClient.authenticate(username, state.password)
                paths.forEach { path ->
                    webDavClient.list(buildWebDavUrl(serverUrl, path))
                }
                val now = System.currentTimeMillis()
                val id = Uuid.random().toString()
                val source = Source(
                    id = id,
                    name = state.name.trim().ifBlank { getWebDavDisplayName(paths.first()) },
                    type = SourceType.WEBDAV,
                    url = serverUrl,
                    path = encodeWebDavSourcePaths(paths),
                    username = username.ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                )
                credentialsRepository.savePassword(id, state.password)
                sourceRepository.upsert(source)
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    successMessage = "保存成功",
                )
            } catch (e: Exception) {
                val message = e.message ?: "保存 WebDAV 音源失败。"
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    errorMessage = message,
                )
            }
        }
    }

    /**
     * 编辑态打开目录浏览：密码留空时从安全存储读原密码；
     * 从根目录重新选择此账号音源包含的多个目录。
     */
    fun startEditBrowse(onReady: (mode: String, initialPath: String, serverUrl: String, username: String, password: String) -> Unit) {
        val state = _formState.value
        val source = state.editingSource ?: return
        viewModelScope.launch {
            var password = state.password
            if (password.isEmpty()) {
                // 密码留空表示保留原密码，从安全存储读取
                password = runCatching { credentialsRepository.getPassword(source.id) }.getOrNull() ?: ""
            }
            if (password.isEmpty()) {
                _formState.value = state.copy(errorMessage = "WebDAV 密码不存在，请输入新密码。")
                return@launch
            }
            startBrowse("edit-multiple", password, onReady)
        }
    }

    /** 添加模式打开目录选择器前先验证连接。 */
    fun startAddBrowse(onBrowse: (mode: String, initialPath: String, serverUrl: String, username: String, password: String) -> Unit) {
        startBrowse("multiple", _formState.value.password, onBrowse)
    }

    /** 新增和编辑共用同一套连接验证与目录浏览入口。 */
    private fun startBrowse(
        mode: String,
        password: String,
        onBrowse: (mode: String, initialPath: String, serverUrl: String, username: String, password: String) -> Unit,
    ) {
        val state = _formState.value
        var hasError = false
        if (state.serverUrl.isBlank()) {
            _formState.value = _formState.value.copy(serverUrlError = "请填写服务器地址")
            hasError = true
        }
        if (state.username.isBlank()) {
            _formState.value = _formState.value.copy(usernameError = "请填写用户名")
            hasError = true
        }
        if (password.isBlank()) {
            _formState.value = _formState.value.copy(passwordError = "请填写密码")
            hasError = true
        }
        if (hasError || _formState.value.isVerifying || _formState.value.isSubmitting) return

        val serverUrl = state.serverUrl.trim()
        val username = state.username.trim()
        val initialPath = state.selectedPaths
            .firstOrNull()
            ?.let(::normalizeWebDavPath)
            ?.let(::parentWebDavPath)
            ?: "/"
        _formState.value = _formState.value.copy(isVerifying = true, errorMessage = null)
        viewModelScope.launch {
            try {
                webDavClient.authenticate(username, password)
                webDavClient.list(buildWebDavUrl(serverUrl, "/"))
                _formState.value = _formState.value.copy(isVerifying = false)
                onBrowse(mode, initialPath, serverUrl, username, password)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isVerifying = false,
                    errorMessage = e.message ?: "读取 WebDAV 目录失败。",
                )
            }
        }
    }

    /**
     * 提交编辑模式表单
     * - 验证连接（如有变更）
     * - 更新音源配置
     */
    fun submitEdit() {
        val state = _formState.value
        val source = state.editingSource ?: return

        // 验证必填字段
        var hasError = false
        if (state.name.isBlank()) {
            _formState.value = state.copy(nameError = "请填写显示名称")
            hasError = true
        }
        if (state.serverUrl.isBlank()) {
            _formState.value = state.copy(serverUrlError = "请填写服务器地址")
            hasError = true
        }
        if (state.username.isBlank()) {
            _formState.value = state.copy(usernameError = "请填写用户名")
            hasError = true
        }
        if (state.selectedPaths.isEmpty()) {
            _formState.value = state.copy(pathError = "请填写目录")
            hasError = true
        }
        if (hasError) return

        _formState.value = state.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                val connectionChanged =
                    state.serverUrl != source.url ||
                        state.username != source.username ||
                        state.selectedPaths.map(::normalizeWebDavPath).toSet() !=
                        decodeWebDavSourcePaths(source.path).map(::normalizeWebDavPath).toSet() ||
                        state.password.isNotEmpty()

                if (connectionChanged) {
                    // 需要验证连接
                    val verificationPassword = if (state.password.isNotEmpty()) {
                        state.password
                    } else {
                        credentialsRepository.getPassword(source.id) ?: ""
                    }

                    if (verificationPassword.isEmpty()) {
                        _formState.value = _formState.value.copy(
                            isSubmitting = false,
                            errorMessage = "WebDAV 密码不存在，请输入新密码。",
                        )
                        return@launch
                    }

                    try {
                        webDavClient.authenticate(state.username, verificationPassword)
                        state.selectedPaths.forEach { path ->
                            webDavClient.list(buildWebDavUrl(state.serverUrl, normalizeWebDavPath(path)))
                        }
                    } catch (e: Exception) {
                        _formState.value = _formState.value.copy(
                            isSubmitting = false,
                            errorMessage = "WebDAV 连接或目标目录验证失败，请检查编辑信息。",
                        )
                        return@launch
                    }
                }

                // 更新音源
                val updatedSource = source.copy(
                    name = state.name.trim(),
                    url = state.serverUrl.trim(),
                    username = state.username.trim(),
                    path = encodeWebDavSourcePaths(state.selectedPaths.map(::normalizeWebDavPath)),
                    updatedAt = System.currentTimeMillis(),
                )
                sourceRepository.upsert(updatedSource)

                // 更新密码（如有）
                if (state.password.isNotEmpty()) {
                    credentialsRepository.savePassword(source.id, state.password)
                }

                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    successMessage = "保存成功",
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    errorMessage = "保存音源修改失败，请稍后重试。",
                )
            }
        }
    }
}

private fun buildWebDavUrl(serverUrl: String, path: String): String {
    val trimmedServer = serverUrl.trim().trimEnd('/')
    val normalizedPath = normalizeWebDavPath(path)
    return "$trimmedServer$normalizedPath"
}

private fun parentWebDavPath(path: String): String {
    val normalizedPath = normalizeWebDavPath(path).trimEnd('/')
    val parentSeparator = normalizedPath.lastIndexOf('/')
    return if (parentSeparator <= 0) "/" else normalizedPath.substring(0, parentSeparator)
}

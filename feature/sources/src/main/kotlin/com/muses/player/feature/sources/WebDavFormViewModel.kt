package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.getParentWebDavPath
import com.muses.player.core.webdav.getWebDavDisplayName
import com.muses.player.core.webdav.normalizeWebDavPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** WebDAV 表单状态 */
data class WebDavFormState(
    // 表单字段
    val name: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val path: String = "",
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

@HiltViewModel
class WebDavFormViewModel @Inject constructor(
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

            _formState.value = current.copy(
                editingSourceId = sourceId,
                editingSource = source,
                name = source.name,
                serverUrl = source.url ?: "",
                username = source.username ?: "",
                password = "", // 密码留空表示保留原密码
                path = source.path ?: "/",
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

    fun updatePath(value: String) {
        _formState.value = _formState.value.copy(path = value, pathError = null)
    }

    fun dismissError() {
        _formState.value = _formState.value.copy(errorMessage = null)
    }

    fun dismissSuccess() {
        _formState.value = _formState.value.copy(successMessage = null)
    }

    /**
     * 消费目录浏览页带回的结果（表单页重新组合时调用，take 语义）：
     * - 编辑模式（single）：回填目录字段；
     * - 添加模式（multiple）：批量建源，成功后提示并返回。
     * 对照 SourceWebDavPage.vue 的 consumeBrowseResult。
     */
    fun consumeBrowseResult() {
        val browsed = WebDavBrowseResultHolder.take() ?: return
        val state = _formState.value
        if (state.editingSourceId != null) {
            // single 单选：回填目录
            if (browsed.paths.isNotEmpty()) {
                _formState.value = state.copy(path = browsed.paths[0])
            }
            return
        }
        addSelectedWebDavSources(browsed)
    }

    /** 添加模式批量建源（对照 addSelectedWebDavSources） */
    private fun addSelectedWebDavSources(browsed: WebDavBrowseResultHolder.BrowseResult) {
        val state = _formState.value
        if (browsed.paths.isEmpty() || state.isSubmitting) return

        viewModelScope.launch {
            _formState.value = state.copy(isSubmitting = true)
            try {
                val now = System.currentTimeMillis()
                val newSources = mutableListOf<Source>()
                for (path in browsed.paths) {
                    val id = UUID.randomUUID().toString()
                    newSources.add(
                        Source(
                            id = id,
                            name = getWebDavDisplayName(path),
                            type = SourceType.WEBDAV,
                            url = browsed.serverUrl,
                            path = normalizeWebDavPath(path),
                            username = browsed.username.ifBlank { null },
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    credentialsRepository.savePassword(id, browsed.password)
                }
                newSources.forEach { sourceRepository.upsert(it) }
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    successMessage = "已添加 ${newSources.size} 个 WebDAV 文件夹。",
                )
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "保存 WebDAV 音源失败。",
                )
            }
        }
    }

    /**
     * 编辑态打开目录浏览：密码留空时从安全存储读原密码；
     * 初始路径取当前目录的上级（可改选同级/子级）。对照 SourceWebDavPage.vue openBrowser。
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
            val formPath = normalizeWebDavPath(state.path.ifBlank { "/" })
            val initialPath = getParentWebDavPath(formPath) ?: formPath
            onReady("single", initialPath, state.serverUrl.trim(), state.username.trim(), password)
        }
    }

    /**
     * 提交添加模式表单
     * - 验证连接（列根目录）
     * - 成功后触发浏览会话（由调用方处理导航）
     */
    fun submitAdd(onBrowse: (mode: String, initialPath: String, serverUrl: String, username: String, password: String) -> Unit) {
        val state = _formState.value

        // 验证必填字段
        var hasError = false
        if (state.serverUrl.isBlank()) {
            _formState.value = state.copy(serverUrlError = "请填写服务器地址")
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
        if (hasError) return

        _formState.value = state.copy(isVerifying = true)
        viewModelScope.launch {
            try {
                webDavClient.authenticate(state.username, state.password)
                webDavClient.list(buildWebDavUrl(state.serverUrl, "/"))

                // 验证成功，触发浏览会话
                _formState.value = _formState.value.copy(isVerifying = false)
                onBrowse("multiple", "/", state.serverUrl, state.username, state.password)
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
        if (state.path.isBlank()) {
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
                        normalizeWebDavPath(state.path) != normalizeWebDavPath(source.path ?: "/") ||
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
                        webDavClient.list(buildWebDavUrl(state.serverUrl, normalizeWebDavPath(state.path)))
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
                    path = normalizeWebDavPath(state.path.trim()),
                    updatedAt = System.currentTimeMillis(),
                )
                sourceRepository.upsert(updatedSource)

                // 更新密码（如有）
                if (state.password.isNotEmpty()) {
                    credentialsRepository.savePassword(source.id, state.password)
                }

                _formState.value = _formState.value.copy(
                    isSubmitting = false,
                    successMessage = "音源修改已保存。",
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

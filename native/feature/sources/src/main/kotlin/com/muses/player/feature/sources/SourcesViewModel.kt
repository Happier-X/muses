package com.muses.player.feature.sources

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 添加音源表单状态 */
data class AddSourceForm(
    val name: String = "",
    val type: SourceType = SourceType.LOCAL,
    // 本地目录
    val localPath: String = "",
    // WebDAV
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    // 测试连接
    val testState: TestState = TestState.Idle,
)

sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data object Success : TestState()
    data class Failure(val message: String) : TestState()
}

/** WebDAV 目录浏览状态 */
data class WebDavBrowseState(
    val currentUrl: String,
    val currentPath: String = "/",
    val items: List<WebDavItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sourceId: String = "",
    val sourceName: String = "",
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
    private val webDavClient: WebDavClient,
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _addForm = MutableStateFlow(AddSourceForm())
    val addForm: StateFlow<AddSourceForm> = _addForm

    private val _showAddForm = MutableStateFlow(false)
    val showAddForm: StateFlow<Boolean> = _showAddForm

    private val _browseState = MutableStateFlow<WebDavBrowseState?>(null)
    val browseState: StateFlow<WebDavBrowseState?> = _browseState

    // ── Salt 复刻交互状态（SourcesPage.vue ref 组）──────────

    /** m-actions：添加音源面板开关 */
    var isAddActionSheetOpen by mutableStateOf(false)
        private set

    /** m-dialog：删除确认目标 */
    var pendingDelete by mutableStateOf<Source?>(null)
        private set

    /** m-dialog：本地音源编辑目标 */
    var pendingEdit by mutableStateOf<Source?>(null)
        private set

    fun openAddActionSheet() {
        isAddActionSheetOpen = true
    }

    fun closeAddActionSheet() {
        isAddActionSheetOpen = false
    }

    /** 按类型预填并打开添加表单（action sheet 两个入口） */
    fun showAddFormForType(type: SourceType) {
        updateFormType(type)
        showAddForm()
    }

    fun confirmDelete(source: Source) {
        pendingDelete = source
    }

    fun dismissDelete() {
        pendingDelete = null
    }

    /** 本地音源编辑（WebDAV 走浏览页） */
    fun openEditForm(source: Source) {
        pendingEdit = source
    }

    fun dismissEdit() {
        pendingEdit = null
    }

    /** 编辑保存：upsert + touch updatedAt（Web updateSource 同语义） */
    fun updateEditedSource(source: Source, name: String, path: String) {
        if (name.isBlank() || path.isBlank()) return
        viewModelScope.launch {
            sourceRepository.upsert(
                source.copy(name = name, path = path, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    fun showAddForm() {
        _showAddForm.value = true
        _addForm.value = AddSourceForm()
    }

    fun dismissAddForm() {
        _showAddForm.value = false
        _addForm.value = AddSourceForm()
    }

    fun updateFormName(name: String) {
        _addForm.value = _addForm.value.copy(name = name)
    }

    fun updateFormType(type: SourceType) {
        _addForm.value = _addForm.value.copy(type = type)
    }

    fun updateFormLocalPath(path: String) {
        _addForm.value = _addForm.value.copy(localPath = path)
    }

    fun updateFormWebdavUrl(url: String) {
        _addForm.value = _addForm.value.copy(webdavUrl = url)
    }

    fun updateFormWebdavUsername(username: String) {
        _addForm.value = _addForm.value.copy(webdavUsername = username)
    }

    fun updateFormWebdavPassword(password: String) {
        _addForm.value = _addForm.value.copy(webdavPassword = password)
    }

    /** 测试 WebDAV 连接 */
    fun testConnection() {
        val form = _addForm.value
        if (form.webdavUrl.isBlank()) {
            _addForm.value = form.copy(testState = TestState.Failure("请输入服务器地址"))
            return
        }

        _addForm.value = form.copy(testState = TestState.Testing)
        viewModelScope.launch {
            try {
                webDavClient.authenticate(form.webdavUsername, form.webdavPassword)
                val ok = webDavClient.probe(form.webdavUrl)
                _addForm.value = _addForm.value.copy(
                    testState = if (ok) TestState.Success else TestState.Failure("连接失败"),
                )
            } catch (e: Exception) {
                _addForm.value = _addForm.value.copy(
                    testState = TestState.Failure(e.message ?: "连接失败"),
                )
            }
        }
    }

    /** 保存音源 */
    fun saveSource() {
        val form = _addForm.value
        if (form.name.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val source = Source(
                id = id,
                name = form.name.trim(),
                type = form.type,
                url = if (form.type == SourceType.WEBDAV) form.webdavUrl.trim() else null,
                path = if (form.type == SourceType.LOCAL) form.localPath.trim() else null,
                username = if (form.type == SourceType.WEBDAV) form.webdavUsername.trim().ifEmpty { null } else null,
                createdAt = now,
                updatedAt = now,
            )
            sourceRepository.upsert(source)

            // WebDAV 密码加密存储
            if (form.type == SourceType.WEBDAV && form.webdavPassword.isNotEmpty()) {
                credentialsRepository.savePassword(id, form.webdavPassword)
            }

            dismissAddForm()
        }
    }

    /** 删除音源 */
    fun deleteSource(source: Source) {
        viewModelScope.launch {
            sourceRepository.deleteById(source.id)
            credentialsRepository.clearPassword(source.id)
        }
    }

    // ── WebDAV 目录浏览 ──────────────────────────────────

    /** 打开 WebDAV 目录浏览 */
    fun openBrowse(source: Source) {
        val url = source.url ?: return
        _browseState.value = WebDavBrowseState(
            currentUrl = url,
            currentPath = "/",
            sourceId = source.id,
            sourceName = source.name,
        )
        browseDirectory(url, source.id)
    }

    fun closeBrowse() {
        _browseState.value = null
    }

    fun browseUp() {
        val state = _browseState.value ?: return
        if (state.currentPath == "/") return
        val parentPath = state.currentPath.trimEnd('/').substringBeforeLast('/') + "/"
        val baseUrl = state.currentUrl.substringBeforeLast('/')
        val parentUrl = "$baseUrl$parentPath"
        _browseState.value = state.copy(currentPath = parentPath)
        browseDirectory(parentUrl, state.sourceId)
    }

    fun browseTo(item: WebDavItem) {
        if (!item.isDirectory) return
        val state = _browseState.value ?: return
        _browseState.value = state.copy(currentPath = item.url.substringAfter(state.sourceName, "/"))
        browseDirectory(item.url, state.sourceId)
    }

    private fun browseDirectory(url: String, sourceId: String) {
        val state = _browseState.value ?: return
        _browseState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // 用存储的密码认证
                val password = credentialsRepository.getPassword(sourceId)
                if (password != null) {
                    webDavClient.authenticate(state.sourceName, password)
                }
                val items = webDavClient.list(url)
                _browseState.value = _browseState.value?.copy(
                    items = items,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _browseState.value = _browseState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败",
                )
            }
        }
    }
}

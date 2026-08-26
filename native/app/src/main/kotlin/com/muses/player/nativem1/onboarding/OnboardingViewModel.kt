package com.muses.player.nativem1.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.media.scanner.ScanWorkScheduler
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavAuthRegistry
import com.muses.player.core.webdav.WebDavClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME, ADD_LOCAL, ADD_WEBDAV
}

sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data object Success : TestState()
    data class Failure(val message: String) : TestState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
    private val webDavClient: WebDavClient,
    private val webDavAuthRegistry: WebDavAuthRegistry,
) : ViewModel() {

    private val _step = MutableStateFlow(OnboardingStep.WELCOME)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // 本地目录
    private val _localPath = MutableStateFlow("")
    val localPath: StateFlow<String> = _localPath.asStateFlow()

    // WebDAV
    private val _webdavName = MutableStateFlow("")
    val webdavName: StateFlow<String> = _webdavName.asStateFlow()

    private val _webdavUrl = MutableStateFlow("")
    val webdavUrl: StateFlow<String> = _webdavUrl.asStateFlow()

    private val _webdavUsername = MutableStateFlow("")
    val webdavUsername: StateFlow<String> = _webdavUsername.asStateFlow()

    private val _webdavPassword = MutableStateFlow("")
    val webdavPassword: StateFlow<String> = _webdavPassword.asStateFlow()

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    fun goToStep(step: OnboardingStep) {
        _step.value = step
    }

    fun updateLocalPath(path: String) {
        _localPath.value = path
    }

    fun updateWebdavName(name: String) {
        _webdavName.value = name
    }

    fun updateWebdavUrl(url: String) {
        _webdavUrl.value = url
    }

    fun updateWebdavUsername(username: String) {
        _webdavUsername.value = username
    }

    fun updateWebdavPassword(password: String) {
        _webdavPassword.value = password
    }

    fun saveLocalSource() {
        if (_localPath.value.isBlank()) return
        viewModelScope.launch {
            val source = Source(
                id = UUID.randomUUID().toString(),
                name = "本地音乐",
                type = SourceType.LOCAL,
                path = _localPath.value.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            sourceRepository.upsert(source)
        }
    }

    fun saveWebdavSource(onComplete: () -> Unit) {
        if (_webdavUrl.value.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val sourceId = UUID.randomUUID().toString()
                val source = Source(
                    id = sourceId,
                    name = _webdavName.value.ifBlank { "WebDAV" },
                    type = SourceType.WEBDAV,
                    url = _webdavUrl.value.trim(),
                    // 用户名入库 + 密码走 Keystore 加密存储（对齐音源页添加流程；
                    // 缺失任一则播放/扫描认证必然失败）
                    username = _webdavUsername.value.trim().ifEmpty { null },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                sourceRepository.upsert(source)
                if (_webdavPassword.value.isNotEmpty()) {
                    credentialsRepository.savePassword(sourceId, _webdavPassword.value)
                }
                // 引导保存后同步播放认证注册表
                webDavAuthRegistry.refresh()
                // 触发扫描
                ScanWorkScheduler.enqueue(context)
                // 完成首次启动
                settingsRepository.completeFirstLaunch()
                onComplete()
            } catch (e: Exception) {
                _testState.value = TestState.Failure(e.message ?: "保存失败")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun complete(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.completeFirstLaunch()
            // 即使没有添加音源也触发扫描（可能已有本地库）
            ScanWorkScheduler.enqueue(context)
            onComplete()
        }
    }
}

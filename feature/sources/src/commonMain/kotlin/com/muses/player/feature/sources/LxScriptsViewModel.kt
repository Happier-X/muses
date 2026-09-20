package com.muses.player.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.lxsdk.LxScriptRepository
import com.muses.player.core.lxsdk.store.LxScriptStore
import com.muses.player.core.lxsdk.store.LxStoredScript
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 脚本列表项（UI 展示用；合并持久化状态与运行态） */
data class LxScriptItem(
    val id: String,
    val name: String,
    val version: String?,
    val author: String?,
    val description: String?,
    val enabled: Boolean,
    /** 脚本运行态声明的源 key（kw/kg/tx/...）；未加载时为空 */
    val platforms: List<String>,
    /** 加载/初始化错误；null = 正常 */
    val loadError: String?,
    /** 声明支持的 action 数（粗略反映能力） */
    val supportsLyric: Boolean,
    val supportsPic: Boolean,
)

/** 导入校验结果 */
sealed interface LxImportValidation {
    data object Idle : LxImportValidation
    data object Validating : LxImportValidation

    /** 校验通过：列出可提供的源 */
    data class Valid(val scriptName: String?, val platforms: List<String>) : LxImportValidation

    /** 校验失败：脚本无法初始化 */
    data class Invalid(val message: String) : LxImportValidation
}

/**
 * 洛雪自定义音源脚本管理 ViewModel。
 *
 * 职责：脚本的导入（含**导入前预检**）、启用/禁用、删除，以及运行态回显
 * （能提供哪些源、是否加载失败）。
 *
 * 导入预检的意义：用户在粘贴/选择脚本时立即知道「这脚本能不能跑、能提供哪些源」，
 * 避免导入后才发现不可用。
 */
@OptIn(ExperimentalUuidApi::class)
class LxScriptsViewModel(
    private val store: LxScriptStore,
    private val repository: LxScriptRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<LxScriptItem>>(emptyList())
    val items: StateFlow<List<LxScriptItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _importValidation = MutableStateFlow<LxImportValidation>(LxImportValidation.Idle)
    val importValidation: StateFlow<LxImportValidation> = _importValidation.asStateFlow()

    /** 待导入的脚本文本（从文件选择器或剪贴板来） */
    private val _pendingSource = MutableStateFlow<String?>(null)
    val pendingSource: StateFlow<String?> = _pendingSource.asStateFlow()

    init {
        refresh()
    }

    /**
     * 刷新列表：重读磁盘 + 触发脚本加载（以回显各脚本能提供的源）。
     *
     * 加载是必要的：未加载的脚本无法得知其声明的源，UI 只能显示空。
     * 脚本数量级小（个位数），加载开销可接受。
     */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val stored = store.list()
                // 同步运行态：把磁盘状态与仓库对齐（新增/删除/启禁用）
                syncRepository(stored)
                // 触发加载以回显源声明与错误
                repository.loadAll()
                _items.value = buildItems(stored)
            } finally {
                _loading.value = false
            }
        }
    }

    /** 设置待导入脚本（来自文件选择器/粘贴），并立即预检 */
    fun stageImport(source: String) {
        _pendingSource.value = source
        validate(source)
    }

    fun clearStagedImport() {
        _pendingSource.value = null
        _importValidation.value = LxImportValidation.Idle
    }

    /** 预检脚本：能否初始化、能提供哪些源 */
    fun validate(source: String) {
        if (source.isBlank()) {
            _importValidation.value = LxImportValidation.Invalid("脚本内容为空。")
            return
        }
        viewModelScope.launch {
            _importValidation.value = LxImportValidation.Validating
            _importValidation.value = try {
                val descriptor = repository.validate(source)
                if (descriptor.sources.isEmpty()) {
                    LxImportValidation.Invalid("脚本可初始化，但未声明任何音源（无法使用）。")
                } else {
                    LxImportValidation.Valid(
                        scriptName = descriptor.meta.name,
                        platforms = descriptor.sources.keys.toList(),
                    )
                }
            } catch (e: Exception) {
                LxImportValidation.Invalid(e.message ?: "脚本初始化失败。")
            }
        }
    }

    /**
     * 确认导入：落盘 + 注册到仓库 + 刷新列表。
     *
     * id 生成：优先用元信息名称派生（可读、便于用户识别），同名时追加序号。
     */
    fun confirmImport() {
        val source = _pendingSource.value ?: return
        viewModelScope.launch {
            val id = generateScriptId(source)
            store.save(id = id, source = source, enabled = true)
            repository.register(id, source)
            clearStagedImport()
            refresh()
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            if (store.setEnabled(id, enabled)) {
                if (enabled) {
                    store.get(id)?.let { repository.register(it.id, it.source) }
                } else {
                    repository.unregister(id)
                }
                refresh()
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            store.delete(id)
            repository.unregister(id)
            refresh()
        }
    }

    // ── 内部 ──

    /** 把磁盘状态同步到运行态仓库：新增未注册的、移除已删除的、停用已禁用的 */
    private suspend fun syncRepository(stored: List<LxStoredScript>) {
        val registered = repository.scripts().associateBy { it.scriptId }
        val enabledIds = stored.filter { it.enabled }.map { it.id }.toSet()

        // 新增/更新：磁盘有但仓库无（或来源已变）
        stored.filter { it.enabled }.forEach { script ->
            val existing = registered[script.id]
            if (existing == null || existing.source != script.source) {
                repository.register(script.id, script.source)
            }
        }
        // 移除：仓库有但磁盘不再启用
        registered.keys.filter { it !in enabledIds }.forEach { repository.unregister(it) }
    }

    private suspend fun buildItems(stored: List<LxStoredScript>): List<LxScriptItem> {
        val loaded = repository.loadAll().associateBy { it.scriptId }
        return stored.map { script ->
            val runtime = loaded[script.id]
            val declarations = runtime?.sources.orEmpty()
            LxScriptItem(
                id = script.id,
                name = script.meta.name ?: script.name,
                version = script.meta.version,
                author = script.meta.author,
                description = script.meta.description,
                enabled = script.enabled,
                platforms = declarations.keys.toList(),
                loadError = if (script.enabled) runtime?.loadError else null,
                supportsLyric = declarations.values.any { "lyric" in it.actions },
                supportsPic = declarations.values.any { "pic" in it.actions },
            )
        }
    }

    /** 由元信息名称生成稳定 id（同 id 覆盖 = 更新同名脚本） */
    private fun generateScriptId(source: String): String {
        val name = com.muses.player.core.lxsdk.LxScriptMetaParser.parse(source).name
        val base = (name ?: "script")
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
            .take(48)
            .ifBlank { "script" }
        return base
    }
}

package com.muses.player.feature.sources

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.media.scanner.LocalLibraryScanner
import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.media.scanner.WebDavLibraryScanner
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavAuthRegistry
import com.muses.player.core.webdav.WebDavClient
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
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

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val songRepository: SongRepository,
    private val scanner: LocalLibraryScanner,
    private val webDavScanner: WebDavLibraryScanner,
    private val settingsRepository: com.muses.player.core.data.repository.SettingsRepository,
    private val songDao: com.muses.player.core.data.dao.SongDao,
    private val scrapeQueueStore: com.muses.player.core.scrape.queue.ScrapeQueueStore,
    private val credentialsRepository: CredentialsRepository,
    private val webDavClient: WebDavClient,
    private val webDavAuthRegistry: WebDavAuthRegistry,
    private val playbackStateRepository: com.muses.player.core.data.repository.PlaybackStateRepository,
    private val recentPlaysRepository: com.muses.player.core.data.repository.RecentPlaysRepository,
    private val playerConnection: com.muses.player.core.media.playback.PlayerConnection,
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _addForm = MutableStateFlow(AddSourceForm())
    val addForm: StateFlow<AddSourceForm> = _addForm

    private val _showAddForm = MutableStateFlow(false)
    val showAddForm: StateFlow<Boolean> = _showAddForm

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

    // ── 扫描流程（对齐 Web SourcesPage.vue openScanSettings/closeScanSettings/startScan）──

    /** m-dialog「扫描设置」目标音源（非空 = 弹窗打开） */
    var pendingScanSource by mutableStateOf<Source?>(null)
        private set

    /** 扫描设置：读取音乐标签（Web 同款默认——WebDAV 不逐文件读 ID3/Vorbis） */
    var scanReadTags by mutableStateOf(true)
        private set

    /** m-dialog「扫描进度」开关 */
    var isScanProgressOpen by mutableStateOf(false)
        private set

    /** 扫描进度统一 UI 流（本地/WebDAV 扫描器各自持有 StateFlow，startScan 时转发到此处） */
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    /** 扫描失败信息（非空 = 进度弹窗显示「扫描失败」态，对照 Web stage=failed + message） */
    var scanError by mutableStateOf<String?>(null)
        private set

    /** 扫描结果汇总 toast 文案（单次事件，UI 消费后调 [clearScanResultMessage] 置空） */
    var scanResultMessage by mutableStateOf<String?>(null)
        private set

    /** 防重入标记：扫描进行中禁止再次 startScan / 关闭进度弹窗 */
    private var isScanning = false

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

    // ── 扫描流程方法 ──────────────────────────────

    /** 打开「扫描设置」弹窗；WebDAV 无选项（标签改由播放懒扫描）直接开扫，仅本地源弹窗 */
    fun openScanSettings(source: Source) {
        pendingScanSource = source
        if (source.type == SourceType.WEBDAV) {
            startScan()
            return
        }
        scanReadTags = true
    }

    /** 关闭「扫描设置」弹窗 */
    fun closeScanSettings() {
        pendingScanSource = null
    }

    /** 更新「读取音乐标签」开关 */
    fun updateScanReadTags(value: Boolean) {
        scanReadTags = value
    }

    /** 关闭「扫描进度」弹窗（扫描进行中禁止关闭，双保险） */
    fun dismissScanProgress() {
        if (isScanning) return
        isScanProgressOpen = false
        scanError = null
        clearScanResultMessage()
    }

    /** UI 消费完 toast 文案后置空（单次事件语义） */
    fun clearScanResultMessage() {
        scanResultMessage = null
    }

    /** 开始扫描：关设置开进度 → 按音源类型分派扫描器 → replaceSourceSongs → 结果汇总/异常入状态 */
    fun startScan() {
        val source = pendingScanSource ?: return
        if (isScanning) return
        closeScanSettings()
        isScanProgressOpen = true
        isScanning = true
        viewModelScope.launch {
            // 按类型分派：WebDAV 走 PROPFIND+缓存下载扫描器，本地走 MediaStore 扫描器；
            // 两扫描器无公共接口（各自 scan 语义差异大），此处直接分支取流/调扫描
            val isWebdav = source.type == SourceType.WEBDAV
            // 把当前扫描器的进度转发到统一的 UI 流，
            // 转发协程随扫描结束在 finally 中取消（最简转发方案，不引入合并流复杂度）
            val progressJob = launch {
                val progressFlow = if (isWebdav) webDavScanner.scanProgress else scanner.scanProgress
                progressFlow.collect { _scanProgress.value = it }
            }
            try {
                val songs = if (isWebdav) {
                    webDavScanner.scan(source)   // WebDAV：纯文件名建库，标签播放时懒扫描
                } else {
                    scanner.scan(source, readTags = scanReadTags)
                }
                songRepository.replaceSourceSongs(source.id, songs)
                scanResultMessage = "扫描完成：共 ${songs.size} 首。"
                // M3 自动补缺：扫描后把无标签歌曲排进刮削队列（开关默认关）
                if (settingsRepository.autoScrapeEnabled.first()) {
                    val untagged = songDao.getUntaggedSongIds()
                    if (untagged.isNotEmpty()) scrapeQueueStore.enqueue(untagged)
                }
            } catch (e: CancellationException) {
                // VM 销毁导致的协程取消：原样抛出交回结构化并发，不误报「扫描失败」
                throw e
            } catch (e: Exception) {
                // scanner 内部 progress 流已中断，异常消息写入 scanError 供进度弹窗显示失败态
                scanError = e.message ?: "扫描失败"
            } finally {
                progressJob.cancel()
                isScanning = false
            }
        }
    }

    /** 编辑保存：upsert + touch updatedAt（Web updateSource 同语义） */
    fun updateEditedSource(source: Source, name: String, path: String) {
        if (name.isBlank() || path.isBlank()) return
        viewModelScope.launch {
            sourceRepository.upsert(
                source.copy(name = name, path = path, updatedAt = System.currentTimeMillis()),
            )
            // 源变更后同步播放认证注册表（本地源无影响，refresh 幂等开销可忽略）
            webDavAuthRegistry.refresh()
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
            // 源新增后立即同步播放认证注册表，避免播放流播首次请求才懒加载到旧数据
            webDavAuthRegistry.refresh()

            dismissAddForm()
        }
    }

    /**
     * 从 SAF tree uri 建本地源：解析出物理绝对路径前缀存入 Source.path，
     * 供 LocalLibraryScanner 的 MediaStore DATA 前缀过滤直接使用。
     * primary:Music → /storage/emulated/0/Music；XXXX-XXXX:dir → /storage/XXXX-XXXX/dir
     */
    fun saveLocalSourceFromTreeUri(treeUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val physicalPath = resolvePhysicalPath(treeUri, context)
                    ?: return@launch
                val displayName = treeUri.lastPathSegment
                    ?.substringAfterLast(':')
                    ?.substringAfterLast('/')
                    ?: "本地文件夹"
                val now = System.currentTimeMillis()
                sourceRepository.upsert(
                    Source(
                        id = UUID.randomUUID().toString(),
                        name = displayName,
                        type = SourceType.LOCAL,
                        path = physicalPath,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } catch (_: Exception) {
                // 选择器取消或解析失败静默（对齐 Web FilePicker 取消语义）
            }
        }
    }

    /** DocumentsContract 文档 id → 物理路径（externalstorage provider 标准格式） */
    private fun resolvePhysicalPath(treeUri: android.net.Uri, context: android.content.Context): String? =
        runCatching {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val (volume, subPath) = docId.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            when {
                volume.equals("primary", ignoreCase = true) ->
                    "/storage/emulated/0" + if (subPath.isNotEmpty()) "/$subPath" else ""
                else -> "/storage/$volume" + if (subPath.isNotEmpty()) "/$subPath" else ""
            }
        }.getOrNull()

    /** 删除音源 */
    fun deleteSource(source: Source) {
        viewModelScope.launch {
            // 先取待删歌曲 ids，用于清理播放快照/最近播放/播放队列
            val songIdsToRemove = try {
                songDao.getBySource(source.id).map { it.id }.toSet()
            } catch (_: Exception) {
                emptySet()
            }
            sourceRepository.deleteById(source.id)
            credentialsRepository.clearPassword(source.id)
            // 同步清理该音源入库歌曲（对齐 Web executeDeleteSource → reconcileSourceSongs(id, [])）
            songRepository.deleteSourceSongs(source.id)
            // 清理播放相关残留，避免底部栏仍显示已删歌曲
            if (songIdsToRemove.isNotEmpty()) {
                try {
                    playbackStateRepository.removeSongs(songIdsToRemove)
                } catch (_: Exception) { }
                try {
                    recentPlaysRepository.removeSongs(songIdsToRemove)
                } catch (_: Exception) { }
                try {
                    playerConnection.removeFromQueue(songIdsToRemove)
                } catch (_: Exception) { }
            }
            // 源删除后同步播放认证注册表，移除残留凭据映射
            webDavAuthRegistry.refresh()
        }
    }
}

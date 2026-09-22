package com.muses.player.feature.sources

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muses.player.core.data.dao.SongDao
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.PlaybackStateRepository
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.repository.SettingsRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.data.store.platformNowMs
import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.scrape.queue.ScrapeQueueStore
import com.muses.player.core.webdav.WebDavAuthRegistry
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 音源 ViewModel（U20 全量上收 commonMain）：数据核 + 页面扩展一体——音源 CRUD/表单/
 * 扫描编排/本地建源进 commonMain，依赖全部为 KMP 数据栈（P2b/W3/W4 产物）+
 * [LibraryScanPort]（androidMain 绑定 MediaStore/WebDAV 扫描器）+ 播放队列清理
 * （经 [com.muses.player.core.playback.PlaybackPort].removeFromQueue 注入回调）。
 * 原安卓子类（扫描器装配 + SAF 树 uri 建源）已并入：SAF 选目录改为
 * [rememberLocalFolderPicker] expect/actual，物理路径解析留安卓 actual。
 */
@OptIn(ExperimentalUuidApi::class)
class SourcesViewModel constructor(
    private val sourceRepository: SourceRepository,
    private val songRepository: SongRepository,
    private val scanPort: LibraryScanPort,
    private val settingsRepository: SettingsRepository,
    private val songDao: SongDao,
    private val scrapeQueueStore: ScrapeQueueStore,
    private val credentialsRepository: CredentialsRepository,
    private val webDavAuthRegistry: WebDavAuthRegistry,
    private val playbackStateRepository: PlaybackStateRepository,
    private val recentPlaysRepository: RecentPlaysRepository,
    /** 删除音源时的播放队列清理（双端接 PlaybackPort.removeFromQueue；可缺省空实现） */
    private val onRemoveFromQueue: (Set<String>) -> Unit = {},
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** 扫描进度统一 UI 流（扫描端口各自持有进度流，startScan 时转发到此处） */
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

    /** 开始扫描：关设置开进度 → 端口按音源类型分派扫描器 → replaceSourceSongs → 结果汇总/异常入状态 */
    fun startScan() {
        val source = pendingScanSource ?: return
        if (isScanning) return
        closeScanSettings()
        isScanProgressOpen = true
        isScanning = true
        viewModelScope.launch {
            // 把扫描端口的进度流转发到统一的 UI 流，
            // 转发协程随扫描结束在 finally 中取消（最简转发方案，不引入合并流复杂度）
            val progressJob = launch {
                scanPort.progressFor(source.type).collect { _scanProgress.value = it }
            }
            try {
                val songs = scanPort.scan(source, readTags = scanReadTags)
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
                source.copy(name = name, path = path, updatedAt = platformNowMs()),
            )
            // 源变更后同步播放认证注册表（本地源无影响，refresh 幂等开销可忽略）
            webDavAuthRegistry.refresh()
        }
    }

    /**
     * 本地目录建源（U20：原安卓 saveLocalSourceFromTreeUri 的平台无关部分）。
     * 物理路径由各端选择器 actual 解析（安卓 SAF tree uri → 绝对路径；桌面原生路径），
     * 供 LocalLibraryScanner 的前缀过滤直接使用。
     */
    fun saveLocalSource(physicalPath: String) {
        if (physicalPath.isBlank()) return
        viewModelScope.launch {
            try {
                val displayName = physicalPath.substringAfterLast('/').ifEmpty { "本地文件夹" }
                val now = platformNowMs()
                sourceRepository.upsert(
                    Source(
                        id = Uuid.random().toString(),
                        name = displayName,
                        type = SourceType.LOCAL,
                        path = physicalPath,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } catch (_: Exception) {
                // 建源失败静默（对齐 Web FilePicker 取消语义）
            }
        }
    }

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
                    onRemoveFromQueue(songIdsToRemove)
                } catch (_: Exception) { }
            }
            // 源删除后同步播放认证注册表，移除残留凭据映射
            webDavAuthRegistry.refresh()
        }
    }
}

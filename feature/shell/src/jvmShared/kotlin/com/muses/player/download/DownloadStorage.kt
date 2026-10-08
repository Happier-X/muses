package com.muses.player.download

import com.muses.player.core.model.download.DownloadTarget
import com.muses.player.core.model.download.DownloadTask
import com.muses.player.core.data.repository.SettingsRepository
import java.io.File

data class SavedDownload(val location: String, val physicalPath: String? = null, val warnings: List<String> = emptyList(), val skippedExisting: Boolean = false)

/** 下载先在私有目录完整写入标签，再提交到目标；目标端禁止覆盖已有文件。 */
interface DownloadStorage {
    val cacheDirectory: File
    suspend fun save(files: List<File>, target: DownloadTarget, progress: suspend (Long, Long) -> Unit): SavedDownload

    /**
     * 已完成任务的目标文件是否还在（本地/设备下载）。
     * 判定不了时返回 true —— 宁可提示「已下载过」，也不要重复下一份文件。
     * WebDAV 是远端地址，判断在 [DownloadManager] 里做。
     */
    suspend fun exists(task: DownloadTask): Boolean
}

expect fun createDownloadStorage(settings: SettingsRepository): DownloadStorage

expect fun keepDownloadsRunning(running: Boolean)

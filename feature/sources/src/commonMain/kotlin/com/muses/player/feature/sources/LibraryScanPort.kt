package com.muses.player.feature.sources

import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.model.Source
import com.muses.player.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * 曲库扫描端口（U11）：本地 MediaStore 扫描器 / WebDAV 扫描器属安卓媒体栈（:core:media），
 * 不进 commonMain——commonMain 的 [SourcesCoreViewModel] 经本端口编排扫描，
 * androidMain 以两扫描器装配真实现（[AndroidLibraryScanPort]）；桌面仅支持 WebDAV，
 * 调用方通过 [supports] 检查各端可扫描的音源类型。
 */
interface LibraryScanPort {

    fun supports(type: com.muses.player.core.model.SourceType): Boolean =
        type != com.muses.player.core.model.SourceType.ONLINE

    /** 按音源类型返回该扫描器的进度流（转发到统一 UI 流） */
    fun progressFor(type: com.muses.player.core.model.SourceType): Flow<ScanProgress>

    /** 执行扫描：WebDAV 纯文件名建库（标签播放时懒扫描）；本地按 [readTags] 决定是否逐文件读标签 */
    suspend fun scan(source: Source, readTags: Boolean): List<Song>
}

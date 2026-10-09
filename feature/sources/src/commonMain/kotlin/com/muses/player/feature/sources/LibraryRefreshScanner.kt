package com.muses.player.feature.sources

import com.muses.player.core.data.repository.ScanMergeResult
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 音源页扫描和曲库刷新共用串行入口，避免扫描器进度与入库互相覆盖。 */
class LibraryRefreshScanner(
    private val sources: SourceRepository,
    private val songs: SongRepository,
    private val port: LibraryScanPort,
) {
    private val mutex = Mutex()

    suspend fun scan(source: Source, readTags: Boolean): ScanMergeResult = mutex.withLock {
        songs.replaceSourceSongs(source.id, port.scan(source, readTags))
    }

    suspend fun refresh(): String = mutex.withLock {
        var added = 0
        val warnings = mutableListOf<String>()
        val targets = sources.observeSources().first().filter { it.type != SourceType.ONLINE }
        for (source in targets) {
            if (!port.supports(source.type)) {
                warnings += "${source.name}：暂不支持扫描"
                continue
            }
            try {
                // 下拉只发现文件，避免重复解析整库标签；新歌沿用播放时补充标签的链路。
                val result = songs.replaceSourceSongs(source.id, port.scan(source, readTags = false))
                added += result.added
                if (result.skipped) warnings += "${source.name}：扫描结果异常，已保留原有歌曲"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                warnings += "${source.name}：扫描失败（${e.message ?: "请检查音源"}）"
            }
        }
        val result = if (targets.isEmpty()) "暂无可扫描的音源" else "刷新完成，新增 $added 首歌曲"
        if (warnings.isEmpty()) result else "$result；${warnings.joinToString("；")}"
    }
}

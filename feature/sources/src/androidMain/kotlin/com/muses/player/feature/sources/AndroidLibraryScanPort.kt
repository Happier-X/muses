package com.muses.player.feature.sources

import com.muses.player.core.media.scanner.LocalLibraryScanner
import com.muses.player.core.media.scanner.ScanProgress
import com.muses.player.core.media.scanner.WebDavLibraryScanner
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import kotlinx.coroutines.flow.Flow
import org.koin.dsl.module

/**
 * [LibraryScanPort] 安卓实现（U11）：本地 MediaStore 扫描器 / WebDAV 扫描器按音源类型分派
 * （原 SourcesViewModel 内联分派逻辑原样平移，两扫描器 scan 语义差异见各自实现）。
 */
class AndroidLibraryScanPort(
    private val scanner: LocalLibraryScanner,
    private val webDavScanner: WebDavLibraryScanner,
) : LibraryScanPort {

    override fun progressFor(type: SourceType): Flow<ScanProgress> =
        when (type) {
            SourceType.LOCAL -> scanner.scanProgress
            SourceType.WEBDAV -> webDavScanner.scanProgress
            SourceType.ONLINE -> error("LX 在线音源不支持曲库扫描")
        }

    override suspend fun scan(source: Source, readTags: Boolean): List<com.muses.player.core.model.Song> =
        when (source.type) {
            SourceType.LOCAL -> scanner.scan(source, readTags = readTags)
            // WebDAV：纯文件名建库，标签播放时懒扫描
            SourceType.WEBDAV -> webDavScanner.scan(source)
            SourceType.ONLINE -> error("LX 在线音源不支持曲库扫描")
        }
}

/**
 * 音源页安卓平台装配（U20）：扫描端口绑定安卓媒体栈扫描器。共享 [SourcesViewModel]
 * 经 sourcesCoreModule 装配（播放队列清理回调直取 PlaybackPort 绑定，安卓即
 * PlayerConnection 同一单例）。
 */
val sourcesPlatformModule = module {
    single<LibraryScanPort> {
        AndroidLibraryScanPort(get(), get())
    }
}

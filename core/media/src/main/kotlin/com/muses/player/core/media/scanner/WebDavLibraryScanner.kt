package com.muses.player.core.media.scanner

import com.muses.player.core.data.log.ErrorLogStore
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * WebDAV 库扫描器（纯发现 + 文件名建库）。
 *
 * - 发现阶段：从 `source.url + source.path` 起 BFS 递归 PROPFIND（复用 [WebDavClient.list]），
 *   按扩展名过滤支持的音频格式；进度 total=0、currentFile=正在列的目录（UI 映射「正在查找文件」）；
 * - 建库阶段：零下载，标题=文件名去扩展名、tagsVersion=0（未读标签），快速建库；
 * - 标签读取已移至播放时懒扫描（PlayerConnection：整文件入缓存后 TagReader + sidecar .lrc，
 *   避免扫描期批量网络请求触发网关限流，且用户只对真正会听的歌付出标签成本）；
 * - 只产出 List<Song>，入库（replaceSourceSongs）由调用方完成。
 */
@Singleton
class WebDavLibraryScanner @Inject constructor(
    private val webDavClient: WebDavClient,
    private val credentialsRepository: CredentialsRepository,
    private val errorLogStore: ErrorLogStore,
) {

    private val progressInternal = MutableStateFlow(ScanProgress())

    /** 扫描进度流；扫描开始时重置 */
    val scanProgress: StateFlow<ScanProgress> = progressInternal.asStateFlow()

    /** 扫描 WebDAV 音源，返回入库用 Song 列表。密码缺失抛 [IllegalStateException]（文案见伴生常量）。 */
    suspend fun scan(source: Source): List<Song> = withContext(Dispatchers.IO) {
        progressInternal.value = ScanProgress()
        try {
            // 密码缺失抛 IllegalStateException（文案见伴生常量）；置于 try 内保证失败时进度置终态
            val password = credentialsRepository.getPassword(source.id)
                ?: throw IllegalStateException(PASSWORD_MISSING_MESSAGE)
            source.username?.let { webDavClient.authenticate(it, password) }

            // ── 发现阶段：BFS 递归列目录 ──────────────────────
            val rootUrl = joinUrl(requireNotNull(source.url) { "音源缺少服务器地址" }, source.path)
            val files = discoverAudioFiles(rootUrl)

            // ── 建库阶段：零下载文件名歌 ──────────────────────
            val songs = files.map { filenameSong(source.id, it) }

            progressInternal.value = ScanProgress(current = files.size, total = files.size, finished = true)
            songs
        } catch (e: Exception) {
            // 发现/认证等整体失败：留痕后进度置终态，异常由调用方展示失败态文案
            if (e !is kotlinx.coroutines.CancellationException) {
                errorLogStore.log(
                    ErrorLogStore.Level.ERROR,
                    "WebDavScan",
                    "扫描失败（${source.name}）：${e.message ?: e::class.java.simpleName}",
                    e,
                )
            }
            progressInternal.value = ScanProgress(finished = true)
            throw e
        }
    }

    /** BFS 队列递归列目录，收集音频文件条目 */
    private suspend fun discoverAudioFiles(rootUrl: String): List<WebDavItem> {
        val files = ArrayList<WebDavItem>()
        val queue = ArrayDeque<String>().apply { add(rootUrl) }
        while (queue.isNotEmpty()) {
            val directory = queue.removeFirst()
            progressInternal.value = ScanProgress(currentFile = directory)
            for (item in webDavClient.list(directory)) {
                if (item.isDirectory) {
                    queue.add(item.url)
                } else if (LocalLibraryScanner.isSupportedAudio(item.name)) {
                    files.add(item)
                }
            }
        }
        return files
    }

    /** 文件名建歌：标题=displayName 去扩展名，零标签零下载；tagsVersion=0 表示「尚未读过标签」（播放时懒扫描补齐） */
    private fun filenameSong(sourceId: String, item: WebDavItem): Song = Song(
        id = LocalLibraryScanner.stableSongId(sourceId, item.url),
        sourceId = sourceId,
        path = item.url,
        title = item.name.substringBeforeLast('.'),
        sourceType = SourceType.WEBDAV,
        tagsVersion = FILENAME_TAGS_VERSION,
    )

    /** 同目录同名 .lrc 的完整 URL（供播放懒扫描使用；移植自旧工程 WebDavPlugin.buildSidecarLyricsUrl） */
    fun buildSidecarLyricsUrl(audioUrl: String): String? = runCatching {
        val uri = android.net.Uri.parse(audioUrl)
        val lastSegment = uri.lastPathSegment ?: return null
        val lyricSegment = lastSegment.substringBeforeLast('.', lastSegment) + ".lrc"
        uri.buildUpon()
            .path(uri.path.orEmpty().substringBeforeLast('/') + "/" + lyricSegment)
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }.getOrNull()

    private fun joinUrl(baseUrl: String, path: String?): String {
        val base = baseUrl.trim().trimEnd('/')
        val subPath = path?.trim()?.trim('/')?.takeIf { it.isNotEmpty() } ?: return base
        return "$base/$subPath"
    }

    companion object {
        const val TAGS_VERSION = LocalLibraryScanner.TAGS_VERSION

        /** 文件名建库的 tagsVersion 占位（< TAGS_VERSION 即待懒扫描） */
        const val FILENAME_TAGS_VERSION = 0

        /** 密码缺失报错文案（对齐 Web src/features/library/scanner.ts） */
        const val PASSWORD_MISSING_MESSAGE = "WebDAV 密码不存在，请重新添加该音源。"
    }
}

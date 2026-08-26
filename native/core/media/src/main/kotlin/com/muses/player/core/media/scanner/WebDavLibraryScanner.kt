package com.muses.player.core.media.scanner

import android.content.Context
import android.net.Uri
import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.media.metadata.TagReader
import com.muses.player.core.model.Song
import com.muses.player.core.model.Source
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.LyricsSource
import com.muses.player.core.webdav.WebDavAudioCache
import com.muses.player.core.webdav.WebDavClient
import com.muses.player.core.webdav.WebDavItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * WebDAV 库扫描器（对齐 Web 层 src/features/library/scanner.ts 语义）。
 *
 * - 发现阶段：从 `source.url + source.path` 起 BFS 递归 PROPFIND（复用 [WebDavClient.list]），
 *   按扩展名过滤支持的音频格式；进度 total=0、currentFile=正在列的目录（UI 映射「正在查找文件」）；
 * - 处理阶段（readTags=true）：逐文件命中 [WebDavAudioCache] 或下载进缓存（putToCache 预热播放 LRU）
 *   → jaudiotagger 读标签/封面 → 无内嵌歌词时抓同目录同名 `.lrc` sidecar 兜底；
 *   单个文件失败降级为文件名建歌，不中断整体扫描（Web readTagsSafely 同语义）；
 * - readTags=false：零下载，直接用文件名去扩展名建歌，快速建库；
 * - 只产出 List&lt;Song&gt;，入库（replaceSourceSongs）由调用方完成。
 */
@Singleton
class WebDavLibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webDavClient: WebDavClient,
    private val audioCache: WebDavAudioCache,
    private val credentialsRepository: CredentialsRepository,
) {

    private val progressInternal = MutableStateFlow(ScanProgress())

    /** 扫描进度流；扫描开始时重置 */
    val scanProgress: StateFlow<ScanProgress> = progressInternal.asStateFlow()

    /** 扫描 WebDAV 音源，返回入库用 Song 列表。密码缺失抛 [IllegalStateException]（文案见伴生常量）。 */
    suspend fun scan(source: Source, readTags: Boolean): List<Song> = withContext(Dispatchers.IO) {
        progressInternal.value = ScanProgress()
        try {
            // 密码缺失抛 IllegalStateException（文案见伴生常量）；置于 try 内保证失败时进度置终态
            val password = credentialsRepository.getPassword(source.id)
                ?: throw IllegalStateException(PASSWORD_MISSING_MESSAGE)
            source.username?.let { webDavClient.authenticate(it, password) }

            // ── 发现阶段：BFS 递归列目录 ──────────────────────
            val rootUrl = joinUrl(requireNotNull(source.url) { "音源缺少服务器地址" }, source.path)
            val files = discoverAudioFiles(rootUrl)

            // ── 处理阶段：串行逐文件（对齐 Web 行为，不做并行加速）──
            var index = 0
            val songs = ArrayList<Song>(files.size)
            for (item in files) {
                index++
                progressInternal.value =
                    ScanProgress(current = index, total = files.size, currentFile = item.name)
                songs.add(buildSong(source.id, item, readTags))
            }

            progressInternal.value = ScanProgress(current = files.size, total = files.size, finished = true)
            songs
        } catch (e: Exception) {
            // 发现/认证等整体失败：进度置终态，异常由调用方展示失败态文案
            progressInternal.value = ScanProgress(finished = true)
            throw e
        }
    }

    /** BFS 队列递归列目录，收集音频文件条目（eTag/lastModified 一并带下供下载后写缓存 meta） */
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

    /**
     * 单文件建歌。readTags=false 走零下载文件名路径；readTags=true 任何环节失败
     * （下载/读标签/sidecar）均降级为文件名歌继续扫描。
     */
    private suspend fun buildSong(sourceId: String, item: WebDavItem, readTags: Boolean): Song {
        val fallbackTitle = item.name.substringBeforeLast('.')
        if (!readTags) {
            return filenameSong(sourceId, item)
        }
        return try {
            val localFile = obtainCachedFile(item)
            val tags = TagReader.read(localFile)

            var lyrics = tags.lyrics?.trim()?.takeIf { it.isNotEmpty() }
            var lyricsSource: LyricsSource? = if (lyrics != null) LyricsSource.EMBEDDED else null

            // sidecar .lrc 仅在无内嵌歌词时请求，省一次网络往返（Web 同语义）
            if (lyrics == null) {
                buildSidecarLyricsUrl(item.url)?.let { lrcUrl ->
                    webDavClient.getString(lrcUrl)?.let { sidecar ->
                        lyrics = sidecar.trim().takeIf { it.isNotEmpty() }
                        if (lyrics != null) lyricsSource = LyricsSource.SIDECAR
                    }
                }
            }

            val songId = LocalLibraryScanner.stableSongId(sourceId, item.url)
            Song(
                id = songId,
                sourceId = sourceId,
                path = item.url,
                title = tags.title?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackTitle,
                artist = tags.artist,
                album = tags.album,
                durationMs = tags.durationSec * 1000L,
                durationSec = tags.durationSec,
                coverUri = tags.coverBytes?.let { CoverCacheWriter.write(context, songId, it) },
                lyrics = lyrics,
                lyricsSource = lyricsSource,
                replayGainTrackDb = tags.replayGainTrackDb,
                sourceType = SourceType.WEBDAV,
                tagsVersion = TAGS_VERSION,
            )
        } catch (e: CancellationException) {
            // 协程取消（用户退出页面/VM 销毁）：必须原样抛出，
            // 否则取消被吞、剩余文件继续逐个走下载+降级，扫描无法停止
            throw e
        } catch (_: Exception) {
            // 单文件标签读取失败降级为文件名（degraded 不中断整体扫描）
            filenameSong(sourceId, item)
        }
    }

    /** 缓存命中优先；未命中则下载到临时目录 → putToCache（预热播放 LRU）→ 返回本地文件 */
    private suspend fun obtainCachedFile(item: WebDavItem): File {
        audioCache.getCachedFile(item.url)?.let { return it }

        val tempDir = File(context.cacheDir, TEMP_SCAN_DIR).apply { mkdirs() }
        val extension = item.name.substringAfterLast('.', DEFAULT_TEMP_EXTENSION)
        val tempFile = File(tempDir, "${CoverCacheWriter.sha256(item.url)}.$extension")
        webDavClient.get(item.url, tempFile)

        // putToCache 为 copy 语义：搬完由本方清理临时文件
        audioCache.putToCache(item.url, tempFile, eTag = item.eTag, lastModified = item.lastModified)
        tempFile.delete()
        return audioCache.getCachedFile(item.url) ?: tempFile
    }

    /** 文件名兜底建歌：标题=displayName 去扩展名，零标签 */
    private fun filenameSong(sourceId: String, item: WebDavItem): Song = Song(
        id = LocalLibraryScanner.stableSongId(sourceId, item.url),
        sourceId = sourceId,
        path = item.url,
        title = item.name.substringBeforeLast('.'),
        sourceType = SourceType.WEBDAV,
        tagsVersion = TAGS_VERSION,
    )

    /** 同目录同名 .lrc 的完整 URL（移植自旧工程 WebDavPlugin.buildSidecarLyricsUrl） */
    private fun buildSidecarLyricsUrl(audioUrl: String): String? = runCatching {
        val uri = Uri.parse(audioUrl)
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

        /** 密码缺失报错文案（对齐 Web src/features/library/scanner.ts） */
        const val PASSWORD_MISSING_MESSAGE = "WebDAV 密码不存在，请重新添加该音源。"

        /** 扫描临时下载目录（cacheDir 下），putToCache 接管后即删 */
        private const val TEMP_SCAN_DIR = "tmp-scan"

        private const val DEFAULT_TEMP_EXTENSION = "audio"
    }
}

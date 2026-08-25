package com.muses.player.core.scrape.writeback

import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.media.metadata.TagWriter
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.webdav.WebDavClient
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频标签文件写入器（规格书 = src/features/scrape/writeback.ts 的 writeFile 分派语义）：
 * 按 song.sourceType 选择本地 / WebDAV 写入方式，失败返回 FileWriteResult 不抛异常。
 */
fun interface AudioTagFileWriter {
    suspend fun write(song: Song, request: TagWriter.TagWriteRequest): FileWriteResult
}

/** 远程封面字节获取（对齐 Web ensureLocalCover：失败返回 null 跳过内嵌，不阻断写回） */
fun interface CoverBytesFetcher {
    suspend fun fetch(remoteUrl: String): ByteArray?
}

/** 默认实现：OkHttp 二进制 GET（ScrapeHttp.getBytes） */
class HttpCoverBytesFetcher(private val http: ScrapeHttp = ScrapeHttp()) : CoverBytesFetcher {
    override suspend fun fetch(remoteUrl: String): ByteArray? = try {
        http.getBytes(remoteUrl)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

// ── WebDAV URL 构造（翻译 src/features/sources/webdav.ts）───────

/** webdav.ts normalizeWebDavPath：保证前导斜杠、折叠连续斜杠、去尾斜杠 */
internal fun normalizeWebDavPath(path: String): String {
    var normalized = path.trim()
    if (!normalized.startsWith("/")) {
        normalized = "/$normalized"
    }
    normalized = normalized.replace(Regex("/+"), "/")
    if (normalized.length > 1 && normalized.endsWith("/")) {
        return normalized.dropLast(1)
    }
    return normalized
}

/** webdav.ts encodePath：逐段 encodeURIComponent 后以 / 连接 */
internal fun encodeWebDavPath(path: String): String {
    val normalized = normalizeWebDavPath(path)
    if (normalized == "/") return "/"
    return "/" + normalized.split("/")
        .filter { it.isNotEmpty() }
        // 复用 S1 的 urlEncode（charset 名重载，规避 minSdk 26 下 API 33 限制）
        .joinToString("/") { segment ->
            com.muses.player.core.scrape.text.provider.urlEncode(segment)
        }
}

/** webdav.ts buildWebDavUrl：serverUrl 去尾斜杠 + 编码后路径 */
internal fun buildWebDavUrl(serverUrl: String, path: String): String =
    serverUrl.trim().trimEnd('/') + encodeWebDavPath(path)

/**
 * 本地写路径：直接对 song.path 指向的物理文件调 TagWriter。
 * 文件不存在/格式不支持均由 TagWriter 折叠为 write_failed 结果（对齐 file-failed 分类）。
 */
class LocalAudioTagFileWriter : AudioTagFileWriter {
    override suspend fun write(song: Song, request: TagWriter.TagWriteRequest): FileWriteResult =
        withContext(Dispatchers.IO) {
            val result = TagWriter.write(File(song.path), request)
            FileWriteResult(ok = result.ok, code = result.code, message = result.message)
        }
}

/**
 * WebDAV 写路径（对齐 writeWebDavFile 五步）：
 * 1. 按 song.sourceId 精确查找音源（多音源读写目标必须一致）；缺失 → no_password 文案 A
 * 2. 取密码；未配置 → no_password 文案 B
 * 3. 完整地址 = serverUrl + encodePath(song.path)（与读取链路一致）
 * 4. 下载到临时文件 → TagWriter 写标签 → put 上传
 * 5. 各阶段失败映射 code：download_failed / write_failed / put_failed
 *
 * 注意：M1 原生 Source 模型暂无 username 字段（既有缺口），认证用户名由调用方
 * 在装配 [webDavClientFactory] 时处理；接线前需先补 username 持久化。
 */
class WebDavAudioTagFileWriter(
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
    /** 提供可用的 WebDAV 客户端（单例复用；串行写回下 authenticate 切换安全） */
    private val webDavClientFactory: suspend () -> WebDavClient,
    /** 下载临时目录（cache 目录，由装配方提供） */
    private val tempDir: File,
) : AudioTagFileWriter {

    override suspend fun write(song: Song, request: TagWriter.TagWriteRequest): FileWriteResult {
        // 1. 按歌曲所属音源精确查找
        val source = sourceRepository.getSource(song.sourceId)
        val serverUrl = source?.url
        if (source == null || source.type != SourceType.WEBDAV || serverUrl.isNullOrBlank()) {
            return FileWriteResult(
                ok = false,
                code = "no_password",
                message = "未找到歌曲所属的 WebDAV 音源，请重新扫描后重试。",
            )
        }

        // 2. 密码
        val password = credentialsRepository.getPassword(source.id)
            ?: return FileWriteResult(ok = false, code = "no_password", message = "WebDAV 密码未配置。")

        val client = webDavClientFactory()
        // M1 缺口：username 未持久化，暂以空串认证（接线前修复）
        client.authenticate(username = "", password = password)

        // 3. 完整文件地址与读取链路一致
        val url = buildWebDavUrl(serverUrl = serverUrl, path = song.path)

        // 4. 下载 → 写标签 → 上传
        val tempFile = File.createTempFile("muses-scrape-", ".tmp", tempDir)
        try {
            try {
                client.get(url, tempFile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return FileWriteResult(
                    ok = false,
                    code = "download_failed",
                    message = e.message ?: "下载 WebDAV 音频失败。",
                )
            }

            val tagResult = withContext(Dispatchers.IO) { TagWriter.write(tempFile, request) }
            if (!tagResult.ok) {
                return FileWriteResult(ok = false, code = tagResult.code, message = tagResult.message)
            }

            try {
                client.put(url, tempFile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return FileWriteResult(
                    ok = false,
                    code = "put_failed",
                    message = e.message ?: "上传 WebDAV 音频失败。",
                )
            }
            return FileWriteResult(ok = true)
        } finally {
            tempFile.delete()
        }
    }
}

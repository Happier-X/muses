package com.muses.player.core.scrape.writeback

import com.muses.player.core.data.repository.CredentialsRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.Song
import com.muses.player.core.model.SourceType
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.http.ScrapeHttp
import com.muses.player.core.scrape.ports.TagPort
import com.muses.player.core.webdav.WebDavClient
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音频标签文件写入器（规格书 = src/features/scrape/writeback.ts 的 writeFile 分派语义）：
 * 按 song.sourceType 选择本地 / WebDAV 写入方式，失败返回 FileWriteResult 不抛异常。
 *
 * W3 上收 commonMain（任务 09-05-scrape-kmp R4）：
 * - 三仓库依赖（Song/Source/CredentialsRepository）为 :core:common commonMain 实体，直连无需 Port；
 * - WebDavClient 走 commonMain 接口（同包名上收）；
 * - TagWriter（core:media）依赖经 [TagPort] 收口，写入请求改传 ScrapeChanges + 封面字节
 *   （原 TagWriter.TagWriteRequest 映射下沉至 TagPort 实现，语义冻结）；
 * - `android.util.Log` → [safeLogW]/[safeLogE] expect/actual；
 * - URL 重建的 `android.net.Uri` 兜底分支改纯字符串解析（取值语义对齐 scheme/authority/path）。
 */
fun interface AudioTagFileWriter {
    suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult
}

/** 远程封面字节获取（对齐 Web ensureLocalCover：失败返回 null 跳过内嵌，不阻断写回） */
fun interface CoverBytesFetcher {
    suspend fun fetch(remoteUrl: String): ByteArray?
}

/** 默认实现：Ktor 二进制 GET（ScrapeHttp.getBytes，P2c 起经 CIO） */
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
 * 本地写路径：直接对 song.path 指向的物理文件经 [TagPort] 写入。
 * 文件不存在/格式不支持均由 TagPort 实现折叠为 write_failed 结果（对齐 file-failed 分类）。
 */
class LocalAudioTagFileWriter(
    private val tagPort: TagPort,
) : AudioTagFileWriter {
    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult =
        withContext(Dispatchers.IO) {
            tagPort.writeTags(File(song.path), changes, coverBytes)
        }
}

/**
 * WebDAV 写路径（对齐 writeWebDavFile 五步）：
 * 1. 按 song.sourceId 精确查找音源（多音源读写目标必须一致）；缺失 → no_password 文案 A
 * 2. 取密码；未配置 → no_password 文案 B
 * 3. 完整地址 = serverUrl + encodePath(song.path)（与读取链路一致）
 * 4. 下载到临时文件 → TagPort 写标签 → put 上传
 * 5. 各阶段失败映射 code：download_failed / write_failed / put_failed
 *
 * 认证用户名取自 source.username（Room v5 起持久化）。
 */
class WebDavAudioTagFileWriter(
    private val sourceRepository: SourceRepository,
    private val credentialsRepository: CredentialsRepository,
    /** 提供可用的 WebDAV 客户端（单例复用；串行写回下 authenticate 切换安全） */
    private val webDavClientFactory: suspend () -> WebDavClient,
    /** 标签写入端口（jaudiotagger 双端实现） */
    private val tagPort: TagPort,
    /** 下载临时目录（cache 目录，由装配方提供） */
    private val tempDir: File,
) : AudioTagFileWriter {

    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
        safeLogW("WebDavWrite", "write start songId=${song.id} path=${song.path} sourceId=${song.sourceId} title=${song.title}")
        // 确保临时目录存在（系统可能清理 cache）
        if (!tempDir.exists()) tempDir.mkdirs()
        // 1. 按歌曲所属音源精确查找
        val source = sourceRepository.getSource(song.sourceId)
        val serverUrl = source?.url
        if (source == null || source.type != SourceType.WEBDAV || serverUrl.isNullOrBlank()) {
            safeLogW("WebDavWrite", "no_password: source missing id=${song.sourceId} found=$source")
            return FileWriteResult(
                ok = false,
                code = "no_password",
                message = "未找到歌曲所属的 WebDAV 音源，请重新扫描后重试。",
            )
        }

        // 2. 密码
        val password = credentialsRepository.getPassword(source.id)
            ?: run {
                safeLogW("WebDavWrite", "no_password: missing credentials for source ${source.id}")
                return FileWriteResult(ok = false, code = "no_password", message = "WebDAV 密码未配置，请到音源设置补全后重试。")
            }

        val client = webDavClientFactory()
        client.authenticate(username = source.username ?: "", password = password)

        // 3. 完整文件地址：历史数据中 song.path 可能为完整 URL（WebDavLibraryScanner 存 item.url）或相对路径，需兼容
        val url = when {
            song.path.startsWith(serverUrl) -> {
                // 完整 URL 且与当前音源一致：抽取相对路径后重新编码，避免双重前缀与未编码中文/空格
                val suffix = song.path.removePrefix(serverUrl)
                buildWebDavUrl(serverUrl = serverUrl, path = suffix.ifEmpty { "/" })
            }
            song.path.startsWith("http://") || song.path.startsWith("https://") -> {
                // 完整 URL 但与当前音源不一致（换源或历史）：尝试按自身 host 重建编码，若失败则直接使用
                // 优先用 java.net.URI（JVM 单测友好）；解析失败回退纯字符串解析
                // （原 android.net.Uri 兜底分支改手动拆分，取值语义对齐 scheme/authority/path）
                try {
                    val parsed = try {
                        java.net.URI(song.path)
                    } catch (_: Exception) {
                        null
                    }
                    if (parsed != null && parsed.scheme != null && parsed.host != null) {
                        val authority = parsed.authority ?: parsed.host
                        val schemeHost = "${parsed.scheme}://$authority"
                        val pathPart = parsed.path ?: "/"
                        buildWebDavUrl(serverUrl = schemeHost, path = pathPart)
                    } else {
                        val uri = parseUrlParts(song.path)
                        val schemeHost = "${uri.first}://${uri.second}"
                        val pathPart = uri.third
                        buildWebDavUrl(serverUrl = schemeHost, path = pathPart)
                    }
                } catch (_: Exception) {
                    song.path
                }
            }
            else -> buildWebDavUrl(serverUrl = serverUrl, path = song.path)
        }
        safeLogW("WebDavWrite", "url=$url serverUrl=$serverUrl rawPath=${song.path}")

        // 4. 下载 → 写标签 → 上传：临时文件需保留原扩展名，否则 jaudiotagger 报 No Reader for .tmp
        // 先剥离 query/fragment，再取最后路径段的扩展名，避免 host/query 中含 . 导致误判（如 ?token=1.2）
        val ext = song.path.substringBefore('?').substringBefore('#').substringAfterLast('/').substringAfterLast('.', "").let { clean ->
            if (clean.isNotEmpty() && clean.length <= 5 && clean.all { it.isLetterOrDigit() }) ".$clean" else ".tmp"
        }
        val tempFile = try {
            File.createTempFile("muses-scrape-", ext, tempDir)
        } catch (e: Exception) {
            safeLogE("WebDavWrite", "createTempFile failed dir=$tempDir exists=${tempDir.exists()} ext=$ext", e)
            return FileWriteResult(ok = false, code = "download_failed", message = "创建临时文件失败: ${e.message}")
        }
        safeLogW("WebDavWrite", "tempFile=${tempFile.absolutePath} size will download")
        try {
            try {
                client.get(url, tempFile)
                safeLogW("WebDavWrite", "download ok size=${tempFile.length()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                safeLogE("WebDavWrite", "download_failed url=$url", e)
                return FileWriteResult(
                    ok = false,
                    code = "download_failed",
                    message = e.message ?: "下载 WebDAV 音频失败。",
                )
            }

            val tagResult = withContext(Dispatchers.IO) { tagPort.writeTags(tempFile, changes, coverBytes) }
            safeLogW("WebDavWrite", "tagWrite ok=${tagResult.ok} code=${tagResult.code} msg=${tagResult.message} changes=$changes")
            if (!tagResult.ok) {
                return FileWriteResult(ok = false, code = tagResult.code, message = tagResult.message)
            }

            try {
                client.put(url, tempFile)
                safeLogW("WebDavWrite", "put ok url=$url size=${tempFile.length()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                safeLogE("WebDavWrite", "put_failed url=$url", e)
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

    /** 纯字符串 URL 拆分：scheme / authority / path（android.net.Uri.parse 取值语义的 common 版） */
    private fun parseUrlParts(raw: String): Triple<String, String, String> {
        val withoutScheme = raw.substringAfter("://")
        val scheme = raw.substringBefore("://")
        val slashIndex = withoutScheme.indexOf('/')
        return if (slashIndex == -1) {
            Triple(scheme, withoutScheme, "/")
        } else {
            Triple(scheme, withoutScheme.take(slashIndex), withoutScheme.substring(slashIndex))
        }
    }
}

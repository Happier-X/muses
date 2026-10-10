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
import java.io.FileOutputStream
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
 *
 * 保存方式优先「同目录临时文件 + 原子替换」，因为写标签失败时原文件零风险；
 * 但安卓外部存储（/storage/emulated/0 下的 SAF 授权目录）不允许应用 rename 不是自己创建的文件，
 * 原子替换会返回 EPERM，所以必须回退为对原文件原地覆盖写入。
 */
class LocalAudioTagFileWriter(
    private val tagPort: TagPort,
    /** 原子替换实现；默认同目录 rename，单测可注入失败以覆盖外部存储回退路径 */
    private val atomicReplace: (File, File) -> Unit = { staged, original ->
        java.nio.file.Files.move(staged.toPath(), original.toPath(),
            java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    },
) : AudioTagFileWriter {
    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult =
        withContext(Dispatchers.IO) {
            val path = if (song.path.startsWith("file://")) java.net.URI(song.path).path else song.path
            val original = File(path)
            if (!original.isFile) return@withContext FileWriteResult(false, "write_failed", "本地音频文件不存在。")
            val staged = File.createTempFile(".muses-tags-", ".${original.extension}", original.absoluteFile.parentFile)
            try {
                original.copyTo(staged, overwrite = true)
                val result = tagPort.writeAndVerify(staged, changes, coverBytes)
                if (!result.ok) return@withContext result
                if (!replaceAtomically(staged, original, atomicReplace)) overwriteInPlace(staged, original)
                tagPort.verifyTags(original, changes, coverBytes)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { FileWriteResult(false, "write_failed", "本地文件保存失败：${e.message}") }
            finally { staged.delete() }
        }
}

/** 原子替换失败（安卓外部存储 EPERM）时改写回退路径；返回是否替换成功。 */
private fun replaceAtomically(staged: File, original: File, atomicReplace: (File, File) -> Unit): Boolean = try {
    atomicReplace(staged, original); true
} catch (e: CancellationException) { throw e }
catch (_: Exception) { false }

/**
 * 原地覆盖写入：SAF 授权目录下允许改写已有文件，只是不允许 rename 替换。
 * 覆盖前先备份原文件，写入失败时回滚，避免半截文件盖掉原音频。
 */
private fun overwriteInPlace(staged: File, original: File) {
    val parent = original.absoluteFile.parentFile
    val backup = File.createTempFile(".muses-backup-", ".${original.extension}", parent)
    original.copyTo(backup, overwrite = true)
    try {
        copyOver(staged, original)
    } catch (e: Exception) {
        runCatching { copyOver(backup, original) }
        throw e
    } finally { backup.delete() }
}

private fun copyOver(source: File, target: File) {
    FileOutputStream(target).use { output ->
        source.inputStream().use { it.copyTo(output) }
        output.fd.sync()
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
    private val pendingUploads: PendingScrapeUploads = PendingScrapeUploads.shared,
) : AudioTagFileWriter {

    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
        pendingUploads.forSong(song.id)?.let {
            error("这首歌曲已有待上传的刮削文件，请到下载页补传后再进行新的刮削。")
        }
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

        val client = webDavClientFactory().newSession()
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

            val originalHash = withContext(Dispatchers.IO) { audioFileHash(tempFile) }
            val tagResult = withContext(Dispatchers.IO) { tagPort.writeAndVerify(tempFile, changes, coverBytes) }
            safeLogW("WebDavWrite", "tagWrite ok=${tagResult.ok} code=${tagResult.code} msg=${tagResult.message} changes=$changes")
            if (!tagResult.ok) {
                return FileWriteResult(ok = false, code = tagResult.code, message = tagResult.message)
            }

            val pending = pendingUploads.prepare(song.id, source.id, serverUrl, url,
                changes.title ?: song.title, tempFile, originalHash,
                PendingScrapeMetadata(changes.title, changes.artist, changes.album,
                    changes.coverUri ?: changes.coverRemoteUrl, changes.lyrics, changes.lyricsFormat?.wire))
            if (!pendingUploads.upload(pending, client)) return FileWriteResult(false,
                "pending_upload", "本地已保存，待上传。可到下载页手动补传。")
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

package com.muses.player.core.scrape.writeback

import com.muses.player.core.data.platform.PlatformDirs
import com.muses.player.core.webdav.WebDavClient
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun audioFileHash(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

@Serializable
data class PendingScrapeUpload(
    val id: String,
    val songId: String,
    val sourceId: String,
    val sourceUrl: String,
    val url: String,
    val title: String,
    val fileName: String,
    val originalHash: String,
    val preparedHash: String,
    val error: String? = null,
    val metadata: PendingScrapeMetadata = PendingScrapeMetadata(),
)

@Serializable
data class PendingScrapeMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: String? = null,
    val lyrics: String? = null,
    val lyricsFormat: String? = null,
)

fun interface UploadedAudioInvalidator { fun invalidate(path: String) }

/** 音频和任务记录均持久保存；只清理已经核验上传成功的应用私有副本。 */
class PendingScrapeUploads(private val directory: File) {
    companion object {
        val shared: PendingScrapeUploads by lazy {
            PendingScrapeUploads(File(PlatformDirs.appDataDir(), "pending-scrape-uploads"))
        }
    }
    private val mutex = Mutex()
    private val uploadMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val manifest = File(directory, "tasks.json")
    private val state = MutableStateFlow(if (manifest.isFile)
        json.decodeFromString<List<PendingScrapeUpload>>(manifest.readText()) else emptyList())
    val tasks = state.asStateFlow()
    private val uploadingState = MutableStateFlow<String?>(null)
    val uploading = uploadingState.asStateFlow()

    fun file(task: PendingScrapeUpload): File = File(directory, "${task.id}/${task.fileName}")
    fun forSong(songId: String) = state.value.firstOrNull { it.songId == songId }

    private fun persist(tasks: List<PendingScrapeUpload>) {
        check(directory.isDirectory || directory.mkdirs()) { "无法创建本地待上传目录" }
        val staging = File(directory, "tasks.json.part")
        java.io.FileOutputStream(staging).use { output ->
            output.write(json.encodeToString(tasks).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        try {
            Files.move(staging.toPath(), manifest.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(staging.toPath(), manifest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        state.value = tasks
    }

    suspend fun prepare(songId: String, sourceId: String, sourceUrl: String, url: String,
        title: String, audio: File, originalHash: String,
        metadata: PendingScrapeMetadata = PendingScrapeMetadata()): PendingScrapeUpload = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(forSong(songId) == null) { "这首歌曲已有待上传的刮削文件，请先补传" }
            val task = PendingScrapeUpload(UUID.randomUUID().toString(), songId, sourceId, sourceUrl,
                url, title, com.muses.player.core.model.download.downloadBaseName(
                    com.muses.player.core.model.download.DownloadTrack(songId, sourceId, url, title)) + "." + audio.extension,
                originalHash, audioFileHash(audio), metadata = metadata)
            val destination = file(task)
            check(destination.parentFile.isDirectory || destination.parentFile.mkdirs()) { "无法创建本地待上传目录" }
            java.io.FileOutputStream(destination).use { output ->
                audio.inputStream().use { it.copyTo(output) }
                output.fd.sync()
            }
            persist(state.value + task)
            task
        }
    }

    /** 回读完整音频核验；超时后再次补传先识别上次已保存的结果，不重复 PUT。 */
    suspend fun upload(task: PendingScrapeUpload, client: WebDavClient,
        onUploaded: suspend (PendingScrapeUpload) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        uploadMutex.withLock {
            if (state.value.none { it.id == task.id }) return@withLock true
            uploadingState.value = task.id
            val probe = File(directory, "${task.id}/remote.part")
            try {
                val audio = file(task)
                check(audio.isFile && audioFileHash(audio) == task.preparedHash) { "本地音频完整性核验失败" }
                val eTag = withTimeout(90_000) { client.strongETag(task.url) }
                withTimeout(90_000) { client.get(task.url, probe) }
                val remoteHash = audioFileHash(probe)
                if (remoteHash != task.preparedHash) {
                    check(remoteHash == task.originalHash) { "远端文件已经变化，已停止补传，请重新核对；本地文件仍已保留" }
                    check(eTag != null) { "服务器未提供可靠的文件版本，已保留本地文件，请导出后自行上传" }
                    withTimeout(120_000) { client.putIfMatch(task.url, audio, eTag) }
                    withTimeout(90_000) { client.get(task.url, probe) }
                    check(audioFileHash(probe) == task.preparedHash) { "尚未确认网盘保存完整，本地文件已保留，请稍后补传" }
                }
                withContext(NonCancellable) {
                    onUploaded(task)
                    mutex.withLock { persist(state.value.filterNot { it.id == task.id }) }
                    audio.delete()
                }
                true
            } catch (e: TimeoutCancellationException) {
                mutex.withLock { persist(state.value.map { if (it.id == task.id)
                    it.copy(error = "网盘保存确认超时，本地文件已保留，请稍后补传") else it }) }
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutex.withLock { persist(state.value.map { if (it.id == task.id)
                    it.copy(error = e.message ?: "上传未完成，请稍后补传") else it }) }
                false
            } finally {
                probe.delete()
                uploadingState.value = null
            }
        }
    }

    suspend fun reportError(id: String, message: String) = withContext(Dispatchers.IO) {
        mutex.withLock { persist(state.value.map { if (it.id == id) it.copy(error = message) else it }) }
    }
}

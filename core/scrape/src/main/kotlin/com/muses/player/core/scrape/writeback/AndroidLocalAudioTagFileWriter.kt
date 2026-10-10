package com.muses.player.core.scrape.writeback

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.TagPort
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 本地歌曲优先通过已授权的目录文档地址写回，暂存内嵌后提交并回读。 */
class AndroidLocalAudioTagFileWriter(private val context: Context, private val tagPort: TagPort) : AudioTagFileWriter {
    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val sourceUri = Uri.parse(song.path)
            val physical = if (!song.path.startsWith("content://")) {
                File(if (song.path.startsWith("file://")) sourceUri.path.orEmpty() else song.path)
            } else runCatching {
                resolver.query(sourceUri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0)?.let(::File) else null
                }
            }.getOrNull()
            val authorizedUri = physical?.let { findAuthorizedFile(it) }
            if (authorizedUri == null && physical?.isFile == true && physical.canWrite()) {
                return@withContext LocalAudioTagFileWriter(tagPort).write(song.copy(path = physical.absolutePath), changes, coverBytes)
            }
            if (authorizedUri == null && !song.path.startsWith("content://")) {
                return@withContext FileWriteResult(false, "write_failed", "本地目录没有写入权限，请在音源设置中重新选择歌曲所在目录后重试。")
            }
            val uri = authorizedUri ?: sourceUri
            val name = runCatching {
                resolver.query(uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull() ?: physical?.name.orEmpty()
            val extension = name.substringAfterLast('.', "").takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
                ?: return@withContext FileWriteResult(false, "write_failed", "无法确认本地音频格式。")
            val directory = File(context.filesDir, "local-tag-writeback").apply { mkdirs() }
            val original = File.createTempFile("original-", ".$extension", directory)
            val prepared = File.createTempFile("prepared-", ".$extension", directory)
            try {
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(original).use { output -> input.copyTo(output); output.fd.sync() }
                } ?: return@withContext FileWriteResult(false, "write_failed", "无法读取本地音频。")
                original.copyTo(prepared, overwrite = true)
                val result = tagPort.writeAndVerify(prepared, changes, coverBytes)
                if (!result.ok) return@withContext result
                // 提交及核验期间不可取消；失败尝试恢复原音频，恢复失败保留备份。
                withContext(kotlinx.coroutines.NonCancellable) {
                    var writeStarted = false
                    fun commit(file: File) {
                        val descriptor = resolver.openFileDescriptor(uri, "rw") ?: error("没有本地音频写入权限")
                        android.os.ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
                                // 成功打开后才开始截断；打开被拒时原文件没有改变，无需恢复。
                                writeStarted = true
                                output.channel.truncate(0)
                                output.channel.position(0)
                                file.inputStream().use { it.copyTo(output) }; output.fd.sync()
                        }
                    }
                    try {
                        commit(prepared)
                        resolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(prepared).use { input.copyTo(it) }
                        } ?: error("无法回读保存后的音频")
                        val verified = tagPort.verifyTags(prepared, changes, coverBytes)
                        check(verified.ok) { verified.message.orEmpty() }
                        original.delete()
                        verified
                    } catch (e: Exception) {
                        android.util.Log.e("LocalTagWrite", "保存本地标签失败 uri=$uri path=${song.path} writeStarted=$writeStarted", e)
                        if (!writeStarted) {
                            original.delete()
                            return@withContext FileWriteResult(false, "write_failed",
                                "本地文件未改动，无法取得写入权限，请在音源设置中重新选择歌曲所在目录后重试：${e.message}")
                        }
                        val recovery = runCatching { commit(original) }
                        recovery.exceptionOrNull()?.let { error ->
                            android.util.Log.e("LocalTagWrite", "恢复原音频失败，备份=${original.absolutePath}", error)
                        }
                        val restored = recovery.isSuccess
                        if (restored) original.delete()
                        FileWriteResult(false, "write_failed", if (restored) "本地文件保存失败，已恢复原音频：${e.message}"
                            else "本地文件保存失败：${e.message}；恢复失败，原音频备份已保留在 ${original.absolutePath}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FileWriteResult(false, "write_failed", "本地标签写入失败：${e.message}")
            } finally {
                prepared.delete()
            }
        }

    /** 目录授权只适用于 Documents URI，不能直接移植到 MediaStore URI 或物理路径。 */
    internal fun findAuthorizedFile(file: File): Uri? {
        val path = file.canonicalPath
        val grant = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission && it.uri.authority == "com.android.externalstorage.documents" }
            .mapNotNull { permission ->
                val id = runCatching { DocumentsContract.getTreeDocumentId(permission.uri) }.getOrNull()
                    ?: return@mapNotNull null
                val documentId = externalStorageDocumentId(path, id, android.os.Environment.getExternalStorageDirectory().canonicalPath)
                    ?: return@mapNotNull null
                Triple(permission.uri, documentId, id.length)
            }.maxByOrNull { it.third } ?: return null
        return DocumentsContract.buildDocumentUriUsingTree(grant.first, grant.second)
    }
}

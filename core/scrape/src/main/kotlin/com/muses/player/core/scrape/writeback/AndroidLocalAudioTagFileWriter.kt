package com.muses.player.core.scrape.writeback

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.muses.player.core.model.Song
import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import com.muses.player.core.scrape.ports.TagPort
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** MediaStore 地址先解析真实文件；不能直写时暂存、内嵌、回读再提交。 */
class AndroidLocalAudioTagFileWriter(private val context: Context, private val tagPort: TagPort) : AudioTagFileWriter {
    override suspend fun write(song: Song, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult =
        withContext(Dispatchers.IO) {
            if (!song.path.startsWith("content://")) {
                return@withContext LocalAudioTagFileWriter(tagPort).write(song, changes, coverBytes)
            }
            val uri = Uri.parse(song.path)
            val resolver = context.contentResolver
            val physical = runCatching {
                resolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0)?.let(::File) else null
                }
            }.getOrNull()
            if (physical?.isFile == true && physical.canWrite()) {
                return@withContext LocalAudioTagFileWriter(tagPort).write(song.copy(path = physical.absolutePath), changes, coverBytes)
            }
            val name = runCatching {
                resolver.query(uri, arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull().orEmpty()
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
                    fun commit(file: File) {
                        resolver.openFileDescriptor(uri, "rwt")?.use { descriptor ->
                            FileOutputStream(descriptor.fileDescriptor).use { output ->
                                file.inputStream().use { it.copyTo(output) }; output.fd.sync()
                            }
                        } ?: error("没有本地音频写入权限")
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
                        val restored = runCatching { commit(original) }.isSuccess
                        if (restored) original.delete()
                        FileWriteResult(false, "write_failed", if (restored) "本地文件保存失败，已恢复原音频：${e.message}"
                            else "本地文件保存失败，原音频备份已保留在 ${original.absolutePath}")
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
}

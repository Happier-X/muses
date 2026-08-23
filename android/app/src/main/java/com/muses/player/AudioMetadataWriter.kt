package com.muses.player

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.getcapacitor.JSObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.vorbiscomment.util.Base64Coder

/**
 * 音频内嵌标签写入（jaudiotagger）。
 * 本地 SAF：content → 临时文件写 tag → openOutputStream 回写。
 * WebDAV：对本地缓存文件写 tag，由调用方 PUT。
 */
class AudioMetadataWriter(private val context: Context) {

    data class WriteRequest(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        val clearLyrics: Boolean = false,
        /** 封面本地 file:// 或绝对路径；clearCover=true 时删除内嵌图 */
        val coverPath: String? = null,
        val clearCover: Boolean = false,
        val replayGainTrackDb: Double? = null,
        val clearReplayGain: Boolean = false,
    )

    fun writeToFile(file: File, request: WriteRequest) {
        // Android 上无 javax.imageio，必须走 AndroidArtwork/AndroidImageHandler（BitmapFactory 实现），
        // 否则 FLAC/OGG 封面写入会因 NoClassDefFoundError 闪退。
        TagOptionSingleton.getInstance().setAndroid(true)
        if (!file.exists() || file.length() <= 0L) {
            throw AudioMetadataException("empty_file", "音频文件为空，无法写入标签。")
        }
        if (!isLikelySupportedExtension(file.name)) {
            throw AudioMetadataException("unsupported_format", "当前格式暂不支持写入标签。")
        }

        val audioFile: AudioFile = try {
            AudioFileIO.read(file)
        } catch (exception: Exception) {
            throw AudioMetadataException("unsupported_format", "无法解析该音频格式以写入标签。")
        }

        val tag = audioFile.tagOrCreateAndSetDefault
            ?: throw AudioMetadataException("unsupported_format", "该容器无法创建标签。")

        applyFields(tag, request)
        try {
            audioFile.commit()
        } catch (exception: Exception) {
            throw AudioMetadataException("write_failed", exception.message ?: "写入标签失败。")
        }
    }

    /**
     * 将 content:// 复制到临时文件、写 tag、再写回 Document。
     * 成功返回 ok；失败抛 AudioMetadataException（含 code）。
     */
    fun writeToContentUri(uri: Uri, request: WriteRequest) {
        val extension = guessExtension(uri)
        val tempFile = File.createTempFile("metadata-write-", extension, context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: throw AudioMetadataException("missingUri", "音频文件不可访问。")

            if (tempFile.length() <= 0L) {
                throw AudioMetadataException("empty_file", "音频文件为空，无法写入标签。")
            }

            writeToFile(tempFile, request)

            val outputStream = try {
                // "wt" 截断写回；部分 DocumentProvider 可能不支持
                context.contentResolver.openOutputStream(uri, "wt")
                    ?: context.contentResolver.openOutputStream(uri)
            } catch (exception: Exception) {
                throw AudioMetadataException("not_writable", "无法打开文件进行写回。")
            } ?: throw AudioMetadataException("not_writable", "该文件不可写。")

            try {
                outputStream.use { output ->
                    FileInputStream(tempFile).use { input -> input.copyTo(output) }
                }
            } catch (exception: Exception) {
                throw AudioMetadataException("not_writable", "写回音频文件失败。")
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun applyFields(tag: Tag, request: WriteRequest) {
        request.title?.let { setOrDelete(tag, FieldKey.TITLE, it) }
        request.artist?.let { setOrDelete(tag, FieldKey.ARTIST, it) }
        request.album?.let { setOrDelete(tag, FieldKey.ALBUM, it) }

        when {
            request.clearLyrics -> runCatching { tag.deleteField(FieldKey.LYRICS) }
            request.lyrics != null -> setOrDelete(tag, FieldKey.LYRICS, request.lyrics)
        }

        when {
            request.clearCover -> runCatching { tag.deleteArtworkField() }
            !request.coverPath.isNullOrBlank() -> {
                val coverFile = resolveLocalFile(request.coverPath)
                    ?: throw AudioMetadataException("cover_not_found", "封面文件不存在。")
                if (!coverFile.exists() || coverFile.length() <= 0L) {
                    throw AudioMetadataException("cover_not_found", "封面文件不存在。")
                }
                try {
                    runCatching { tag.deleteArtworkField() }
                    // AndroidArtwork.setImageFromData/getImage 无条件抛 UnsupportedOperationException，
                    // FlacTag.createField(artwork) 内部依赖该解码判断，Vorbis 系（FLAC/OGG）必须直接构造字段。
                    val bytes = coverFile.readBytes()
                    if (bytes.isEmpty()) {
                        throw AudioMetadataException("cover_not_found", "封面文件为空。")
                    }
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(coverFile.absolutePath, opts)
                    val mime = opts.outMimeType ?: guessCoverMime(coverFile.name)
                    when (tag) {
                        // FLAC：Picture 元数据块，createArtworkField 直接构造，不经 Artwork 解码
                        is FlacTag -> tag.setField(
                            tag.createArtworkField(
                                bytes,
                                3, /* PictureTypes.PICTURE_TYPE_FRONT_COVER */
                                mime,
                                "",
                                opts.outWidth,
                                opts.outHeight,
                                0,
                                0,
                            ),
                        )
                        // OGG：METADATA_BLOCK_PICTURE = base64(picture block)，镜像库内部 createField(Artwork) 逻辑但绕开 setImageFromData
                        is VorbisCommentTag -> {
                            val picture = MetadataBlockDataPicture(
                                bytes,
                                3,
                                mime,
                                "",
                                opts.outWidth,
                                opts.outHeight,
                                0,
                                0,
                            )
                            val encoded = String(Base64Coder.encode(picture.rawContent))
                            tag.setField(
                                tag.createField(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE, encoded),
                            )
                        }
                        else -> {
                            val artwork = AndroidArtwork()
                            artwork.binaryData = bytes
                            artwork.mimeType = mime
                            artwork.pictureType = 3
                            artwork.description = ""
                            tag.setField(artwork)
                        }
                    }
                } catch (exception: AudioMetadataException) {
                    throw exception
                } catch (t: Throwable) {
                    // NoClassDefFoundError 等 Error 也可能击穿插件线程，统一转成受控异常
                    throw AudioMetadataException(
                        "write_failed",
                        "写入封面失败：${t.message ?: t::class.java.simpleName}",
                    )
                }
            }
        }

        when {
            request.clearReplayGain -> deleteReplayGainTrack(tag)
            request.replayGainTrackDb != null -> writeReplayGainTrack(tag, request.replayGainTrackDb)
        }
    }

    private fun setOrDelete(tag: Tag, key: FieldKey, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            runCatching { tag.deleteField(key) }
            return
        }
        tag.setField(key, trimmed)
    }

    /**
     * jaudiotagger 3.0.1 无标准 REPLAYGAIN FieldKey。
     * - Vorbis/FLAC：setField(String, String) 写 REPLAYGAIN_TRACK_GAIN
     * - ID3：TXXX frame（description=REPLAYGAIN_TRACK_GAIN）
     * 格式："-6.50 dB"（两位小数）。失败不抛——库侧已按 D4 保存。
     */
    private fun writeReplayGainTrack(tag: Tag, db: Double) {
        if (!db.isFinite()) {
            return
        }
        val formatted = String.format(java.util.Locale.US, "%.2f dB", db)
        deleteReplayGainTrack(tag)

        // Vorbis comment / FLAC / 部分通用 Tag
        val wroteVorbis = runCatching {
            val m = tag.javaClass.getMethod("setField", String::class.java, String::class.java)
            m.invoke(tag, "REPLAYGAIN_TRACK_GAIN", formatted)
            true
        }.getOrDefault(false)

        if (wroteVorbis) {
            return
        }

        // ID3v2 TXXX
        runCatching {
            val txxxClass = Class.forName("org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX")
            val body = txxxClass.getDeclaredConstructor().newInstance()
            txxxClass.getMethod("setDescription", String::class.java)
                .invoke(body, "REPLAYGAIN_TRACK_GAIN")
            txxxClass.getMethod("setText", String::class.java).invoke(body, formatted)
            val frameClass = Class.forName("org.jaudiotagger.tag.id3.ID3v24Frame")
            val frame = frameClass.getConstructor(String::class.java).newInstance("TXXX")
            frameClass.getMethod("setBody", Class.forName("org.jaudiotagger.tag.id3.AbstractTagFrameBody"))
                .invoke(frame, body)
            tag.setField(frame as org.jaudiotagger.tag.TagField)
        }
    }

    private fun deleteReplayGainTrack(tag: Tag) {
        runCatching {
            val m = tag.javaClass.getMethod("deleteField", String::class.java)
            m.invoke(tag, "REPLAYGAIN_TRACK_GAIN")
            m.invoke(tag, "R128_TRACK_GAIN")
        }
        // 尽力清理旧 TXXX（失败忽略）
        runCatching {
            val fields = tag.getFields("TXXX")
            // 无法按 description 精确删时整类清理风险高，仅 no-op
            fields
        }
    }

    private fun resolveLocalFile(pathOrUri: String): File? {
        val trimmed = pathOrUri.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        return when {
            trimmed.startsWith("file://", ignoreCase = true) -> {
                runCatching { File(Uri.parse(trimmed).path ?: return null) }.getOrNull()
            }
            trimmed.startsWith("/") -> File(trimmed)
            else -> File(trimmed)
        }
    }

    /** BitmapFactory 解不出 mime 时按扩展名兜底。 */
    private fun guessCoverMime(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun guessExtension(uri: Uri): String {
        val last = uri.lastPathSegment.orEmpty()
        val ext = last.substringAfterLast('.', "").lowercase()
        return if (ext in SUPPORTED_EXTENSIONS) ".$ext" else ".audio"
    }

    private fun isLikelySupportedExtension(name: String): Boolean {
        // 剥离已知临时后缀，避免工作副本命名（如 <name>.write-tmp.mp3）被误判为不支持的格式
        var stripped = name
        for (suffix in listOf(".write-tmp", ".tmp", ".partial")) {
            if (stripped.endsWith(suffix, ignoreCase = true)) {
                stripped = stripped.removeSuffix(suffix)
                break
            }
        }
        val ext = stripped.substringAfterLast('.', "").lowercase()
        // 无扩展名时仍尝试（临时文件可能是 .audio）
        if (ext.isEmpty() || ext == "audio") {
            return true
        }
        return ext in SUPPORTED_EXTENSIONS
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "flac", "m4a", "m4b", "aac", "ogg", "opus", "wav", "aiff", "aif", "wma", "ape",
        )

        fun requestFromCall(
            title: String?,
            artist: String?,
            album: String?,
            lyrics: String?,
            clearLyrics: Boolean,
            coverPath: String?,
            clearCover: Boolean,
            replayGainTrackDb: Double?,
            clearReplayGain: Boolean,
        ): WriteRequest {
            return WriteRequest(
                title = title,
                artist = artist,
                album = album,
                lyrics = lyrics,
                clearLyrics = clearLyrics,
                coverPath = coverPath,
                clearCover = clearCover,
                replayGainTrackDb = replayGainTrackDb,
                clearReplayGain = clearReplayGain,
            )
        }

        fun successResult(): JSObject {
            return JSObject().put("ok", true)
        }

        fun failureResult(code: String, message: String): JSObject {
            return JSObject()
                .put("ok", false)
                .put("code", code)
                .put("message", message)
        }

        /** base64 图片字节写入 covers 缓存，返回 file:// */
        fun cacheCoverBytes(context: Context, cacheKey: String, base64Data: String): String? {
            val raw = base64Data.trim().let { value ->
                val comma = value.indexOf(',')
                if (value.startsWith("data:", ignoreCase = true) && comma >= 0) {
                    value.substring(comma + 1)
                } else {
                    value
                }
            }
            val bytes = try {
                Base64.decode(raw, Base64.DEFAULT)
            } catch (_: Exception) {
                return null
            }
            if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) {
                return null
            }
            val isImage =
                (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) ||
                    (bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) ||
                    (bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[8] == 0x57.toByte()) ||
                    (bytes.size >= 6 && bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte())
            if (!isImage) {
                return null
            }
            val directory = File(context.cacheDir, "covers").apply { mkdirs() }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(cacheKey.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val file = File(directory, "$digest.jpg")
            file.writeBytes(bytes)
            return Uri.fromFile(file).toString()
        }
    }
}

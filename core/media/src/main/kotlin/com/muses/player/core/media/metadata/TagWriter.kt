package com.muses.player.core.media.metadata

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

/**
 * jaudiotagger 标签写入器（任务 08-25-native-m3-scrape-engine / S0）。
 *
 * 规格书 = Web 层 src/features/library/native.ts 的 WriteMetadataOptions / WriteMetadataResult：
 * - ok=true 成功；失败带 code 便于文案映射，**不抛异常**
 * - null 字段 = 不修改；clearXxx = 显式清空该字段
 * - 格式兼容性失败统一归入 code=write_failed（对应 Web 写回编排的 file-failed 分类）
 */
object TagWriter {

    /** 写标签结果（对齐 Web WriteMetadataResult 字段与语义） */
    data class WriteResult(
        val ok: Boolean,
        val code: String? = null,
        val message: String? = null,
    ) {
        companion object {
            fun success(): WriteResult = WriteResult(ok = true)
            fun failure(code: String, message: String): WriteResult =
                WriteResult(ok = false, code = code, message = message)
        }
    }

    /**
     * 标签写入请求（字段集对齐 Web WriteMetadataOptions 的本地写路径）。
     * 与 [TrackTags] 不同：这是「待写入值」而非「已读取值」，
     * null 语义为不修改（Web undefined），clear* 为显式清空（Web clearLyrics/clearCover）。
     */
    data class TagWriteRequest(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        /** 歌词正文；空串配合 [clearLyrics]=false 时同样视为清空（调用方也可显式传 clearLyrics） */
        val lyrics: String? = null,
        val clearLyrics: Boolean = false,
        /** 内嵌封面原始字节（jpg/png 均可，由容器自行封装） */
        val coverBytes: ByteArray? = null,
        val clearCover: Boolean = false,
    )

    /**
     * 写入音频文件标签。读文件失败 / 无可写标签 / 容器不支持均返回失败结果，不抛异常。
     */
    fun write(file: File, request: TagWriteRequest): WriteResult {
        return try {
            val audioFile = AudioFileIO.read(file)
            // 无标签时创建并挂上容器默认标签（Mp3→ID3v2.4 等）
            val tag = audioFile.tagOrCreateAndSetDefault
            applyRequest(tag, request)
            AudioFileIO.write(audioFile)
            WriteResult.success()
        } catch (e: Exception) {
            // 对齐 Web writeLocalAudioMetadata：异常折叠为 ok=false + code，不让上层崩溃
            WriteResult.failure(
                code = "write_failed",
                message = e.message ?: "写入音频标签失败。",
            )
        }
    }

    private fun applyRequest(tag: Tag, request: TagWriteRequest) {
        setFieldIfPresent(tag, FieldKey.TITLE, request.title)
        setFieldIfPresent(tag, FieldKey.ARTIST, request.artist)
        setFieldIfPresent(tag, FieldKey.ALBUM, request.album)

        if (request.clearLyrics || (request.lyrics != null && request.lyrics.isEmpty())) {
            runCatching { tag.deleteField(FieldKey.LYRICS) }
        } else if (!request.lyrics.isNullOrBlank()) {
            tag.setField(FieldKey.LYRICS, request.lyrics)
        }

        when {
            request.clearCover -> runCatching { tag.deleteArtworkField() }
            request.coverBytes != null && request.coverBytes.isNotEmpty() -> {
                runCatching { tag.deleteArtworkField() }
                tag.addField(createArtwork(request.coverBytes))
            }
        }
    }

    /** 由字节签名构造 Artwork（jpg/png）；jaudiotagger 3.0.1 无按字节创建的工厂方法 */
    private fun createArtwork(bytes: ByteArray): Artwork {
        val artwork = ArtworkFactory.getNew()
        artwork.binaryData = bytes
        artwork.mimeType = sniffImageMime(bytes)
        return artwork
    }

    private fun sniffImageMime(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()
            && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
        else -> null
    }

    private fun setFieldIfPresent(tag: Tag, key: FieldKey, value: String?) {
        if (value == null) return
        if (value.isBlank()) return
        tag.setField(key, value)
    }
}

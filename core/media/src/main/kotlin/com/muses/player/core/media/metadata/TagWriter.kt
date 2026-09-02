package com.muses.player.core.media.metadata

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.Artwork
import java.io.File

/**
 * jaudiotagger 标签写入器（任务 08-25-native-m3-scrape-engine / S0 / 09-02 ImageIO 修复）。
 *
 * 规格书 = Web 层 src/features/library/native.ts 的 WriteMetadataOptions / WriteMetadataResult：
 * - ok=true 成功；失败带 code 便于文案映射，**不抛异常**
 * - null 字段 = 不修改；clearXxx = 显式清空该字段
 * - 格式兼容性失败统一归入 code=write_failed（对应 Web 写回编排的 file-failed 分类）
 *
 * 09-02 修复：Android 端禁止使用 javax.imageio.ImageIO（StandardArtwork 在 FLAC 路径会触发
 * NoClassDefFoundError），改用 [AndroidSafeArtwork] 绕开 ImageIO，并兜底 Throwable。
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
     * 注意：捕获 Throwable 以兜底 NoClassDefFoundError（如 ImageIO）、LinkageError 等 Error。
     */
    fun write(file: File, request: TagWriteRequest): WriteResult {
        return try {
            val audioFile = AudioFileIO.read(file)
            // 无标签时创建并挂上容器默认标签（Mp3→ID3v2.4 等）
            val tag = audioFile.tagOrCreateAndSetDefault
            applyRequest(tag, request)
            AudioFileIO.write(audioFile)
            WriteResult.success()
        } catch (e: Throwable) {
            // 协程取消需原样重抛，不计为 write_failed
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (e is InterruptedException) throw e
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

    /** 由字节签名构造 Artwork（jpg/png）；Android 安全实现，绕开 javax.imageio */
    private fun createArtwork(bytes: ByteArray): Artwork {
        val artwork = AndroidSafeArtwork()
        artwork.binaryData = bytes
        artwork.mimeType = sniffImageMime(bytes)
        // pictureType 默认 3（Cover front），与 PictureTypes.DEFAULT_ID 一致
        artwork.pictureType = 3
        artwork.description = ""
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

    /**
     * Android 安全的 Artwork 实现：
     * - 不依赖 javax.imageio / java.awt（StandardArtwork 的崩溃根因）
     * - setImageFromData() 优先用反射调 android.graphics.BitmapFactory 解宽高，失败则 0×0 并返回 true，
     *   满足 FlacTag/VorbisCommentTag 对 setImageFromData()==true 的校验
     * - getImage() 保持抛 UnsupportedOperationException，与 AndroidArtwork 一致
     */
    private class AndroidSafeArtwork : Artwork {
        private var data: ByteArray? = null
        private var mime: String? = ""
        private var desc: String = ""
        private var linked: Boolean = false
        private var url: String = ""
        private var picType: Int = 3
        private var w: Int = 0
        private var h: Int = 0

        override fun getBinaryData(): ByteArray? = data
        override fun setBinaryData(data: ByteArray?) { this.data = data }
        override fun getMimeType(): String? = mime
        override fun setMimeType(mime: String?) { this.mime = mime }
        override fun getDescription(): String = desc
        override fun setDescription(desc: String?) { this.desc = desc ?: "" }
        override fun getWidth(): Int = w
        override fun setWidth(width: Int) { w = width }
        override fun getHeight(): Int = h
        override fun setHeight(height: Int) { h = height }
        override fun isLinked(): Boolean = linked
        override fun setLinked(linked: Boolean) { this.linked = linked }
        override fun getImageUrl(): String = url
        override fun setImageUrl(url: String?) { this.url = url ?: "" }
        override fun getPictureType(): Int = picType
        override fun setPictureType(type: Int) { picType = type }

        override fun setFromFile(file: File) {
            throw UnsupportedOperationException("setFromFile not supported")
        }

        override fun setFromMetadataBlockDataPicture(coverArt: MetadataBlockDataPicture) {
            mime = coverArt.mimeType
            desc = coverArt.description ?: ""
            picType = coverArt.pictureType
            if (coverArt.isImageUrl) {
                linked = true
                url = coverArt.imageUrl ?: ""
            } else {
                data = coverArt.imageData
            }
            w = coverArt.width
            h = coverArt.height
        }

        override fun getImage(): Any {
            throw UnsupportedOperationException("getImage not supported on Android")
        }

        override fun setImageFromData(): Boolean {
            val bytes = data ?: return true
            // 尝试通过反射取 BitmapFactory 宽高（Android 真机）；JVM 单测无此则回退 0
            val decoded = tryDecodeBounds(bytes)
            if (decoded != null) {
                w = decoded.first
                h = decoded.second
            } else {
                w = 0
                h = 0
            }
            return true
        }

        private fun tryDecodeBounds(bytes: ByteArray): Pair<Int, Int>? {
            return try {
                val factoryClazz = Class.forName("android.graphics.BitmapFactory")
                val optionsClazz = Class.forName("android.graphics.BitmapFactory\$Options")
                val opts = optionsClazz.getDeclaredConstructor().newInstance()
                optionsClazz.getField("inJustDecodeBounds").setBoolean(opts, true)
                val method = factoryClazz.getMethod(
                    "decodeByteArray",
                    ByteArray::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    optionsClazz,
                )
                method.invoke(null, bytes, 0, bytes.size, opts)
                val outW = optionsClazz.getField("outWidth").getInt(opts)
                val outH = optionsClazz.getField("outHeight").getInt(opts)
                if (outW > 0 && outH > 0) outW to outH else null
            } catch (_: Throwable) {
                null
            }
        }

        // 保留二进制兼容：部分版本 jaudiotagger 的 Artwork 含此重载，Kotlin 编译期不需要但运行时需兼容
        @Suppress("unused")
        fun setLinkedFromURL(url: String) {
            linked = true
            this.url = url
        }
    }
}

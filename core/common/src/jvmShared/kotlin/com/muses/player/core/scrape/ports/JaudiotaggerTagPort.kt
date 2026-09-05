package com.muses.player.core.scrape.ports

import com.muses.player.core.model.scrape.FileWriteResult
import com.muses.player.core.model.scrape.ScrapeChanges
import java.io.File
import kotlinx.coroutines.CancellationException
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.Artwork

/**
 * jaudiotagger 标签端口实现（W3 写回链 KMP 化，design.md §1「jvmMain & androidMain 同库双端」）。
 *
 * 放置决策（core:common jvmShared 而非 core:media）：
 * - core:media 是 android library，composeApp 桌面（jvm）无法依赖——若实现留在 core:media，
 *   W4 桌面装配将无 TagPort 可注入，被迫在 composeApp 复制第三份 jaudiotagger 逻辑；
 * - jvmShared 中间层让同一份代码同时进入 androidMain 与 jvmMain（jaudiotagger 纯 JVM，双端可载）；
 * - 写逻辑逐字上收自 core/media TagWriter（任务 08-25 / 09-02 ImageIO 修复版），写回语义冻结。
 *   core/media TagWriter 本体保留（TagWriterTest 存量回归 + 09-02 修复档案），生产引用归零后
 *   由装配层切换注入本实现。
 *
 * Android 适配语义逐字保留：
 * - [AndroidSafeArtwork] 绕开 javax.imageio（StandardArtwork 的 FLAC 崩溃根因）；
 *   setImageFromData 反射 BitmapFactory 取宽高，JVM 桌面无此类时回退 0×0 并 return true（写路径不受影响）
 * - `catch (Throwable)` 兜底 NoClassDefFoundError / LinkageError；CancellationException/InterruptedException 原样重抛
 * - 进程级初始化（MusesApplication.onCreate 的 `TagOptionSingleton.setAndroid(true)`）不受迁移影响
 */
object JaudiotaggerTagPort : TagPort {

    // ── TagPort：读 ───────────────────────────────────────

    /** 通用读（AudioFileIO 全量解析，取文本字段 + 首帧封面 + 时长）；失败返回 null */
    override fun readTags(file: File): TagPortTags? {
        return try {
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            TagPortTags(
                title = tag?.getFirst(FieldKey.TITLE),
                artist = tag?.getFirst(FieldKey.ARTIST),
                album = tag?.getFirst(FieldKey.ALBUM),
                lyrics = tag?.getFirst(FieldKey.LYRICS),
                cover = tag?.firstArtwork?.binaryData,
                durationMs = audioFile.audioHeader?.trackLength?.times(1000L) ?: 0L,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    // ── TagPort：写（文件 + ScrapeChanges → 结果）──────────

    /**
     * 写入刮削变更（对齐原 WritebackOrchestrator.buildTagRequest 映射，语义冻结）：
     * null = 不修改；`lyrics == ""` → clearLyrics；`coverUri == ""` → clearCover。
     */
    override fun writeTags(file: File, changes: ScrapeChanges, coverBytes: ByteArray?): FileWriteResult {
        val result = write(
            file = file,
            request = TagWriteRequest(
                title = changes.title,
                artist = changes.artist,
                album = changes.album,
                lyrics = changes.lyrics,
                clearLyrics = changes.lyrics == "",
                coverBytes = coverBytes,
                clearCover = changes.coverUri == "",
            ),
        )
        return FileWriteResult(ok = result.ok, code = result.code, message = result.message)
    }

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
     * 写入音频文件标签（上收自 core/media TagWriter.write，逐字保留）。
     * 读文件失败 / 无可写标签 / 容器不支持均返回失败结果，不抛异常。
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
            if (e is CancellationException) throw e
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
     * Android 安全的 Artwork 实现（上收自 core/media TagWriter.AndroidSafeArtwork，逐字保留）：
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

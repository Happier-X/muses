package com.muses.player.core.media.scanner

import java.io.File
import org.jaudiotagger.audio.AudioFileIO

/** 下载替换只比较实际音频头；未知、不同有损编码或指标交叉升降均不自动替换。 */
data class DownloadAudioQuality(
    val lossless: Boolean,
    val bitDepth: Int,
    val sampleRate: Int,
    val bitrate: Long,
    val codec: String,
) {
    fun higherThan(other: DownloadAudioQuality): Boolean = when {
        lossless != other.lossless -> lossless
        lossless -> bitDepth > 0 && sampleRate > 0 && other.bitDepth > 0 && other.sampleRate > 0 &&
            bitDepth >= other.bitDepth && sampleRate >= other.sampleRate &&
            (bitDepth > other.bitDepth || sampleRate > other.sampleRate)
        else -> codec.isNotBlank() && codec == other.codec && bitrate > 0 && other.bitrate > 0 && bitrate > other.bitrate
    }

    companion object {
        fun read(file: File): DownloadAudioQuality? = try {
            val header = AudioFileIO.read(file).audioHeader
            val result = DownloadAudioQuality(header.isLossless,
                runCatching { header.bitsPerSample }.getOrDefault(0),
                runCatching { header.sampleRateAsNumber }.getOrDefault(0),
                runCatching { header.bitRateAsNumber }.getOrDefault(0),
                runCatching { header.encodingType.lowercase() }.getOrDefault(""))
            result.takeIf { if (it.lossless) it.bitDepth > 0 && it.sampleRate > 0 else it.bitrate > 0 }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException || e is InterruptedException) throw e
            null
        }
    }
}

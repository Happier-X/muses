package com.muses.player.core.media.scanner

import com.muses.player.core.model.classifyAudioQuality
import org.jaudiotagger.audio.AudioHeader

/** 不支持的音频头字段单独兜底，避免影响标题、歌词等标签读取。 */
fun AudioHeader?.qualityLabel(): String? = this?.let { header ->
    classifyAudioQuality(
        lossless = runCatching { header.isLossless }.getOrDefault(false),
        bitDepth = runCatching { header.bitsPerSample }.getOrDefault(0),
        sampleRate = runCatching { header.sampleRateAsNumber }.getOrDefault(0),
        bitrateKbps = runCatching { header.bitRateAsNumber }.getOrDefault(0),
    )
}

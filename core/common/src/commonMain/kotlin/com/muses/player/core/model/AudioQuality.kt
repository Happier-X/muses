package com.muses.player.core.model

/** 从音频头判断文件音质，不从文件名推断码率或位深。 */
fun classifyAudioQuality(lossless: Boolean, bitDepth: Int, sampleRate: Int, bitrateKbps: Long): String? = when {
    lossless && (bitDepth > 16 || sampleRate > 48_000) -> "Hi-Res"
    lossless -> "SQ"
    bitrateKbps >= 256 -> "HQ"
    bitrateKbps > 0 -> "标准"
    else -> null
}

/** 尚未读取音频头时只展示已知文件格式，不能据此标为高音质。 */
val Song.libraryQualityLabel: String?
    get() = audioQuality ?: path.substringBefore('?').substringBefore('#').substringAfterLast('.')
        .lowercase().takeIf { it in setOf("flac", "ape", "wav", "aiff", "aif", "mp3", "aac", "m4a", "ogg", "opus", "wma") }
        ?.uppercase()

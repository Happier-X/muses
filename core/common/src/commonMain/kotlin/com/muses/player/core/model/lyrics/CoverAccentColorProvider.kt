package com.muses.player.core.model.lyrics

/** 悬浮歌词复用播放器的封面强调色，不依赖播放页是否打开。 */
fun interface CoverAccentColorProvider {
    suspend fun colorFor(coverUri: String): Int?
}

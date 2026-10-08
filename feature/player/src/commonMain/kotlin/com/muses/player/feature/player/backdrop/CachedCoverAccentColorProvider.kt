package com.muses.player.feature.player.backdrop

import androidx.compose.ui.graphics.toArgb
import com.muses.player.core.model.lyrics.CoverAccentColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 小尺寸解码和提色在后台执行，避免歌词进度轮询重复读取封面。 */
internal class CachedCoverAccentColorProvider : CoverAccentColorProvider {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, Int>(24, 0.75f, true)

    override suspend fun colorFor(coverUri: String): Int? = mutex.withLock {
        if (cache.containsKey(coverUri)) return@withLock cache[coverUri]
        val decoded = withContext(Dispatchers.IO) { decodeCoverPixels(coverUri) }
        val color = withContext(Dispatchers.Default) { decoded?.let { coverContentColor(it).toArgb() } }
        if (color != null) cache[coverUri] = color
        if (cache.size > 20) cache.remove(cache.keys.first())
        color
    }
}

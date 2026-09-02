package com.muses.player.feature.player.lyric

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics

/**
 * 沉浸式歌词面板 — 自研原生版（替换 WebView）
 * 原先基于 accompanist lyrics-ui / LyricWebView 的实现已下线，改为 NativeLyricsPanel 手搓
 */
@Composable
fun LyricsPanel(
    syncedLyrics: SyncedLyrics?,
    positionProvider: () -> Int,
    translationEnabled: Boolean,
    hasTranslation: Boolean,
    onToggleTranslation: () -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    showPlayFab: Boolean,
    isTablet: Boolean,
    albumArtUri: String? = null,
    modifier: Modifier = Modifier,
    onLyricAtTopChange: (Boolean) -> Unit = {},
) {
    NativeLyricsPanel(
        syncedLyrics = syncedLyrics,
        positionProvider = positionProvider,
        translationEnabled = translationEnabled,
        hasTranslation = hasTranslation,
        onToggleTranslation = onToggleTranslation,
        isPlaying = isPlaying,
        onPlayPause = onPlayPause,
        onSeek = onSeek,
        showPlayFab = showPlayFab,
        isTablet = isTablet,
        albumArtUri = albumArtUri,
        onLyricAtTopChange = onLyricAtTopChange,
        modifier = modifier,
    )
}

/** 保留给五行小窗等自绘场景的当前行索引计算（卡拉OK 面板已由渲染器内部处理） */
internal fun computeCurrentIndex(lines: List<AmllLyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].startTime <= positionMs) idx = i else break
    }
    if (idx == -1) return 0
    val last = lines.last()
    if (positionMs > last.endTime) return lines.lastIndex
    return idx.coerceIn(0, lines.lastIndex)
}

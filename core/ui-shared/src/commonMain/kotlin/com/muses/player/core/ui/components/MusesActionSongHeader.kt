package com.muses.player.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 展示被操作的歌曲，与当前正在播放的歌曲无关。 */
data class MusesActionSongInfo(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val coverUri: String? = null,
)

@Composable
internal fun MusesActionSongHeader(song: MusesActionSongInfo) {
    val scheme = MiuixTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MusesCover(uri = song.coverUri, size = SongListLayout.coverSize, radius = MusesCoverRadius.SM)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SongListLayout.textGap)) {
            Text(song.title, style = MiuixTheme.textStyles.body1.copy(fontSize = SongListLayout.titleSize, lineHeight = SongListLayout.titleLineHeight), fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis, color = scheme.onBackground)
            Text(listOfNotNull(song.artist?.takeIf { it.isNotBlank() } ?: "未知歌手", song.album?.takeIf { it.isNotBlank() }).joinToString(" - "),
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = SongListLayout.subtitleSize, lineHeight = SongListLayout.subtitleLineHeight), color = scheme.onBackgroundVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

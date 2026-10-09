package com.muses.player.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.playback.RecentPlayEntry
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val recentPlaysRepository = koinInject<RecentPlaysRepository>()
    val songRepository = koinInject<SongRepository>()
    val playback = koinInject<PlaybackPort>()
    val history by remember(recentPlaysRepository) { recentPlaysRepository.observe() }
        .collectAsState(initial = emptyList())
    val historyGroups = remember(history) { history.groupBy { dayKey(it.playedAt) } }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "历史记录",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            MusesEmpty(
                title = "空空如也~",
                modifier = Modifier.fillMaxSize().padding(padding),
                bottomInset = LocalBottomChromePadding.current,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp + LocalBottomChromePadding.current),
            ) {
                historyGroups.forEach { (key, entries) ->
                    item(key = "day-$key") {
                        SmallTitle(text = "${formatDayLabel(key, entries.first().playedAt)} · ${entries.size} 首")
                    }
                    items(entries, key = RecentPlayEntry::songId) { entry ->
                        MusesListRow(
                            songLayout = true,
                            songId = entry.songId,
                            title = entry.title,
                            subtitle = "${entry.subtitle.ifBlank { "未知艺术家" }} · ${formatPlayedAt(entry.playedAt)}",
                            onClick = {
                                scope.launch {
                                    val songsById = songRepository.getSongs(history.map { it.songId })
                                    val song = songsById[entry.songId]
                                    if (song == null) {
                                        MusesSnackbar.show("歌曲已不在曲库中")
                                    } else {
                                        playback.play(song.id, history.mapNotNull { songsById[it.songId] })
                                    }
                                }
                            },
                            leading = {
                                MusesCover(
                                    uri = entry.coverUri,
                                    size = com.muses.player.core.ui.components.SongListLayout.coverSize,
                                    radius = com.muses.player.core.ui.components.MusesCoverRadius.SM,
                                    modifier = Modifier.padding(end = com.muses.player.core.ui.components.SongListLayout.coverGap),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

}

private fun formatPlayedAt(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun dayKey(timestamp: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))

private fun formatDayLabel(key: String, timestamp: Long): String {
    val today = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis)
    return when (key) {
        today -> "今天"
        yesterday -> "昨天"
        else -> SimpleDateFormat("M月d日 EEE", Locale.getDefault()).format(Date(timestamp))
    }
}

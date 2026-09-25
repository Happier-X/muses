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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.data.repository.RecentPlaysRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.model.playback.RecentPlayEntry
import com.muses.player.core.playback.PlaybackPort
import com.muses.player.core.ui.components.MusesCover
import com.muses.player.core.ui.components.MusesDialog
import com.muses.player.core.ui.components.MusesEmpty
import com.muses.player.core.ui.components.MusesIconButton
import com.muses.player.core.ui.components.MusesListRow
import com.muses.player.core.ui.components.MusesSnackbar
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.icons.TablerIcons
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val recentPlaysRepository = koinInject<RecentPlaysRepository>()
    val songRepository = koinInject<SongRepository>()
    val playback = koinInject<PlaybackPort>()
    val history by remember(recentPlaysRepository) { recentPlaysRepository.observe() }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MusesTopBar(
                title = "历史记录",
                onBack = onBack,
                actions = {
                    if (history.isNotEmpty()) {
                        MusesIconButton(
                            onClick = { confirmClear = true },
                            imageVector = TablerIcons.Delete,
                            contentDescription = "清空历史记录",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            MusesEmpty(
                title = "还没有播放记录",
                description = "播放歌曲后会显示在这里",
                icon = TablerIcons.MusicNote,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp + LocalBottomChromePadding.current),
            ) {
                items(history, key = RecentPlayEntry::songId) { entry ->
                    MusesListRow(
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
                                size = 48.dp,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        MusesDialog(
            onDismiss = { confirmClear = false },
            title = "清空历史记录",
            message = "确定清空最近播放的歌曲记录吗？",
            dismissText = "取消",
            confirmText = "清空",
            destructiveConfirm = true,
            onConfirm = {
                confirmClear = false
                scope.launch {
                    recentPlaysRepository.clear()
                }
            },
        )
    }
}

private fun formatPlayedAt(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

package com.muses.player.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.data.repository.AlbumRepository
import com.muses.player.core.data.repository.ArtistRepository
import com.muses.player.core.data.repository.SongRepository
import com.muses.player.core.data.repository.SourceRepository
import com.muses.player.core.model.SourceType
import com.muses.player.core.ui.components.MusesTopBar
import com.muses.player.core.ui.components.SettingsBlockTitle
import com.muses.player.core.ui.theme.LocalBottomChromePadding
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val songRepository = koinInject<SongRepository>()
    val albumRepository = koinInject<AlbumRepository>()
    val artistRepository = koinInject<ArtistRepository>()
    val sourceRepository = koinInject<SourceRepository>()
    val songs by remember(songRepository) { songRepository.observeSongs() }.collectAsState(initial = emptyList())
    val albums by remember(albumRepository) { albumRepository.observeAlbums() }.collectAsState(initial = emptyList())
    val artists by remember(artistRepository) { artistRepository.observeArtists() }.collectAsState(initial = emptyList())
    val sources by remember(sourceRepository) { sourceRepository.observeSources() }.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MusesTopBar(title = "统计", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(top = 8.dp),
        ) {
            SettingsBlockTitle("曲库概览")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                StatisticRow("歌曲", songs.size)
                StatisticRow("专辑", albums.size)
                StatisticRow("艺术家", artists.size)
                StatisticRow("音源", sources.size)
            }
            SettingsBlockTitle("歌曲来源")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                StatisticRow("本地", songs.count { it.sourceType == SourceType.LOCAL })
                StatisticRow("WebDAV", songs.count { it.sourceType == SourceType.WEBDAV })
                StatisticRow("在线", songs.count { it.sourceType == SourceType.ONLINE })
            }
            Spacer(Modifier.height(16.dp + LocalBottomChromePadding.current))
        }
    }
}

@Composable
private fun StatisticRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = MiuixTheme.colorScheme.onBackground)
        Text(text = count.toString(), color = MiuixTheme.colorScheme.onBackgroundVariant)
    }
}

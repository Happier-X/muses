package com.muses.player.feature.scrape

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.scrape.editmeta.CloudLyricsPlatformId
import com.muses.player.core.scrape.editmeta.CloudPlatformId
import com.muses.player.core.ui.components.MusesBottomSheet
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchSourcesFields(viewModel: ScrapeReviewViewModel) {
    val sources by viewModel.searchSources.collectAsState()
    var diagnosing by remember { mutableStateOf(false) }
    var choosingMetadata by remember { mutableStateOf(false) }
    var choosingLyrics by remember { mutableStateOf(false) }
    var configuringLrcApi by remember { mutableStateOf(false) }
    val baseUrl by viewModel.lrcApiBaseUrl.collectAsState(initial = com.muses.player.core.scrape.international.DEFAULT_LRC_API_URL)
    Column {
        BasicComponent(title = "歌曲信息与封面来源", summary = metadataSourceLabel(sources.metadata),
            onClick = { choosingMetadata = true })
        BasicComponent(title = "歌词来源", summary = lyricsSourceLabel(sources.lyrics),
            onClick = { choosingLyrics = true })
        BasicComponent(title = "来源诊断", summary = "按当前搜索词检查各来源的结果和耗时", onClick = { diagnosing = true })
        BasicComponent(title = "LrcApi 服务地址", summary = baseUrl, onClick = { configuringLrcApi = true })
        Text("歌词同时查询 AMLL 歌词库；修改来源后点击重新搜索生效",
            style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onBackgroundVariant)
    }
    if (diagnosing) SourceDiagnosticsSheet(viewModel, onDismiss = { diagnosing = false })
    if (configuringLrcApi) {
        var address by remember(baseUrl) { mutableStateOf(baseUrl) }
        MusesBottomSheet(title = "LrcApi 服务地址", onDismiss = { configuringLrcApi = false }) {
            Column {
                Text("默认使用官方公开服务；也可填写无需鉴权的自托管服务地址。留空恢复默认。",
                    style = MiuixTheme.textStyles.footnote2)
                com.muses.player.core.ui.components.MusesTextField(value = address, onValueChange = { address = it }, label = "服务地址")
                ScrapeTextAction(text = "保存地址", onClick = {
                    viewModel.saveLrcApiBaseUrl(address)
                    configuringLrcApi = false
                })
            }
        }
    }
    if (choosingMetadata) {
        SearchSourceSheet("选择歌曲信息与封面来源", CloudPlatformId.entries, sources.metadata,
            ::metadataSourceLabel, onDismiss = { choosingMetadata = false }) {
            viewModel.updateMetadataSource(it)
            choosingMetadata = false
        }
    }
    if (choosingLyrics) {
        SearchSourceSheet("选择歌词来源", CloudLyricsPlatformId.entries, sources.lyrics,
            ::lyricsSourceLabel, onDismiss = { choosingLyrics = false }) {
            viewModel.updateLyricsSource(it)
            choosingLyrics = false
        }
    }
}

@Composable
private fun <T> SearchSourceSheet(
    title: String,
    sources: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    MusesBottomSheet(title = title, onDismiss = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            items(sources) { source ->
                BasicComponent(title = label(source),
                    endActions = { if (source == selected) Text("已选", color = MiuixTheme.colorScheme.primary) },
                    onClick = { onSelect(source) })
            }
        }
    }
}

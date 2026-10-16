package com.muses.player.feature.scrape

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muses.player.core.scrape.editmeta.*
import com.muses.player.core.ui.components.MusesBottomSheet
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Text

data class SourceDiagnosticsState(
    val query: EditCloudMetaQuery? = null,
    val sources: ScrapeSearchSources = ScrapeSearchSources(),
    val running: Boolean = false,
    val targets: List<SourceDiagnosticTarget> = emptyList(),
    val rows: List<SourceDiagnosticResult> = emptyList(),
)

@Composable
internal fun SourceDiagnosticsSheet(vm: ScrapeReviewViewModel, onDismiss: () -> Unit) {
    androidx.compose.runtime.DisposableEffect(vm) { onDispose { vm.stopSourceDiagnostics() } }
    val state by vm.diagnostics.collectAsState()
    val keyword by vm.keyword.collectAsState()
    val sources by vm.searchSources.collectAsState()
    MusesBottomSheet(title = "来源诊断", onDismiss = { vm.stopSourceDiagnostics(); onDismiss() }) {
        LazyColumn(Modifier.heightIn(max = 560.dp)) {
            item {
                Column {
                    Text("按当前搜索词和来源逐项查询，不改审核结果、曲库或音频文件。未找到候选不代表来源故障。")
                    Text("当前：${keyword.title} · ${keyword.artist} · ${keyword.album}")
                    Text("${metadataSourceLabel(sources.metadata)} / ${lyricsSourceLabel(sources.lyrics)}；歌词包含 AMLL")
                    Text("AMLL 可复用已加载的索引；命中时重新请求歌词原文。封面仅检查响应头与图片签名。")
                    ScrapeTextAction(text = if (state.running) "停止诊断" else "开始诊断", onClick = {
                        if (state.running) vm.stopSourceDiagnostics() else vm.startSourceDiagnostics()
                    }, enabled = state.running || !keyword.titleBlank)
                    state.query?.let { query ->
                        Text("本次：${query.title} · ${query.artist.orEmpty()} · ${query.album.orEmpty()}")
                        Text("${metadataSourceLabel(state.sources.metadata)} / ${lyricsSourceLabel(state.sources.lyrics)}")
                        Text("${if (state.running) "诊断中" else "已停止或完成"} ${state.rows.size}/${state.targets.size}")
                    }
                }
            }
            items(state.targets) { target ->
                val row = state.rows.firstOrNull { it.target == target }
                val dimension = when (target.dimension) {
                    EditDimKey.TEXT -> "歌曲信息"
                    EditDimKey.COVER -> "封面"
                    EditDimKey.LYRICS -> "歌词"
                }
                BasicComponent(title = "${scrapeSourceLabel(target.source)} · $dimension", summary = row?.let(::diagnosticSummary)
                    ?: if (state.running) "等待结果" else "未执行")
            }
        }
    }
}

internal fun diagnosticSummary(row: SourceDiagnosticResult): String {
    val message = when (row.status) {
        SourceDiagnosticStatus.OK -> if (row.imageChecked) "图片头检查通过" else "成功，${row.candidateCount} 个候选"
        SourceDiagnosticStatus.NO_MATCH -> "未找到候选"
        SourceDiagnosticStatus.UNVERIFIED -> "已取得地址，图片尚未验证"
        SourceDiagnosticStatus.FAILED -> (if (row.imageChecked) "图片检查失败：" else "") +
            (row.failure?.kind?.message ?: "请求失败，请稍后重试") +
            (row.failure?.httpStatus?.let { "（HTTP $it）" } ?: "")
    }
    return "$message · ${row.elapsedMs} 毫秒"
}

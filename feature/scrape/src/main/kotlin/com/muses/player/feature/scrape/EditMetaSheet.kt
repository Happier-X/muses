package com.muses.player.feature.scrape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.muses.player.core.model.Song
import com.muses.player.core.scrape.editmeta.EditCloudMetaResult
import com.muses.player.core.scrape.editmeta.EditDimStatus

/**
 * 编辑歌曲信息弹窗 —— editmeta 三维云搜（文本/封面/歌词）+ 应用写回。
 * 入口 = 播放页原生「更多」键回调；宿主在 MusesApp 层。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMetaSheet(
    song: Song?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditMetaViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    // song 变化时载入初始字段
    androidx.compose.runtime.LaunchedEffect(song?.id) {
        if (song != null) viewModel.load(song)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("编辑歌曲信息", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = ui.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.artist,
                onValueChange = viewModel::updateArtist,
                label = { Text("艺术家") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.album,
                onValueChange = viewModel::updateAlbum,
                label = { Text("专辑") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (ui.searchFailed && ui.result == null) {
                Text(
                    "云端搜索失败，请检查网络后重试",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }

            // 云搜按钮 / 进行中 / 结果封面候选
            when {
                ui.searching -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(Modifier.height(20.dp))
                    Text("云端搜索中…")
                }
                ui.result != null -> {
                    Text("封面候选", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    val covers = ui.result!!.cover
                    if (covers.status == EditDimStatus.OK && covers.items.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(covers.items.size) { idx ->
                                val candidate = covers.items[idx]
                                androidx.compose.material3.Surface(
                                    onClick = { viewModel.selectCover(idx) },
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    AsyncImage(
                                        model = candidate.remoteUrl,
                                        contentDescription = "封面候选 ${idx + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    } else {
                        Text("未找到可用封面", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                    if (ui.result!!.lyrics.status == EditDimStatus.OK) {
                        Text(
                            "歌词已找到 ${ui.result!!.lyrics.items.size} 条候选（应用后写入）",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text("歌词聚合未接入或无结果，已跳过歌词维度", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.search() },
                    enabled = !ui.searching && song != null,
                    modifier = Modifier.weight(1f),
                ) { Text(if (ui.result == null) "云端搜索" else "重新搜索") }
                Button(
                    onClick = { viewModel.apply(); onDismiss() },
                    enabled = !ui.searching && song != null,
                    modifier = Modifier.weight(1f),
                ) { Text("应用并写回") }
            }
        }
    }
}
